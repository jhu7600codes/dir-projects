#include "fetch.h"
#include "http.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define STEP_MS 5000L
#define HARD_CAP_MS (6L * 60 * 60 * 1000)
#define MAX_ATTEMPTS 3

int fetch_should_continue(long next_offset_ms, long total_ms, int last_step_had_data) {
    if (next_offset_ms >= HARD_CAP_MS) return 0;
    if (total_ms >= 0) return next_offset_ms < total_ms;
    return last_step_had_data;
}

int fetch_video(const char *base_url, const char *video_id, int itag,
                 const char *video_path, const char *audio_path,
                 void (*on_progress)(fetch_progress progress, void *userdata),
                 void *userdata) {
    sabr_session session;
    int rc = sabr_resolve(base_url, video_id, &session);
    if (rc != 0) return -1;
    if (session.is_live) return -1; /* not supported, same as Android */

    FILE *video_out = fopen(video_path, "wb");
    FILE *audio_out = fopen(audio_path, "wb");
    if (!video_out || !audio_out) {
        if (video_out) fclose(video_out);
        if (audio_out) fclose(audio_out);
        return -1;
    }

    long next_offset = 0;
    long fetched_ms = 0;
    int keep_going = 1;
    sabr_part parts[16];

    while (keep_going) {
        int had_data = 0;
        int got_any_response = 0;

        for (int attempt = 0; attempt < MAX_ATTEMPTS && !got_any_response; attempt++) {
            char url[900];
            if (itag >= 0) {
                snprintf(url, sizeof(url), "%s%s&offset=%ld&hd=1%s&user_video_itag=%d",
                         base_url, session.sabr_path, next_offset,
                         attempt > 0 ? "&force_replayer=1" : "", itag);
            } else {
                snprintf(url, sizeof(url), "%s%s&offset=%ld&hd=1%s",
                         base_url, session.sabr_path, next_offset,
                         attempt > 0 ? "&force_replayer=1" : "");
            }

            http_response resp;
            if (http_get(url, SABR_COOKIE, &resp) != 0) continue;
            if (resp.status < 200 || resp.status >= 300) {
                http_response_free(&resp);
                continue;
            }

            int n = sabr_parse_parts((const unsigned char *)resp.data, resp.size,
                                      (int)resp.x_part_count, parts);
            if (n < 0) {
                http_response_free(&resp);
                continue;
            }

            got_any_response = 1;
            if (n > 0) had_data = 1;
            for (int i = 0; i < n; i++) {
                FILE *dst = parts[i].is_audio ? audio_out : video_out;
                fwrite(parts[i].data, 1, parts[i].length, dst);
            }
            fflush(video_out);
            fflush(audio_out);
            http_response_free(&resp);
        }

        if (!got_any_response) {
            fclose(video_out);
            fclose(audio_out);
            return -1;
        }

        fetched_ms = next_offset + STEP_MS;
        if (on_progress) {
            fetch_progress p = { fetched_ms, session.total_ms };
            on_progress(p, userdata);
        }

        next_offset += STEP_MS;
        keep_going = fetch_should_continue(next_offset, session.total_ms, had_data);
    }

    fclose(video_out);
    fclose(audio_out);
    return 0;
}
