#include "player.h"

#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
#include <libavutil/imgutils.h>
#include <libavutil/channel_layout.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* Output audio format the ring buffer and SDL device both agree on. */
#define OUT_SAMPLE_RATE 48000
#define OUT_CHANNELS 2
#define BYTES_PER_SEC (OUT_SAMPLE_RATE * OUT_CHANNELS * 2 /* s16 */)

/* ~1s of audio -- enough slack that the decode-side refill (done once per
   main-loop iteration, not on a separate thread) comfortably stays ahead
   of the SDL callback pulling from the other end. */
#define RING_CAPACITY (BYTES_PER_SEC)

/* Frames further than this behind the audio clock are dropped instead of
   shown, so a slow decode (this is a homebrew build on Vita hardware, not
   a desktop) catches back up instead of drifting the whole rest of
   playback further and further out of sync. */
#define DROP_THRESHOLD_SEC 0.5
#define MAX_SLEEP_MS 5

typedef struct {
    AVFormatContext *fmt;
    AVCodecContext *dec;
    int stream_idx;
} decoder;

typedef struct {
    uint8_t buf[RING_CAPACITY];
    size_t read_pos;
    size_t write_pos;
    size_t filled; /* bytes currently held */
    int eof;       /* no more audio will ever be pushed */
    SDL_mutex *lock;
} audio_ring;

typedef struct {
    audio_ring *ring;
    double clock_sec; /* how much has actually been handed to the audio HW */
} audio_cb_state;

static void ring_push(audio_ring *ring, const uint8_t *data, size_t len) {
    SDL_LockMutex(ring->lock);
    for (size_t i = 0; i < len; i++) {
        if (ring->filled >= RING_CAPACITY) break; /* drop tail; shouldn't happen, we only refill when low */
        ring->buf[ring->write_pos] = data[i];
        ring->write_pos = (ring->write_pos + 1) % RING_CAPACITY;
        ring->filled++;
    }
    SDL_UnlockMutex(ring->lock);
}

static void sdl_audio_callback(void *userdata, Uint8 *stream, int len) {
    audio_cb_state *st = (audio_cb_state *)userdata;
    audio_ring *ring = st->ring;
    SDL_LockMutex(ring->lock);
    size_t avail = ring->filled;
    size_t take = (size_t)len < avail ? (size_t)len : avail;
    for (size_t i = 0; i < take; i++) {
        stream[i] = ring->buf[ring->read_pos];
        ring->read_pos = (ring->read_pos + 1) % RING_CAPACITY;
    }
    ring->filled -= take;
    SDL_UnlockMutex(ring->lock);
    if (take < (size_t)len) {
        /* underrun: pad with silence rather than replaying stale bytes */
        memset(stream + take, 0, (size_t)len - take);
    }
    st->clock_sec += (double)len / BYTES_PER_SEC;
}

static int open_decoder(const char *path, enum AVMediaType type, decoder *d) {
    memset(d, 0, sizeof(*d));
    if (avformat_open_input(&d->fmt, path, NULL, NULL) < 0) return -1;
    if (avformat_find_stream_info(d->fmt, NULL) < 0) return -1;

    const AVCodec *codec = NULL;
    int idx = av_find_best_stream(d->fmt, type, -1, -1, &codec, 0);
    if (idx < 0 || !codec) return -1;
    d->stream_idx = idx;

    d->dec = avcodec_alloc_context3(codec);
    if (!d->dec) return -1;
    if (avcodec_parameters_to_context(d->dec, d->fmt->streams[idx]->codecpar) < 0) return -1;
    if (avcodec_open2(d->dec, codec, NULL) < 0) return -1;
    return 0;
}

static void close_decoder(decoder *d) {
    if (d->dec) avcodec_free_context(&d->dec);
    if (d->fmt) avformat_close_input(&d->fmt);
}

/* Pulls and decodes audio packets until the ring buffer is comfortably
   full or the stream is exhausted. Resamples whatever the source format
   is to s16/48kHz/stereo so the SDL device (opened once, up front, with
   that fixed spec) never has to be reconfigured mid-playback. */
static void refill_audio(decoder *ad, audio_ring *ring, SwrContext *swr,
                          AVPacket *pkt, AVFrame *frame) {
    while (!ring->eof && ring->filled < RING_CAPACITY - RING_CAPACITY / 4) {
        int rc = av_read_frame(ad->fmt, pkt);
        if (rc < 0) {
            /* flush the decoder, then mark EOF */
            avcodec_send_packet(ad->dec, NULL);
            for (;;) {
                int rr = avcodec_receive_frame(ad->dec, frame);
                if (rr < 0) break;
                uint8_t *out = NULL;
                int out_linesize;
                int out_samples = av_rescale_rnd(
                    swr_get_delay(swr, frame->sample_rate) + frame->nb_samples,
                    OUT_SAMPLE_RATE, frame->sample_rate, AV_ROUND_UP);
                av_samples_alloc(&out, &out_linesize, OUT_CHANNELS, out_samples, AV_SAMPLE_FMT_S16, 0);
                int converted = swr_convert(swr, &out, out_samples,
                                             (const uint8_t **)frame->data, frame->nb_samples);
                if (converted > 0) ring_push(ring, out, (size_t)converted * OUT_CHANNELS * 2);
                av_freep(&out);
                av_frame_unref(frame);
            }
            ring->eof = 1;
            break;
        }
        if (pkt->stream_index != ad->stream_idx) { av_packet_unref(pkt); continue; }
        if (avcodec_send_packet(ad->dec, pkt) == 0) {
            for (;;) {
                int rr = avcodec_receive_frame(ad->dec, frame);
                if (rr < 0) break;
                uint8_t *out = NULL;
                int out_linesize;
                int out_samples = av_rescale_rnd(
                    swr_get_delay(swr, frame->sample_rate) + frame->nb_samples,
                    OUT_SAMPLE_RATE, frame->sample_rate, AV_ROUND_UP);
                av_samples_alloc(&out, &out_linesize, OUT_CHANNELS, out_samples, AV_SAMPLE_FMT_S16, 0);
                int converted = swr_convert(swr, &out, out_samples,
                                             (const uint8_t **)frame->data, frame->nb_samples);
                if (converted > 0) ring_push(ring, out, (size_t)converted * OUT_CHANNELS * 2);
                av_freep(&out);
                av_frame_unref(frame);
            }
        }
        av_packet_unref(pkt);
    }
}

int player_play(SDL_Renderer *renderer,
                 const char *video_path, const char *audio_path,
                 int (*should_stop)(void *userdata), void *userdata) {
    decoder vd, ad;
    int have_video = (open_decoder(video_path, AVMEDIA_TYPE_VIDEO, &vd) == 0);
    int have_audio = (open_decoder(audio_path, AVMEDIA_TYPE_AUDIO, &ad) == 0);
    if (!have_video && !have_audio) return -1;

    SwrContext *swr = NULL;
    audio_ring ring;
    memset(&ring, 0, sizeof(ring));
    ring.lock = SDL_CreateMutex();
    audio_cb_state cb_state = { &ring, 0.0 };
    SDL_AudioDeviceID audio_dev = 0;

    if (have_audio) {
        swr = swr_alloc();
#if LIBAVUTIL_VERSION_MAJOR >= 57
        AVChannelLayout out_layout;
        av_channel_layout_default(&out_layout, OUT_CHANNELS);
        av_opt_set_chlayout(swr, "in_chlayout", &ad.dec->ch_layout, 0);
        av_opt_set_chlayout(swr, "out_chlayout", &out_layout, 0);
        av_channel_layout_uninit(&out_layout);
#else
        av_opt_set_channel_layout(swr, "in_channel_layout",
                                   ad.dec->channel_layout ? (int64_t)ad.dec->channel_layout
                                                           : av_get_default_channel_layout(ad.dec->channels), 0);
        av_opt_set_channel_layout(swr, "out_channel_layout", AV_CH_LAYOUT_STEREO, 0);
#endif
        av_opt_set_int(swr, "in_sample_rate", ad.dec->sample_rate, 0);
        av_opt_set_int(swr, "out_sample_rate", OUT_SAMPLE_RATE, 0);
        av_opt_set_sample_fmt(swr, "in_sample_fmt", ad.dec->sample_fmt, 0);
        av_opt_set_sample_fmt(swr, "out_sample_fmt", AV_SAMPLE_FMT_S16, 0);
        if (swr_init(swr) < 0) { have_audio = 0; }
    }

    if (have_audio) {
        SDL_AudioSpec want, got;
        memset(&want, 0, sizeof(want));
        want.freq = OUT_SAMPLE_RATE;
        want.format = AUDIO_S16SYS;
        want.channels = OUT_CHANNELS;
        want.samples = 4096;
        want.callback = sdl_audio_callback;
        want.userdata = &cb_state;
        audio_dev = SDL_OpenAudioDevice(NULL, 0, &want, &got, 0);
        if (audio_dev == 0) have_audio = 0;
        else SDL_PauseAudioDevice(audio_dev, 0);
    }

    struct SwsContext *sws = NULL;
    SDL_Texture *tex = NULL;
    int tex_w = 0, tex_h = 0;
    enum AVPixelFormat tex_src_fmt = AV_PIX_FMT_NONE;

    AVPacket *pkt = av_packet_alloc();
    AVFrame *aframe = av_frame_alloc();
    AVFrame *vframe = av_frame_alloc();
    AVFrame *vframe_conv = av_frame_alloc();
    int video_eof = !have_video;
    int pending_video = 0; /* vframe holds a decoded, not-yet-shown frame */
    int stopped = 0;

    while (1) {
        if (should_stop && should_stop(userdata)) { stopped = 1; break; }

        if (have_audio) refill_audio(&ad, &ring, swr, pkt, aframe);

        if (have_video && !pending_video && !video_eof) {
            int got = 0;
            while (!got) {
                int rr = avcodec_receive_frame(vd.dec, vframe);
                if (rr == 0) { got = 1; break; }
                int rc = av_read_frame(vd.fmt, pkt);
                if (rc < 0) {
                    avcodec_send_packet(vd.dec, NULL);
                    int rr2 = avcodec_receive_frame(vd.dec, vframe);
                    if (rr2 == 0) { got = 1; }
                    else { video_eof = 1; }
                    break;
                }
                if (pkt->stream_index == vd.stream_idx) avcodec_send_packet(vd.dec, pkt);
                av_packet_unref(pkt);
            }
            if (got) pending_video = 1;
        }

        double audio_clock = have_audio ? cb_state.clock_sec : 1e18 /* no audio: never wait on it */;

        if (have_video && pending_video) {
            double pts = vframe->best_effort_timestamp != AV_NOPTS_VALUE
                             ? vframe->best_effort_timestamp * av_q2d(vd.fmt->streams[vd.stream_idx]->time_base)
                             : audio_clock;

            if (!have_audio || pts <= audio_clock) {
                if (pts >= audio_clock - DROP_THRESHOLD_SEC || !have_audio) {
                    if (!tex || tex_w != vframe->width || tex_h != vframe->height || tex_src_fmt != vframe->format) {
                        if (tex) SDL_DestroyTexture(tex);
                        tex = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_IYUV, SDL_TEXTUREACCESS_STREAMING,
                                                 vframe->width, vframe->height);
                        tex_w = vframe->width; tex_h = vframe->height; tex_src_fmt = vframe->format;
                    }

                    AVFrame *src = vframe;
                    if (vframe->format != AV_PIX_FMT_YUV420P && vframe->format != AV_PIX_FMT_YUVJ420P) {
                        if (!sws) {
                            sws = sws_getContext(vframe->width, vframe->height, vframe->format,
                                                  vframe->width, vframe->height, AV_PIX_FMT_YUV420P,
                                                  SWS_BILINEAR, NULL, NULL, NULL);
                            av_image_alloc(vframe_conv->data, vframe_conv->linesize,
                                           vframe->width, vframe->height, AV_PIX_FMT_YUV420P, 1);
                        }
                        sws_scale(sws, (const uint8_t *const *)vframe->data, vframe->linesize,
                                  0, vframe->height, vframe_conv->data, vframe_conv->linesize);
                        src = vframe_conv;
                    }

                    SDL_UpdateYUVTexture(tex, NULL,
                                          src->data[0], src->linesize[0],
                                          src->data[1], src->linesize[1],
                                          src->data[2], src->linesize[2]);
                    SDL_RenderClear(renderer);
                    SDL_RenderCopy(renderer, tex, NULL, NULL);
                    SDL_RenderPresent(renderer);
                }
                av_frame_unref(vframe);
                pending_video = 0;
            }
        } else if (!have_video) {
            /* audio-only fallback: still pump the renderer so the app
               doesn't look frozen, and pace the loop off wall time since
               there's no video frame to gate on. */
            SDL_RenderClear(renderer);
            SDL_RenderPresent(renderer);
            SDL_Delay(MAX_SLEEP_MS);
        }

        if (have_video && pending_video) {
            /* next frame isn't due yet: don't busy-spin */
            SDL_Delay(1);
        }

        int audio_done = !have_audio || (ring.eof && ring.filled == 0);
        int video_done = !have_video || (video_eof && !pending_video);
        if (audio_done && video_done) break;
    }

    if (tex) SDL_DestroyTexture(tex);
    if (sws) sws_freeContext(sws);
    if (vframe_conv->data[0]) av_freep(&vframe_conv->data[0]);
    av_frame_free(&vframe_conv);
    av_frame_free(&vframe);
    av_frame_free(&aframe);
    av_packet_free(&pkt);
    if (audio_dev) SDL_CloseAudioDevice(audio_dev);
    if (swr) swr_free(&swr);
    SDL_DestroyMutex(ring.lock);
    if (have_video) close_decoder(&vd);
    if (have_audio) close_decoder(&ad);

    return stopped ? 0 : 0;
}
