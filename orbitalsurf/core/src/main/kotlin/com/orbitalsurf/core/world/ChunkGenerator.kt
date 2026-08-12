package com.orbitalsurf.core.world

import com.orbitalsurf.core.math.MathUtils
import com.orbitalsurf.core.math.SeededSequence
import kotlin.math.abs
import kotlin.math.sin

/**
 * Turns `(seed, chunkIndex)` into a `Chunk` -- and only that: no field on `ChunkGenerator`
 * carries state between calls, and no chunk's generation reads another chunk's *generated
 * output*. That's what makes three things all work correctly at once:
 *  - infinite streaming (`WorldStreamer` can generate chunks arbitrarily far ahead),
 *  - eviction/regeneration (a chunk that scrolled off and gets regenerated later is
 *    byte-identical to the first time), and
 *  - Headstarts (`WorldStreamer.reset` can start the window at chunk 4000 with no need to
 *    have ever generated chunks 0..3999).
 *
 * Continuity across a chunk boundary (so the rooftop height/lateral position on one side of
 * a seam matches the other side) is achieved without any of that: [boundaryHeight] and
 * [lateralCenterAt] are themselves pure functions of a boundary index, hashed independently
 * of chunk content. Chunk `i` and chunk `i+1` each independently compute the same value for
 * boundary `i+1` and therefore always agree on the seam, even though neither one is generated
 * "from" the other.
 */
object ChunkGenerator {
    private const val RAMP_LENGTH = 6.0
    private const val MIN_HEIGHT = 3.0
    private const val BASE_HEIGHT = 12.0
    private const val WAVE_AMPLITUDE = 4.0
    private const val WAVE_FREQUENCY = 2.0 * Math.PI / 240.0
    private const val LATERAL_DRIFT_RANGE = 5.0
    private const val WIDTH_MIN = 7.0
    private const val WIDTH_MAX = 13.0
    private const val HEIGHT_EPSILON = 0.05
    private const val CONTENT_STEP = 2.0
    private const val GAP_CHANCE_MAX = 0.35
    private const val EDGE_MARGIN = 1.0
    private const val PER_STEP_SCALE = 0.15

    // Independent hash streams for each kind of draw, keyed by (category, index) so that,
    // say, tweaking obstacle-placement logic can never perturb boundary heights and break
    // continuity. Index here is a chunk or chunk-boundary index, not a within-stream counter.
    private const val CATEGORY_STRIDE = 10_000_000L
    private const val CATEGORY_BOUNDARY_HEIGHT = 1L
    private const val CATEGORY_LATERAL_CENTER = 2L
    private const val CATEGORY_WIDTH = 3L
    private const val CATEGORY_CONTENT = 4L

    fun generate(seed: Long, chunkIndex: Long): Chunk {
        val startDistance = chunkIndex * WorldConstants.CHUNK_LENGTH
        val endDistance = startDistance + WorldConstants.CHUNK_LENGTH
        val difficulty = DifficultyCurve.paramsAt(startDistance)

        val startHeight = boundaryHeight(seed, chunkIndex)
        val endHeight = boundaryHeight(seed, chunkIndex + 1)
        val startLateral = lateralCenterAt(seed, chunkIndex)
        val endLateral = lateralCenterAt(seed, chunkIndex + 1)
        val halfWidth = widthAt(seed, chunkIndex) / 2.0
        fun lateralAt(distance: Double): Double {
            val t = MathUtils.invLerp(startDistance, endDistance, distance)
            return MathUtils.lerp(startLateral, endLateral, t)
        }

        val checkpointNumber = CheckpointSchedule.checkpointAt(chunkIndex)
        val contentSeq = SeededSequence(seed, streamIndex(CATEGORY_CONTENT, chunkIndex))

        val segments = mutableListOf<SurfaceSegment>()
        val obstacles = mutableListOf<Obstacle>()
        val pickups = mutableListOf<PickupPlacement>()

        if (checkpointNumber != null) {
            buildCheckpointChunk(
                checkpointNumber, startDistance, endDistance, startHeight, endHeight,
                ::lateralAt, halfWidth, contentSeq, chunkIndex, segments, pickups,
            )
        } else {
            buildRegularChunk(
                startDistance, endDistance, startHeight, endHeight, ::lateralAt, halfWidth,
                difficulty, contentSeq, segments,
            )
            scatterContent(segments, difficulty, contentSeq, chunkIndex, obstacles, pickups)
        }

        return Chunk(chunkIndex, startDistance, endDistance, segments, obstacles, pickups, checkpointNumber)
    }

    private fun buildCheckpointChunk(
        checkpointNumber: Int,
        startDistance: Double,
        endDistance: Double,
        startHeight: Double,
        endHeight: Double,
        lateralAt: (Double) -> Double,
        halfWidth: Double,
        contentSeq: SeededSequence,
        chunkIndex: Long,
        segments: MutableList<SurfaceSegment>,
        pickups: MutableList<PickupPlacement>,
    ) {
        val interiorHeight = (startHeight + endHeight) / 2.0
        val rampInEnd = (startDistance + RAMP_LENGTH).coerceAtMost(endDistance)
        val rampOutStart = (endDistance - RAMP_LENGTH).coerceAtLeast(rampInEnd)

        segments += rampOrSlab(startDistance, rampInEnd, startHeight, interiorHeight, lateralAt, halfWidth)
        segments += SurfaceSegment.CheckpointInterior(
            startDistance = rampInEnd,
            endDistance = rampOutStart,
            lateralMin = lateralAt((rampInEnd + rampOutStart) / 2.0) - halfWidth,
            lateralMax = lateralAt((rampInEnd + rampOutStart) / 2.0) + halfWidth,
            height = interiorHeight,
            checkpointIndex = checkpointNumber,
        )
        segments += rampOrSlab(rampOutStart, endDistance, interiorHeight, endHeight, lateralAt, halfWidth)

        // A guaranteed-safe passage still deserves a small reward for finding it.
        val rewardCount = contentSeq.nextInt(3)
        repeat(rewardCount) { i ->
            val distance = contentSeq.nextInRange(rampInEnd, rampOutStart)
            val lateral = safeLateral(contentSeq, lateralAt(distance), halfWidth)
            pickups += PickupPlacement(
                id = "chk$chunkIndex-reward-$i",
                distance = distance,
                lateral = lateral,
                height = 1.2,
                kind = PickupKind.PlatesCoin,
            )
        }
    }

    private fun buildRegularChunk(
        startDistance: Double,
        endDistance: Double,
        startHeight: Double,
        endHeight: Double,
        lateralAt: (Double) -> Double,
        halfWidth: Double,
        difficulty: DifficultyParams,
        contentSeq: SeededSequence,
        segments: MutableList<SurfaceSegment>,
    ) {
        val gapChance = MathUtils.lerp(0.10, GAP_CHANCE_MAX, difficulty.obstacleDensity / OBSTACLE_DENSITY_HARD_REFERENCE)
        val placeGap = contentSeq.nextBool(gapChance)
        if (placeGap) {
            val gapWidth = contentSeq.nextInRange(difficulty.gapWidthMin, difficulty.gapWidthMax)
            val gapCenter = contentSeq.nextInRange(
                startDistance + WorldConstants.CHUNK_LENGTH * 0.35,
                startDistance + WorldConstants.CHUNK_LENGTH * 0.65,
            )
            val gapStart = gapCenter - gapWidth / 2.0
            val gapEnd = gapCenter + gapWidth / 2.0

            segments += SurfaceSegment.RooftopSlab(
                startDistance, gapStart,
                lateralAt(startDistance) - halfWidth, lateralAt(startDistance) + halfWidth,
                startHeight,
            )
            segments += SurfaceSegment.Gap(
                gapStart, gapEnd,
                lateralAt(gapCenter) - halfWidth, lateralAt(gapCenter) + halfWidth,
            )
            segments += rampOrSlab(gapEnd, endDistance, startHeight, endHeight, lateralAt, halfWidth)
        } else {
            segments += rampOrSlab(startDistance, endDistance, startHeight, endHeight, lateralAt, halfWidth)
        }
    }

    private fun scatterContent(
        segments: List<SurfaceSegment>,
        difficulty: DifficultyParams,
        contentSeq: SeededSequence,
        chunkIndex: Long,
        obstacles: MutableList<Obstacle>,
        pickups: MutableList<PickupPlacement>,
    ) {
        var distance = segments.firstOrNull()?.startDistance ?: return
        val end = segments.last().endDistance
        var i = 0
        while (distance < end) {
            val segment = segments.firstOrNull { it.containsDistance(distance) }
            if (segment != null && segment !is SurfaceSegment.Gap) {
                val lateral = safeLateral(contentSeq, (segment.lateralMin + segment.lateralMax) / 2.0, (segment.lateralMax - segment.lateralMin) / 2.0)
                // difficulty.*Density is a 0..1 weight; PER_STEP_SCALE keeps the actual
                // per-2m-step placement chance from saturating (an obstacle at nearly every
                // step even at max difficulty would make the path unplayable).
                when {
                    contentSeq.nextBool(difficulty.obstacleDensity * PER_STEP_SCALE) -> {
                        obstacles += Obstacle(
                            id = "chk$chunkIndex-obs-$i",
                            distance = distance,
                            lateral = lateral,
                            halfWidthLateral = 0.6,
                            halfWidthDistance = 0.6,
                            height = 1.2,
                        )
                    }
                    contentSeq.nextBool(difficulty.powerupDensity * PER_STEP_SCALE) -> {
                        val kind = if (contentSeq.nextBool(0.6)) {
                            PickupKind.PlatesCoin
                        } else {
                            PickupKind.Powerup(contentSeq.pick(PowerupType.defaults()))
                        }
                        pickups += PickupPlacement(
                            id = "chk$chunkIndex-pk-$i",
                            distance = distance,
                            lateral = lateral,
                            height = 1.0,
                            kind = kind,
                        )
                    }
                }
            }
            distance += CONTENT_STEP
            i++
        }
    }

    private fun safeLateral(seq: SeededSequence, center: Double, half: Double): Double {
        val usable = half - EDGE_MARGIN
        return if (usable <= 0.0) center else seq.nextInRange(center - usable, center + usable)
    }

    private fun rampOrSlab(
        segStart: Double,
        segEnd: Double,
        hStart: Double,
        hEnd: Double,
        lateralAt: (Double) -> Double,
        halfWidth: Double,
    ): SurfaceSegment {
        val lat = lateralAt((segStart + segEnd) / 2.0)
        return if (abs(hEnd - hStart) < HEIGHT_EPSILON) {
            SurfaceSegment.RooftopSlab(segStart, segEnd, lat - halfWidth, lat + halfWidth, hStart)
        } else {
            SurfaceSegment.Ramp(segStart, segEnd, lat - halfWidth, lat + halfWidth, hStart, hEnd)
        }
    }

    /** The smooth, non-random skyline envelope -- gives the city a rolling baseline before per-boundary jitter is added. */
    private fun expectedHeightAt(distance: Double): Double =
        BASE_HEIGHT + WAVE_AMPLITUDE * sin(distance * WAVE_FREQUENCY)

    /** Rooftop height at chunk boundary [boundaryIndex] (i.e. the seam between chunk boundaryIndex-1 and boundaryIndex). */
    private fun boundaryHeight(seed: Long, boundaryIndex: Long): Double {
        val distance = boundaryIndex * WorldConstants.CHUNK_LENGTH
        val variance = DifficultyCurve.paramsAt(distance).buildingHeightVariance
        val seq = SeededSequence(seed, streamIndex(CATEGORY_BOUNDARY_HEIGHT, boundaryIndex))
        val jitter = seq.nextInRange(-variance, variance)
        return (expectedHeightAt(distance) + jitter).coerceAtLeast(MIN_HEIGHT)
    }

    private fun lateralCenterAt(seed: Long, boundaryIndex: Long): Double {
        val seq = SeededSequence(seed, streamIndex(CATEGORY_LATERAL_CENTER, boundaryIndex))
        return seq.nextInRange(-LATERAL_DRIFT_RANGE, LATERAL_DRIFT_RANGE)
    }

    private fun widthAt(seed: Long, chunkIndex: Long): Double {
        val seq = SeededSequence(seed, streamIndex(CATEGORY_WIDTH, chunkIndex))
        return seq.nextInRange(WIDTH_MIN, WIDTH_MAX)
    }

    private fun streamIndex(category: Long, index: Long): Long = category * CATEGORY_STRIDE + index

    // Reference obstacleDensity value at RAMP_UP_DISTANCE (fully ramped), used only to
    // normalize the gap-chance lerp above onto the same 0..1 scale as difficulty itself.
    private val OBSTACLE_DENSITY_HARD_REFERENCE = DifficultyCurve.paramsAt(1_000_000.0).obstacleDensity
}
