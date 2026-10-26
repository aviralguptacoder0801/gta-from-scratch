package com.example.game.gl

import android.opengl.GLES20
import android.util.Log

object Gta3DShader {
  const val VERTEX_SHADER_CODE = """
    uniform mat4 u_MVPMatrix;
    uniform mat4 u_MVMatrix;
    uniform vec3 u_LightPos;
    
    attribute vec4 a_Position;
    attribute vec3 a_Normal;
    attribute vec4 a_Color;
    
    varying vec4 v_Color;
    varying float v_FogDepth;
    varying vec3 v_Normal;
    varying vec3 v_EyeVec;
    varying vec3 v_LightDir;
    
    void main() {
        gl_Position = u_MVPMatrix * a_Position;
        
        vec4 modelViewVertex = u_MVMatrix * a_Position;
        v_Normal = normalize(vec3(u_MVMatrix * vec4(a_Normal, 0.0)));
        v_LightDir = normalize(u_LightPos - modelViewVertex.xyz);
        v_EyeVec = normalize(-modelViewVertex.xyz);
        
        v_Color = a_Color;
        v_FogDepth = length(modelViewVertex.xyz);
    }
  """

  const val FRAGMENT_SHADER_CODE = """
    precision mediump float;
    
    uniform vec4 u_FogColor;
    uniform float u_FogNear;
    uniform float u_FogFar;
    uniform float u_Emissive;
    uniform vec3 u_PoliceLightColor;
    uniform float u_PoliceLightIntensity;
    
    varying vec4 v_Color;
    varying float v_FogDepth;
    varying vec3 v_Normal;
    varying vec3 v_EyeVec;
    varying vec3 v_LightDir;
    
    void main() {
        if (u_Emissive > 0.5) {
            gl_FragColor = v_Color;
            return;
        }
        
        vec3 norm = normalize(v_Normal);
        vec3 light = normalize(v_LightDir);
        vec3 eye = normalize(v_EyeVec);
        
        float ambient = 0.42;
        float diff = max(dot(norm, light), 0.0) * 0.65;
        
        vec3 halfVec = normalize(light + eye);
        float spec = pow(max(dot(norm, halfVec), 0.0), 16.0) * 0.35 * v_Color.a;
        
        vec3 shadedRgb = v_Color.rgb * (ambient + diff) + vec3(spec);
        
        if (u_PoliceLightIntensity > 0.01) {
            shadedRgb += u_PoliceLightColor * u_PoliceLightIntensity;
        }
        
        float fogFactor = clamp((u_FogFar - v_FogDepth) / (u_FogFar - u_FogNear), 0.0, 1.0);
        vec3 finalColor = mix(u_FogColor.rgb, shadedRgb, fogFactor);
        
        gl_FragColor = vec4(finalColor, 1.0);
    }
  """

  fun createProgram(): Int {
    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
    val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)

    if (vertexShader == 0 || fragmentShader == 0) {
      Log.e("Gta3DShader", "Failed to compile shaders")
      return 0
    }

    val program = GLES20.glCreateProgram()
    if (program != 0) {
      GLES20.glAttachShader(program, vertexShader)
      GLES20.glAttachShader(program, fragmentShader)
      GLES20.glLinkProgram(program)

      val linkStatus = IntArray(1)
      GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
      if (linkStatus[0] != GLES20.GL_TRUE) {
        Log.e("Gta3DShader", "Could not link program: " + GLES20.glGetProgramInfoLog(program))
        GLES20.glDeleteProgram(program)
        return 0
      }
    }
    return program
  }

  private fun loadShader(type: Int, shaderCode: String): Int {
    val shader = GLES20.glCreateShader(type)
    if (shader != 0) {
      GLES20.glShaderSource(shader, shaderCode)
      GLES20.glCompileShader(shader)

      val compiled = IntArray(1)
      GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
      if (compiled[0] == 0) {
        Log.e("Gta3DShader", "Could not compile shader $type: " + GLES20.glGetShaderInfoLog(shader))
        GLES20.glDeleteShader(shader)
        return 0
      }
    }
    return shader
  }
}
