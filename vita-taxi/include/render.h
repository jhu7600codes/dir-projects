#ifndef VITA_TAXI_RENDER_H
#define VITA_TAXI_RENDER_H

#include <vita2d.h>
#include "car.h"
#include "world.h"

void draw_car(const Car *car, const World *world);
void draw_marker(Vec2 world_pos, const World *world, unsigned int color, const char *label, vita2d_pgf *font);
void draw_hud_arrow(Vec2 car_pos, Vec2 target, unsigned int color);
void draw_hud_score(int score, int paused, vita2d_pgf *font);

#endif
