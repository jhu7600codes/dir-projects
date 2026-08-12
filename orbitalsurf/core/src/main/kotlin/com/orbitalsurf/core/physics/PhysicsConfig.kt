package com.orbitalsurf.core.physics

/** Tunable constants for [BallSimulator]. This is a simplified custom simulation, not a full rigid-body engine. */
data class PhysicsConfig(
    val gravity: Double = 22.0,
    val jumpImpulse: Double = 9.0,
    val maxLateralSpeed: Double = 7.0,
    val lateralAccel: Double = 30.0,
    val ballRadius: Double = 0.5,
    /** How close to the surface (after resolving this tick's motion) still counts as "grounded", to absorb floating-point slop. */
    val groundSnapTolerance: Double = 0.05,
    /** Below this world height with no surface underneath, the ball is considered to have fallen off the city, not just mid-jump. */
    val fellOffHeight: Double = -10.0,
    /**
     * Steering authority while airborne, as a fraction of grounded authority -- a real rolling
     * ball can't cut a sharp turn with no wheels on the ground, so jumps commit you to roughly
     * the direction you left the ground in, more than lets you redirect mid-air.
     */
    val airControlFactor: Double = 0.45,
    /** Forward speed (m/s) below which full turning authority applies -- no penalty yet. */
    val highSpeedTurnStart: Double = 10.0,
    /** Forward speed range over which turning authority tapers from full down to [highSpeedTurnFloor]. */
    val highSpeedTurnRange: Double = 12.0,
    /** The minimum fraction of [maxLateralSpeed] still available once forward speed is [highSpeedTurnStart] + [highSpeedTurnRange] or beyond -- momentum resists a sharp turn at high speed, it never fully locks the wheel. */
    val highSpeedTurnFloor: Double = 0.55,
) {
    companion object {
        val DEFAULT = PhysicsConfig()
    }
}
