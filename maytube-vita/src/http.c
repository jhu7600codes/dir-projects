#include "http.h"

#include <curl/curl.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#ifdef __vita__
#include <psp2/sysmodule.h>
#include <psp2/net/net.h>
#include <psp2/net/netctl.h>

/* curl on Vita rides sceNet directly -- unlike a desktop libcurl, nothing
   brings the net stack up for it automatically, so this has to happen
   once before the first request or every curl_easy_perform() just fails.
   Static, not malloc'd: this lives for the whole process, same as
   curl_global_init()'s own state. */
static char net_pool[256 * 1024];
static int net_ready = 0;

static int vita_net_init(void) {
    if (net_ready) return 0;
    if (sceSysmoduleLoadModule(SCE_SYSMODULE_NET) < 0) return -1;

    SceNetInitParam init_param;
    init_param.memory = net_pool;
    init_param.size = sizeof(net_pool);
    init_param.flags = 0;
    if (sceNetInit(&init_param) < 0) return -1;
    if (sceNetCtlInit() < 0) return -1;

    net_ready = 1;
    return 0;
}
#endif

typedef struct {
    char *data;
    size_t size;
    size_t cap;
} growbuf;

static void growbuf_init(growbuf *b) {
    b->data = NULL;
    b->size = 0;
    b->cap = 0;
}

static size_t write_cb(void *ptr, size_t size, size_t nmemb, void *userdata) {
    growbuf *b = (growbuf *)userdata;
    size_t add = size * nmemb;
    if (b->size + add + 1 > b->cap) {
        size_t newcap = b->cap ? b->cap * 2 : 16384;
        while (newcap < b->size + add + 1) newcap *= 2;
        char *n = realloc(b->data, newcap);
        if (!n) return 0; /* tells curl to abort */
        b->data = n;
        b->cap = newcap;
    }
    memcpy(b->data + b->size, ptr, add);
    b->size += add;
    b->data[b->size] = '\0';
    return add;
}

/* SabrFragmentFetcher.kt reads exactly one response header
   ("x-part-count") -- curl's header callback fires once per header line
   including the terminating blank line/status line, so this just scans
   for the one we care about, case-insensitively (HTTP header names are
   case-insensitive per RFC 7230, and yt2009's own Express server may not
   always send the same casing back). */
static size_t header_cb(char *buffer, size_t size, size_t nitems, void *userdata) {
    http_response *resp = (http_response *)userdata;
    size_t len = size * nitems;
    static const char key[] = "x-part-count:";
    if (len >= sizeof(key) - 1) {
        char lower[32];
        size_t n = sizeof(key) - 1;
        for (size_t i = 0; i < n && i < sizeof(lower) - 1; i++) {
            char c = buffer[i];
            lower[i] = (c >= 'A' && c <= 'Z') ? (char)(c - 'A' + 'a') : c;
        }
        lower[n] = '\0';
        if (strncmp(lower, key, n) == 0) {
            resp->x_part_count = atol(buffer + n);
        }
    }
    return len;
}

int http_init(void) {
#ifdef __vita__
    if (vita_net_init() != 0) return -1;
#endif
    return curl_global_init(CURL_GLOBAL_DEFAULT) == CURLE_OK ? 0 : -1;
}

void http_cleanup(void) {
    curl_global_cleanup();
}

int http_get(const char *url, const char *cookie_header, http_response *resp) {
    growbuf buf;
    growbuf_init(&buf);
    resp->data = NULL;
    resp->size = 0;
    resp->status = 0;
    resp->x_part_count = -1;

    CURL *curl = curl_easy_init();
    if (!curl) return -1;

    struct curl_slist *headers = NULL;
    if (cookie_header && cookie_header[0]) {
        char *line = malloc(strlen(cookie_header) + 8);
        sprintf(line, "Cookie: %s", cookie_header);
        headers = curl_slist_append(headers, line);
        free(line);
    }

    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_cb);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &buf);
    curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, header_cb);
    curl_easy_setopt(curl, CURLOPT_HEADERDATA, resp);
    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
    curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, 15L);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 30L);
    if (headers) curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

    CURLcode rc = curl_easy_perform(curl);
    if (rc == CURLE_OK) {
        long code = 0;
        curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &code);
        resp->status = code;
        resp->data = buf.data;
        resp->size = buf.size;
    } else {
        free(buf.data);
    }

    if (headers) curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
    return rc == CURLE_OK ? 0 : -1;
}

void http_response_free(http_response *resp) {
    free(resp->data);
    resp->data = NULL;
    resp->size = 0;
}
