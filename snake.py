#!/usr/bin/env python3
"""Terminal Snake — arrow keys to move, q to quit, r to restart."""

import curses
import random
import time

WIDTH = 40
HEIGHT = 20

UP    = (-1,  0)
DOWN  = ( 1,  0)
LEFT  = ( 0, -1)
RIGHT = ( 0,  1)

KEY_MAP = {
    curses.KEY_UP:    UP,
    curses.KEY_DOWN:  DOWN,
    curses.KEY_LEFT:  LEFT,
    curses.KEY_RIGHT: RIGHT,
    ord('w'): UP,
    ord('s'): DOWN,
    ord('a'): LEFT,
    ord('d'): RIGHT,
}

OPPOSITES = {UP: DOWN, DOWN: UP, LEFT: RIGHT, RIGHT: LEFT}

SPEEDS = [
    (1,   0.18),
    (5,   0.14),
    (10,  0.10),
    (20,  0.07),
    (35,  0.05),
    (50,  0.035),
]

def speed_for(score):
    delay = SPEEDS[0][1]
    for threshold, d in SPEEDS:
        if score >= threshold:
            delay = d
    return delay


def place_food(snake_set):
    while True:
        r = random.randint(1, HEIGHT - 2)
        c = random.randint(1, WIDTH - 2)
        if (r, c) not in snake_set:
            return (r, c)


def draw_border(win):
    win.border()
    title = " SNAKE "
    win.addstr(0, (WIDTH - len(title)) // 2, title, curses.color_pair(3) | curses.A_BOLD)


def draw_hud(win, score, hi):
    hud = f" Score: {score}  Hi: {hi}  WASD/Arrows · q quit · r restart "
    win.addstr(HEIGHT - 1, 1, hud[: WIDTH - 2], curses.color_pair(3))


def run_game(stdscr, hi):
    curses.curs_set(0)
    stdscr.nodelay(True)
    stdscr.keypad(True)

    curses.start_color()
    curses.use_default_colors()
    curses.init_pair(1, curses.COLOR_GREEN,  -1)  # snake
    curses.init_pair(2, curses.COLOR_RED,    -1)  # food
    curses.init_pair(3, curses.COLOR_YELLOW, -1)  # hud / border
    curses.init_pair(4, curses.COLOR_CYAN,   -1)  # head

    win = curses.newwin(HEIGHT, WIDTH, 0, 0)
    win.keypad(True)
    win.nodelay(True)

    snake = [(HEIGHT // 2, WIDTH // 2 - i) for i in range(4)]
    snake_set = set(snake)
    direction = RIGHT
    food = place_food(snake_set)
    score = 0
    last_move = time.monotonic()

    while True:
        key = stdscr.getch()
        if key == ord('q'):
            return None, hi
        if key == ord('r'):
            return 'restart', hi

        new_dir = KEY_MAP.get(key)
        if new_dir and new_dir != OPPOSITES.get(direction):
            direction = new_dir

        now = time.monotonic()
        if now - last_move < speed_for(score):
            time.sleep(0.005)
            continue
        last_move = now

        head = (snake[0][0] + direction[0], snake[0][1] + direction[1])

        # wall collision
        if head[0] <= 0 or head[0] >= HEIGHT - 1 or head[1] <= 0 or head[1] >= WIDTH - 1:
            return 'dead', max(score, hi)

        # self collision
        if head in snake_set:
            return 'dead', max(score, hi)

        snake.insert(0, head)
        snake_set.add(head)

        if head == food:
            score += 1
            hi = max(score, hi)
            food = place_food(snake_set)
        else:
            tail = snake.pop()
            snake_set.discard(tail)

        # draw
        win.erase()
        draw_border(win)
        draw_hud(win, score, hi)

        win.addch(food[0], food[1], '●', curses.color_pair(2) | curses.A_BOLD)

        for i, (r, c) in enumerate(snake):
            if 0 < r < HEIGHT - 1 and 0 < c < WIDTH - 1:
                if i == 0:
                    win.addch(r, c, '◆', curses.color_pair(4) | curses.A_BOLD)
                else:
                    win.addch(r, c, '█', curses.color_pair(1))

        win.refresh()


def death_screen(stdscr, score, hi):
    stdscr.clear()
    lines = [
        "╔══════════════════╗",
        "║    GAME  OVER    ║",
        f"║  Score : {score:<8}║",
        f"║  Best  : {hi:<8}║",
        "╠══════════════════╣",
        "║  r  →  restart  ║",
        "║  q  →  quit     ║",
        "╚══════════════════╝",
    ]
    row = (HEIGHT - len(lines)) // 2
    col = (WIDTH - len(lines[0])) // 2
    for i, line in enumerate(lines):
        color = curses.color_pair(3) if i in (0, 4, 7) else curses.color_pair(1)
        try:
            stdscr.addstr(row + i, col, line, color | curses.A_BOLD)
        except curses.error:
            pass
    stdscr.refresh()

    while True:
        k = stdscr.getch()
        if k == ord('q'):
            return 'quit'
        if k == ord('r'):
            return 'restart'
        time.sleep(0.02)


def main(stdscr):
    hi = 0
    while True:
        result, hi = run_game(stdscr, hi)
        if result is None:
            break
        if result == 'dead':
            action = death_screen(stdscr, hi, hi)
            if action == 'quit':
                break
        # 'restart' loops back


def splash(stdscr):
    curses.start_color()
    curses.use_default_colors()
    curses.init_pair(1, curses.COLOR_GREEN,  -1)
    curses.init_pair(3, curses.COLOR_YELLOW, -1)
    curses.curs_set(0)
    stdscr.clear()
    art = [
        r" ____  _   _    _    _  _______",
        r"/ ___|| \ | |  / \  | |/ / ____|",
        r"\___ \|  \| | / _ \ | ' /|  _|  ",
        r" ___) | |\  |/ ___ \| . \| |___ ",
        r"|____/|_| \_/_/   \_\_|\_\_____|",
    ]
    row = 2
    for i, line in enumerate(art):
        try:
            stdscr.addstr(row + i, 1, line, curses.color_pair(1) | curses.A_BOLD)
        except curses.error:
            pass

    hints = [
        "",
        "  Arrow keys or WASD to move",
        "  Eat ● to grow and score",
        "  Don't hit walls or yourself",
        "",
        "  Press any key to start …",
    ]
    for i, h in enumerate(hints):
        try:
            stdscr.addstr(row + len(art) + i, 0, h, curses.color_pair(3))
        except curses.error:
            pass

    stdscr.refresh()
    stdscr.nodelay(False)
    stdscr.getch()
    stdscr.nodelay(True)


curses.wrapper(lambda s: (splash(s), main(s)))
