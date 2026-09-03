#ifndef MAYTUBE_HTTP_H
#define MAYTUBE_HTTP_H

#include <stddef.h>

/* Thin curl wrapper. A yt2009 instance is plain HTTP on a LAN almost always
   (same assumption maytube's Android client makes -- see its README), and
   the same cookie header is sent on every request the way OkHttp's cookie
   jar effectively did there, since Vita's curl port has no browser-style
   cookie jar to lean on instead. */

typedef struct {
    char *data;   /* malloc'd; caller frees with http_response_free */
    size_t size;
    long status;
    /* value of the "x-part-count" response header, or -1 if absent --
       the only response header SABR fragment fetches actually need
       (SabrFragmentFetcher.kt's response.header("x-part-count")) */
    long x_part_count;
} http_response;

int http_init(void);
void http_cleanup(void);

/* GET url with an optional Cookie header (NULL for none). Returns 0 on a
   completed HTTP exchange (check resp->status yourself), non-zero only on
   a real transport failure (couldn't connect at all, etc). */
int http_get(const char *url, const char *cookie_header, http_response *resp);

void http_response_free(http_response *resp);

#endif
