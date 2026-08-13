package com.vaultgame.app.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.vaultgame.app.data.SkinVisual
import com.vaultgame.core.physics.PhysicsConfig
import com.vaultgame.core.physics.PlayerState
import com.vaultgame.core.powerups.ActivePowerups
import com.vaultgame.core.powerups.PowerupType
import com.vaultgame.core.session.GameSession
import com.vaultgame.core.world.AvoidAction
import com.vaultgame.core.world.Obstacle
import com.vaultgame.core.world.ObstacleType
import com.vaultgame.core.world.Pickup
import com.vaultgame.core.world.PickupType
import kotlin.math.sin

/**
 * Draws one frame of the run to a [Canvas]. There's no art pipeline behind this game -- every
 * shape (skyline, rooftops, obstacles, the runner) is flat-color procedural geometry projected
 * with a cheap pseudo-3D perspective (see [project]), the same "road narrows to a horizon"
 * trick classic 2D racers use, rather than a real 3D pipeline.
 */
class GameRenderer(private val skinVisual: SkinVisual) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cachedSkyShader: Shader? = null
    private var cachedSkyHeight = -1

    fun draw(canvas: Canvas, session: GameSession, widthPx: Int, heightPx: Int, elapsedSeconds: Double) {
        drawSky(canvas, widthPx, heightPx)
        drawSkyline(canvas, widthPx, heightPx, session.playerState.distance)
        drawRoad(canvas, widthPx, heightPx)

        val powerups = session.activePowerupsSnapshot()
        val visibleObstacles = session.visibleSegments.flatMap { it.obstacles }
        val visiblePickups = session.visibleSegments.flatMap { it.pickups }

        // Painter's algorithm: draw farthest-first so nearer geometry overlaps it correctly.
        val forwardSorted = (visibleObstacles.map { DrawItem.ObstacleItem(it) } + visiblePickups.map { DrawItem.PickupItem(it) })
            .filter { it.forward(session.playerState.distance) in 0.0..VIEW_DEPTH_METERS }
            .sortedByDescending { it.forward(session.playerState.distance) }

        for (item in forwardSorted) {
            when (item) {
                is DrawItem.ObstacleItem -> drawObstacle(canvas, widthPx, heightPx, item.obstacle, session.playerState.distance)
                is DrawItem.PickupItem -> if (!item.pickup.collected) {
                    drawPickup(canvas, widthPx, heightPx, item.pickup, session.playerState.distance, elapsedSeconds)
                }
            }
        }

        drawPlayer(canvas, widthPx, heightPx, session.playerState, powerups, elapsedSeconds)
    }

    private sealed class DrawItem {
        abstract fun forward(playerDistance: Double): Double
        data class ObstacleItem(val obstacle: Obstacle) : DrawItem() {
            override fun forward(playerDistance: Double) = obstacle.distance - playerDistance
        }
        data class PickupItem(val pickup: Pickup) : DrawItem() {
            override fun forward(playerDistance: Double) = pickup.distance - playerDistance
        }
    }

    // ---- Perspective projection -------------------------------------------------------------

    /** 1.0 at the player's feet, approaching 0 toward the horizon. */
    private fun perspectiveFactor(forward: Double): Double =
        CAMERA_DEPTH / (CAMERA_DEPTH + forward.coerceAtLeast(0.0))

    /** Projects a world position (lane offset in lane-units, forward distance in meters, height
     * in world units) to screen (x, y, scale). */
    private fun project(laneOffset: Double, forward: Double, worldHeight: Double, w: Int, h: Int): Triple<Float, Float, Float> {
        val f = perspectiveFactor(forward)
        val horizonY = h * HORIZON_FRACTION
        val groundY = horizonY + (h - horizonY) * f
        val screenX = w / 2.0 + laneOffset * (w * 0.34) * f
        val screenY = groundY - worldHeight * (h * 0.9) * f
        return Triple(screenX.toFloat(), screenY.toFloat(), f.toFloat())
    }

    // ---- Background ----------------------------------------------------------------------

    private fun drawSky(canvas: Canvas, w: Int, h: Int) {
        if (cachedSkyShader == null || cachedSkyHeight != h) {
            cachedSkyShader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(Color.parseColor("#2B2350"), Color.parseColor("#FF6F3C"), Color.parseColor("#FFC24D")),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
            cachedSkyHeight = h
        }
        paint.shader = cachedSkyShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
    }

    private fun drawSkyline(canvas: Canvas, w: Int, h: Int, distance: Double) {
        paint.color = Color.parseColor("#161A2E")
        val horizonY = (h * HORIZON_FRACTION).toFloat()
        val baseY = horizonY + h * 0.06f
        // Slow horizontal drift tied to distance for a cheap parallax sense of motion.
        val scroll = (distance * 6.0) % (w / 4.0)
        val blockWidth = w / 7f
        var x = -scroll.toFloat() - blockWidth
        var i = 0
        while (x < w + blockWidth) {
            val blockHeight = (h * (0.05 + 0.04 * ((i * 37) % 5))).toFloat()
            canvas.drawRect(x, baseY - blockHeight, x + blockWidth * 0.8f, baseY, paint)
            x += blockWidth
            i++
        }
    }

    private fun drawRoad(canvas: Canvas, w: Int, h: Int) {
        // The rooftop surface: a trapezoid from the horizon to the bottom of the screen.
        val horizonY = (h * HORIZON_FRACTION).toFloat()
        val (leftFarX, _, _) = project(-1.7, VIEW_DEPTH_METERS, 0.0, w, h)
        val (rightFarX, _, _) = project(1.7, VIEW_DEPTH_METERS, 0.0, w, h)
        val (leftNearX, _, _) = project(-1.7, 0.0, 0.0, w, h)
        val (rightNearX, _, _) = project(1.7, 0.0, 0.0, w, h)

        paint.color = Color.parseColor("#3A3358")
        val path = android.graphics.Path().apply {
            moveTo(leftFarX, horizonY)
            lineTo(rightFarX, horizonY)
            lineTo(rightNearX, h.toFloat())
            lineTo(leftNearX, h.toFloat())
            close()
        }
        canvas.drawPath(path, paint)

        // Lane divider lines at -0.5 and +0.5 lane offsets.
        paint.color = Color.parseColor("#FFD9A0")
        paint.strokeWidth = 3f
        for (laneOffset in listOf(-0.5, 0.5)) {
            val (nx, ny, _) = project(laneOffset, 0.0, 0.0, w, h)
            val (fx, fy, _) = project(laneOffset, VIEW_DEPTH_METERS, 0.0, w, h)
            canvas.drawLine(nx, ny, fx, fy, paint)
        }
    }

    // ---- Obstacles / pickups ---------------------------------------------------------------

    private fun drawObstacle(canvas: Canvas, w: Int, h: Int, obstacle: Obstacle, playerDistance: Double) {
        val forward = obstacle.distance - playerDistance
        val lanes = if (obstacle.type.spansAllLanes) listOf(-1.0, 0.0, 1.0) else listOf(obstacle.lane.index.toDouble())
        val obstacleColor = when (obstacle.type) {
            ObstacleType.LOW_VENT -> Color.parseColor("#7A6BC9")
            ObstacleType.OVERHEAD_PIPE -> Color.parseColor("#5FA8D3")
            ObstacleType.CRATE_STACK -> Color.parseColor("#B5793B")
            ObstacleType.ROOF_GAP -> Color.parseColor("#0E0C1A")
            ObstacleType.CLOTHESLINE -> Color.parseColor("#E8544B")
        }
        paint.color = obstacleColor
        for (laneOffset in lanes) {
            val heightAboveGround = when (obstacle.type.avoidedBy) {
                AvoidAction.SLIDE -> 0.55 // waist-height obstacle you duck under
                AvoidAction.JUMP -> 0.0 // low, ground-level
                AvoidAction.SWITCH_LANE_ONLY -> 0.0
            }
            val (cx, cy, scale) = project(laneOffset, forward, heightAboveGround, w, h)
            val size = (w * 0.11 * scale).toFloat().coerceAtLeast(2f)
            when (obstacle.type.avoidedBy) {
                AvoidAction.SLIDE -> canvas.drawRect(cx - size, cy - size * 0.22f, cx + size, cy + size * 0.22f, paint)
                AvoidAction.JUMP -> canvas.drawRoundRect(cx - size * 0.6f, cy - size * 0.7f, cx + size * 0.6f, cy, size * 0.15f, size * 0.15f, paint)
                AvoidAction.SWITCH_LANE_ONLY -> {
                    canvas.drawRect(cx - size * 0.65f, cy - size * 1.3f, cx + size * 0.65f, cy, paint)
                    canvas.drawRect(cx - size * 0.65f, cy - size * 0.65f, cx + size * 0.65f, cy - size * 0.55f, Paint(paint).apply { color = Color.parseColor("#8C5A2B") })
                }
            }
        }
    }

    private fun drawPickup(canvas: Canvas, w: Int, h: Int, pickup: Pickup, playerDistance: Double, elapsedSeconds: Double) {
        val forward = pickup.distance - playerDistance
        val bob = (sin(elapsedSeconds * 4.0 + pickup.distance) * 0.05).toFloat()
        val (cx, cy, scale) = project(pickup.lane.index.toDouble(), forward, 0.45 + bob, w, h)
        val radius = (w * 0.028 * scale).toFloat().coerceAtLeast(2f)
        when (val type = pickup.type) {
            is PickupType.Coin -> {
                paint.color = Color.parseColor("#FFD65A")
                canvas.drawCircle(cx, cy, radius, paint)
            }
            is PickupType.Powerup -> {
                paint.color = powerupColor(type.type)
                canvas.drawCircle(cx, cy, radius * 1.4f, paint)
                paint.color = Color.parseColor("#1C1B33")
                canvas.drawCircle(cx, cy, radius * 0.7f, paint)
            }
        }
    }

    private fun powerupColor(type: PowerupType): Int = when (type) {
        PowerupType.MAGNET -> Color.parseColor("#7A6BC9")
        PowerupType.JETPACK -> Color.parseColor("#5FA8D3")
        PowerupType.SPEED_BOOST -> Color.parseColor("#4CC9A0")
        PowerupType.SHIELD -> Color.parseColor("#8CE0FF")
        PowerupType.COIN_MULTIPLIER -> Color.parseColor("#FFD65A")
    }

    // ---- Player ------------------------------------------------------------------------------

    private fun drawPlayer(canvas: Canvas, w: Int, h: Int, state: PlayerState, powerups: ActivePowerups, elapsedSeconds: Double) {
        val jumpHeight = if (state.isJumping) {
            val t = (state.jumpElapsed / PhysicsConfig.JUMP_DURATION).coerceIn(0.0, 1.0)
            PhysicsConfig.JUMP_HEIGHT * 4.0 * t * (1.0 - t) // simple parabolic arc
        } else 0.0
        val crouch = state.isSliding
        val (cx, groundY, scale) = project(state.laneOffset, 0.0, jumpHeight, w, h)

        val bodyHeight = (h * (if (crouch) 0.10 else 0.16) * scale).toFloat().coerceAtLeast(6f)
        val bodyWidth = (h * 0.09 * scale).toFloat().coerceAtLeast(5f)

        if (powerups.isActive(PowerupType.SHIELD)) {
            paint.color = Color.argb(90, 140, 224, 255)
            canvas.drawCircle(cx, groundY - bodyHeight * 0.6f, bodyWidth * 1.6f, paint)
        }
        if (powerups.isFlying()) {
            paint.color = Color.argb(140, 95, 168, 211)
            canvas.drawRect(cx - bodyWidth * 0.5f, groundY - bodyHeight * 0.3f, cx + bodyWidth * 0.5f, groundY + bodyHeight * 0.4f, paint)
        }

        // Legs
        paint.color = skinVisual.shoeColor
        canvas.drawRect(cx - bodyWidth * 0.35f, groundY - bodyHeight * 0.25f, cx - bodyWidth * 0.05f, groundY, paint)
        canvas.drawRect(cx + bodyWidth * 0.05f, groundY - bodyHeight * 0.25f, cx + bodyWidth * 0.35f, groundY, paint)

        // Jacket (torso)
        paint.color = skinVisual.jacketColor
        val torsoTop = groundY - bodyHeight
        canvas.drawRoundRect(cx - bodyWidth * 0.5f, torsoTop, cx + bodyWidth * 0.5f, groundY - bodyHeight * 0.22f, bodyWidth * 0.2f, bodyWidth * 0.2f, paint)

        // Jacket accent stripe
        paint.color = skinVisual.jacketAccentColor
        canvas.drawRect(cx - bodyWidth * 0.5f, torsoTop + bodyHeight * 0.35f, cx + bodyWidth * 0.5f, torsoTop + bodyHeight * 0.45f, paint)

        // Beanie (head)
        paint.color = skinVisual.beanieColor
        val headRadius = bodyWidth * 0.45f
        canvas.drawCircle(cx, torsoTop - headRadius * 0.7f, headRadius, paint)

        if (powerups.isActive(PowerupType.SPEED_BOOST)) {
            paint.color = Color.argb(120, 76, 201, 160)
            for (i in 1..3) {
                canvas.drawLine(cx, groundY - bodyHeight * 0.5f, cx, groundY - bodyHeight * 0.5f + bodyHeight * 0.6f + i * 6f, paint)
            }
        }
    }

    companion object {
        const val VIEW_DEPTH_METERS = 24.0
        private const val CAMERA_DEPTH = 5.0
        private const val HORIZON_FRACTION = 0.38
    }
}
