#include <math.h>
#include "car.h"

#define MAX_SPEED            260.0f  /* px/s forward */
#define MAX_REVERSE_SPEED   -100.0f
#define ACCEL                240.0f  /* px/s^2 */
#define BRAKE_DECEL          420.0f  /* px/s^2, opposing current motion */
#define FRICTION_DECEL       140.0f  /* px/s^2, passive decay, no throttle */
#define TURN_RATE_MAX          3.0f  /* rad/s at low speed */
#define TURN_SPEED_FALLOFF    0.55f  /* fraction of turn rate lost at MAX_SPEED */
#define MIN_SPEED_FOR_TURN    20.0f  /* px/s; below this, turning ramps to 0 */
#define STEER_DEADZONE        0.12f
#define THROTTLE_DEADZONE     0.12f

void car_init(Car *car, float x, float y) {
    car->x = x;
    car->y = y;
    car->heading = 0.0f;
    car->speed = 0.0f;
}

void car_update(Car *car, float steer, float throttle, float dt) {
    if (fabsf(steer) < STEER_DEADZONE) steer = 0.0f;
    if (fabsf(throttle) < THROTTLE_DEADZONE) throttle = 0.0f;

    int moving_forward = car->speed > 1.0f;
    int moving_reverse = car->speed < -1.0f;

    if (throttle > 0.0f) {
        /* accelerate forward, or brake hard if currently reversing */
        car->speed += (moving_reverse ? BRAKE_DECEL : ACCEL) * throttle * dt;
    } else if (throttle < 0.0f) {
        /* accelerate backward, or brake hard if currently moving forward */
        car->speed += (moving_forward ? BRAKE_DECEL : ACCEL) * throttle * dt;
    } else if (car->speed > 0.0f) {
        car->speed -= FRICTION_DECEL * dt;
        if (car->speed < 0.0f) car->speed = 0.0f;
    } else if (car->speed < 0.0f) {
        car->speed += FRICTION_DECEL * dt;
        if (car->speed > 0.0f) car->speed = 0.0f;
    }

    if (car->speed > MAX_SPEED) car->speed = MAX_SPEED;
    if (car->speed < MAX_REVERSE_SPEED) car->speed = MAX_REVERSE_SPEED;

    float abs_speed = fabsf(car->speed);
    float speed_frac = abs_speed / MAX_SPEED; /* 0..1 */
    float turn_avail = (abs_speed > MIN_SPEED_FOR_TURN)
                            ? 1.0f
                            : (abs_speed / MIN_SPEED_FOR_TURN); /* ramps in from a stop */
    float turn_rate = TURN_RATE_MAX * (1.0f - TURN_SPEED_FALLOFF * speed_frac);
    float turn = steer * turn_rate * turn_avail;
    if (car->speed < 0.0f) turn = -turn; /* steering inverts in reverse, like a real car */

    car->heading += turn * dt;
    car->x += cosf(car->heading) * car->speed * dt;
    car->y += sinf(car->heading) * car->speed * dt;
}
