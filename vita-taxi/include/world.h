#ifndef VITA_TAXI_WORLD_H
#define VITA_TAXI_WORLD_H

#define SCREEN_W 960
#define SCREEN_H 544

#define WORLD_W 1600.0f
#define WORLD_H 1600.0f

#define CAR_RADIUS 14.0f

typedef struct {
    float x, y;
} Vec2;

typedef struct {
    Vec2 camera; /* top-left of the viewport, in world space */
} World;

void world_init(World *world);

/* Keep the camera centered on (focus_x, focus_y), clamped so it never
 * shows anything outside the world bounds. */
void world_update_camera(World *world, float focus_x, float focus_y);

/* Clamp a position to stay inside the world bounds (minus a radius),
 * bleeding off speed on impact. Returns 1 if a bounce happened. */
int world_clamp_and_bounce(float *x, float *y, float *speed, float radius);

/* Pick a random point at least min_dist away from `from`, staying
 * inside the world bounds with a margin. */
Vec2 world_random_point_away_from(Vec2 from, float min_dist);

float vec2_dist(Vec2 a, Vec2 b);

void world_render(const World *world);

#endif
