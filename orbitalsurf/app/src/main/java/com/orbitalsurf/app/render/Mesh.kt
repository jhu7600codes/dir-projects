package com.orbitalsurf.app.render

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * A static piece of GPU-drawable geometry: interleaved position(3)/normal(3)/color(4) vertices
 * plus a triangle index list, drawn with `GL_TRIANGLES`. No texture coordinates -- every mesh
 * in this game is flat/Lambert-shaded procedural geometry with per-vertex color, not textured
 * (there's no art pipeline behind this project; see the project README).
 *
 * Deliberately uses client-side vertex arrays (plain NIO buffers passed straight to
 * `glVertexAttribPointer`/`glDrawElements`) rather than real GL buffer objects
 * (`glGenBuffers`/`glBufferData`). That's a simplification: real VBOs would upload once and be
 * faster to redraw, but client arrays need no GL-side lifecycle management at all -- a `Mesh`
 * going out of scope just gets garbage collected, nothing to explicitly free. Given this
 * project's geometry is modest (a handful of chunks' worth of boxes at a time, see
 * `ChunkMeshCache`), that trade-off favors correctness over the performance real VBOs would
 * add -- a reasonable first optimization to make later if profiling calls for it.
 */
class Mesh(vertices: FloatArray, indices: ShortArray) {
    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertices.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(vertices); position(0) }

    private val indexBuffer: ShortBuffer =
        ByteBuffer.allocateDirect(indices.size * BYTES_PER_SHORT)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply { put(indices); position(0) }

    val indexCount: Int = indices.size

    fun draw(shader: ShaderProgram) {
        vertexBuffer.position(POSITION_OFFSET_FLOATS)
        GLES20.glVertexAttribPointer(shader.attribPosition, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, vertexBuffer)
        GLES20.glEnableVertexAttribArray(shader.attribPosition)

        vertexBuffer.position(NORMAL_OFFSET_FLOATS)
        GLES20.glVertexAttribPointer(shader.attribNormal, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, vertexBuffer)
        GLES20.glEnableVertexAttribArray(shader.attribNormal)

        vertexBuffer.position(COLOR_OFFSET_FLOATS)
        GLES20.glVertexAttribPointer(shader.attribColor, 4, GLES20.GL_FLOAT, false, STRIDE_BYTES, vertexBuffer)
        GLES20.glEnableVertexAttribArray(shader.attribColor)

        indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }

    companion object {
        const val FLOATS_PER_VERTEX = 10 // 3 position + 3 normal + 4 color
        private const val POSITION_OFFSET_FLOATS = 0
        private const val NORMAL_OFFSET_FLOATS = 3
        private const val COLOR_OFFSET_FLOATS = 6
        private const val BYTES_PER_FLOAT = 4
        private const val BYTES_PER_SHORT = 2
        const val STRIDE_BYTES = FLOATS_PER_VERTEX * BYTES_PER_FLOAT
    }
}
