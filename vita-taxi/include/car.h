#ifndef VITA_TAXI_CAR_H
#define VITA_TAXI_CAR_H

typedef struct {
    float x, y;     /* world-space position, px */
    float heading;  /* radians, 0 = facing +x */
    float speed;    /* signed px/s, + forward, - reverse */
} Car;

void car_init(Car *car, float x, float y);

/* steer: -1..1 (left..right). throttle: -1..1 (brake/reverse..accel). */
void car_update(Car *car, float steer, float throttle, float dt);

#endif
