package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngine
import com.example.ui.theme.GtaAbilityYellow
import com.example.ui.theme.GtaGreen
import com.example.ui.theme.GtaPoliceBlue
import com.example.ui.theme.GtaPoliceRed
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun VirtualControls(
  engine: GameEngine,
  isInVehicle: Boolean,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
  ) {
    // ---------------- RIGHT: TOUCH-TO-LOOK 3D CAMERA AREA ----------------
    Box(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .fillMaxWidth(0.75f)
        .fillMaxHeight()
        .padding(top = 56.dp, bottom = 80.dp)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            // Natural touch look / orbit: drag right turns camera right, drag up looks up
            engine.playerYaw = (engine.playerYaw + dragAmount.x * 0.28f) % 360f
            if (engine.playerYaw < 0) engine.playerYaw += 360f
            engine.playerPitch = (engine.playerPitch - dragAmount.y * 0.22f).coerceIn(-60f, 60f)
          }
        }
        .testTag("look_swipe_area")
    )

    // ---------------- LEFT: ANALOG JOYSTICK (LANDSCAPE) ----------------
    val density = LocalDensity.current
    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var knobOffsetY by remember { mutableFloatStateOf(0f) }
    val maxRadiusPx = with(density) { 46.dp.toPx() }
    val centerPx = with(density) { 56.dp.toPx() }

    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 18.dp, bottom = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // Joystick Inversion & Mode Toggle Pill
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(if (engine.invertJoystickY) GtaPoliceRed.copy(alpha = 0.85f) else Color(0x991E293B))
          .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(8.dp))
          .clickable { engine.toggleJoystickInvert() }
          .padding(horizontal = 8.dp, vertical = 2.dp)
          .testTag("btn_toggle_invert_stick")
      ) {
        Text(
          text = if (engine.invertJoystickY) "STICK: INVERTED" else "STICK: NORMAL",
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }

      // Analog Joystick Base with Directional Guides
      Box(
        modifier = Modifier
          .size(116.dp)
          .clip(CircleShape)
          .background(Color(0x880F172A))
          .border(2.dp, Color(0xBB94A3B8), CircleShape)
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { offset ->
                val dx = offset.x - centerPx
                val dy = offset.y - centerPx
                val dist = hypot(dx, dy)
                val scale = if (dist > maxRadiusPx) maxRadiusPx / dist else 1f
                knobOffsetX = dx * scale
                knobOffsetY = dy * scale
                engine.joystickX = (knobOffsetX / maxRadiusPx).coerceIn(-1f, 1f)
                // -dy because screen Y points downwards; pushing UP must give positive forward
                engine.joystickY = (-knobOffsetY / maxRadiusPx).coerceIn(-1f, 1f)
              },
              onDrag = { change, dragAmount ->
                change.consume()
                val nx = knobOffsetX + dragAmount.x
                val ny = knobOffsetY + dragAmount.y
                val dist = hypot(nx, ny)
                val scale = if (dist > maxRadiusPx) maxRadiusPx / dist else 1f
                knobOffsetX = nx * scale
                knobOffsetY = ny * scale
                engine.joystickX = (knobOffsetX / maxRadiusPx).coerceIn(-1f, 1f)
                engine.joystickY = (-knobOffsetY / maxRadiusPx).coerceIn(-1f, 1f)
              },
              onDragEnd = {
                knobOffsetX = 0f
                knobOffsetY = 0f
                engine.joystickX = 0f
                engine.joystickY = 0f
              },
              onDragCancel = {
                knobOffsetX = 0f
                knobOffsetY = 0f
                engine.joystickX = 0f
                engine.joystickY = 0f
              }
            )
          }
          .testTag("virtual_joystick_base"),
        contentAlignment = Alignment.Center
      ) {
        // Directional HUD Markings
        Text(
          text = "▲",
          fontSize = 11.sp,
          color = Color(0x88FFFFFF),
          modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
        )
        Text(
          text = "▼",
          fontSize = 11.sp,
          color = Color(0x88FFFFFF),
          modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        )
        Text(
          text = "◄",
          fontSize = 11.sp,
          color = Color(0x88FFFFFF),
          modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
        )
        Text(
          text = "►",
          fontSize = 11.sp,
          color = Color(0x88FFFFFF),
          modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
        )

        // Joystick Thumb Knob with inner concentric ring
        Box(
          modifier = Modifier
            .offset { IntOffset(knobOffsetX.roundToInt(), knobOffsetY.roundToInt()) }
            .size(52.dp)
            .clip(CircleShape)
            .background(Color(0xF01E293B))
            .border(2.5.dp, Color.White, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(20.dp)
              .clip(CircleShape)
              .background(GtaGreen.copy(alpha = 0.85f))
          )
        }
      }
    }

    // ---------------- RIGHT: ACTION CONTROLS ----------------
    if (isInVehicle) {
      // VEHICLE CONTROLS (Gas, Brake, Drift, Horn, Radio, Exit)
      Column(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 18.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Radio Toggle Button
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(CircleShape)
              .background(Color(0xCC1E293B))
              .border(1.5.dp, Color(0xFFFACC15), CircleShape)
              .pointerInput(Unit) {
                detectTapGestures(onTap = {
                  engine.soundManager.cycleRadio()
                })
              }
              .testTag("btn_radio"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = "Radio",
              tint = Color(0xFFFACC15),
              modifier = Modifier.size(24.dp)
            )
          }

          // Horn Button
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(CircleShape)
              .background(Color(0xCC1E293B))
              .border(1.5.dp, Color.White, CircleShape)
              .pointerInput(Unit) {
                detectTapGestures(onTap = {
                  engine.soundManager.playHorn()
                })
              }
              .testTag("btn_horn"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Campaign,
              contentDescription = "Horn",
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }

          // Exit Car Button
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(GtaPoliceRed.copy(alpha = 0.85f))
              .border(2.dp, Color.White, CircleShape)
              .pointerInput(Unit) {
                detectTapGestures(onTap = {
                  engine.enterOrExitVehicle()
                })
              }
              .testTag("btn_exit_car"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Upload,
              contentDescription = "Exit Vehicle",
              tint = Color.White,
              modifier = Modifier.size(26.dp)
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Handbrake / Drift Button
          Box(
            modifier = Modifier
              .size(62.dp)
              .clip(CircleShape)
              .background(Color(0xCC0284C7))
              .border(2.dp, Color.White, CircleShape)
              .pointerInput(Unit) {
                detectTapGestures(
                  onPress = {
                    engine.isHandbrake = true
                    tryAwaitRelease()
                    engine.isHandbrake = false
                  }
                )
              }
              .testTag("btn_drift"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "DRIFT",
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }

          // Brake / Reverse Pedal
          Box(
            modifier = Modifier
              .size(68.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(GtaPoliceRed.copy(alpha = 0.85f))
              .border(2.dp, Color.White, RoundedCornerShape(12.dp))
              .pointerInput(Unit) {
                detectTapGestures(
                  onPress = {
                    engine.isPedalBrake = true
                    tryAwaitRelease()
                    engine.isPedalBrake = false
                  }
                )
              }
              .testTag("btn_brake"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "BRAKE",
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }

          // Accelerate / Gas Pedal
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(GtaGreen.copy(alpha = 0.9f))
              .border(2.dp, Color.White, RoundedCornerShape(12.dp))
              .pointerInput(Unit) {
                detectTapGestures(
                  onPress = {
                    engine.isPedalGas = true
                    tryAwaitRelease()
                    engine.isPedalGas = false
                  }
                )
              }
              .testTag("btn_gas"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "GAS",
              fontSize = 16.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }
        }
      }
    } else {
      // ON FOOT CONTROLS (Shoot, Enter Car, Sprint, Weapon Cycle, Ability)
      Column(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 18.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Special Ability Button
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(if (engine.isAbilityActive) GtaAbilityYellow else Color(0xCC1E293B))
              .border(2.dp, GtaAbilityYellow, CircleShape)
              .pointerInput(Unit) {
                detectTapGestures(onTap = {
                  engine.toggleSpecialAbility()
                })
              }
              .testTag("btn_ability"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = "Special Ability",
              tint = if (engine.isAbilityActive) Color.Black else GtaAbilityYellow,
              modifier = Modifier.size(24.dp)
            )
          }

          // Cycle Weapon Button
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(Color(0xCC1E293B))
              .border(1.5.dp, Color.White, CircleShape)
              .pointerInput(Unit) {
                detectTapGestures(onTap = {
                  engine.cycleWeapon()
                })
              }
              .testTag("btn_cycle_weapon"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Switch Weapon",
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }

          // Enter / Carjack Vehicle Button
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(Color(0xDD10B981))
              .border(2.dp, Color.White, CircleShape)
              .pointerInput(Unit) {
                detectTapGestures(onTap = {
                  engine.enterOrExitVehicle()
                })
              }
              .testTag("btn_enter_vehicle"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.DirectionsCar,
              contentDescription = "Enter Car",
              tint = Color.White,
              modifier = Modifier.size(26.dp)
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Sprint / Roll Button
          Box(
            modifier = Modifier
              .size(62.dp)
              .clip(CircleShape)
              .background(Color(0xCC334155))
              .border(2.dp, Color.White, CircleShape)
              .pointerInput(Unit) {
                detectTapGestures(
                  onPress = {
                    engine.isPedalGas = true // Sprint
                    tryAwaitRelease()
                    engine.isPedalGas = false
                  }
                )
              }
              .testTag("btn_sprint"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.DirectionsRun,
              contentDescription = "Sprint",
              tint = Color.White,
              modifier = Modifier.size(28.dp)
            )
          }

          // Shoot / Attack Button
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
              .background(GtaPoliceRed.copy(alpha = 0.9f))
              .border(2.5.dp, Color.White, CircleShape)
              .pointerInput(Unit) {
                detectTapGestures(
                  onPress = {
                    engine.shootWeapon()
                  }
                )
              }
              .testTag("btn_attack"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "FIRE",
              fontSize = 16.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }
        }
      }
    }
  }
}
