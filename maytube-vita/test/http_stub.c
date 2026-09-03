/* Host-test-only stand-in for http.c's real (libcurl-based) implementation.
   sabr.c's sabr_resolve() references these symbols, but sabr_test.c never
   calls sabr_resolve() -- it only exercises the pure byte-parsing logic
   (sabr_parse_parts/sabr_parse_clock), which needs no networking at all.
   This exists purely so the test binary links without needing libcurl-dev
   on the host (not present in this environment) -- the real http.c (built
   against the actual VitaSDK libcurl port) is what the Vita target build
   uses instead of this file. */
#include "../src/http.h"

int http_init(void) { return 0; }
void http_cleanup(void) {}
int http_get(const char *url, const char *cookie_header, http_response *resp) {
    (void)url;
    (void)cookie_header;
    resp->data = 0;
    resp->size = 0;
    resp->status = 0;
    resp->x_part_count = -1;
    return -1;
}
void http_response_free(http_response *resp) {
    (void)resp;
}
