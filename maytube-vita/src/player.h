#ifndef MAYTUBE_PLAYER_H
#define MAYTUBE_PLAYER_H

#include <SDL2/SDL.h>

/* Plays a completed pair of video-only/audio-only fragmented-MP4 files
   (as fetch_video() produces) to an existing SDL renderer, blocking until
   playback finishes or the caller's should_stop callback returns nonzero
   (checked once per frame, so e.g. a Circle-button-pressed poll can cancel
   playback early -- same "keep the loop responsive" spirit as fetch.c's
   per-step progress callback). Audio is the sync master, matching the
   textbook ffplay approach: video frames are shown, delayed, or dropped to
   track a running audio_clock rather than the other way around, since
   audio glitches are far more noticeable than an occasionally-dropped
   video frame.

   NOT host-testable, unlike sabr.c/scrape.c/fetch.c's pure logic -- this
   is real decode+present against SDL2/ffmpeg. Only verified by compiling
   clean for the Vita target; there's no Vita hardware or emulator in this
   environment to run it on. See the project README's "What's verified".

   Returns 0 if playback ran to completion or was stopped via
   should_stop, -1 on a hard failure to even start (bad files, no decoder,
   SDL/renderer errors). */
int player_play(SDL_Renderer *renderer,
                 const char *video_path, const char *audio_path,
                 int (*should_stop)(void *userdata), void *userdata);

#endif
