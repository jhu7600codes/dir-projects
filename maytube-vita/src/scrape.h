#ifndef MAYTUBE_SCRAPE_H
#define MAYTUBE_SCRAPE_H

#define SCRAPE_MAX_TITLE 160
#define SCRAPE_MAX_ID 32

typedef struct {
    char video_id[SCRAPE_MAX_ID];
    char title[SCRAPE_MAX_TITLE];
} scraped_video;

/* Fetches yt2009's /videos browse page and pulls out real video-cell
   entries (back/yt2009videos.js's own apply(), which server-renders
   real data-id-tagged .video-cell markup via yt2009templates.js's
   videoCell() directly in the initial HTML response -- unlike the
   homepage's "Recommended for You" module, which needs a watch-history
   `ids` header this app has none of on a fresh install, or the
   homepage's own "Videos Being Watched Now"/"Featured" modules, which
   don't carry a `data-id` attribute at all). Returns the number of
   videos found (0..max_videos), or -1 on a network failure. */
int scrape_videos_page(const char *base_url, scraped_video *out, int max_videos);

#endif
