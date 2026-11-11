/* Plain assert-based test harness (no framework needed for this) for
   sabr_parse_parts/sabr_parse_clock -- the two pieces of sabr.c that are
   pure logic, host-compilable, and worth checking without a Vita in hand,
   the same reasoning MobileInjectorTest asserts on generated CSS/JS text
   rather than trusting it by eye. See test/http_stub.c for why this
   doesn't need libcurl on the host. Run: test/run.sh */

#include "../src/sabr.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

static int failures = 0;

#define CHECK(cond, msg) do { \
    if (!(cond)) { \
        fprintf(stderr, "FAIL: %s (%s:%d)\n", msg, __FILE__, __LINE__); \
        failures++; \
    } \
} while (0)

/* Builds one real "//SPART-"<itag>-<chunk>"-CL=<len>//<bytes>" part into
   buf, returns bytes written. Exactly the framing SabrFragmentParser.kt
   documents and back/backend.js actually emits. */
static size_t append_part(unsigned char *buf, int itag, int chunk,
                           const unsigned char *data, size_t len) {
    char header[64];
    int header_len = snprintf(header, sizeof(header), "//SPART-\"%d-%d\"-CL=%zu//",
                               itag, chunk, len);
    memcpy(buf, header, (size_t)header_len);
    memcpy(buf + header_len, data, len);
    return (size_t)header_len + len;
}

static void test_single_video_part(void) {
    unsigned char body[512];
    size_t pos = 0;
    memcpy(body, "SABER-START///", 14); /* 14 real chars, not the string literal's trailing NUL */
    pos += 14;

    unsigned char fake_moof[] = {0x00, 0x01, 0x02, 0x03, 0xFF, 0xFE};
    pos += append_part(body + pos, 136, 0, fake_moof, sizeof(fake_moof));

    sabr_part parts[4];
    int n = sabr_parse_parts(body, pos, 1, parts);
    CHECK(n == 1, "single part: expected 1 part parsed");
    CHECK(parts[0].itag == 136, "single part: itag");
    CHECK(parts[0].chunk_number == 0, "single part: chunk number");
    CHECK(parts[0].is_audio == 0, "single part: itag 136 is video, not audio");
    CHECK(parts[0].length == sizeof(fake_moof), "single part: length");
    CHECK(memcmp(parts[0].data, fake_moof, sizeof(fake_moof)) == 0, "single part: data bytes");
}

static void test_video_and_audio_parts(void) {
    unsigned char body[1024];
    size_t pos = 0;
    memcpy(body, "SABER-START///", 14); /* 14 real chars, not the string literal's trailing NUL */
    pos += 14;

    unsigned char video_data[] = {0xAA, 0xBB, 0xCC};
    unsigned char audio_data[] = {0x11, 0x22, 0x33, 0x44, 0x55};
    pos += append_part(body + pos, 136, 12, video_data, sizeof(video_data));
    pos += append_part(body + pos, 140, 12, audio_data, sizeof(audio_data));

    sabr_part parts[4];
    int n = sabr_parse_parts(body, pos, 2, parts);
    CHECK(n == 2, "video+audio: expected 2 parts");
    CHECK(parts[0].is_audio == 0, "video+audio: part 0 is video (itag 136)");
    CHECK(parts[1].is_audio == 1, "video+audio: part 1 is audio (itag 140 in AUDIO_ITAGS)");
    CHECK(parts[1].itag == 140, "video+audio: part 1 itag");
    CHECK(memcmp(parts[1].data, audio_data, sizeof(audio_data)) == 0, "video+audio: audio bytes");
}

static void test_audio_itag_139_also_recognized(void) {
    unsigned char body[256];
    size_t pos = 0;
    memcpy(body, "SABER-START///", 14); /* 14 real chars, not the string literal's trailing NUL */
    pos += 14;
    unsigned char data[] = {0x01};
    pos += append_part(body + pos, 139, 0, data, sizeof(data));

    sabr_part parts[2];
    int n = sabr_parse_parts(body, pos, 1, parts);
    CHECK(n == 1, "itag 139: expected 1 part");
    CHECK(parts[0].is_audio == 1, "itag 139: should also be recognized as audio");
}

static void test_wrong_prefix_rejected(void) {
    unsigned char body[] = "NOT-THE-RIGHT-PREFIX///garbage";
    sabr_part parts[2];
    int n = sabr_parse_parts(body, sizeof(body) - 1, 1, parts);
    CHECK(n == -1, "wrong prefix: must be rejected as malformed");
}

static void test_truncated_response_rejected(void) {
    unsigned char body[64];
    size_t pos = 0;
    memcpy(body, "SABER-START///", 14); /* 14 real chars, not the string literal's trailing NUL */
    pos += 14;
    /* header claims 100 bytes but the buffer doesn't actually have them */
    const char *header = "//SPART-\"136-0\"-CL=100//";
    memcpy(body + pos, header, strlen(header));
    pos += strlen(header);
    pos += 5; /* only 5 of the claimed 100 bytes actually present */

    sabr_part parts[2];
    int n = sabr_parse_parts(body, pos, 1, parts);
    CHECK(n == -1, "truncated response: must be rejected, not read out of bounds");
}

static void test_zero_expected_parts_is_a_no_op(void) {
    unsigned char body[] = "SABER-START///";
    sabr_part parts[1];
    int n = sabr_parse_parts(body, sizeof(body) - 1, 0, parts);
    CHECK(n == 0, "zero expected parts: not an error, just nothing to parse");
}

static void test_parse_clock(void) {
    CHECK(sabr_parse_clock("1:23") == 83000, "clock M:SS: 1:23 -> 83000ms");
    CHECK(sabr_parse_clock("0:05") == 5000, "clock M:SS: 0:05 -> 5000ms");
    CHECK(sabr_parse_clock("1:02:03") == 3723000, "clock H:MM:SS: 1:02:03 -> 3723000ms");
    CHECK(sabr_parse_clock("") == -1, "clock: empty string is unparseable");
    CHECK(sabr_parse_clock("garbage") == -1, "clock: non-numeric is unparseable");
}

int main(void) {
    test_single_video_part();
    test_video_and_audio_parts();
    test_audio_itag_139_also_recognized();
    test_wrong_prefix_rejected();
    test_truncated_response_rejected();
    test_zero_expected_parts_is_a_no_op();
    test_parse_clock();

    if (failures == 0) {
        printf("all sabr tests passed\n");
        return 0;
    }
    fprintf(stderr, "%d sabr test(s) failed\n", failures);
    return 1;
}
