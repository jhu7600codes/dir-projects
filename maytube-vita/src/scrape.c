#include "scrape.h"
#include "http.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>

/* Minimal, dependency-free decode of the handful of entities yt2009's own
   title escaping (utils.xss, back/yt2009utils.js) actually produces --
   not a general HTML entity decoder, just the ones titles routinely
   contain. Writes into dst (size dst_cap), always NUL-terminated. */
static void decode_entities(const char *src, char *dst, size_t dst_cap) {
    size_t di = 0;
    for (const char *p = src; *p && di + 1 < dst_cap; ) {
        if (*p == '&') {
            if (strncmp(p, "&amp;", 5) == 0) { dst[di++] = '&'; p += 5; continue; }
            if (strncmp(p, "&quot;", 6) == 0) { dst[di++] = '"'; p += 6; continue; }
            if (strncmp(p, "&#39;", 5) == 0) { dst[di++] = '\''; p += 5; continue; }
            if (strncmp(p, "&apos;", 6) == 0) { dst[di++] = '\''; p += 6; continue; }
            if (strncmp(p, "&lt;", 4) == 0) { dst[di++] = '<'; p += 4; continue; }
            if (strncmp(p, "&gt;", 4) == 0) { dst[di++] = '>'; p += 4; continue; }
        }
        dst[di++] = *p++;
    }
    dst[di] = '\0';
}

int scrape_videos_page(const char *base_url, scraped_video *out, int max_videos) {
    char url[512];
    snprintf(url, sizeof(url), "%s/videos", base_url);

    http_response resp;
    if (http_get(url, NULL, &resp) != 0) return -1;
    if (resp.status < 200 || resp.status >= 300) {
        http_response_free(&resp);
        return -1;
    }

    int count = 0;
    const char *cursor = resp.data;

    while (count < max_videos) {
        /* back/yt2009templates.js's videoCell(): every real listing cell
           carries data-id="<video id>" */
        const char *id_marker = strstr(cursor, "data-id=\"");
        if (!id_marker) break;
        id_marker += strlen("data-id=\"");
        const char *id_end = strchr(id_marker, '"');
        if (!id_end) break;

        size_t id_len = (size_t)(id_end - id_marker);
        if (id_len == 0 || id_len >= SCRAPE_MAX_ID) {
            cursor = id_end + 1;
            continue;
        }

        char video_id[SCRAPE_MAX_ID];
        memcpy(video_id, id_marker, id_len);
        video_id[id_len] = '\0';

        /* videoCell()'s title anchor: class="yt-uix-hovercard-target"
           title="<the real title, HTML-entity-escaped>" -- searched
           forward from this cell's data-id, bounded to a generous window
           so an unrelated later cell's title can't leak in if this one's
           own anchor is ever missing for some reason. */
        const char *search_end = id_end + 4000;
        const char *title_marker = NULL;
        {
            /* strstr has no bounded variant in newlib; scan a bounded
               window manually against a known-length needle instead. */
            static const char NEEDLE[] = "class=\"yt-uix-hovercard-target\" title=\"";
            size_t needle_len = sizeof(NEEDLE) - 1;
            for (const char *p = id_end; p < search_end && *p; p++) {
                if (strncmp(p, NEEDLE, needle_len) == 0) {
                    title_marker = p + needle_len;
                    break;
                }
            }
        }

        if (title_marker) {
            const char *title_end = strchr(title_marker, '"');
            if (title_end && (size_t)(title_end - title_marker) < 2048) {
                char raw[2048];
                size_t raw_len = (size_t)(title_end - title_marker);
                memcpy(raw, title_marker, raw_len);
                raw[raw_len] = '\0';

                strncpy(out[count].video_id, video_id, SCRAPE_MAX_ID - 1);
                out[count].video_id[SCRAPE_MAX_ID - 1] = '\0';
                decode_entities(raw, out[count].title, SCRAPE_MAX_TITLE);
                count++;
            }
            cursor = title_marker;
        } else {
            cursor = id_end + 1;
        }
    }

    http_response_free(&resp);
    return count;
}
