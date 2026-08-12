package com.orbitalsurf.app.render

import com.orbitalsurf.core.world.Chunk
import com.orbitalsurf.core.world.PickupKind
import com.orbitalsurf.core.world.PickupPlacement
import com.orbitalsurf.core.world.PowerupType
import com.orbitalsurf.core.world.SurfaceSampler
import com.orbitalsurf.core.world.SurfaceSegment
import kotlin.math.sqrt

/**
 * Builds every mesh in the game from plain geometry -- there's no art pipeline behind this
 * project (see the project README), so buildings, ramps, obstacles, pickups, and the ball
 * itself are all boxes/wedges/an octahedron/a sphere generated here at runtime, not modeled
 * assets. Face winding for every helper here is verified by hand to be counter-clockwise
 * (matching each face's stated outward normal) via the standard `cross(v1-v0, v2-v0)` check;
 * `GameRenderer` still leaves face culling disabled as a second line of defense, so even a
 * missed winding case stays visible (at worst mis-lit) rather than invisible.
 */
object ProceduralMeshFactory {
    // How far below a segment's walkable surface its rendered "block" extends -- purely a
    // visual thickness, not tied to the actual (unspecified) height of the building below.
    private const val WALL_DEPTH = 6f
    private const val BRIDGE_THICKNESS = 0.6f
    private const val ROOM_HEIGHT = 3.5f
    private const val OBSTACLE_INSET = 0.02f // shrinks the obstacle box slightly so it doesn't z-fight with the surface it sits on
    private const val PICKUP_RADIUS = 0.42f
    private const val SIDE_DARKEN_FACTOR = 0.62f

    private val ROOFTOP_PALETTE = listOf(
        floatArrayOf(0.78f, 0.58f, 0.46f, 1f), // terracotta
        floatArrayOf(0.58f, 0.53f, 0.63f, 1f), // mauve
        floatArrayOf(0.68f, 0.63f, 0.47f, 1f), // sandstone
        floatArrayOf(0.52f, 0.58f, 0.63f, 1f), // slate
        floatArrayOf(0.70f, 0.50f, 0.55f, 1f), // dusty rose
    )
    private val BRIDGE_COLOR = floatArrayOf(0.45f, 0.42f, 0.40f, 1f)
    private val OBSTACLE_COLOR = floatArrayOf(0.85f, 0.22f, 0.20f, 1f)
    private val CHECKPOINT_FLOOR_COLOR = floatArrayOf(0.36f, 0.56f, 0.86f, 1f)
    private val CHECKPOINT_CEILING_COLOR = floatArrayOf(0.20f, 0.30f, 0.50f, 1f)
    private val PLATES_COIN_COLOR = floatArrayOf(1.0f, 0.76f, 0.27f, 1f)

    private val POWERUP_COLORS: Map<String, FloatArray> = mapOf(
        PowerupType.ID_MAGNET to floatArrayOf(0.62f, 0.35f, 0.85f, 1f),
        PowerupType.ID_FLIGHT to floatArrayOf(0.30f, 0.80f, 0.85f, 1f),
        PowerupType.ID_SHIELD to floatArrayOf(0.35f, 0.85f, 0.55f, 1f),
        PowerupType.ID_PLATES_MULTIPLIER to floatArrayOf(1.0f, 0.85f, 0.35f, 1f),
        PowerupType.ID_SCORE_MULTIPLIER to floatArrayOf(0.95f, 0.40f, 0.55f, 1f),
    )

    fun rooftopColorFor(chunkIndex: Long): FloatArray {
        val i = chunkIndex.mod(ROOFTOP_PALETTE.size.toLong()).toInt()
        return ROOFTOP_PALETTE[i]
    }

    /** One combined static mesh for a whole chunk's segments + obstacles (NOT pickups -- see `ChunkMeshCache`). */
    fun buildChunkStructure(chunk: Chunk): Mesh {
        val builder = MeshBuilder()
        val roofColor = rooftopColorFor(chunk.index)
        val sideColor = darken(roofColor, SIDE_DARKEN_FACTOR)

        for (segment in chunk.segments) {
            when (segment) {
                is SurfaceSegment.RooftopSlab -> addBox(
                    builder,
                    segment.lateralMin.toFloat(), segment.lateralMax.toFloat(),
                    (segment.height - WALL_DEPTH).toFloat(), segment.height.toFloat(),
                    segment.startDistance.toFloat(), segment.endDistance.toFloat(),
                    roofColor, sideColor,
                )
                is SurfaceSegment.Bridge -> addBox(
                    builder,
                    segment.lateralMin.toFloat(), segment.lateralMax.toFloat(),
                    (segment.height - BRIDGE_THICKNESS).toFloat(), segment.height.toFloat(),
                    segment.startDistance.toFloat(), segment.endDistance.toFloat(),
                    BRIDGE_COLOR, darken(BRIDGE_COLOR, SIDE_DARKEN_FACTOR),
                )
                is SurfaceSegment.Ramp -> addSlopedBox(
                    builder,
                    segment.lateralMin.toFloat(), segment.lateralMax.toFloat(),
                    segment.startDistance.toFloat(), segment.endDistance.toFloat(),
                    segment.startHeight.toFloat(), segment.endHeight.toFloat(),
                    (minOf(segment.startHeight, segment.endHeight) - WALL_DEPTH).toFloat(),
                    roofColor, sideColor,
                )
                is SurfaceSegment.CheckpointInterior -> addCheckpointRoom(builder, segment)
                is SurfaceSegment.Gap -> Unit // nothing to render -- the hole IS the absence of geometry
            }
        }

        for (obstacle in chunk.obstacles) {
            val groundHeight = SurfaceSampler.sampleHeight(listOf(chunk), obstacle.distance, obstacle.lateral)?.height ?: 0.0
            addBox(
                builder,
                (obstacle.lateral - obstacle.halfWidthLateral + OBSTACLE_INSET).toFloat(),
                (obstacle.lateral + obstacle.halfWidthLateral - OBSTACLE_INSET).toFloat(),
                groundHeight.toFloat(),
                (groundHeight + obstacle.height).toFloat(),
                (obstacle.distance - obstacle.halfWidthDistance + OBSTACLE_INSET).toFloat(),
                (obstacle.distance + obstacle.halfWidthDistance - OBSTACLE_INSET).toFloat(),
                OBSTACLE_COLOR, darken(OBSTACLE_COLOR, SIDE_DARKEN_FACTOR),
            )
        }

        return builder.build()
    }

    /** One small mesh per pickup, cached individually so a collected pickup can simply be skipped when drawing (no rebuild needed). */
    fun buildPickup(pickup: PickupPlacement, groundHeight: Double): Mesh {
        val builder = MeshBuilder()
        val color = colorForPickup(pickup.kind)
        val cx = pickup.lateral.toFloat()
        val cy = (groundHeight + pickup.height).toFloat()
        val cz = pickup.distance.toFloat()
        addOctahedron(builder, cx, cy, cz, PICKUP_RADIUS, color)
        return builder.build()
    }

    /** A low-poly UV sphere in local space (centered at the origin), for the ball -- positioned/rotated per-frame via a model matrix. */
    fun buildSphere(radius: Float, color: FloatArray, stacks: Int = 10, slices: Int = 16): Mesh {
        val builder = MeshBuilder()
        for (i in 0 until stacks) {
            val lat0 = Math.PI * (-0.5 + i.toDouble() / stacks)
            val lat1 = Math.PI * (-0.5 + (i + 1).toDouble() / stacks)
            val y0 = Math.sin(lat0); val r0 = Math.cos(lat0)
            val y1 = Math.sin(lat1); val r1 = Math.cos(lat1)
            for (j in 0 until slices) {
                val lng0 = 2 * Math.PI * j.toDouble() / slices
                val lng1 = 2 * Math.PI * (j + 1).toDouble() / slices

                val p00 = spherePoint(lng0, r0, y0, radius)
                val p01 = spherePoint(lng1, r0, y0, radius)
                val p10 = spherePoint(lng0, r1, y1, radius)
                val p11 = spherePoint(lng1, r1, y1, radius)

                // Sphere normals are just the normalized local-space position -- correct
                // regardless of triangle winding, unlike the flat faces above. Each stack is
                // a quad split into 2 triangles, except at the poles where one half of the
                // ring collapses to a point (r=0): (p00,p10,p11) degenerates at the TOP pole
                // (i == stacks-1, where p10==p11), and (p00,p11,p01) degenerates at the
                // BOTTOM pole (i == 0, where p00==p01) -- so each is skipped at the stack
                // where IT would be the degenerate one, not the other way around.
                if (i < stacks - 1) builder.addTriangle(p00, p10, p11, normalize(p00), normalize(p10), normalize(p11), color)
                if (i > 0) builder.addTriangle(p00, p11, p01, normalize(p00), normalize(p11), normalize(p01), color)
            }
        }
        return builder.build()
    }

    private fun spherePoint(longitude: Double, ringRadius: Double, y: Double, radius: Float): FloatArray {
        val x = Math.cos(longitude) * ringRadius
        val z = Math.sin(longitude) * ringRadius
        return floatArrayOf((x * radius).toFloat(), (y * radius).toFloat(), (z * radius).toFloat())
    }

    private fun addCheckpointRoom(builder: MeshBuilder, segment: SurfaceSegment.CheckpointInterior) {
        addBox(
            builder,
            segment.lateralMin.toFloat(), segment.lateralMax.toFloat(),
            (segment.height - WALL_DEPTH).toFloat(), segment.height.toFloat(),
            segment.startDistance.toFloat(), segment.endDistance.toFloat(),
            CHECKPOINT_FLOOR_COLOR, darken(CHECKPOINT_FLOOR_COLOR, SIDE_DARKEN_FACTOR),
        )
        val ceilingY = (segment.height + ROOM_HEIGHT).toFloat()
        builder.addQuad(
            floatArrayOf(segment.lateralMin.toFloat(), ceilingY, segment.startDistance.toFloat()),
            floatArrayOf(segment.lateralMin.toFloat(), ceilingY, segment.endDistance.toFloat()),
            floatArrayOf(segment.lateralMax.toFloat(), ceilingY, segment.endDistance.toFloat()),
            floatArrayOf(segment.lateralMax.toFloat(), ceilingY, segment.startDistance.toFloat()),
            floatArrayOf(0f, -1f, 0f),
            CHECKPOINT_CEILING_COLOR,
        )
    }

    private fun colorForPickup(kind: PickupKind): FloatArray = when (kind) {
        PickupKind.PlatesCoin -> PLATES_COIN_COLOR
        is PickupKind.Powerup -> POWERUP_COLORS[kind.type.id] ?: PLATES_COIN_COLOR
    }

    /** An axis-aligned box from 8 corners -- see the class kdoc re: winding verification. */
    private fun addBox(
        builder: MeshBuilder,
        minX: Float, maxX: Float,
        minY: Float, maxY: Float,
        minZ: Float, maxZ: Float,
        topColor: FloatArray,
        sideColor: FloatArray,
    ) {
        val p000 = floatArrayOf(minX, minY, minZ)
        val p100 = floatArrayOf(maxX, minY, minZ)
        val p010 = floatArrayOf(minX, maxY, minZ)
        val p110 = floatArrayOf(maxX, maxY, minZ)
        val p001 = floatArrayOf(minX, minY, maxZ)
        val p101 = floatArrayOf(maxX, minY, maxZ)
        val p011 = floatArrayOf(minX, maxY, maxZ)
        val p111 = floatArrayOf(maxX, maxY, maxZ)

        builder.addQuad(p010, p011, p111, p110, floatArrayOf(0f, 1f, 0f), topColor) // top
        builder.addQuad(p000, p100, p101, p001, floatArrayOf(0f, -1f, 0f), sideColor) // bottom
        builder.addQuad(p000, p010, p110, p100, floatArrayOf(0f, 0f, -1f), sideColor) // -Z
        builder.addQuad(p101, p111, p011, p001, floatArrayOf(0f, 0f, 1f), sideColor) // +Z
        builder.addQuad(p001, p011, p010, p000, floatArrayOf(-1f, 0f, 0f), sideColor) // -X
        builder.addQuad(p100, p110, p111, p101, floatArrayOf(1f, 0f, 0f), sideColor) // +X
    }

    /**
     * A box whose top face follows a slope from `startTopY` (at `startZ`) to `endTopY` (at
     * `endZ`), for ramps. The top face's normal matches `SurfaceSampler`'s `(0, 1, -slope)`
     * convention exactly, so a ramp looks lit the same way its physics slope behaves.
     */
    private fun addSlopedBox(
        builder: MeshBuilder,
        minX: Float, maxX: Float,
        startZ: Float, endZ: Float,
        startTopY: Float, endTopY: Float,
        bottomY: Float,
        topColor: FloatArray,
        sideColor: FloatArray,
    ) {
        val tp00 = floatArrayOf(minX, startTopY, startZ)
        val tp01 = floatArrayOf(minX, endTopY, endZ)
        val tp11 = floatArrayOf(maxX, endTopY, endZ)
        val tp10 = floatArrayOf(maxX, startTopY, startZ)

        val bp00 = floatArrayOf(minX, bottomY, startZ)
        val bp01 = floatArrayOf(minX, bottomY, endZ)
        val bp11 = floatArrayOf(maxX, bottomY, endZ)
        val bp10 = floatArrayOf(maxX, bottomY, startZ)

        val span = endZ - startZ
        val slope = if (span != 0f) (endTopY - startTopY) / span else 0f
        val topNormal = normalize(floatArrayOf(0f, 1f, -slope))

        builder.addQuad(tp00, tp01, tp11, tp10, topNormal, topColor)
        builder.addQuad(bp00, bp10, bp11, bp01, floatArrayOf(0f, -1f, 0f), sideColor)
        builder.addQuad(bp00, tp00, tp10, bp10, floatArrayOf(0f, 0f, -1f), sideColor)
        builder.addQuad(bp11, tp11, tp01, bp01, floatArrayOf(0f, 0f, 1f), sideColor)
        builder.addQuad(bp01, tp01, tp00, bp00, floatArrayOf(-1f, 0f, 0f), sideColor)
        builder.addQuad(bp10, tp10, tp11, bp11, floatArrayOf(1f, 0f, 0f), sideColor)
    }

    /** A small 6-vertex bipyramid (two square pyramids base-to-base) marking a pickup -- distinct silhouette from the boxy environment. */
    private fun addOctahedron(builder: MeshBuilder, cx: Float, cy: Float, cz: Float, r: Float, color: FloatArray) {
        val center = floatArrayOf(cx, cy, cz)
        val top = floatArrayOf(cx, cy + r, cz)
        val bottom = floatArrayOf(cx, cy - r, cz)
        val a = floatArrayOf(cx + r, cy, cz)
        val b = floatArrayOf(cx, cy, cz + r)
        val c = floatArrayOf(cx - r, cy, cz)
        val d = floatArrayOf(cx, cy, cz - r)

        val ring = listOf(a, b, c, d)
        for (i in ring.indices) {
            val v0 = ring[i]
            val v1 = ring[(i + 1) % ring.size]
            addOutwardFace(builder, center, top, v0, v1, color)
            addOutwardFace(builder, center, bottom, v1, v0, color)
        }
    }

    /**
     * Adds a triangle guaranteed to face away from [center] -- computes the natural winding's
     * normal, and if it points inward, swaps two vertices and flips the normal instead of
     * trusting a hand-derived winding order (unlike the box/ramp helpers above, which are
     * simple enough to verify directly).
     */
    private fun addOutwardFace(builder: MeshBuilder, center: FloatArray, a: FloatArray, b: FloatArray, c: FloatArray, color: FloatArray) {
        val normal = triangleNormal(a, b, c)
        val centroid = floatArrayOf((a[0] + b[0] + c[0]) / 3f, (a[1] + b[1] + c[1]) / 3f, (a[2] + b[2] + c[2]) / 3f)
        val outward = floatArrayOf(centroid[0] - center[0], centroid[1] - center[1], centroid[2] - center[2])
        val facingOutward = normal[0] * outward[0] + normal[1] * outward[1] + normal[2] * outward[2] >= 0f
        if (facingOutward) {
            builder.addTriangle(a, b, c, normal, normal, normal, color)
        } else {
            val flipped = floatArrayOf(-normal[0], -normal[1], -normal[2])
            builder.addTriangle(a, c, b, flipped, flipped, flipped, color)
        }
    }

    private fun triangleNormal(a: FloatArray, b: FloatArray, c: FloatArray): FloatArray {
        val ux = b[0] - a[0]; val uy = b[1] - a[1]; val uz = b[2] - a[2]
        val vx = c[0] - a[0]; val vy = c[1] - a[1]; val vz = c[2] - a[2]
        val nx = uy * vz - uz * vy
        val ny = uz * vx - ux * vz
        val nz = ux * vy - uy * vx
        return normalize(floatArrayOf(nx, ny, nz))
    }

    private fun normalize(v: FloatArray): FloatArray {
        val len = sqrt((v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble()).toFloat()
        return if (len < 1e-6f) v else floatArrayOf(v[0] / len, v[1] / len, v[2] / len)
    }

    private fun darken(color: FloatArray, factor: Float): FloatArray =
        floatArrayOf(color[0] * factor, color[1] * factor, color[2] * factor, color[3])
}
