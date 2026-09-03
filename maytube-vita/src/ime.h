#ifndef MAYTUBE_IME_H
#define MAYTUBE_IME_H

#include <stddef.h>

/* Prompts with the Vita's on-screen keyboard (SceImeDialog) for a single
   line of text -- ASCII only, which is all a server URL ever needs.
   Blocks until the user confirms or cancels. The dialog draws as its own
   system overlay independent of the app's GXM render target (unlike
   sceMsgDialog, IME needs no render-target compositing call), so nothing
   else about the app's rendering has to change around this call -- but
   button/controller events can still queue up on SDL's event queue while
   it's open, so the caller should drain pending SDL events once this
   returns, before resuming normal input handling.

   Host (non-Vita) builds always return -1 -- this is Vita-only, same as
   main.c/player.c, and isn't something the host unit tests exercise.

   Returns 0 and fills out_utf8 (NUL-terminated, up to out_cap-1 bytes) if
   the user confirmed; -1 on cancel or error, leaving out_utf8 untouched. */
int ime_prompt_text(const char *title, const char *initial_text, char *out_utf8, size_t out_cap);

#endif
