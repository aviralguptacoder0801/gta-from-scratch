package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.engine.GameEngine
import com.example.game.world.LosSantosWorld
import com.example.ui.theme.GtaGold
import com.example.ui.theme.GtaGreen
import com.example.ui.theme.GtaPoliceBlue
import com.example.ui.theme.GtaPoliceRed

@Composable
fun PauseAndMapDialog(
  engine: GameEngine,
  onDismiss: () -> Unit
) {
  var selectedDistrict by remember { mutableStateOf<String?>(null) }
  var mapToast by remember { mutableStateOf<String?>("Tap any location to set GPS Waypoint") }

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(640.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(Color(0xF50F172A))
        .border(2.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
        .padding(14.dp)
        .testTag("map_pause_dialog")
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "LOS SANTOS MAP & GPS",
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              letterSpacing = 1.sp
            )
            Text(
              text = "Current: ${engine.currentDistrict}",
              fontSize = 10.sp,
              color = GtaGold
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        mapToast?.let {
          Text(
            text = it,
            fontSize = 10.sp,
            color = GtaGreen,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
          )
        }

        // Full Interactive Map Canvas
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B0F19))
            .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
              detectTapGestures { tapOffset ->
                // Map screen coordinates to world coordinates
                val worldX = (tapOffset.x / size.width) * LosSantosWorld.WORLD_WIDTH
                val worldY = (tapOffset.y / size.height) * LosSantosWorld.WORLD_HEIGHT
                val district = LosSantosWorld.getDistrictFor(worldX, worldY)
                selectedDistrict = district
                mapToast = "Waypoint set: $district (${worldX.toInt()}, ${worldY.toInt()})"
                engine.soundManager.playCashChime()
              }
            }
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val mapW = size.width
            val mapH = size.height
            val scaleX = mapW / LosSantosWorld.WORLD_WIDTH
            val scaleY = mapH / LosSantosWorld.WORLD_HEIGHT

            // Base Background
            drawRect(color = Color(0xFF1E2430), size = size)

            // Ocean (West)
            drawRect(
              color = Color(0xFF0284C7),
              topLeft = Offset(0f, 0f),
              size = Size(380f * scaleX, mapH)
            )

            // Beach Sand
            drawRect(
              color = Color(0xFFD4B37F),
              topLeft = Offset(380f * scaleX, 0f),
              size = Size(320f * scaleX, mapH)
            )

            // Vinewood Hills (North Green)
            drawRect(
              color = Color(0xFF166534),
              topLeft = Offset(1200f * scaleX, 0f),
              size = Size(2000f * scaleX, 1050f * scaleY)
            )

            // Sandy Shores (Desert)
            drawRect(
              color = Color(0xFFC29B62),
              topLeft = Offset(3000f * scaleX, 0f),
              size = Size(1200f * scaleX, 2000f * scaleY)
            )

            // Draw Roads
            for (road in LosSantosWorld.roads) {
              val sx = road.startX * scaleX
              val sy = road.startY * scaleY
              val ex = road.endX * scaleX
              val ey = road.endY * scaleY
              drawLine(
                color = Color(0xFF64748B),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = if (road.isHighway) 4f else 2f
              )
            }

            // Draw Special Locations
            for (loc in LosSantosWorld.specialLocations) {
              val lx = loc.x * scaleX
              val ly = loc.y * scaleY
              val color = when (loc.type) {
                "RESPRAY" -> Color(0xFF10B981)
                "GUN_STORE" -> Color(0xFFEF4444)
                "HEIST" -> Color(0xFFFACC15)
                else -> Color(0xFFA855F7)
              }
              drawCircle(color = color, radius = 4f, center = Offset(lx, ly))
            }

            // Draw Active Mission Waypoint
            engine.activeMission?.let { m ->
              if (!m.isCompleted) {
                val mx = m.targetX * scaleX
                val my = m.targetY * scaleY
                drawCircle(color = GtaGold, radius = 6.5f, center = Offset(mx, my))
              }
            }

            // Draw Player Marker
            val px = engine.playerX * scaleX
            val py = engine.playerY * scaleY
            drawCircle(color = Color.White, radius = 5.5f, center = Offset(px, py))
            drawCircle(color = engine.currentCharacter.themeColor, radius = 3.5f, center = Offset(px, py))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fast Travel & Quick Locations
        Text("FAST TRAVEL TO DISTRICT:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FastTravelChip(title = "Vinewood", modifier = Modifier.weight(1f)) {
            engine.playerX = 2100f
            engine.playerY = 400f
            mapToast = "Fast traveled to Vinewood Hills!"
            engine.soundManager.playCashChime()
          }
          FastTravelChip(title = "Downtown", modifier = Modifier.weight(1f)) {
            engine.playerX = 2100f
            engine.playerY = 1750f
            mapToast = "Fast traveled to Maze Bank Downtown!"
            engine.soundManager.playCashChime()
          }
          FastTravelChip(title = "South LS", modifier = Modifier.weight(1f)) {
            engine.playerX = 1600f
            engine.playerY = 3100f
            mapToast = "Fast traveled to South LS!"
            engine.soundManager.playCashChime()
          }
          FastTravelChip(title = "Desert", modifier = Modifier.weight(1f)) {
            engine.playerX = 3700f
            engine.playerY = 850f
            mapToast = "Fast traveled to Sandy Shores Desert!"
            engine.soundManager.playCashChime()
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Resume Button
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = GtaGreen),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("RESUME GAME", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
      }
    }
  }
}

@Composable
private fun FastTravelChip(
  title: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF1E293B))
      .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = Color.White
    )
  }
}
