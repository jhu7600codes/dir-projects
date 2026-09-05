#include <math.h>
#include <stdlib.h>
#include <vita2d.h>
#include "world.h"

#define MARKER_MARGIN 60.0f
#define ROAD_SPACING 300.0f
#define ROAD_WIDTH 40.0f

static float clampf(float v, float lo, float hi) {
    if (v < lo) return lo;
    if (v > hi) return hi;
    return v;
}

void world_init(World *world) {
    world->camera.x = 0.0f;
    world->camera.y = 0.0f;
}

void world_update_camera(World *world, float focus_x, float focus_y) {
    world->camera.x = clampf(focus_x - SCREEN_W / 2.0f, 0.0f, WORLD_W - SCREEN_W);
    world->camera.y = clampf(focus_y - SCREEN_H / 2.0f, 0.0f, WORLD_H - SCREEN_H);
}

int world_clamp_and_bounce(float *x, float *y, float *speed, float radius) {
    int bounced = 0;

    if (*x < radius) {
        *x = radius;
        bounced = 1;
    } else if (*x > WORLD_W - radius) {
        *x = WORLD_W - radius;
        bounced = 1;
    }

    if (*y < radius) {
        *y = radius;
        bounced = 1;
    } else if (*y > WORLD_H - radius) {
        *y = WORLD_H - radius;
        bounced = 1;
    }

    if (bounced) {
        *speed *= 0.4f;
    }
    return bounced;
}

float vec2_dist(Vec2 a, Vec2 b) {
    float dx = a.x - b.x;
    float dy = a.y - b.y;
    return sqrtf(dx * dx + dy * dy);
}

Vec2 world_random_point_away_from(Vec2 from, float min_dist) {
    Vec2 p;
    int tries = 0;
    float span_x = WORLD_W - 2.0f * MARKER_MARGIN;
    float span_y = WORLD_H - 2.0f * MARKER_MARGIN;

    do {
        p.x = MARKER_MARGIN + (float)(rand() % (int)span_x);
        p.y = MARKER_MARGIN + (float)(rand() % (int)span_y);
        tries++;
    } while (vec2_dist(p, from) < min_dist && tries < 20);

    return p;
}

void world_render(const World *world) {
    /* decorative road grid, purely visual, no collision against it */
    unsigned int road_color = RGBA8(90, 90, 95, 255);

    for (float wx = 0.0f; wx < WORLD_W; wx += ROAD_SPACING) {
        float sx = wx - world->camera.x;
        if (sx + ROAD_WIDTH < 0.0f || sx > SCREEN_W) continue;
        vita2d_draw_rectangle(sx - ROAD_WIDTH / 2.0f, 0.0f, ROAD_WIDTH, (float)SCREEN_H, road_color);
    }

    for (float wy = 0.0f; wy < WORLD_H; wy += ROAD_SPACING) {
        float sy = wy - world->camera.y;
        if (sy + ROAD_WIDTH < 0.0f || sy > SCREEN_H) continue;
        vita2d_draw_rectangle(0.0f, sy - ROAD_WIDTH / 2.0f, (float)SCREEN_W, ROAD_WIDTH, road_color);
    }
}
