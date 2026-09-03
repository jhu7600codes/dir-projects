/* Host test for scrape_videos_page's extraction logic. Can't hit a real
   yt2009 instance from here, so this checks the parsing against a fixture
   built from the exact shape back/yt2009templates.js's videoCell()
   actually emits (read directly from that file, not guessed) -- the
   same discipline this whole project's Android side used throughout. */

#include "../src/scrape.h"
#include "../src/http.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

static int failures = 0;
#define CHECK(cond, msg) do { \
    if (!(cond)) { fprintf(stderr, "FAIL: %s (%s:%d)\n", msg, __FILE__, __LINE__); failures++; } \
} while (0)

/* This override shadows the real http_get (linked from http_stub.c) is
   not possible in C without a build flag, so scrape_test.c is compiled
   against its own tiny stub directly (see test/run.sh) that serves this
   fixture instead of hitting the network. */
static const char *FIXTURE =
    "<html><body>"
    "<div class=\"video-cell *vl\" style=\"width:19.5%\" data-id=\"dQw4w9WgXcQ\">"
    "<div class=\"video-entry\"><div class=\"v120WideEntry\"><div class=\"v120WrapperOuter\">"
    "<div class=\"v120WrapperInner\">"
    "<a class=\"video-thumb-link\" href=\"/watch?v=dQw4w9WgXcQ\" onmouseover=\"videosPreview(this, 'dQw4w9WgXcQ')\" onmouseout=\"removeVideoPreview()\"><img title=\"Rick Astley - Never Gonna Give You Up\" src=\"/thumb.jpg\"></a>"
    "</div></div></div>"
    "<div class=\"video-main-content\"><div class=\"video-title \"><div class=\"video-short-title\">"
    "<a href=\"/watch?v=dQw4w9WgXcQ\" class=\"yt-uix-hovercard-target\" title=\"Rick Astley - Never Gonna Give You Up\" rel=\"nofollow\">Rick Astley - Never Gonna Give You Up</a>"
    "</div></div>"
    "<div class=\"video-facets\"><span class=\"video-view-count\">1,000,000,000 views</span></div>"
    "</div><div class=\"video-clear-list-left\"></div></div></div>"
    "<div class=\"video-cell *vl\" style=\"width:19.5%\" data-id=\"abc12345678\">"
    "<div class=\"video-entry\"><div class=\"v120WideEntry\"><div class=\"v120WrapperOuter\">"
    "<div class=\"v120WrapperInner\"><a class=\"video-thumb-link\" href=\"/watch?v=abc12345678\"><img title=\"Bob &amp; the &quot;Widgets&quot;\" src=\"/thumb2.jpg\"></a></div>"
    "</div></div>"
    "<div class=\"video-main-content\"><div class=\"video-title \"><div class=\"video-short-title\">"
    "<a href=\"/watch?v=abc12345678\" class=\"yt-uix-hovercard-target\" title=\"Bob &amp; the &quot;Widgets&quot;\" rel=\"nofollow\">Bob &amp; the &quot;Widgets&quot;</a>"
    "</div></div></div><div class=\"video-clear-list-left\"></div></div></div>"
    "</body></html>";

int http_init(void) { return 0; }
void http_cleanup(void) {}
int http_get(const char *url, const char *cookie_header, http_response *resp) {
    (void)url;
    (void)cookie_header;
    size_t len = strlen(FIXTURE);
    resp->data = malloc(len + 1);
    memcpy(resp->data, FIXTURE, len + 1);
    resp->size = len;
    resp->status = 200;
    resp->x_part_count = -1;
    return 0;
}
void http_response_free(http_response *resp) {
    free(resp->data);
    resp->data = NULL;
}

static void test_extracts_two_real_videos(void) {
    scraped_video videos[8];
    int n = scrape_videos_page("http://example.test:3000", videos, 8);
    CHECK(n == 2, "expected exactly 2 videos from the fixture");
    if (n >= 1) {
        CHECK(strcmp(videos[0].video_id, "dQw4w9WgXcQ") == 0, "video 0 id");
        CHECK(strcmp(videos[0].title, "Rick Astley - Never Gonna Give You Up") == 0, "video 0 title");
    }
    if (n >= 2) {
        CHECK(strcmp(videos[1].video_id, "abc12345678") == 0, "video 1 id");
        CHECK(strcmp(videos[1].title, "Bob & the \"Widgets\"") == 0,
              "video 1 title decodes &amp;/&quot; entities");
    }
}

static void test_respects_max_videos_cap(void) {
    scraped_video videos[1];
    int n = scrape_videos_page("http://example.test:3000", videos, 1);
    CHECK(n == 1, "capped at max_videos even though the fixture has 2");
}

int main(void) {
    test_extracts_two_real_videos();
    test_respects_max_videos_cap();
    if (failures == 0) {
        printf("all scrape tests passed\n");
        return 0;
    }
    fprintf(stderr, "%d scrape test(s) failed\n", failures);
    return 1;
}
