package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngine
import com.example.game.model.WeaponCategory
import com.example.game.world.LosSantosWorld
import com.example.ui.theme.GtaAbilityYellow
import com.example.ui.theme.GtaArmorBlue
import com.example.ui.theme.GtaDarkBg
import com.example.ui.theme.GtaGold
import com.example.ui.theme.GtaGreen
import com.example.ui.theme.GtaHealthGreen
import com.example.ui.theme.GtaPoliceBlue
import com.example.ui.theme.GtaPoliceRed
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun HudOverlay(
  engine: GameEngine,
  engineState: GameEngine.EngineState,
  onOpenPhone: () -> Unit,
  onOpenCharacterWheel: () -> Unit,
  onOpenMap: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
    maximumFractionDigits = 0
  }

  val infiniteTransition = rememberInfiniteTransition(label = "siren_pulse")
  val starAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(280),
      repeatMode = RepeatMode.Reverse
    ),
    label = "star_blink"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("hud_overlay")
  ) {
    // ---------------- TOP BAR ----------------
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      // Top Left: Quick Shortcut Buttons (Phone, Switch Hero, Map)
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Open iFruit Phone
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xCC1E293B))
            .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
            .clickable { onOpenPhone() }
            .testTag("btn_open_phone"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = "iFruit Phone",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }

        // Switch Protagonist
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xCC1E293B))
            .border(2.dp, engineState.character.themeColor, CircleShape)
            .clickable { onOpenCharacterWheel() }
            .testTag("btn_switch_character"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.SwitchAccount,
            contentDescription = "Switch Character",
            tint = engineState.character.themeColor,
            modifier = Modifier.size(22.dp)
          )
        }

        // Full GPS Map
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xCC1E293B))
            .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
            .clickable { onOpenMap() }
            .testTag("btn_open_map"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Map,
            contentDescription = "Map & GPS",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }

        // POV Switcher (1P / 3P)
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xCC1E293B))
            .border(1.5.dp, if (engineState.isFirstPerson) GtaGold else Color(0x66FFFFFF), CircleShape)
            .clickable { engine.togglePOV() }
            .testTag("btn_toggle_pov"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (engineState.isFirstPerson) "1P" else "3P",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = if (engineState.isFirstPerson) GtaGold else Color.White
          )
        }
      }

      // Top Center: Active Mission Banner
      engineState.activeMission?.let { mission ->
        if (!mission.isCompleted) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xDD0F172A))
              .border(1.dp, GtaGold.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag("mission_banner")
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = mission.title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = GtaGold,
                fontFamily = FontFamily.SansSerif
              )
              Text(
                text = mission.description,
                fontSize = 9.5.sp,
                color = Color.White,
                maxLines = 1
              )
            }
          }
        }
      }

      // Top Right: Money, Weapon, Wanted Stars
      Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.testTag("hud_stats_column")
      ) {
        // Classic GTA Money Display
        Text(
          text = currencyFormatter.format(engineState.playerCash),
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          color = GtaGreen,
          letterSpacing = 1.sp,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.testTag("cash_display")
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Weapon Display (Name & Ammo)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = engineState.currentWeapon.type.displayName.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          val ammoText = if (engineState.currentWeapon.type == WeaponCategory.FISTS) {
            "MELEE"
          } else {
            "${engineState.currentWeapon.currentClip} / ${engineState.currentWeapon.reserveAmmo}"
          }
          Text(
            text = ammoText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = GtaGold
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Wanted Stars (1 to 5)
        Row(
          horizontalArrangement = Arrangement.spacedBy(3.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.testTag("wanted_stars_row")
        ) {
          for (i in 1..5) {
            val isGiven = i <= engineState.wantedStars
            val starColor = if (isGiven) {
              if (engineState.isEvading) {
                // Flashing grey/white during evasion cooldown
                Color.White.copy(alpha = starAlpha)
              } else {
                // Flashing Red & Blue police siren colors
                val isRed = (System.currentTimeMillis() / 250) % 2 == 0L
                if (isRed) GtaPoliceRed else GtaPoliceBlue
              }
            } else {
              Color(0x33475569) // Empty slot
            }

            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Star $i",
              tint = starColor,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    // ---------------- TOP LEFT: GTA RADAR MINIMAP (LANDSCAPE OPTIMIZED) ----------------
    Column(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = 14.dp, top = 58.dp)
        .testTag("minimap_container")
    ) {
      // District Name
      Text(
        text = engineState.currentDistrict,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF94A3B8),
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 3.dp)
      )

      // Circular Radar
      Box(
        modifier = Modifier
          .size(88.dp)
          .clip(CircleShape)
          .background(Color(0xEE0B0F19))
          .border(2.dp, Color(0x66475569), CircleShape)
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val radarRadius = size.width / 2f
          val center = Offset(radarRadius, radarRadius)
          val scale = 0.07f // Radar zoom factor

          // Draw nearby roads on radar
          for (road in LosSantosWorld.roads) {
            val sx = center.x + (road.startX - engine.playerX) * scale
            val sy = center.y + (road.startY - engine.playerY) * scale
            val ex = center.x + (road.endX - engine.playerX) * scale
            val ey = center.y + (road.endY - engine.playerY) * scale

            drawLine(
              color = Color(0x8864748B),
              start = Offset(sx, sy),
              end = Offset(ex, ey),
              strokeWidth = if (road.isHighway) 5f else 3.5f
            )
          }

          // Draw Mission Target blip on radar
          engine.activeMission?.let { m ->
            if (!m.isCompleted) {
              val mx = center.x + (m.targetX - engine.playerX) * scale
              val my = center.y + (m.targetY - engine.playerY) * scale
              drawCircle(
                color = GtaGold,
                radius = 5.5f,
                center = Offset(mx.coerceIn(8f, size.width - 8f), my.coerceIn(8f, size.height - 8f))
              )
            }
          }

          // Draw Police blips on radar
          for (cop in engine.vehicles.filter { it.isPolice && !it.isDestroyed }) {
            val cx = center.x + (cop.x - engine.playerX) * scale
            val cy = center.y + (cop.y - engine.playerY) * scale
            val isRed = (System.currentTimeMillis() / 200) % 2 == 0L
            drawCircle(
              color = if (isRed) GtaPoliceRed else GtaPoliceBlue,
              radius = 4.5f,
              center = Offset(cx.coerceIn(6f, size.width - 6f), cy.coerceIn(6f, size.height - 6f))
            )
          }

          // Radar Rings
          drawCircle(
            color = Color(0x33475569),
            radius = radarRadius * 0.5f,
            center = center,
            style = Stroke(width = 1f)
          )

          // Player Blip (Center Arrow)
          drawCircle(
            color = Color.White,
            radius = 4f,
            center = center
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Health, Armor & Ability Arcs under radar
      Row(
        modifier = Modifier.width(88.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        // Health bar (Green)
        Box(
          modifier = Modifier
            .weight(1f)
            .height(5.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0x5514532D))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(engineState.playerHealth / 100f)
              .height(5.dp)
              .background(GtaHealthGreen)
          )
        }

        // Armor bar (Blue)
        Box(
          modifier = Modifier
            .weight(1f)
            .height(5.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0x550369A1))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(engineState.playerArmor / 100f)
              .height(5.dp)
              .background(GtaArmorBlue)
          )
        }

        // Special Ability bar (Yellow)
        Box(
          modifier = Modifier
            .weight(1f)
            .height(5.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0x55854D0E))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(engineState.playerAbility / 100f)
              .height(5.dp)
              .background(GtaAbilityYellow)
          )
        }
      }
    }

    // ---------------- BOTTOM CENTER: VEHICLE SPEED & RADIO ----------------
    if (engineState.isInVehicle) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 12.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xCC0F172A))
          .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp))
          .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${(abs(engineState.currentVehicleSpeed) * 8.5f).toInt()} MPH",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = engine.soundManager.radioStations[engine.soundManager.currentRadioIndex].first.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = GtaGold
          )
        }
      }
    }

    // ---------------- CENTER: FIRST-PERSON PRECISION RETICLE ----------------
    if (engineState.isFirstPerson && !engineState.isInVehicle) {
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .size(32.dp)
          .testTag("first_person_crosshair")
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width / 2f
          val cy = size.height / 2f
          drawCircle(color = Color.White, radius = 2.5f, center = Offset(cx, cy))
          val tick = 7f
          val gap = 5f
          drawLine(color = Color(0xDDFFFFFF), start = Offset(cx - gap - tick, cy), end = Offset(cx - gap, cy), strokeWidth = 2f)
          drawLine(color = Color(0xDDFFFFFF), start = Offset(cx + gap, cy), end = Offset(cx + gap + tick, cy), strokeWidth = 2f)
          drawLine(color = Color(0xDDFFFFFF), start = Offset(cx, cy - gap - tick), end = Offset(cx, cy - gap), strokeWidth = 2f)
          drawLine(color = Color(0xDDFFFFFF), start = Offset(cx, cy + gap), end = Offset(cx, cy + gap + tick), strokeWidth = 2f)
        }
      }
    }
  }
}
