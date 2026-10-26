package com.example.game.world

import androidx.compose.ui.geometry.Rect
import com.example.game.model.DistrictArea

object LosSantosWorld {
  const val WORLD_WIDTH = 4200f
  const val WORLD_HEIGHT = 4200f

  val districts = listOf(
    DistrictArea("VINEWOOD HILLS", 1200f, 3200f, 0f, 1100f, "#22C55E"),
    DistrictArea("DOWNTOWN LOS SANTOS", 1200f, 3000f, 1100f, 2400f, "#3B82F6"),
    DistrictArea("DEL PERRO & PIER", 0f, 1200f, 1000f, 2600f, "#06B6D4"),
    DistrictArea("SOUTH LOS SANTOS", 800f, 2800f, 2400f, 3500f, "#EAB308"),
    DistrictArea("PORT OF LOS SANTOS", 800f, 3400f, 3500f, 4200f, "#64748B"),
    DistrictArea("SANDY SHORES", 3000f, 4200f, 0f, 2000f, "#F97316"),
    DistrictArea("FORT ZANCUDO", 3000f, 4200f, 2000f, 3400f, "#EF4444")
  )

  fun getDistrictFor(x: Float, y: Float): String {
    for (d in districts) {
      if (x in d.minX..d.maxX && y in d.minY..d.maxY) {
        return d.name
      }
    }
    return "LOS SANTOS COUNTY"
  }

  data class RoadSegment(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val width: Float = 64f,
    val isHighway: Boolean = false
  )

  // Major road network
  val roads = listOf(
    // Del Perro Freeway (Horizontal)
    RoadSegment(200f, 1100f, 4000f, 1100f, width = 80f, isHighway = true),
    // Olympic Freeway (Horizontal)
    RoadSegment(300f, 2400f, 4100f, 2400f, width = 80f, isHighway = true),
    // South LS Cross-boulevard
    RoadSegment(500f, 3400f, 3800f, 3400f, width = 70f, isHighway = false),
    // Vinewood Hills Boulevard
    RoadSegment(1200f, 450f, 3200f, 450f, width = 60f, isHighway = false),

    // Coastal Highway (Vertical West)
    RoadSegment(650f, 400f, 650f, 3800f, width = 74f, isHighway = true),
    // Vinewood to South LS Grid (Vertical Central 1)
    RoadSegment(1500f, 200f, 1500f, 4000f, width = 70f, isHighway = false),
    // Downtown Main Street (Vertical Central 2)
    RoadSegment(2100f, 200f, 2100f, 4000f, width = 84f, isHighway = true),
    // East Downtown / Industrial (Vertical Central 3)
    RoadSegment(2700f, 300f, 2700f, 4100f, width = 70f, isHighway = false),
    // Desert Senora Highway (Vertical East)
    RoadSegment(3400f, 200f, 3400f, 3800f, width = 78f, isHighway = true),

    // Downtown Secondary Streets (Grid connections)
    RoadSegment(1200f, 1600f, 3000f, 1600f, width = 64f, isHighway = false),
    RoadSegment(1200f, 2000f, 3000f, 2000f, width = 64f, isHighway = false),
    RoadSegment(1800f, 1100f, 1800f, 2400f, width = 64f, isHighway = false),
    RoadSegment(2400f, 1100f, 2400f, 2400f, width = 64f, isHighway = false),

    // South LS Streets
    RoadSegment(800f, 2900f, 2800f, 2900f, width = 60f, isHighway = false),
    RoadSegment(1200f, 2400f, 1200f, 3500f, width = 60f, isHighway = false),

    // Sandy Shores Airstrip
    RoadSegment(3200f, 1300f, 4100f, 1300f, width = 110f, isHighway = true)
  )

  data class BuildingBlock(
    val rect: Rect,
    val name: String,
    val height: Float,
    val colorHex: Long,
    val roofColorHex: Long,
    val isSpecial: Boolean = false
  )

  // Buildings and structures
  val buildings = listOf(
    // DOWNTOWN SKYSCRAPERS
    BuildingBlock(Rect(1950f, 1620f, 2250f, 1880f), "MAZE BANK TOWER", 280f, 0xFF1E293B, 0xFF334155, isSpecial = true),
    BuildingBlock(Rect(1620f, 1250f, 1780f, 1480f), "FIB HEADQUARTERS", 220f, 0xFF0F172A, 0xFF1E293B),
    BuildingBlock(Rect(1850f, 1250f, 2050f, 1480f), "IAA BUILDING", 200f, 0xFF1E293B, 0xFF334155),
    BuildingBlock(Rect(2200f, 1250f, 2380f, 1480f), "UNION DEPOSITORY", 180f, 0xFF334155, 0xFF475569, isSpecial = true),
    BuildingBlock(Rect(2480f, 1250f, 2650f, 1500f), "ARCADIUS BUSINESS", 210f, 0xFF0F172A, 0xFF1E293B),

    BuildingBlock(Rect(1600f, 1700f, 1750f, 1950f), "DOWNTOWN PLAZA", 160f, 0xFF334155, 0xFF475569),
    BuildingBlock(Rect(2200f, 2080f, 2380f, 2300f), "METRO STATION", 70f, 0xFF475569, 0xFF64748B),
    BuildingBlock(Rect(2480f, 1700f, 2650f, 1950f), "LOS SANTOS CUSTOMS", 55f, 0xFF1E293B, 0xFFEAB308, isSpecial = true),

    // VINEWOOD HILLS
    BuildingBlock(Rect(1700f, 250f, 1950f, 400f), "GALILEO OBSERVATORY", 80f, 0xFFE2E8F0, 0xFFCBD5E1, isSpecial = true),
    BuildingBlock(Rect(2100f, 320f, 2350f, 420f), "MICHAEL'S MANSION", 45f, 0xFFE2E8F0, 0xFF94A3B8, isSpecial = true),
    BuildingBlock(Rect(2500f, 280f, 2750f, 400f), "VINEWOOD LUXURY VILLA", 50f, 0xFFCBD5E1, 0xFF94A3B8),
    BuildingBlock(Rect(1350f, 300f, 1550f, 420f), "HILLS ESTATE", 48f, 0xFFE2E8F0, 0xFFCBD5E1),

    // SOUTH LOS SANTOS
    BuildingBlock(Rect(1520f, 2650f, 1720f, 2820f), "PAY 'N' SPRAY GARAGE", 40f, 0xFF1E293B, 0xFF10B981, isSpecial = true),
    BuildingBlock(Rect(1920f, 2650f, 2080f, 2820f), "AMMU-NATION GUNS", 45f, 0xFF334155, 0xFFEF4444, isSpecial = true),
    BuildingBlock(Rect(1350f, 3050f, 1520f, 3250f), "FRANKLIN'S HOUSE", 35f, 0xFF475569, 0xFF10B981, isSpecial = true),
    BuildingBlock(Rect(1700f, 3050f, 1950f, 3280f), "GROVE STREET RESIDENCES", 35f, 0xFF64748B, 0xFF475569),
    BuildingBlock(Rect(2200f, 3050f, 2400f, 3280f), "SIMEON CAR REPO LOT", 40f, 0xFF1E293B, 0xFF3B82F6, isSpecial = true),

    // DEL PERRO BEACH
    BuildingBlock(Rect(400f, 1700f, 550f, 1950f), "DEL PERRO PIER BOARDWALK", 30f, 0xFFD97706, 0xFFF59E0B, isSpecial = true),
    BuildingBlock(Rect(750f, 1300f, 920f, 1550f), "BEACHFRONT CONDOS", 90f, 0xFFE2E8F0, 0xFF38BDF8),
    BuildingBlock(Rect(750f, 1750f, 920f, 2000f), "PACIFIC HOTEL", 110f, 0xFFCBD5E1, 0xFF0284C7),

    // SANDY SHORES
    BuildingBlock(Rect(3650f, 750f, 3850f, 900f), "TREVOR'S TRAILER & LAB", 30f, 0xFF78350F, 0xFFF97316, isSpecial = true),
    BuildingBlock(Rect(3250f, 1150f, 3400f, 1260f), "AIRSTRIP HANGAR", 50f, 0xFF475569, 0xFF64748B, isSpecial = true),

    // FORT ZANCUDO
    BuildingBlock(Rect(3200f, 2200f, 3450f, 2450f), "MILITARY RADAR BUNKER", 95f, 0xFF1C1917, 0xFF292524, isSpecial = true),
    BuildingBlock(Rect(3550f, 2200f, 3850f, 2500f), "JET & TANK HANGAR", 70f, 0xFF292524, 0xFF44403C)
  )

  data class SpecialLocation(
    val name: String,
    val x: Float,
    val y: Float,
    val type: String,
    val description: String
  )

  val specialLocations = listOf(
    SpecialLocation("Pay 'n' Spray", 1620f, 2735f, "RESPRAY", "Repair vehicle and lose all wanted stars"),
    SpecialLocation("Ammu-Nation", 2000f, 2735f, "GUN_STORE", "Stock up on weapons, rockets, and body armor"),
    SpecialLocation("Maze Bank Tower", 2100f, 1750f, "HEIST", "Central bank vault for major scores"),
    SpecialLocation("Simeon's Dealership", 2300f, 3165f, "MISSION", "High-value exotic car repossession jobs"),
    SpecialLocation("Trevor's Outpost", 3750f, 825f, "TREVOR_BASE", "Desert hideout with off-road vehicles & RPGs"),
    SpecialLocation("Del Perro Pier", 475f, 1825f, "LANDMARK", "Scenic boardwalk with ferris wheel & beach bikes"),
    SpecialLocation("Galileo Observatory", 1825f, 325f, "LANDMARK", "Panoramic lookout over the entire city of Los Santos")
  )
}
