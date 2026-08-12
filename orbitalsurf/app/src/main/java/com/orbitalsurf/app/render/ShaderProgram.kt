package com.orbitalsurf.app.render

import android.opengl.GLES20

/** Compiles/links a vertex+fragment shader pair and caches every attribute/uniform location `Mesh`/`GameRenderer` need. */
class ShaderProgram(vertexSource: String, fragmentSource: String) {
    val programId: Int = linkProgram(
        compileShader(GLES20.GL_VERTEX_SHADER, vertexSource),
        compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource),
    )

    val attribPosition = GLES20.glGetAttribLocation(programId, "a_Position")
    val attribNormal = GLES20.glGetAttribLocation(programId, "a_Normal")
    val attribColor = GLES20.glGetAttribLocation(programId, "a_Color")

    val uniformMvpMatrix = GLES20.glGetUniformLocation(programId, "u_MvpMatrix")
    val uniformModelMatrix = GLES20.glGetUniformLocation(programId, "u_ModelMatrix")
    val uniformLightDirection = GLES20.glGetUniformLocation(programId, "u_LightDirection")
    val uniformLightColor = GLES20.glGetUniformLocation(programId, "u_LightColor")
    val uniformAmbientColor = GLES20.glGetUniformLocation(programId, "u_AmbientColor")
    val uniformFogColor = GLES20.glGetUniformLocation(programId, "u_FogColor")
    val uniformFogNear = GLES20.glGetUniformLocation(programId, "u_FogNear")
    val uniformFogFar = GLES20.glGetUniformLocation(programId, "u_FogFar")
    val uniformCameraPosition = GLES20.glGetUniformLocation(programId, "u_CameraPosition")

    fun use() {
        GLES20.glUseProgram(programId)
    }

    companion object {
        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                error("Shader compile failed: $log")
            }
            return shader
        }

        private fun linkProgram(vertexShader: Int, fragmentShader: Int): Int {
            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            val status = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                error("Program link failed: $log")
            }
            // Once linked, the individual shader objects are no longer needed.
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            return program
        }
    }
}
