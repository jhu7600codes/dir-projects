package com.orbitalsurf.core.progression

/**
 * Accrues score from distance travelled and pickups, with whatever multiplier is currently in
 * effect (mission-set escalation tier * any active ScoreMultiplier powerup -- `GameSession`
 * combines those and passes the product in). Keeps an internal `Double` accumulator rather
 * than truncating every tick to a `Long`, since a single tick's point gain is routinely well
 * under 1.0 and would otherwise round away to nothing.
 */
class ScoreSystem(
    private val distancePointsPerMeter: Double = 10.0,
    private val pickupPoints: Long = 50,
    private val powerupPickupPoints: Long = 25,
) {
    private var rawScore: Double = 0.0

    var totalDistanceTraveled: Double = 0.0
        private set

    val score: Long get() = rawScore.toLong()

    fun addDistance(deltaMeters: Double, multiplier: Double) {
        require(deltaMeters >= 0.0) { "the ball never moves backward, got deltaMeters=$deltaMeters" }
        totalDistanceTraveled += deltaMeters
        rawScore += deltaMeters * distancePointsPerMeter * multiplier
    }

    fun addPlatesPickupPoints(multiplier: Double) {
        rawScore += pickupPoints * multiplier
    }

    fun addPowerupPickupPoints(multiplier: Double) {
        rawScore += powerupPickupPoints * multiplier
    }
}
