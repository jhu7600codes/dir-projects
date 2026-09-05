#include <math.h>
#include "game.h"
#include "render.h"

#define MARKER_RADIUS   18.0f
#define MIN_SPAWN_DIST 250.0f
#define BASE_FARE        50
#define FARE_PER_PIXEL  0.08f

void game_init(Game *game) {
    world_init(&game->world);
    car_init(&game->car, WORLD_W / 2.0f, WORLD_H / 2.0f);

    game->pstate = PASSENGER_NONE;
    game->score = 0;
    game->paused = 0;
    game->trip_distance = 0.0f;

    Vec2 car_pos = {game->car.x, game->car.y};
    game->pickup = world_random_point_away_from(car_pos, MIN_SPAWN_DIST);
    game->dropoff = car_pos; /* unused until a passenger is picked up */
}

void game_update(Game *game, const InputState *input, float dt) {
    if (input->pause_pressed) {
        game->paused = !game->paused;
    }

    if (game->paused) {
        return;
    }

    car_update(&game->car, input->steer, input->throttle, dt);
    world_clamp_and_bounce(&game->car.x, &game->car.y, &game->car.speed, CAR_RADIUS);
    world_update_camera(&game->world, game->car.x, game->car.y);

    Vec2 car_pos = {game->car.x, game->car.y};
    Vec2 target = (game->pstate == PASSENGER_NONE) ? game->pickup : game->dropoff;
    float d = vec2_dist(car_pos, target);

    if (game->pstate == PASSENGER_RIDING) {
        game->trip_distance += fabsf(game->car.speed) * dt;
    }

    if (d < MARKER_RADIUS + CAR_RADIUS) {
        if (game->pstate == PASSENGER_NONE) {
            game->pstate = PASSENGER_RIDING;
            game->trip_distance = 0.0f;
            game->dropoff = world_random_point_away_from(game->pickup, MIN_SPAWN_DIST);
        } else {
            int fare = BASE_FARE + (int)(game->trip_distance * FARE_PER_PIXEL);
            game->score += fare;
            game->pstate = PASSENGER_NONE;
            game->pickup = world_random_point_away_from(game->dropoff, MIN_SPAWN_DIST);
        }
    }
}

void game_render(const Game *game, vita2d_pgf *font) {
    world_render(&game->world);

    Vec2 target;
    unsigned int color;
    const char *label;

    if (game->pstate == PASSENGER_NONE) {
        target = game->pickup;
        color = RGBA8(240, 200, 20, 255);
        label = "PICKUP";
    } else {
        target = game->dropoff;
        color = RGBA8(40, 200, 80, 255);
        label = "DROPOFF";
    }

    draw_marker(target, &game->world, color, label, font);
    draw_car(&game->car, &game->world);

    Vec2 car_pos = {game->car.x, game->car.y};
    draw_hud_arrow(car_pos, target, color);
    draw_hud_score(game->score, game->paused, font);
}
