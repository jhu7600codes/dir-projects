#include <psp2/ctrl.h>
#include "input.h"

static float clamp1(float v) {
    if (v > 1.0f) return 1.0f;
    if (v < -1.0f) return -1.0f;
    return v;
}

void input_poll(InputState *out, unsigned int *prev_buttons) {
    SceCtrlData pad;
    sceCtrlPeekBufferPositive(0, &pad, 1);

    /* lx/ly: 0..255, 128 = centered. ly is inverted (0 = up). */
    out->steer = clamp1(((float)pad.lx - 128.0f) / 128.0f);
    out->throttle = clamp1((128.0f - (float)pad.ly) / 128.0f);

    unsigned int now = pad.buttons;
    out->pause_pressed = (now & SCE_CTRL_START) && !(*prev_buttons & SCE_CTRL_START);
    out->quit_requested = (now & SCE_CTRL_START) && (now & SCE_CTRL_SELECT);

    *prev_buttons = now;
}
