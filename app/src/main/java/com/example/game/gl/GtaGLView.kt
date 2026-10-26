package com.example.game.gl

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.example.game.engine.GameEngine

@SuppressLint("ViewConstructor")
class GtaGLView(
  context: Context,
  val engine: GameEngine
) : GLSurfaceView(context) {

  private val renderer: Gta3DRenderer

  private var previousX = 0f
  private var previousY = 0f
  private val lookSensitivity = 0.22f

  init {
    setEGLContextClientVersion(2)
    renderer = Gta3DRenderer(engine)
    setRenderer(renderer)
    renderMode = RENDERMODE_CONTINUOUSLY
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    val x = event.x
    val y = event.y

    // Only process look swipe if touch is on the right half of the screen
    if (x > width * 0.35f) {
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          previousX = x
          previousY = y
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = x - previousX
          val dy = y - previousY

          engine.playerYaw = (engine.playerYaw + dx * lookSensitivity) % 360f
          if (engine.playerYaw < 0) engine.playerYaw += 360f

          engine.playerPitch = (engine.playerPitch - dy * lookSensitivity).coerceIn(-60f, 60f)

          previousX = x
          previousY = y
        }
      }
    }
    return true
  }
}
