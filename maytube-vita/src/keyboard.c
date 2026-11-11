#include "keyboard.h"

#include <string.h>

/* A standard on-screen QWERTY layout (digits, then three letter rows) --
   the same idiom a PS console's own text-entry screens use, and plenty
   for a server URL (e.g. http://192.168.1.20:3000): all 26 lowercase
   letters plus . : / -. This replaces an earlier attempt at using
   SceImeDialog (the system on-screen keyboard) -- that needed GXM
   render-target compositing this app's SDL2 renderer doesn't expose, and
   in practice just hung on real hardware. This widget instead uses
   nothing but the same SDL2/SDL2_ttf draw calls the video list screen
   already uses, so there's no unverified system-dialog interaction left
   in the config flow at all. */

#define KB_ROWS 4
#define KB_COLS 10
static const char *const KB_KEYS[KB_ROWS] = {
    "1234567890",
    "qwertyuiop",
    "asdfghjkl-",
    "zxcvbnm.:/",
};

#define KEY_SIZE 64
#define KEY_GAP 6
#define GRID_LEFT 40
#define GRID_TOP 220

void keyboard_init(keyboard_state *kb, const char *initial_text) {
    memset(kb, 0, sizeof(*kb));
    if (initial_text) {
        strncpy(kb->text, initial_text, KEYBOARD_MAX_TEXT - 1);
    }
    kb->cursor_row = 0;
    kb->cursor_col = 0;
}

keyboard_result keyboard_handle_button(keyboard_state *kb, SDL_GameControllerButton button) {
    size_t len = strlen(kb->text);
    switch (button) {
        case SDL_CONTROLLER_BUTTON_DPAD_UP:
            kb->cursor_row = (kb->cursor_row + KB_ROWS - 1) % KB_ROWS;
            break;
        case SDL_CONTROLLER_BUTTON_DPAD_DOWN:
            kb->cursor_row = (kb->cursor_row + 1) % KB_ROWS;
            break;
        case SDL_CONTROLLER_BUTTON_DPAD_LEFT:
            kb->cursor_col = (kb->cursor_col + KB_COLS - 1) % KB_COLS;
            break;
        case SDL_CONTROLLER_BUTTON_DPAD_RIGHT:
            kb->cursor_col = (kb->cursor_col + 1) % KB_COLS;
            break;
        case SDL_CONTROLLER_BUTTON_A: /* Cross: type the highlighted key */
            if (len + 1 < KEYBOARD_MAX_TEXT) {
                kb->text[len] = KB_KEYS[kb->cursor_row][kb->cursor_col];
                kb->text[len + 1] = '\0';
            }
            break;
        case SDL_CONTROLLER_BUTTON_B: /* Circle: backspace */
            if (len > 0) kb->text[len - 1] = '\0';
            break;
        case SDL_CONTROLLER_BUTTON_X: /* Square: clear the whole line */
            kb->text[0] = '\0';
            break;
        case SDL_CONTROLLER_BUTTON_START: /* Start: confirm */
            return KEYBOARD_CONFIRMED;
        case SDL_CONTROLLER_BUTTON_BACK: /* Select: cancel */
            return KEYBOARD_CANCELLED;
        default:
            break;
    }
    return KEYBOARD_EDITING;
}

static void kb_draw_text(SDL_Renderer *renderer, TTF_Font *font, const char *text, int x, int y, SDL_Color color) {
    if (!font || !text || !text[0]) return;
    SDL_Surface *surf = TTF_RenderUTF8_Blended(font, text, color);
    if (!surf) return;
    SDL_Texture *tex = SDL_CreateTextureFromSurface(renderer, surf);
    if (tex) {
        SDL_Rect dst = { x, y, surf->w, surf->h };
        SDL_RenderCopy(renderer, tex, NULL, &dst);
        SDL_DestroyTexture(tex);
    }
    SDL_FreeSurface(surf);
}

void keyboard_render(keyboard_state *kb, SDL_Renderer *renderer, TTF_Font *font, const char *title) {
    SDL_Color white = { 255, 255, 255, 255 };
    SDL_Color dim = { 160, 160, 160, 255 };
    SDL_Color black = { 20, 20, 20, 255 };

    SDL_SetRenderDrawColor(renderer, 18, 18, 20, 255);
    SDL_RenderClear(renderer);

    kb_draw_text(renderer, font, title, 40, 30, white);

    /* text-entry box */
    SDL_SetRenderDrawColor(renderer, 40, 40, 45, 255);
    SDL_Rect box = { 40, 70, 880, 48 };
    SDL_RenderFillRect(renderer, &box);
    char shown[KEYBOARD_MAX_TEXT + 2];
    snprintf(shown, sizeof(shown), "%s_", kb->text);
    kb_draw_text(renderer, font, shown, 50, 80, white);

    /* key grid */
    for (int r = 0; r < KB_ROWS; r++) {
        for (int c = 0; c < KB_COLS; c++) {
            int x = GRID_LEFT + c * (KEY_SIZE + KEY_GAP);
            int y = GRID_TOP + r * (KEY_SIZE + KEY_GAP);
            int selected = (r == kb->cursor_row && c == kb->cursor_col);

            SDL_SetRenderDrawColor(renderer, selected ? 40 : 55, selected ? 80 : 55, selected ? 160 : 60, 255);
            SDL_Rect key = { x, y, KEY_SIZE, KEY_SIZE };
            SDL_RenderFillRect(renderer, &key);

            char label[2] = { KB_KEYS[r][c], '\0' };
            kb_draw_text(renderer, font, label, x + KEY_SIZE / 2 - 6, y + KEY_SIZE / 2 - 12,
                         selected ? black : white);
        }
    }

    kb_draw_text(renderer, font,
                 "D-Pad: move   Cross: type   Circle: backspace   Square: clear   Start: done   Select: cancel",
                 40, GRID_TOP + KB_ROWS * (KEY_SIZE + KEY_GAP) + 16, dim);

    SDL_RenderPresent(renderer);
}
