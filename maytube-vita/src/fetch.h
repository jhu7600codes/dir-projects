#ifndef MAYTUBE_FETCH_H
#define MAYTUBE_FETCH_H

#include "sabr.h"

/* Buffer-then-play, not true live streaming (unlike the Android client's
   StreamingPlayer): fetches every SABR fragment for a video into two
   local files (video-only, audio-only -- SABR delivers real fragmented
   MP4, so no muxing step is needed, same reasoning as the Android side)
   before playback starts at all, mirroring the *original*, simpler
   PlayerActivity this project's Android side itself started with before
   StreamingPlayer's incremental-growing-file approach replaced it there.
   True live streaming here is a real further improvement (see the
   project README's "Known limitations"), just not one attempted in this
   pass: it would need ffmpeg fed via a custom AVIOContext that blocks for
   more bytes while a background thread is still writing them, plus
   manual dual-track A/V sync since there's no MergingMediaSource
   equivalent to lean on outside of ExoPlayer.

   Serial fetches (not StreamingPlayer's concurrency=3), for the same
   reason: nothing here could be verified against a real device's actual
   network conditions, and correctness matters more than a faster
   download loop that was never tested. */

typedef struct {
    long fetched_ms;
    long total_ms; /* -1 if unknown */
} fetch_progress;

/* Fetches every fragment for video_id at 5-second steps (same stepMs
   StreamingPlayer.kt uses) into video_path/audio_path, calling
   on_progress after each step if non-NULL. itag: -1 for server default,
   otherwise pins a specific quality the same way user_video_itag does
   server-side. Returns 0 on success, -1 on failure. */
int fetch_video(const char *base_url, const char *video_id, int itag,
                 const char *video_path, const char *audio_path,
                 void (*on_progress)(fetch_progress progress, void *userdata),
                 void *userdata);

/* Pure decision logic factored out of fetch_video so it's host-testable
   without a network: given what's known so far, should the fetch loop
   continue to the next offset, or has it reached the end? Mirrors
   StreamingPlayer.kt's fetchAllFragments reachedEnd computation exactly
   (known duration: stop once next_offset_ms reaches it; unknown duration:
   stop once a step returns no data at all). */
int fetch_should_continue(long next_offset_ms, long total_ms, int last_step_had_data);

#endif
