#include <stdlib.h>
#include <vita2d.h>
#include <psp2/kernel/processmgr.h>
#include <psp2/kernel/threadmgr.h>
#include <psp2/ctrl.h>
#include "car.h"
#include "world.h"
#include "game.h"
#include "input.h"

int main(void) {
    vita2d_init();
    vita2d_set_clear_color(RGBA8(60, 140, 60, 255)); /* grass green */
    vita2d_pgf *font = vita2d_load_default_pgf();

    sceCtrlSetSamplingMode(SCE_CTRL_MODE_ANALOG_WIDE);

    srand((unsigned int)sceKernelGetProcessTimeWide());

    Game game;
    game_init(&game);

    unsigned int prev_buttons = 0;
    SceUInt64 last_time = sceKernelGetProcessTimeWide();

    for (;;) {
        InputState input;
        input_poll(&input, &prev_buttons);
        if (input.quit_requested) break;

        SceUInt64 now = sceKernelGetProcessTimeWide();
        float dt = (now - last_time) / 1000000.0f;
        last_time = now;
        if (dt > 0.05f) dt = 0.05f; /* clamp spikes (debugger, hitches) */

        game_update(&game, &input, dt);

        vita2d_start_drawing();
        vita2d_clear_screen();
        game_render(&game, font);
        vita2d_end_drawing();
        vita2d_swap_buffers();
    }

    vita2d_free_pgf(font);
    vita2d_fini();
    sceKernelExitProcess(0);
    return 0;
}
