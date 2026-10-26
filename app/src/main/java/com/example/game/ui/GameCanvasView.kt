package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import com.example.game.engine.GameEngine
import com.example.game.model.CharacterType
import com.example.game.model.ParticleType
import com.example.game.model.VehicleModel
import com.example.game.world.LosSantosWorld
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvasView(
  engine: GameEngine,
  modifier: Modifier = Modifier
) {
  Canvas(
    modifier = modifier
      .fillMaxSize()
      .testTag("game_canvas")
  ) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val halfW = canvasWidth / 2f
    val halfH = canvasHeight / 2f

    // Camera translates so player is at center of screen
    val camX = engine.playerX
    val camY = engine.playerY

    withTransform({
      translate(left = halfW - camX, top = halfH - camY)
    }) {
      // 1. Draw Base Terrain
      drawBaseTerrain(this, camX, camY, halfW, halfH)

      // 2. Draw Roads & Intersections
      drawRoads(this)

      // 3. Draw Skid Marks
      for (p in engine.particles.filter { it.type == ParticleType.SKID_MARK }) {
        drawCircle(
          color = Color(p.color).copy(alpha = (p.life / p.maxLife) * 0.45f),
          radius = p.radius,
          center = Offset(p.x, p.y)
        )
      }

      // 4. Draw Buildings with 2.5D Depth
      drawBuildings(this)

      // 5. Draw Special Location Markers (Pay 'n' Spray, Ammu-Nation, Heist)
      drawLocationMarkers(this)

      // 6. Draw Vehicles
      for (v in engine.vehicles) {
        if (v.isDestroyed) {
          drawWreckedVehicle(this, v)
        } else {
          drawVehicle(this, v, isPlayer = (v == engine.currentVehicle))
        }
      }

      // 7. Draw Pedestrians
      for (p in engine.pedestrians.filter { it.isAlive }) {
        drawPedestrian(this, p.x, p.y, p.angleDeg, isFleeing = p.isFleeing, isPolice = p.isPolice)
      }

      // 8. Draw Player (if on foot)
      if (engine.currentVehicle == null) {
        drawPlayerCharacter(
          this,
          engine.playerX,
          engine.playerY,
          engine.playerAngle,
          engine.currentCharacter,
          engine.isAbilityActive
        )
      }

      // 9. Draw Bullets & Rockets
      for (b in engine.bullets) {
        if (b.isRocket) {
          drawCircle(
            color = Color(0xFFEF4444),
            radius = 6f,
            center = Offset(b.x, b.y)
          )
          drawCircle(
            color = Color(0xFFFACC15),
            radius = 3f,
            center = Offset(b.x, b.y)
          )
        } else {
          drawLine(
            color = Color(0xFFFEF08A),
            start = Offset(b.x - b.vx * 0.8f, b.y - b.vy * 0.8f),
            end = Offset(b.x, b.y),
            strokeWidth = 3f
          )
        }
      }

      // 10. Draw Particles (Fire, Smoke, Blood, Explosions)
      for (p in engine.particles.filter { it.type != ParticleType.SKID_MARK }) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        when (p.type) {
          ParticleType.EXPLOSION_SHOCKWAVE -> {
            drawCircle(
              color = Color(p.color).copy(alpha = alpha * 0.6f),
              radius = p.radius * (1f - alpha * 0.5f),
              center = Offset(p.x, p.y),
              style = Stroke(width = 8f * alpha)
            )
          }
          ParticleType.FIRE -> {
            drawCircle(
              color = Color(p.color).copy(alpha = alpha),
              radius = p.radius * alpha,
              center = Offset(p.x, p.y)
            )
          }
          ParticleType.BLOOD, ParticleType.SPARK -> {
            drawCircle(
              color = Color(p.color).copy(alpha = alpha),
              radius = p.radius,
              center = Offset(p.x, p.y)
            )
          }
          else -> {}
        }
      }

      // 11. Mission Waypoint Marker
      engine.activeMission?.let { m ->
        if (!m.isCompleted) {
          drawMissionWaypoint(this, m.targetX, m.targetY, m.title)
        }
      }
    }

    // 12. Full-Screen Special Ability Vignette Overlay
    if (engine.isAbilityActive) {
      val vignetteColor = when (engine.currentCharacter) {
        CharacterType.FRANKLIN -> Color(0x3310B981) // Green Drive Focus
        CharacterType.MICHAEL -> Color(0x333B82F6)  // Blue Bullet Time
        CharacterType.TREVOR -> Color(0x44EF4444)   // Red Mist Rage
      }
      drawRect(
        color = vignetteColor,
        size = size
      )
    }
  }
}

private fun drawBaseTerrain(
  scope: DrawScope,
  camX: Float,
  camY: Float,
  halfW: Float,
  halfH: Float
) {
  // Ground default (urban asphalt gray)
  scope.drawRect(
    color = Color(0xFF1E2430),
    topLeft = Offset(0f, 0f),
    size = Size(LosSantosWorld.WORLD_WIDTH, LosSantosWorld.WORLD_HEIGHT)
  )

  // Del Perro Ocean Water (West)
  scope.drawRect(
    color = Color(0xFF0284C7),
    topLeft = Offset(0f, 0f),
    size = Size(380f, LosSantosWorld.WORLD_HEIGHT)
  )

  // Del Perro Beach Sand (West)
  scope.drawRect(
    color = Color(0xFFD4B37F),
    topLeft = Offset(380f, 0f),
    size = Size(320f, LosSantosWorld.WORLD_HEIGHT)
  )

  // Vinewood Hills Lush Green (North)
  scope.drawRect(
    color = Color(0xFF166534),
    topLeft = Offset(1200f, 0f),
    size = Size(2000f, 1050f)
  )

  // Sandy Shores Desert (North East)
  scope.drawRect(
    color = Color(0xFFC29B62),
    topLeft = Offset(3000f, 0f),
    size = Size(1200f, 2000f)
  )
}

private fun drawRoads(scope: DrawScope) {
  for (road in LosSantosWorld.roads) {
    val isVertical = road.startX == road.endX
    val left = if (isVertical) road.startX - road.width / 2f else road.startX
    val top = if (isVertical) road.startY else road.startY - road.width / 2f
    val width = if (isVertical) road.width else (road.endX - road.startX)
    val height = if (isVertical) (road.endY - road.startY) else road.width

    // Road asphalt
    scope.drawRect(
      color = Color(0xFF334155),
      topLeft = Offset(left, top),
      size = Size(width, height)
    )

    // Center divider line (yellow dashes)
    if (isVertical) {
      scope.drawLine(
        color = Color(0xFFFBBF24),
        start = Offset(road.startX, road.startY),
        end = Offset(road.endX, road.endY),
        strokeWidth = 3f
      )
    } else {
      scope.drawLine(
        color = Color(0xFFFBBF24),
        start = Offset(road.startX, road.startY),
        end = Offset(road.endX, road.endY),
        strokeWidth = 3f
      )
    }
  }
}

private fun drawBuildings(scope: DrawScope) {
  for (b in LosSantosWorld.buildings) {
    val r = b.rect
    val heightScale = b.height * 0.12f

    // 2.5D Drop Shadow
    scope.drawRect(
      color = Color(0x66000000),
      topLeft = Offset(r.left + heightScale * 0.6f, r.top + heightScale * 0.6f),
      size = Size(r.width, r.height)
    )

    // Base Walls
    scope.drawRect(
      color = Color(b.colorHex),
      topLeft = Offset(r.left, r.top),
      size = Size(r.width, r.height)
    )

    // Elevated Roof Slab
    val roofOffset = heightScale * 0.4f
    scope.drawRect(
      color = Color(b.roofColorHex),
      topLeft = Offset(r.left - roofOffset, r.top - roofOffset),
      size = Size(r.width, r.height)
    )

    // Roof details / helipads
    if (b.isSpecial && b.name.contains("MAZE BANK")) {
      // Golden Spire & Helipad
      val cx = (r.left + r.right) / 2f - roofOffset
      val cy = (r.top + r.bottom) / 2f - roofOffset
      scope.drawCircle(
        color = Color(0xFFFACC15),
        radius = 28f,
        center = Offset(cx, cy),
        style = Stroke(width = 4f)
      )
    }
  }
}

private fun drawLocationMarkers(scope: DrawScope) {
  for (loc in LosSantosWorld.specialLocations) {
    val color = when (loc.type) {
      "RESPRAY" -> Color(0xFF10B981)
      "GUN_STORE" -> Color(0xFFEF4444)
      "HEIST" -> Color(0xFFFACC15)
      "MISSION" -> Color(0xFF3B82F6)
      else -> Color(0xFFA855F7)
    }

    // Outer radar ring
    scope.drawCircle(
      color = color.copy(alpha = 0.35f),
      radius = 45f,
      center = Offset(loc.x, loc.y)
    )
    scope.drawCircle(
      color = color,
      radius = 16f,
      center = Offset(loc.x, loc.y)
    )
  }
}

private fun drawVehicle(
  scope: DrawScope,
  v: com.example.game.model.VehicleEntity,
  isPlayer: Boolean
) {
  val length = v.model.length
  val width = v.model.width

  scope.rotate(degrees = v.angleDeg, pivot = Offset(v.x, v.y)) {
    // Headlight beams if driving
    val rad = v.angleDeg * PI.toFloat() / 180f
    scope.drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0x55FEF08A), Color.Transparent),
        center = Offset(v.x + length * 0.6f, v.y),
        radius = 110f
      ),
      center = Offset(v.x + length * 0.6f, v.y),
      radius = 110f
    )

    // Car Body Shadow
    scope.drawRoundRect(
      color = Color(0x44000000),
      topLeft = Offset(v.x - length / 2f + 4f, v.y - width / 2f + 4f),
      size = Size(length, width),
      cornerRadius = CornerRadius(8f, 8f)
    )

    // Main Chassis
    scope.drawRoundRect(
      color = Color(v.color),
      topLeft = Offset(v.x - length / 2f, v.y - width / 2f),
      size = Size(length, width),
      cornerRadius = CornerRadius(8f, 8f)
    )

    // Windshield & Cabin Roof
    val cabinLength = length * 0.45f
    val cabinWidth = width * 0.75f
    scope.drawRoundRect(
      color = Color(0xFF0F172A),
      topLeft = Offset(v.x - cabinLength / 2f, v.y - cabinWidth / 2f),
      size = Size(cabinLength, cabinWidth),
      cornerRadius = CornerRadius(5f, 5f)
    )

    // Headlights (Front)
    scope.drawCircle(
      color = Color(0xFFFEF08A),
      radius = 4f,
      center = Offset(v.x + length / 2f - 3f, v.y - width / 2f + 6f)
    )
    scope.drawCircle(
      color = Color(0xFFFEF08A),
      radius = 4f,
      center = Offset(v.x + length / 2f - 3f, v.y + width / 2f - 6f)
    )

    // Taillights (Rear)
    scope.drawCircle(
      color = Color(0xFFDC2626),
      radius = 4f,
      center = Offset(v.x - length / 2f + 3f, v.y - width / 2f + 6f)
    )
    scope.drawCircle(
      color = Color(0xFFDC2626),
      radius = 4f,
      center = Offset(v.x - length / 2f + 3f, v.y + width / 2f - 6f)
    )

    // Police Siren Lightbar
    if (v.isPolice && v.sirenOn) {
      val now = System.currentTimeMillis()
      val isRedFlash = (now / 180) % 2 == 0L
      scope.drawRoundRect(
        color = if (isRedFlash) Color(0xFFEF4444) else Color(0xFF3B82F6),
        topLeft = Offset(v.x - 4f, v.y - cabinWidth / 2f + 2f),
        size = Size(8f, cabinWidth - 4f),
        cornerRadius = CornerRadius(2f, 2f)
      )
    }

    // Helicopter blades
    if (v.isHelicopter) {
      val rotorAngle = (System.currentTimeMillis() % 360).toFloat() * 4f
      scope.rotate(rotorAngle, pivot = Offset(v.x, v.y)) {
        scope.drawLine(
          color = Color(0xAAFFFFFF),
          start = Offset(v.x - 70f, v.y),
          end = Offset(v.x + 70f, v.y),
          strokeWidth = 5f
        )
      }
    }
  }
}

private fun drawWreckedVehicle(scope: DrawScope, v: com.example.game.model.VehicleEntity) {
  scope.rotate(degrees = v.angleDeg, pivot = Offset(v.x, v.y)) {
    scope.drawRoundRect(
      color = Color(0xFF1E1E1E),
      topLeft = Offset(v.x - v.model.length / 2f, v.y - v.model.width / 2f),
      size = Size(v.model.length, v.model.width),
      cornerRadius = CornerRadius(6f, 6f)
    )
  }
  // Smoke plume from engine
  scope.drawCircle(
    color = Color(0x88333333),
    radius = 24f,
    center = Offset(v.x, v.y)
  )
}

private fun drawPedestrian(
  scope: DrawScope,
  x: Float,
  y: Float,
  angleDeg: Float,
  isFleeing: Boolean,
  isPolice: Boolean
) {
  scope.rotate(degrees = angleDeg, pivot = Offset(x, y)) {
    // Shoulders
    scope.drawRoundRect(
      color = if (isPolice) Color(0xFF1E3A8A) else Color(0xFF475569),
      topLeft = Offset(x - 9f, y - 6f),
      size = Size(18f, 12f),
      cornerRadius = CornerRadius(4f, 4f)
    )
    // Head
    scope.drawCircle(
      color = if (isFleeing) Color(0xFFFBBF24) else Color(0xFFFED7AA),
      radius = 5.5f,
      center = Offset(x, y)
    )
  }
}

private fun drawPlayerCharacter(
  scope: DrawScope,
  x: Float,
  y: Float,
  angleDeg: Float,
  character: CharacterType,
  isAbilityActive: Boolean
) {
  // Glow aura if special ability is active
  if (isAbilityActive) {
    scope.drawCircle(
      color = character.themeColor.copy(alpha = 0.4f),
      radius = 32f,
      center = Offset(x, y)
    )
  }

  scope.rotate(degrees = angleDeg, pivot = Offset(x, y)) {
    // Shoulders
    scope.drawRoundRect(
      color = character.themeColor,
      topLeft = Offset(x - 11f, y - 8f),
      size = Size(22f, 16f),
      cornerRadius = CornerRadius(5f, 5f)
    )

    // Hands holding weapon
    scope.drawCircle(
      color = Color(0xFF1E293B),
      radius = 3.5f,
      center = Offset(x + 12f, y - 4f)
    )
    scope.drawCircle(
      color = Color(0xFF1E293B),
      radius = 3.5f,
      center = Offset(x + 12f, y + 4f)
    )

    // Head
    scope.drawCircle(
      color = Color(0xFFFED7AA),
      radius = 7f,
      center = Offset(x, y)
    )

    // Cap/hair indicator
    val capColor = when (character) {
      CharacterType.FRANKLIN -> Color(0xFF15803D) // Green cap
      CharacterType.MICHAEL -> Color(0xFF334155)  // Slicked dark hair
      CharacterType.TREVOR -> Color(0xFF94A3B8)   // Receding grey hair
    }
    scope.drawCircle(
      color = capColor,
      radius = 5.5f,
      center = Offset(x - 1f, y)
    )
  }
}

private fun drawMissionWaypoint(
  scope: DrawScope,
  x: Float,
  y: Float,
  title: String
) {
  val pulse = (System.currentTimeMillis() % 1000) / 1000f
  scope.drawCircle(
    color = Color(0xFFFACC15).copy(alpha = 1f - pulse),
    radius = 40f + pulse * 25f,
    center = Offset(x, y),
    style = Stroke(width = 4f)
  )
  scope.drawCircle(
    color = Color(0xFFFACC15),
    radius = 16f,
    center = Offset(x, y)
  )
}
