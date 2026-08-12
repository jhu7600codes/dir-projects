package com.orbitalsurf.app.render

/**
 * Accumulates interleaved vertex data + indices for one mesh (e.g. one chunk's whole
 * structure, baked as world-space geometry into a single draw call -- see
 * `ProceduralMeshFactory.buildChunkStructure`). Vertex indices are 16-bit (`Short`), so a
 * single mesh built from one `MeshBuilder` is capped around 32k vertices; the geometry this
 * project generates per chunk (a handful of boxes/ramps/obstacles) stays far under that.
 */
class MeshBuilder {
    private val vertices = mutableListOf<Float>()
    private val indices = mutableListOf<Short>()

    /** A flat-shaded quad: 4 corners sharing one normal, wound so p0->p1->p2->p3 is counter-clockwise when viewed from the front. */
    fun addQuad(p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray, normal: FloatArray, color: FloatArray) {
        val base = vertexCount()
        addVertex(p0, normal, color)
        addVertex(p1, normal, color)
        addVertex(p2, normal, color)
        addVertex(p3, normal, color)
        addTriangleIndices(base, (base + 1).toShort(), (base + 2).toShort())
        addTriangleIndices(base, (base + 2).toShort(), (base + 3).toShort())
    }

    /** A triangle with its own per-vertex normals (curved surfaces like the ball, unlike a box's flat faces). */
    fun addTriangle(
        p0: FloatArray, p1: FloatArray, p2: FloatArray,
        n0: FloatArray, n1: FloatArray, n2: FloatArray,
        color: FloatArray,
    ) {
        val base = vertexCount()
        addVertex(p0, n0, color)
        addVertex(p1, n1, color)
        addVertex(p2, n2, color)
        addTriangleIndices(base, (base + 1).toShort(), (base + 2).toShort())
    }

    fun isEmpty(): Boolean = indices.isEmpty()

    fun build(): Mesh = Mesh(vertices.toFloatArray(), indices.toShortArray())

    private fun vertexCount(): Short = (vertices.size / Mesh.FLOATS_PER_VERTEX).toShort()

    private fun addTriangleIndices(a: Short, b: Short, c: Short) {
        indices += a
        indices += b
        indices += c
    }

    private fun addVertex(p: FloatArray, n: FloatArray, c: FloatArray) {
        vertices += p[0]; vertices += p[1]; vertices += p[2]
        vertices += n[0]; vertices += n[1]; vertices += n[2]
        vertices += c[0]; vertices += c[1]; vertices += c[2]; vertices += c[3]
    }
}
