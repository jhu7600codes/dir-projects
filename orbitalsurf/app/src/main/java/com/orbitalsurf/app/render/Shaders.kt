package com.orbitalsurf.app.render

/**
 * The one shader pair the whole game uses: per-vertex flat color, single directional light
 * (Lambert diffuse + a flat ambient term), and distance fog blended in past `u_FogNear` --
 * which is what hides new chunks popping into existence at the edge of the streamed world
 * instead of a hard visible edge.
 */
object Shaders {
    const val VERTEX_SHADER = """
        uniform mat4 u_MvpMatrix;
        uniform mat4 u_ModelMatrix;

        attribute vec4 a_Position;
        attribute vec3 a_Normal;
        attribute vec4 a_Color;

        varying vec3 v_WorldPosition;
        varying vec3 v_WorldNormal;
        varying vec4 v_Color;

        void main() {
            vec4 worldPosition = u_ModelMatrix * a_Position;
            v_WorldPosition = worldPosition.xyz;
            v_WorldNormal = normalize(mat3(u_ModelMatrix) * a_Normal);
            v_Color = a_Color;
            gl_Position = u_MvpMatrix * a_Position;
        }
    """

    const val FRAGMENT_SHADER = """
        precision mediump float;

        uniform vec3 u_LightDirection;
        uniform vec3 u_LightColor;
        uniform vec3 u_AmbientColor;
        uniform vec3 u_FogColor;
        uniform float u_FogNear;
        uniform float u_FogFar;
        uniform vec3 u_CameraPosition;

        varying vec3 v_WorldPosition;
        varying vec3 v_WorldNormal;
        varying vec4 v_Color;

        void main() {
            vec3 normal = normalize(v_WorldNormal);
            float diffuse = max(dot(normal, u_LightDirection), 0.0);
            vec3 lit = v_Color.rgb * (u_AmbientColor + u_LightColor * diffuse);

            float dist = distance(u_CameraPosition, v_WorldPosition);
            float fogFactor = clamp((u_FogFar - dist) / (u_FogFar - u_FogNear), 0.0, 1.0);
            vec3 finalColor = mix(u_FogColor, lit, fogFactor);

            gl_FragColor = vec4(finalColor, v_Color.a);
        }
    """
}
