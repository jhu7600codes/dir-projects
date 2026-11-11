#include "sabr.h"
#include "http.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

const char *SABR_COOKIE = "maytube_dl_flags=exp_sabr";

/* newlib (vitasdk's libc) doesn't ship memmem -- bounded substring search
   over raw bytes, used only within the small (<=70 byte) header peek
   window, never over the binary fragment payload itself (mirrors
   SabrFragmentParser.kt's own "peek a bounded window" comment). */
static const unsigned char *bytes_find(const unsigned char *haystack, size_t hlen,
                                        const char *needle, size_t start) {
    size_t nlen = strlen(needle);
    if (nlen == 0 || start + nlen > hlen) return NULL;
    for (size_t i = start; i + nlen <= hlen; i++) {
        if (memcmp(haystack + i, needle, nlen) == 0) return haystack + i;
    }
    return NULL;
}

static const char *str_find(const char *haystack, const char *needle) {
    return strstr(haystack, needle);
}

/* "0:00 / <clock>" where clock is [H:]M:SS -- SabrSession.kt's parseClock,
   same left-to-right base-60 accumulation. Returns -1 if nothing usable.
   Not static: sabr_test.c links against it directly to check it in
   isolation, the same way MobileInjectorTest asserts on generated script
   text rather than trusting the CSS "looks right" by eye. */
long sabr_parse_clock(const char *clock) {
    long seconds = 0;
    int had_any = 0;
    const char *p = clock;
    while (*p) {
        char *end;
        long part = strtol(p, &end, 10);
        if (end == p) break;
        seconds = seconds * 60 + part;
        had_any = 1;
        p = end;
        if (*p == ':') p++;
        else break;
    }
    return had_any ? seconds * 1000 : -1;
}

int sabr_resolve(const char *base_url, const char *video_id, sabr_session *out) {
    char url[768];
    snprintf(url, sizeof(url), "%s/watch?v=%s", base_url, video_id);

    http_response resp;
    if (http_get(url, SABR_COOKIE, &resp) != 0) {
        return -1;
    }
    if (resp.status < 200 || resp.status >= 300) {
        http_response_free(&resp);
        return -1;
    }

    memset(out, 0, sizeof(*out));
    out->total_ms = -1;

    /* var sabrBase = "/sabr_playback?pid=...."; */
    const char *marker = str_find(resp.data, "var sabrBase = \"");
    if (!marker) {
        http_response_free(&resp);
        return -2;
    }
    marker += strlen("var sabrBase = \"");
    const char *end = strchr(marker, '"');
    if (!end || (size_t)(end - marker) >= sizeof(out->sabr_path)) {
        http_response_free(&resp);
        return -2;
    }
    memcpy(out->sabr_path, marker, end - marker);
    out->sabr_path[end - marker] = '\0';

    out->is_live = str_find(resp.data, "initAsLive()") != NULL;

    const char *clock_marker = str_find(resp.data, "0:00");
    if (clock_marker) {
        /* skip "0:00", then whitespace, a slash, then more whitespace,
           before the clock digits -- same shape as SabrSession.kt's
           regex for this (0:00, whitespace, a slash, whitespace, then
           one or more digit groups separated by colons). Written out in
           prose here rather than as a literal regex fragment, which
           would otherwise close this very comment block early (it
           contains its own "star slash"). */
        const char *p = clock_marker + 4;
        while (*p == ' ') p++;
        if (*p == '/') {
            p++;
            while (*p == ' ') p++;
            if (isdigit((unsigned char)*p)) {
                long ms = sabr_parse_clock(p);
                if (ms >= 0) out->total_ms = ms;
            }
        }
    }

    http_response_free(&resp);
    return 0;
}

int sabr_parse_parts(const unsigned char *body, size_t body_len,
                      int expected_part_count, sabr_part *out_parts) {
    static const char HEADER_START[] = "SABER-START///";
    const size_t header_start_len = sizeof(HEADER_START) - 1;

    if (expected_part_count <= 0) return 0;
    if (body_len < header_start_len || memcmp(body, HEADER_START, header_start_len) != 0) {
        return -1;
    }

    size_t cursor = header_start_len;
    int n = 0;

    for (int i = 0; i < expected_part_count; i++) {
        if (cursor >= body_len) return -1;

        size_t peek_end = cursor + 70;
        if (peek_end > body_len) peek_end = body_len;
        size_t peek_len = peek_end - cursor;

        /* header must start with "//" right at cursor */
        if (peek_len < 2 || body[cursor] != '/' || body[cursor + 1] != '/') return -1;

        const unsigned char *close = bytes_find(body + cursor, peek_len, "//", 2);
        if (!close) return -1;
        size_t close_index = (size_t)(close - (body + cursor)); /* offset from cursor */

        /* headerText = peek.substring(2, closeIndex) -- "SPART-"itag-chunk"-CL=len" */
        size_t header_text_len = close_index - 2;
        char header_text[96];
        if (header_text_len >= sizeof(header_text)) return -1;
        memcpy(header_text, body + cursor + 2, header_text_len);
        header_text[header_text_len] = '\0';

        size_t header_length = close_index + 2; /* up to and including closing "//" */

        /* SPART-"<itag>-<chunk>"-CL=<len> */
        const char *id_start = strstr(header_text, "SPART-\"");
        if (!id_start) return -1;
        id_start += strlen("SPART-\"");
        const char *id_end = strchr(id_start, '"');
        if (!id_end) return -1;

        char part_id[48];
        size_t id_len = (size_t)(id_end - id_start);
        if (id_len >= sizeof(part_id)) return -1;
        memcpy(part_id, id_start, id_len);
        part_id[id_len] = '\0';

        const char *cl_marker = strstr(header_text, "-CL=");
        if (!cl_marker) return -1;
        long length = strtol(cl_marker + 4, NULL, 10);
        if (length < 0) return -1;

        size_t data_start = cursor + header_length;
        size_t data_end = data_start + (size_t)length;
        if (data_end > body_len) return -1;

        char *dash = strchr(part_id, '-');
        int itag;
        int chunk_number = 0;
        if (dash) {
            *dash = '\0';
            itag = (int)strtol(part_id, NULL, 10);
            chunk_number = (int)strtol(dash + 1, NULL, 10);
        } else {
            itag = (int)strtol(part_id, NULL, 10);
        }

        out_parts[n].itag = itag;
        out_parts[n].chunk_number = chunk_number;
        out_parts[n].is_audio = (itag == 139 || itag == 140);
        out_parts[n].data = body + data_start;
        out_parts[n].length = (size_t)length;
        n++;

        cursor = data_end;
    }

    return n;
}
