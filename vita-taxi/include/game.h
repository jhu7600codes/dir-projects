#ifndef VITA_TAXI_GAME_H
#define VITA_TAXI_GAME_H

#include <vita2d.h>
#include "car.h"
#include "world.h"
#include "input.h"

typedef enum {
    PASSENGER_NONE,   /* no passenger yet, driving to pickup */
    PASSENGER_RIDING  /* passenger aboard, driving to dropoff */
} PassengerState;

typedef struct {
    Car car;
    World world;
    PassengerState pstate;
    Vec2 pickup;
    Vec2 dropoff;
    int score;
    int paused;
    float trip_distance;
} Game;

void game_init(Game *game);
void game_update(Game *game, const InputState *input, float dt);
void game_render(const Game *game, vita2d_pgf *font);

#endif
