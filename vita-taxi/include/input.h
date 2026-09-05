#ifndef VITA_TAXI_INPUT_H
#define VITA_TAXI_INPUT_H

typedef struct {
    float steer;         /* -1..1 */
    float throttle;      /* -1..1 */
    int pause_pressed;   /* edge-triggered */
    int quit_requested;
} InputState;

/* prev_buttons is owned by the caller and threaded through calls so
 * pause can be edge-triggered instead of firing every frame. */
void input_poll(InputState *out, unsigned int *prev_buttons);

#endif
