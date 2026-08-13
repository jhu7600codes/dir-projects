package com.vaultgame.core.world

/**
 * The three rooftop lanes the runner can occupy. [index] is the lateral slot (-1/0/1); the app
 * module multiplies it by a lane width in world units to get an actual X offset for rendering.
 */
enum class Lane(val index: Int) {
    LEFT(-1),
    CENTER(0),
    RIGHT(1);

    fun shift(delta: Int): Lane = fromIndex((index + delta).coerceIn(-1, 1))

    companion object {
        fun fromIndex(index: Int): Lane = entries.first { it.index == index }
    }
}
