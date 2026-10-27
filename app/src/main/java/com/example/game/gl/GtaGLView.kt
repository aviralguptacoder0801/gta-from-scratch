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
    // Return false so Compose overlay controls, joystick, and touch-to-look drag gestures receive all input
    return false
  }
}
