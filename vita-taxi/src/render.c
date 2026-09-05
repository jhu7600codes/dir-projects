#include <math.h>
#include <vita2d.h>
#include "render.h"

static Vec2 rotate(Vec2 p, float angle) {
    Vec2 r;
    r.x = p.x * cosf(angle) - p.y * sinf(angle);
    r.y = p.x * sinf(angle) + p.y * cosf(angle);
    return r;
}

void draw_car(const Car *car, const World *world) {
    float sx = car->x - world->camera.x;
    float sy = car->y - world->camera.y;

    vita2d_draw_fill_circle(sx, sy, CAR_RADIUS, RGBA8(240, 220, 40, 255));

    /* nose line showing heading */
    float nose_x = sx + cosf(car->heading) * (CAR_RADIUS + 8.0f);
    float nose_y = sy + sinf(car->heading) * (CAR_RADIUS + 8.0f);
    vita2d_draw_line(sx, sy, nose_x, nose_y, RGBA8(20, 20, 20, 255));
}

void draw_marker(Vec2 world_pos, const World *world, unsigned int color, const char *label, vita2d_pgf *font) {
    float sx = world_pos.x - world->camera.x;
    float sy = world_pos.y - world->camera.y;

    /* only draw if roughly on-screen, cheap cull */
    if (sx < -60 || sx > SCREEN_W + 60 || sy < -60 || sy > SCREEN_H + 60) return;

    vita2d_draw_fill_circle(sx, sy, 18.0f, color);
    if (font) {
        vita2d_pgf_draw_text(font, (int)(sx - 20.0f), (int)(sy - 26.0f), RGBA8(255, 255, 255, 255), 0.8f, label);
    }
}

void draw_hud_arrow(Vec2 car_pos, Vec2 target, unsigned int color) {
    Vec2 anchor = {SCREEN_W / 2.0f, 50.0f};
    float angle = atan2f(target.y - car_pos.y, target.x - car_pos.x);

    Vec2 tip = {14.0f, 0.0f};
    Vec2 left = {-8.0f, -8.0f};
    Vec2 right = {-8.0f, 8.0f};

    tip = rotate(tip, angle);
    left = rotate(left, angle);
    right = rotate(right, angle);

    float tx = anchor.x + tip.x, ty = anchor.y + tip.y;
    float lx = anchor.x + left.x, ly = anchor.y + left.y;
    float rx = anchor.x + right.x, ry = anchor.y + right.y;

    vita2d_draw_line(tx, ty, lx, ly, color);
    vita2d_draw_line(tx, ty, rx, ry, color);
    vita2d_draw_line(lx, ly, rx, ry, color);
}

void draw_hud_score(int score, int paused, vita2d_pgf *font) {
    if (!font) return;
    vita2d_pgf_draw_textf(font, 20, 30, RGBA8(255, 255, 255, 255), 1.0f, "Fare: $%d", score);
    if (paused) {
        vita2d_pgf_draw_text(font, SCREEN_W / 2 - 40, SCREEN_H / 2, RGBA8(255, 255, 255, 255), 1.2f, "PAUSED");
    }
}
