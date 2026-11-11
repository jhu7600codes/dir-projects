/* maytube-vita: entry point, input, and the browse -> fetch -> play state
   machine. Kept as one file (unlike the Android app's many-screen split)
   because there's really only one screen here in v1 -- a video list --
   plus two full-screen status states (fetching progress, playing) that
   don't need their own persistent widgets. See the README for what's in
   and out of scope for this pass.

   This file (state machine, rendering, input, app lifecycle) is the one
   part of maytube-vita that is NOT host-testable even in principle -- it
   is nothing but SDL2/libc calls against real hardware. Like player.c,
   its only verification in this environment is "compiles clean for the
   real Vita target"; there is no Vita or emulator here to run it on. */

#include <SDL2/SDL.h>
#include <SDL2/SDL_ttf.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef __vita__
#include <psp2/power.h>
#include <psp2/io/stat.h>
#endif

#include "http.h"
#include "scrape.h"
#include "fetch.h"
#include "player.h"
#include "keyboard.h"

#define SCREEN_W 960
#define SCREEN_H 544
#define MAX_VIDEOS 64
#define ROW_HEIGHT 36
#define VISIBLE_ROWS 12

#define DATA_DIR    "ux0:data/maytube"
#define CONFIG_PATH "ux0:data/maytube/config.txt"
#define VIDEO_PATH  "ux0:data/maytube/video.mp4"
#define AUDIO_PATH  "ux0:data/maytube/audio.mp4"
/* Bundled into the vpk itself (assets/font.ttf, DejaVu Sans -- Bitstream
   Vera license, redistribution/embedding explicitly permitted, see
   assets/DejaVuSans-LICENSE.txt) via CMakeLists.txt's vita_create_vpk
   FILE clause, so it lands under the app's own read-only app0: bundle --
   no manual per-user setup needed, unlike CONFIG_PATH above. */
#define FONT_PATH   "app0:font.ttf"

typedef enum {
    STATE_LIST,
    STATE_FETCHING,
    STATE_PLAYING,
    STATE_ERROR,
    STATE_CONFIG
} app_state;

typedef struct {
    SDL_Renderer *renderer;
    TTF_Font *font;
    char base_url[256];
    scraped_video videos[MAX_VIDEOS];
    int video_count;
    int selected;
    int scroll_offset;
    app_state state;
    char error_msg[256];
    long fetched_ms;
    long total_ms;
    int stop_requested;
    keyboard_state kb;
} app;

/* The server address -- the same thing the Android app's Settings screen
   asks for -- is entered in-app via the self-drawn on-screen keyboard
   (see keyboard.c and STATE_CONFIG's handling below) and persisted here,
   one line, so it doesn't have to be re-typed on every launch. */
static int load_config(char *base_url, size_t cap) {
    FILE *f = fopen(CONFIG_PATH, "r");
    if (!f) return -1;
    if (!fgets(base_url, (int)cap, f)) { fclose(f); return -1; }
    fclose(f);
    size_t len = strlen(base_url);
    while (len > 0 && (base_url[len - 1] == '\n' || base_url[len - 1] == '\r' || base_url[len - 1] == ' ')) {
        base_url[--len] = '\0';
    }
    return len > 0 ? 0 : -1;
}

static void save_config(const char *base_url) {
#ifdef __vita__
    sceIoMkdir(DATA_DIR, 0777); /* ignore the error if it already exists */
#endif
    FILE *f = fopen(CONFIG_PATH, "w");
    if (!f) return;
    fputs(base_url, f);
    fputc('\n', f);
    fclose(f);
}

static void draw_text(app *a, const char *text, int x, int y, SDL_Color color) {
    if (!a->font || !text || !text[0]) return;
    SDL_Surface *surf = TTF_RenderUTF8_Blended(a->font, text, color);
    if (!surf) return;
    SDL_Texture *tex = SDL_CreateTextureFromSurface(a->renderer, surf);
    if (tex) {
        SDL_Rect dst = { x, y, surf->w, surf->h };
        SDL_RenderCopy(a->renderer, tex, NULL, &dst);
        SDL_DestroyTexture(tex);
    }
    SDL_FreeSurface(surf);
}

static void set_error(app *a, const char *msg) {
    strncpy(a->error_msg, msg, sizeof(a->error_msg) - 1);
    a->error_msg[sizeof(a->error_msg) - 1] = '\0';
    a->state = STATE_ERROR;
}

static void refresh_video_list(app *a) {
    int n = scrape_videos_page(a->base_url, a->videos, MAX_VIDEOS);
    if (n < 0) {
        char msg[256];
        snprintf(msg, sizeof(msg),
                 "Could not reach the server (%s). Press Select to change the address, or X to retry.",
                 http_last_error());
        set_error(a, msg);
        return;
    }
    a->video_count = n;
    a->selected = 0;
    a->scroll_offset = 0;
    a->state = STATE_LIST;
}

/* Switches to the on-screen keyboard screen to enter/edit the server
   address -- the in-app replacement for hand-editing config.txt over
   FTP. Unlike a system dialog this doesn't block: STATE_CONFIG's own
   input/render handling below runs it one keypress and one frame at a
   time through the normal main loop, the same way every other screen in
   this app works. */
static void enter_config_state(app *a) {
    keyboard_init(&a->kb, a->base_url[0] ? a->base_url : "http://");
    a->state = STATE_CONFIG;
}

/* Called once STATE_CONFIG's keyboard reports CONFIRMED or CANCELLED.
   An empty confirm is treated as if nothing were entered. A cancelled
   edit of an already-working address just keeps it; cancelling with
   nothing configured yet re-opens the keyboard rather than dead-ending. */
static void finish_config_state(app *a, keyboard_result result) {
    if (result == KEYBOARD_CONFIRMED && a->kb.text[0]) {
        strncpy(a->base_url, a->kb.text, sizeof(a->base_url) - 1);
        a->base_url[sizeof(a->base_url) - 1] = '\0';
        save_config(a->base_url);
        refresh_video_list(a); /* sets STATE_LIST or STATE_ERROR itself */
    } else if (a->base_url[0]) {
        a->state = STATE_LIST;
    } else {
        enter_config_state(a);
    }
}

static void render_list(app *a) {
    SDL_SetRenderDrawColor(a->renderer, 18, 18, 20, 255);
    SDL_RenderClear(a->renderer);

    SDL_Color title_color = { 255, 255, 255, 255 };
    SDL_Color dim = { 160, 160, 160, 255 };
    draw_text(a, "maytube", 20, 12, title_color);

    if (a->video_count == 0) {
        draw_text(a, "No videos found.", 20, 60, dim);
    }

    if (a->selected < a->scroll_offset) a->scroll_offset = a->selected;
    if (a->selected >= a->scroll_offset + VISIBLE_ROWS) a->scroll_offset = a->selected - VISIBLE_ROWS + 1;

    int y = 56;
    for (int i = a->scroll_offset; i < a->video_count && i < a->scroll_offset + VISIBLE_ROWS; i++) {
        SDL_Color color = (i == a->selected) ? title_color : dim;
        if (i == a->selected) {
            SDL_SetRenderDrawColor(a->renderer, 40, 80, 160, 255);
            SDL_Rect bar = { 10, y - 2, SCREEN_W - 20, ROW_HEIGHT - 4 };
            SDL_RenderFillRect(a->renderer, &bar);
        }
        draw_text(a, a->videos[i].title, 20, y, color);
        y += ROW_HEIGHT;
    }

    draw_text(a, "D-Pad: navigate   X: play   Start: refresh   Select: server address", 20, SCREEN_H - 28, dim);
    SDL_RenderPresent(a->renderer);
}

static void render_status(app *a, const char *line1, const char *line2) {
    SDL_SetRenderDrawColor(a->renderer, 18, 18, 20, 255);
    SDL_RenderClear(a->renderer);
    SDL_Color c = { 255, 255, 255, 255 };
    draw_text(a, line1, 20, SCREEN_H / 2 - 20, c);
    if (line2) draw_text(a, line2, 20, SCREEN_H / 2 + 10, c);
    SDL_RenderPresent(a->renderer);
}

/* fetch_video()'s progress callback: fires roughly once per 5-second
   step, so this is also the only place a still-buffering fetch gets to
   redraw and pump events -- otherwise the OS would see an unresponsive
   window for however long a full fetch takes. There's no cancel path
   here yet (fetch_video() itself has none); see the README. */
static void on_fetch_progress(fetch_progress progress, void *userdata) {
    app *a = (app *)userdata;
    a->fetched_ms = progress.fetched_ms;
    a->total_ms = progress.total_ms;

    SDL_Event e;
    while (SDL_PollEvent(&e)) {
        if (e.type == SDL_QUIT) exit(0);
    }

    char line[128];
    if (a->total_ms > 0) {
        int pct = (int)(progress.fetched_ms * 100 / a->total_ms);
        if (pct > 100) pct = 100;
        snprintf(line, sizeof(line), "Buffering... %d%%", pct);
    } else {
        snprintf(line, sizeof(line), "Buffering... %ld sec", progress.fetched_ms / 1000);
    }
    render_status(a, line, "(buffer-then-play: playback starts once this finishes)");
}

static int should_stop_playback(void *userdata) {
    app *a = (app *)userdata;
    SDL_Event e;
    while (SDL_PollEvent(&e)) {
        if (e.type == SDL_QUIT) { a->stop_requested = 1; }
        if (e.type == SDL_CONTROLLERBUTTONDOWN && e.cbutton.button == SDL_CONTROLLER_BUTTON_B) {
            a->stop_requested = 1;
        }
    }
    return a->stop_requested;
}

int main(int argc, char *argv[]) {
    (void)argc; (void)argv;

#ifdef __vita__
    /* Bump the original ("phat", PCH-1000) Vita off its default clocks --
       standard homebrew practice; the CPU/bus/GPU ceilings here are the
       same ones VitaSDK sample code and most homebrew ship with, safe on
       stock hardware. Slim (PCH-2000) accepts the same calls fine, but
       phat is the model this app is tuned for, per the project brief. */
    scePowerSetArmClockFrequency(444);
    scePowerSetBusClockFrequency(222);
    scePowerSetGpuClockFrequency(166);
    scePowerSetGpuXbarClockFrequency(166);
#endif

    if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_INIT_GAMECONTROLLER) != 0) {
        return 1;
    }
    if (TTF_Init() != 0) {
        SDL_Quit();
        return 1;
    }

    SDL_Window *window = SDL_CreateWindow("maytube", SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED,
                                           SCREEN_W, SCREEN_H, 0);
    SDL_Renderer *renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED);

    app a;
    memset(&a, 0, sizeof(a));
    a.renderer = renderer;
    a.font = TTF_OpenFont(FONT_PATH, 22);
    a.state = STATE_LIST;

    if (SDL_NumJoysticks() > 0 && SDL_IsGameController(0)) {
        SDL_GameControllerOpen(0);
    }

    int net_ok = (http_init() == 0);

    if (!a.font) {
        /* Shouldn't happen -- font.ttf ships bundled inside the vpk (see
           FONT_PATH's comment) -- but if the read somehow fails, say so
           rather than silently rendering no text at all. */
        set_error(&a, "Could not load the bundled font. Try reinstalling the app.");
    } else if (!net_ok) {
        /* Distinct from "could not reach the server": this means the
           network stack itself never came up (sceNet/sceNetCtl bring-up
           failed), so no request was even attempted yet -- previously
           silently ignored here, which meant a real init failure just
           looked identical to a bad server address. */
        char msg[256];
        snprintf(msg, sizeof(msg), "Could not start networking (%s).", http_last_error());
        set_error(&a, msg);
    } else if (load_config(a.base_url, sizeof(a.base_url)) == 0) {
        refresh_video_list(&a);
    } else {
        enter_config_state(&a); /* first launch: no server address saved yet */
    }

    int running = 1;
    while (running) {
        SDL_Event e;
        while (SDL_PollEvent(&e)) {
            if (e.type == SDL_QUIT) { running = 0; break; }

            if (e.type == SDL_CONTROLLERBUTTONDOWN) {
                switch (a.state) {
                case STATE_LIST:
                    if (e.cbutton.button == SDL_CONTROLLER_BUTTON_DPAD_UP && a.selected > 0) {
                        a.selected--;
                    } else if (e.cbutton.button == SDL_CONTROLLER_BUTTON_DPAD_DOWN &&
                               a.selected < a.video_count - 1) {
                        a.selected++;
                    } else if (e.cbutton.button == SDL_CONTROLLER_BUTTON_A && a.video_count > 0) {
                        a.state = STATE_FETCHING;
                        render_status(&a, "Buffering...", NULL);
                        scraped_video *v = &a.videos[a.selected];
                        int rc = fetch_video(a.base_url, v->video_id, -1, VIDEO_PATH, AUDIO_PATH,
                                              on_fetch_progress, &a);
                        if (rc != 0) {
                            set_error(&a, "Could not fetch that video. Press X to go back.");
                        } else {
                            a.state = STATE_PLAYING;
                            render_status(&a, "Playing...", NULL);
                            a.stop_requested = 0;
                            player_play(renderer, VIDEO_PATH, AUDIO_PATH, should_stop_playback, &a);
                            if (a.stop_requested) { running = 0; }
                            else { a.state = STATE_LIST; }
                        }
                    } else if (e.cbutton.button == SDL_CONTROLLER_BUTTON_START) {
                        render_status(&a, "Refreshing...", NULL);
                        refresh_video_list(&a);
                    } else if (e.cbutton.button == SDL_CONTROLLER_BUTTON_BACK) {
                        enter_config_state(&a); /* Select: change server address */
                    }
                    break;
                case STATE_ERROR:
                    if (e.cbutton.button == SDL_CONTROLLER_BUTTON_A) {
                        render_status(&a, "Loading...", NULL);
                        refresh_video_list(&a);
                    } else if (e.cbutton.button == SDL_CONTROLLER_BUTTON_BACK) {
                        enter_config_state(&a);
                    }
                    break;
                case STATE_CONFIG: {
                    keyboard_result kr = keyboard_handle_button(&a.kb, e.cbutton.button);
                    if (kr != KEYBOARD_EDITING) finish_config_state(&a, kr);
                    break;
                }
                default:
                    break;
                }
            }
        }

        if (a.state == STATE_LIST) render_list(&a);
        else if (a.state == STATE_ERROR) render_status(&a, "Error", a.error_msg);
        else if (a.state == STATE_CONFIG) {
            keyboard_render(&a.kb, renderer, a.font, "Server address (http://ip:port)");
        }

        SDL_Delay(16);
    }

    http_cleanup();
    if (a.font) TTF_CloseFont(a.font);
    TTF_Quit();
    SDL_DestroyRenderer(renderer);
    SDL_DestroyWindow(window);
    SDL_Quit();
    return 0;
}
