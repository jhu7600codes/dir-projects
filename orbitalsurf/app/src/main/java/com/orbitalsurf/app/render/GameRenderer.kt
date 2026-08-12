package com.orbitalsurf.app.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.orbitalsurf.app.data.SkinVisual
import com.orbitalsurf.app.input.GameInputState
import com.orbitalsurf.core.physics.PhysicsConfig
import com.orbitalsurf.core.session.GameSession
import com.orbitalsurf.core.session.RunFrameResult
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI

/**
 * Drives one [GameSession] every frame: steps physics with whatever input
 * [GameInputState] has accumulated since the last frame, syncs [ChunkMeshCache] with the
 * session's current streamed window, and draws the scene. Runs entirely on the GLSurfaceView's
 * dedicated GL thread -- [onFrame] is invoked from that same thread, so anything it does that
 * touches Views or Activities must hop back to the main thread itself.
 */
class GameRenderer(
    private val gameSession: GameSession,
    private val inputState: GameInputState,
    private val skinVisual: SkinVisual,
    private val onFrame: (RunFrameResult) -> Unit,
) : GLSurfaceView.Renderer {

    private lateinit var shader: ShaderProgram
    private lateinit var ballMesh: Mesh
    private val camera = Camera()
    private val chunkMeshCache = ChunkMeshCache()
    private val trailRenderer = TrailRenderer()

    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private var lastFrameTimeNanos = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(SKY_TOP[0], SKY_TOP[1], SKY_TOP[2], 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        // Face culling deliberately left disabled: the one line of defense if any hand-authored
        // winding in ProceduralMeshFactory is wrong is that the face stays visible (at worst
        // mis-lit) instead of silently disappearing. See that file's kdoc.

        shader = ShaderProgram(Shaders.VERTEX_SHADER, Shaders.FRAGMENT_SHADER)
        ballMesh = ProceduralMeshFactory.buildSphere(PhysicsConfig.DEFAULT.ballRadius.toFloat(), skinVisual.baseColor)
        lastFrameTimeNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        camera.aspectRatio = width.toFloat() / height.toFloat().coerceAtLeast(1f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameTimeNanos) / NANOS_PER_SECOND).coerceIn(0.0, MAX_DT_SECONDS)
        lastFrameTimeNanos = now

        val frame = gameSession.update(dt, inputState.consumeInput())
        camera.update(frame.ballState, dt)
        trailRenderer.update(frame.ballState)
        chunkMeshCache.sync(gameSession.activeChunks)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        shader.use()
        applyLightingAndFog()

        drawChunks()
        drawPickups()
        drawTrail()
        drawBall(frame)

        onFrame(frame)
    }

    private fun applyLightingAndFog() {
        GLES20.glUniform3f(shader.uniformLightDirection, LIGHT_DIRECTION[0], LIGHT_DIRECTION[1], LIGHT_DIRECTION[2])
        GLES20.glUniform3f(shader.uniformLightColor, LIGHT_COLOR[0], LIGHT_COLOR[1], LIGHT_COLOR[2])
        GLES20.glUniform3f(shader.uniformAmbientColor, AMBIENT_COLOR[0], AMBIENT_COLOR[1], AMBIENT_COLOR[2])
        GLES20.glUniform3f(shader.uniformFogColor, SKY_TOP[0], SKY_TOP[1], SKY_TOP[2])
        GLES20.glUniform1f(shader.uniformFogNear, Camera.FAR_PLANE * FOG_NEAR_FRACTION)
        GLES20.glUniform1f(shader.uniformFogFar, Camera.FAR_PLANE)
        val camPos = camera.position()
        GLES20.glUniform3f(shader.uniformCameraPosition, camPos[0], camPos[1], camPos[2])
    }

    private fun drawChunks() {
        Matrix.setIdentityM(modelMatrix, 0)
        for (chunk in gameSession.activeChunks) {
            val mesh = chunkMeshCache.structureMeshFor(chunk.index) ?: continue
            drawMesh(mesh, modelMatrix)
        }
    }

    private fun drawPickups() {
        Matrix.setIdentityM(modelMatrix, 0)
        val collected = gameSession.collectedPickupIds
        for (chunk in gameSession.activeChunks) {
            for (pickup in chunk.pickups) {
                if (pickup.id in collected) continue
                val mesh = chunkMeshCache.pickupMesh(pickup.id) ?: continue
                drawMesh(mesh, modelMatrix)
            }
        }
    }

    private fun drawTrail() {
        val mesh = trailRenderer.buildMesh(skinVisual.trailColor) ?: return
        Matrix.setIdentityM(modelMatrix, 0)
        drawMesh(mesh, modelMatrix)
    }

    private fun drawBall(frame: RunFrameResult) {
        // Rolling rotation is derived straight from total distance travelled (arc length =
        // radius * angle), not accumulated frame-to-frame -- so it can never drift out of
        // sync with how far the ball has actually moved.
        val radius = PhysicsConfig.DEFAULT.ballRadius
        val rotationDegrees = ((frame.distanceTraveled / radius) * RAD_TO_DEG).toFloat()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(
            modelMatrix, 0,
            frame.ballState.lateral.toFloat(), frame.ballState.height.toFloat(), frame.ballState.distance.toFloat(),
        )
        Matrix.rotateM(modelMatrix, 0, rotationDegrees, 1f, 0f, 0f)
        drawMesh(ballMesh, modelMatrix)
    }

    private fun drawMesh(mesh: Mesh, model: FloatArray) {
        Matrix.multiplyMM(mvpMatrix, 0, camera.viewProjectionMatrix, 0, model, 0)
        GLES20.glUniformMatrix4fv(shader.uniformMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uniformModelMatrix, 1, false, model, 0)
        mesh.draw(shader)
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
        // Caps the simulated step after a stall (app resume, GC pause, dropped frame) so
        // physics never has to resolve a huge single leap.
        const val MAX_DT_SECONDS = 0.1
        const val RAD_TO_DEG = 180.0 / PI
        const val FOG_NEAR_FRACTION = 0.4f

        val SKY_TOP = floatArrayOf(0.043f, 0.071f, 0.188f)
        val LIGHT_DIRECTION = normalized(floatArrayOf(-0.35f, 0.8f, -0.45f))
        val LIGHT_COLOR = floatArrayOf(1.0f, 0.92f, 0.80f)
        val AMBIENT_COLOR = floatArrayOf(0.35f, 0.38f, 0.48f)

        fun normalized(v: FloatArray): FloatArray {
            val len = Math.sqrt((v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble()).toFloat()
            return floatArrayOf(v[0] / len, v[1] / len, v[2] / len)
        }
    }
}
