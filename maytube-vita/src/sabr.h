#ifndef MAYTUBE_SABR_H
#define MAYTUBE_SABR_H

#include <stddef.h>

/* Faithful C port of maytube's Android SABR pipeline
   (app/src/main/java/com/maytube/app/download/SabrSession.kt,
   SabrFragmentParser.kt, SabrFragmentFetcher.kt) -- see those files for the
   full "why", this only restates the wire format actually needed to
   reimplement it. */

typedef struct {
    /* "/sabr_playback?pid=..." -- relative, appended straight to base_url */
    char sabr_path[512];
    long total_ms;   /* -1 if unknown (SabrSession.Session.totalMs == null) */
    int is_live;     /* initAsLive() seen -> not supported, same as Android */
} sabr_session;

/* Resolves the SABR session from a video's watch page (back/yt2009html.js
   embeds `var sabrBase = "/sabr_playback?pid=...";` inline, the same
   thing html5-player.js itself reads client-side). base_url has no
   trailing slash, e.g. "http://192.168.1.50:3000".
   Returns 0 on success, -1 on failure (network) or -2 (no sabrBase found
   -- SabrSession.ResolveException's case). */
int sabr_resolve(const char *base_url, const char *video_id, sabr_session *out);

/* One SABR fragment part, after SabrFragmentParser.parse() (SPART framing
   already stripped, points at raw fragment bytes owned by the response
   buffer this was parsed from -- copy before the response is freed). */
typedef struct {
    int itag;
    int chunk_number;
    int is_audio;
    const unsigned char *data;
    size_t length;
} sabr_part;

/* Parses one /sabr_playback response body (the "SABER-START///" + repeated
   `//SPART-"<itag>-<chunk>"-CL=<len>//<bytes>` framing --
   SabrFragmentParser.kt's exact format). expected_part_count comes from
   the response's x-part-count header. Fills `out_parts` (caller-allocated,
   at least expected_part_count entries) and returns the number of parts
   actually parsed, or -1 on a malformed response (mirrors
   MalformedResponseException). */
int sabr_parse_parts(const unsigned char *body, size_t body_len,
                      int expected_part_count, sabr_part *out_parts);

/* Cookie value SabrSession.cookieHeader always includes -- exp_sabr forced
   on for this request regardless of any live-playback WebView setting,
   since downloading/native playback is an independent choice there too. */
extern const char *SABR_COOKIE;

/* [H:]M:SS -> milliseconds, or -1 if unparseable. Exposed (not static) so
   sabr_test.c can check it directly -- see sabr.c's kdoc comment on it. */
long sabr_parse_clock(const char *clock);

#endif
