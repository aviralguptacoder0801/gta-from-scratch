package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.audio.SoundManager
import com.example.game.engine.GameEngine
import com.example.game.gl.GtaGLView
import com.example.game.ui.CharacterWheelDialog
import com.example.game.ui.GameCanvasView
import com.example.game.ui.HudOverlay
import com.example.game.ui.PauseAndMapDialog
import com.example.game.ui.PhoneDialog
import com.example.game.ui.VirtualControls
import com.example.ui.theme.GtaDarkBg
import com.example.ui.theme.GtaGold
import com.example.ui.theme.GtaGreen
import com.example.ui.theme.GtaTheme

class MainActivity : ComponentActivity() {
  private var soundManager: SoundManager? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val sm = SoundManager(applicationContext)
    soundManager = sm
    val engine = GameEngine(sm)

    setContent {
      GtaTheme {
        GtaGameApp(engine = engine)
      }
    }
  }

  override fun onPause() {
    super.onPause()
    soundManager?.stopRadio()
    soundManager?.setPoliceSirenActive(false)
  }

  override fun onDestroy() {
    super.onDestroy()
    soundManager?.release()
    soundManager = null
  }
}

@Composable
fun GtaGameApp(engine: GameEngine) {
  val engineState by engine.stateFlow.collectAsState()

  var isPhoneOpen by remember { mutableStateOf(false) }
  var isCharacterWheelOpen by remember { mutableStateOf(false) }
  var isMapOpen by remember { mutableStateOf(false) }
  var showWelcomeBanner by remember { mutableStateOf(false) }

  // 60 FPS Game Loop
  LaunchedEffect(Unit) {
    var lastNanoTime = System.nanoTime()
    while (true) {
      withFrameNanos { nowNanos ->
        val deltaSec = ((nowNanos - lastNanoTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
        lastNanoTime = nowNanos
        engine.update(deltaSec)
      }
    }
  }

  BackHandler(enabled = isPhoneOpen || isCharacterWheelOpen || isMapOpen) {
    if (isPhoneOpen) isPhoneOpen = false
    else if (isCharacterWheelOpen) isCharacterWheelOpen = false
    else if (isMapOpen) isMapOpen = false
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(GtaDarkBg)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .testTag("gta_game_container")
  ) {
    // 1. Hardware-Accelerated High-Resolution 3D OpenGL ES Engine (First-Person POV & 3D City)
    AndroidView(
      factory = { ctx ->
        GtaGLView(ctx, engine).apply {
          layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
          )
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .testTag("gta_3d_glview")
    )

    // 2. Virtual Joystick & Dual-Mode Action Controls
    VirtualControls(
      engine = engine,
      isInVehicle = engineState.isInVehicle,
      modifier = Modifier.fillMaxSize()
    )

    // 3. GTA V Heads-Up Display (Radar Minimap, Health/Armor/Ability, Cash, Wanted Stars)
    HudOverlay(
      engine = engine,
      engineState = engineState,
      onOpenPhone = { isPhoneOpen = true },
      onOpenCharacterWheel = { isCharacterWheelOpen = true },
      onOpenMap = { isMapOpen = true },
      modifier = Modifier.fillMaxSize()
    )

    // 4. Modals & Dialogs
    if (isPhoneOpen) {
      PhoneDialog(
        engine = engine,
        onDismiss = { isPhoneOpen = false }
      )
    }

    if (isCharacterWheelOpen) {
      CharacterWheelDialog(
        engine = engine,
        onDismiss = { isCharacterWheelOpen = false }
      )
    }

    if (isMapOpen) {
      PauseAndMapDialog(
        engine = engine,
        onDismiss = { isMapOpen = false }
      )
    }

    // 5. Welcome Splash Overlay (Intro Cinematic Banner)
    AnimatedVisibility(
      visible = showWelcomeBanner,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xE6000000))
          .clickable { showWelcomeBanner = false }
          .padding(20.dp)
          .testTag("welcome_splash_overlay"),
        contentAlignment = Alignment.Center
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(20.dp),
          modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF00F172A))
            .border(2.dp, GtaGold, RoundedCornerShape(20.dp))
            .padding(16.dp)
        ) {
          // Left: Banner Artwork
          Image(
            painter = painterResource(id = R.drawable.img_gta_banner),
            contentDescription = "GTA V Los Santos Banner",
            modifier = Modifier
              .weight(1.1f)
              .height(200.dp)
              .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
          )

          // Right: Content & Launch Button
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
          ) {
            Text(
              text = "GRAND THEFT AUTO V",
              fontSize = 20.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              letterSpacing = 1.5.sp,
              fontFamily = FontFamily.SansSerif
            )

            Text(
              text = "LOS SANTOS COUNTY • 3D FIRST-PERSON",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = GtaGreen,
              letterSpacing = 1.2.sp
            )

            Text(
              text = "High-resolution 3D open-world. Switch between Franklin, Michael & Trevor, hijack supercars, evade 5-star police pursuits, and execute heists.",
              fontSize = 11.sp,
              color = Color(0xFFCBD5E1),
              lineHeight = 15.sp
            )

            Button(
              onClick = {
                showWelcomeBanner = false
                engine.soundManager.playMissionFanfare()
              },
              colors = ButtonDefaults.buttonColors(containerColor = GtaGreen),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .testTag("btn_enter_los_santos")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                Text("ENTER LOS SANTOS", fontWeight = FontWeight.Black, color = Color.Black)
              }
            }
          }
        }
      }
    }
  }
}
