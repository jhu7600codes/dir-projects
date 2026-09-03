#ifndef MAYTUBE_KEYBOARD_H
#define MAYTUBE_KEYBOARD_H

#include <SDL2/SDL.h>
#include <SDL2/SDL_ttf.h>

#define KEYBOARD_MAX_TEXT 256

typedef struct {
    char text[KEYBOARD_MAX_TEXT];
    int cursor_row;
    int cursor_col;
} keyboard_state;

typedef enum {
    KEYBOARD_EDITING,
    KEYBOARD_CONFIRMED,
    KEYBOARD_CANCELLED
} keyboard_result;

void keyboard_init(keyboard_state *kb, const char *initial_text);

/* Handles one D-Pad/face-button press. Returns EDITING (keep going),
   CONFIRMED (Start -- kb->text is final), or CANCELLED (Select -- the
   caller should discard kb->text). */
keyboard_result keyboard_handle_button(keyboard_state *kb, SDL_GameControllerButton button);

void keyboard_render(keyboard_state *kb, SDL_Renderer *renderer, TTF_Font *font, const char *title);

#endif
