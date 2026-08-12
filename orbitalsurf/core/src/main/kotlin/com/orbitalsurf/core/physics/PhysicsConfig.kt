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
) {
    companion object {
        val DEFAULT = PhysicsConfig()
    }
}
