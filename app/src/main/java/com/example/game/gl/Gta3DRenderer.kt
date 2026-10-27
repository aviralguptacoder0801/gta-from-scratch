package com.example.game.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.GameEngine
import com.example.game.model.CharacterType
import com.example.game.model.VehicleModel
import com.example.game.world.LosSantosWorld
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance 3D GPU Renderer for Los Santos:
 * - High-detail realistic roads with dashed center lines, solid shoulder lines, curbs, sidewalks, and zebra crosswalks
 * - Multi-biome terrain: Pacific Ocean coastline, Del Perro golden beach, lush emerald grass, Legion Square park, and Sandy Shores desert
 * - 3D Streetlamps along avenues with glowing emissive warm amber bulbs and ground light puddles
 * - 3D Traffic signals with emissive red, amber, and green lights
 * - 3D Foliage: California palm trees, manicured park hedges, and roadside bushes
 * - Landmark detailing: Maze Bank helipad & glass tower bands, Ammu-Nation & Pay 'n' Spray signage
 * - Zero per-frame allocations for silky-smooth 60 FPS in landscape mode
 */
class Gta3DRenderer(val engine: GameEngine) : GLSurfaceView.Renderer {

  private var programId = 0

  // Shader Uniform Locations
  private var uMVPMatrixLoc = 0
  private var uMVMatrixLoc = 0
  private var uLightPosLoc = 0
  private var uFogColorLoc = 0
  private var uFogNearLoc = 0
  private var uFogFarLoc = 0
  private var uEmissiveLoc = 0
  private var uPoliceLightColorLoc = 0
  private var uPoliceLightIntensityLoc = 0

  // Shader Attribute Locations
  private var aPositionLoc = 0
  private var aNormalLoc = 0
  private var aColorLoc = 0

  // Pre-allocated Matrices (ZERO garbage collection during render loop)
  private val mProjectionMatrix = FloatArray(16)
  private val mViewMatrix = FloatArray(16)
  private val mModelMatrix = FloatArray(16)
  private val mMVPMatrix = FloatArray(16)
  private val mMVMatrix = FloatArray(16)
  private val mTempMatrix = FloatArray(16)
  private val weaponProjMatrix = FloatArray(16)
  private val weaponViewMatrix = FloatArray(16)

  // Consolidated Static Meshes (pre-batched for ultra-fast GPU rendering)
  private var terrainMesh: MeshData? = null
  private var mountainMesh: MeshData? = null
  private var oceanMesh: MeshData? = null
  private var beachMesh: MeshData? = null
  private var desertMesh: MeshData? = null
  private var parksAndGrassMesh: MeshData? = null
  private var roadsMesh: MeshData? = null
  private var sidewalksMesh: MeshData? = null
  private var roadStripesMesh: MeshData? = null
  private var foliageMesh: MeshData? = null
  private var streetlampsSolidMesh: MeshData? = null
  private var streetlampsEmissiveMesh: MeshData? = null
  private var trafficSignalsSolidMesh: MeshData? = null
  private var trafficSignalsEmissiveMesh: MeshData? = null
  private var streetPropsSolidMesh: MeshData? = null
  private var streetPropsEmissiveMesh: MeshData? = null
  private var highwayGantriesSolidMesh: MeshData? = null
  private var highwayGantriesEmissiveMesh: MeshData? = null
  private var billboardsSolidMesh: MeshData? = null
  private var billboardsEmissiveMesh: MeshData? = null
  private val buildingMeshes = mutableListOf<Pair<MeshData, FloatArray>>()
  private var buildingEmissivesMesh: MeshData? = null

  // Smooth camera interpolation state
  private var camSmoothEyeX = 0f
  private var camSmoothEyeY = 0f
  private var camSmoothEyeZ = 20f
  private var camInitialized = false

  // Realistic Dynamic Vehicles (Solid + Glowing Emissives)
  private var supercarSolidMesh: MeshData? = null
  private var supercarEmissiveMesh: MeshData? = null
  private var buffaloSolidMesh: MeshData? = null
  private var buffaloEmissiveMesh: MeshData? = null
  private var policeSolidMesh: MeshData? = null
  private var policeEmissiveMesh: MeshData? = null
  private var tankSolidMesh: MeshData? = null
  private var firstPersonCockpitMesh: MeshData? = null
  private val weaponViewModelMeshes = mutableMapOf<Int, MeshData>()

  // Realistic 3D Human Characters & Animated Pedestrians
  private var playerFranklinMesh: MeshData? = null
  private var playerMichaelMesh: MeshData? = null
  private var playerTrevorMesh: MeshData? = null
  private val pedestrianWalkMeshes = Array(3) { arrayOfNulls<MeshData>(4) }
  private val policeOfficerWalkMeshes = arrayOfNulls<MeshData>(4)

  private var viewportWidth = 1920
  private var viewportHeight = 1080

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    programId = Gta3DShader.createProgram()

    uMVPMatrixLoc = GLES20.glGetUniformLocation(programId, "u_MVPMatrix")
    uMVMatrixLoc = GLES20.glGetUniformLocation(programId, "u_MVMatrix")
    uLightPosLoc = GLES20.glGetUniformLocation(programId, "u_LightPos")
    uFogColorLoc = GLES20.glGetUniformLocation(programId, "u_FogColor")
    uFogNearLoc = GLES20.glGetUniformLocation(programId, "u_FogNear")
    uFogFarLoc = GLES20.glGetUniformLocation(programId, "u_FogFar")
    uEmissiveLoc = GLES20.glGetUniformLocation(programId, "u_Emissive")
    uPoliceLightColorLoc = GLES20.glGetUniformLocation(programId, "u_PoliceLightColor")
    uPoliceLightIntensityLoc = GLES20.glGetUniformLocation(programId, "u_PoliceLightIntensity")

    aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position")
    aNormalLoc = GLES20.glGetAttribLocation(programId, "a_Normal")
    aColorLoc = GLES20.glGetAttribLocation(programId, "a_Color")

    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glDepthFunc(GLES20.GL_LEQUAL)
    GLES20.glDisable(GLES20.GL_CULL_FACE)

    // Los Santos Sunny California Sky
    GLES20.glClearColor(0.35f, 0.65f, 0.95f, 1.0f)

    buildWorldGeometry()
  }

  private fun buildWorldGeometry() {
    // -------------------------------------------------------------
    // 0. DISTANT MOUNTAIN RIDGE SILHOUETTE (Mount Chiliad & Vinewood)
    // -------------------------------------------------------------
    val mtnBuilder = MeshBuilder(4096)
    Gta3DGeometry.addMountainBackdrop(mtnBuilder)
    mountainMesh = mtnBuilder.build()

    // -------------------------------------------------------------
    // 1. TERRAIN & BIOMES
    // -------------------------------------------------------------
    // Base Green Coastal Turf
    val grassCol = floatArrayOf(0.24f, 0.48f, 0.26f, 1f)
    terrainMesh = Gta3DGeometry.buildGroundQuad(
      0f, 0f,
      LosSantosWorld.WORLD_WIDTH, 0f,
      LosSantosWorld.WORLD_WIDTH, LosSantosWorld.WORLD_HEIGHT,
      0f, LosSantosWorld.WORLD_HEIGHT,
      z = 0f,
      color = grassCol
    )

    // Pacific Ocean (West coastline x: 0..400)
    val oceanCol = floatArrayOf(0.12f, 0.44f, 0.76f, 1f)
    oceanMesh = Gta3DGeometry.buildGroundQuad(
      0f, 0f, 400f, 0f, 400f, LosSantosWorld.WORLD_HEIGHT, 0f, LosSantosWorld.WORLD_HEIGHT,
      z = 0.2f, color = oceanCol
    )

    // Del Perro Beach Golden Sand (x: 400..600)
    val beachCol = floatArrayOf(0.92f, 0.84f, 0.62f, 1f)
    beachMesh = Gta3DGeometry.buildGroundQuad(
      400f, 0f, 600f, 0f, 600f, LosSantosWorld.WORLD_HEIGHT, 400f, LosSantosWorld.WORLD_HEIGHT,
      z = 0.3f, color = beachCol
    )

    // Sandy Shores Desert Clay & Sand (Northeast sector x: 2950..4200, y: 0..1900)
    val desertCol = floatArrayOf(0.84f, 0.70f, 0.48f, 1f)
    desertMesh = Gta3DGeometry.buildGroundQuad(
      2950f, 0f, LosSantosWorld.WORLD_WIDTH, 0f,
      LosSantosWorld.WORLD_WIDTH, 1900f, 2950f, 1900f,
      z = 0.25f, color = desertCol
    )

    // -------------------------------------------------------------
    // 2. PARKS, LAWNS & PLAZAS
    // -------------------------------------------------------------
    val parkBuilder = MeshBuilder(4096)
    val lushEmerald = floatArrayOf(0.18f, 0.62f, 0.24f, 1f)
    val manicuredLawn = floatArrayOf(0.22f, 0.56f, 0.22f, 1f)
    val stonePath = floatArrayOf(0.74f, 0.74f, 0.76f, 1f)
    val waterBlue = floatArrayOf(0.20f, 0.75f, 0.95f, 1f)
    val flowerBorder = floatArrayOf(0.85f, 0.25f, 0.35f, 1f)

    // A. Legion Square Downtown Central Park (x: 1620..1980, y: 1720..2020)
    parkBuilder.addGroundQuad(1620f, 1720f, 1980f, 1720f, 1980f, 2020f, 1620f, 2020f, 0.6f, lushEmerald)
    // Cross promenades
    parkBuilder.addGroundQuad(1620f, 1855f, 1980f, 1855f, 1980f, 1885f, 1620f, 1885f, 0.7f, stonePath)
    parkBuilder.addGroundQuad(1785f, 1720f, 1815f, 1720f, 1815f, 2020f, 1785f, 2020f, 0.7f, stonePath)
    // Central fountain basin
    parkBuilder.addBox(1775f, 1845f, 0.6f, 1825f, 1895f, 3.2f, stonePath, stonePath)
    parkBuilder.addGroundQuad(1778f, 1848f, 1822f, 1848f, 1822f, 1892f, 1778f, 1892f, 3.1f, waterBlue)
    // Fountain center obelisk
    parkBuilder.addBox(1796f, 1866f, 3.2f, 1804f, 1874f, 14f, floatArrayOf(0.90f, 0.90f, 0.92f, 1f))
    // Flowerbeds around square
    parkBuilder.addBox(1640f, 1740f, 0.6f, 1760f, 1755f, 1.4f, flowerBorder, lushEmerald)
    parkBuilder.addBox(1840f, 1740f, 0.6f, 1960f, 1755f, 1.4f, flowerBorder, lushEmerald)
    parkBuilder.addBox(1640f, 1985f, 0.6f, 1760f, 2000f, 1.4f, flowerBorder, lushEmerald)
    parkBuilder.addBox(1840f, 1985f, 0.6f, 1960f, 2000f, 1.4f, flowerBorder, lushEmerald)

    // B. Vinewood Hills Terraced Lawns (Around Michael's Mansion & Observatory)
    parkBuilder.addGroundQuad(2050f, 260f, 2400f, 260f, 2400f, 480f, 2050f, 480f, 0.5f, manicuredLawn)
    parkBuilder.addGroundQuad(1650f, 200f, 2000f, 200f, 2000f, 440f, 1650f, 440f, 0.5f, manicuredLawn)

    // C. South Los Santos Neighborhood Lawns (Grove Street & Franklin's)
    parkBuilder.addGroundQuad(1300f, 3000f, 1600f, 3000f, 1600f, 3320f, 1300f, 3320f, 0.5f, manicuredLawn)
    parkBuilder.addGroundQuad(1650f, 3000f, 2020f, 3000f, 2020f, 3320f, 1650f, 3320f, 0.5f, manicuredLawn)

    parksAndGrassMesh = parkBuilder.build()

    // -------------------------------------------------------------
    // 3. REALISTIC ROADS, SIDEWALKS & MARKINGS
    // -------------------------------------------------------------
    val roadBuilder = MeshBuilder(4096)
    val sidewalkBuilder = MeshBuilder(4096)
    val stripeBuilder = MeshBuilder(8192)

    val asphaltCol = floatArrayOf(0.17f, 0.18f, 0.20f, 1f)
    val sidewalkCol = floatArrayOf(0.70f, 0.72f, 0.74f, 1f)
    val curbBevelCol = floatArrayOf(0.56f, 0.58f, 0.60f, 1f)
    val yellowStripe = floatArrayOf(0.98f, 0.82f, 0.12f, 1f)
    val whiteStripe = floatArrayOf(0.95f, 0.95f, 0.96f, 1f)

    for (road in LosSantosWorld.roads) {
      val isVert = road.startX == road.endX
      val totalW = road.width
      val curbW = 10f
      val asphaltW = totalW - curbW * 2f

      val (rx1, ry1, rx2, ry2) = if (isVert) {
        listOf(road.startX - asphaltW / 2f, road.startY, road.startX + asphaltW / 2f, road.endY)
      } else {
        listOf(road.startX, road.startY - asphaltW / 2f, road.endX, road.startY + asphaltW / 2f)
      }

      // Asphalt roadway surface
      roadBuilder.addGroundQuad(rx1, ry1, rx2, ry1, rx2, ry2, rx1, ry2, z = 0.55f, col = asphaltCol)

      // Sidewalks on left and right sides
      if (isVert) {
        // Left sidewalk
        sidewalkBuilder.addBox(rx1 - curbW, ry1, 0f, rx1, ry2, 0.85f, curbBevelCol, sidewalkCol)
        // Right sidewalk
        sidewalkBuilder.addBox(rx2, ry1, 0f, rx2 + curbW, ry2, 0.85f, curbBevelCol, sidewalkCol)

        // White solid lane edge lines
        stripeBuilder.addGroundQuad(rx1 + 1.2f, ry1, rx1 + 3.0f, ry1, rx1 + 3.0f, ry2, rx1 + 1.2f, ry2, 0.62f, whiteStripe)
        stripeBuilder.addGroundQuad(rx2 - 3.0f, ry1, rx2 - 1.2f, ry1, rx2 - 1.2f, ry2, rx2 - 3.0f, ry2, 0.62f, whiteStripe)

        // Dashed Center Divider Stripe (18f dash, 14f gap)
        val cx = road.startX
        var curY = road.startY
        while (curY < road.endY) {
          val dashLen = minOf(18f, road.endY - curY)
          if (road.isHighway) {
            // Double yellow for highways
            stripeBuilder.addGroundQuad(cx - 3.2f, curY, cx - 1.2f, curY, cx - 1.2f, curY + dashLen, cx - 3.2f, curY + dashLen, 0.65f, yellowStripe)
            stripeBuilder.addGroundQuad(cx + 1.2f, curY, cx + 3.2f, curY, cx + 3.2f, curY + dashLen, cx + 1.2f, curY + dashLen, 0.65f, yellowStripe)
          } else {
            // Single yellow stripe
            stripeBuilder.addGroundQuad(cx - 1.4f, curY, cx + 1.4f, curY, cx + 1.4f, curY + dashLen, cx - 1.4f, curY + dashLen, 0.65f, yellowStripe)
          }
          curY += 32f
        }
      } else {
        // Left (top) sidewalk
        sidewalkBuilder.addBox(rx1, ry1 - curbW, 0f, rx2, ry1, 0.85f, curbBevelCol, sidewalkCol)
        // Right (bottom) sidewalk
        sidewalkBuilder.addBox(rx1, ry2, 0f, rx2, ry2 + curbW, 0.85f, curbBevelCol, sidewalkCol)

        // White solid lane edge lines
        stripeBuilder.addGroundQuad(rx1, ry1 + 1.2f, rx2, ry1 + 1.2f, rx2, ry1 + 3.0f, rx1, ry1 + 3.0f, 0.62f, whiteStripe)
        stripeBuilder.addGroundQuad(rx1, ry2 - 3.0f, rx2, ry2 - 3.0f, rx2, ry2 - 1.2f, rx1, ry2 - 1.2f, 0.62f, whiteStripe)

        // Dashed Center Divider Stripe
        val cy = road.startY
        var curX = road.startX
        while (curX < road.endX) {
          val dashLen = minOf(18f, road.endX - curX)
          if (road.isHighway) {
            stripeBuilder.addGroundQuad(curX, cy - 3.2f, curX + dashLen, cy - 3.2f, curX + dashLen, cy - 1.2f, curX, cy - 1.2f, 0.65f, yellowStripe)
            stripeBuilder.addGroundQuad(curX, cy + 1.2f, curX + dashLen, cy + 1.2f, curX + dashLen, cy + 3.2f, curX, cy + 3.2f, 0.65f, yellowStripe)
          } else {
            stripeBuilder.addGroundQuad(curX, cy - 1.4f, curX + dashLen, cy - 1.4f, curX + dashLen, cy + 1.4f, curX, cy + 1.4f, 0.65f, yellowStripe)
          }
          curX += 32f
        }
      }
    }

    // Zebra Crosswalks at major intersections
    val intersections = listOf(
      Pair(1500f, 1100f), Pair(1800f, 1100f), Pair(2100f, 1100f), Pair(2400f, 1100f), Pair(2700f, 1100f),
      Pair(1500f, 1600f), Pair(1800f, 1600f), Pair(2100f, 1600f), Pair(2400f, 1600f), Pair(2700f, 1600f),
      Pair(1500f, 2000f), Pair(1800f, 2000f), Pair(2100f, 2000f), Pair(2400f, 2000f), Pair(2700f, 2000f),
      Pair(1500f, 2400f), Pair(1800f, 2400f), Pair(2100f, 2400f), Pair(2400f, 2400f), Pair(2700f, 2400f),
      Pair(1500f, 2900f), Pair(2100f, 2900f), Pair(2700f, 2900f),
      Pair(650f, 1100f), Pair(650f, 2400f), Pair(3400f, 1100f), Pair(3400f, 2400f)
    )

    for ((ix, iy) in intersections) {
      val span = 36f
      // 4 approach crosswalks: North, South, East, West
      // North approach
      for (k in -3..3) {
        val barX = ix + k * 8f
        stripeBuilder.addGroundQuad(barX - 2.2f, iy - span - 12f, barX + 2.2f, iy - span - 12f, barX + 2.2f, iy - span, barX - 2.2f, iy - span, 0.68f, whiteStripe)
      }
      // South approach
      for (k in -3..3) {
        val barX = ix + k * 8f
        stripeBuilder.addGroundQuad(barX - 2.2f, iy + span, barX + 2.2f, iy + span, barX + 2.2f, iy + span + 12f, barX - 2.2f, iy + span + 12f, 0.68f, whiteStripe)
      }
      // West approach
      for (k in -3..3) {
        val barY = iy + k * 8f
        stripeBuilder.addGroundQuad(ix - span - 12f, barY - 2.2f, ix - span, barY - 2.2f, ix - span, barY + 2.2f, ix - span - 12f, barY + 2.2f, 0.68f, whiteStripe)
      }
      // East approach
      for (k in -3..3) {
        val barY = iy + k * 8f
        stripeBuilder.addGroundQuad(ix + span, barY - 2.2f, ix + span + 12f, barY - 2.2f, ix + span + 12f, barY + 2.2f, ix + span, barY + 2.2f, 0.68f, whiteStripe)
      }

      // Intersection Road Details: Embedded Manhole Cover & Painted Direction Arrows
      Gta3DGeometry.addRoadSurfaceDetails(stripeBuilder, ix, iy, isVert = true)
      Gta3DGeometry.addRoadSurfaceDetails(stripeBuilder, ix, iy, isVert = false)
    }

    roadsMesh = roadBuilder.build()
    sidewalksMesh = sidewalkBuilder.build()
    roadStripesMesh = stripeBuilder.build()

    // -------------------------------------------------------------
    // 4. 3D STREETLAMPS & GLOWING EMISSIVE BULBS
    // -------------------------------------------------------------
    val lampSolidBuilder = MeshBuilder(4096)
    val lampEmissiveBuilder = MeshBuilder(2048)

    // Streetlamps placed along Downtown Main Street (x = 2100), Del Perro (y = 1100), Olympic Fwy (y = 2400), and South LS
    val lampPoints = mutableListOf<Pair<Pair<Float, Float>, Pair<Float, Float>>>()

    // Along Downtown Main Street (x = 2100)
    for (y in 300..3800 step 180) {
      lampPoints.add(Pair(Pair(2055f, y.toFloat()), Pair(1f, 0f))) // West sidewalk aiming East
      lampPoints.add(Pair(Pair(2145f, (y + 90).toFloat()), Pair(-1f, 0f))) // East sidewalk aiming West
    }
    // Along Central Avenue (x = 1500)
    for (y in 400..3700 step 220) {
      lampPoints.add(Pair(Pair(1460f, y.toFloat()), Pair(1f, 0f)))
      lampPoints.add(Pair(Pair(1540f, (y + 110).toFloat()), Pair(-1f, 0f)))
    }
    // Along Del Perro Boulevard (y = 1100)
    for (x in 700..3800 step 200) {
      lampPoints.add(Pair(Pair(x.toFloat(), 1055f), Pair(0f, 1f)))
      lampPoints.add(Pair(Pair((x + 100).toFloat(), 1145f), Pair(0f, -1f)))
    }
    // Along Olympic Freeway (y = 2400)
    for (x in 700..3800 step 220) {
      lampPoints.add(Pair(Pair(x.toFloat(), 2355f), Pair(0f, 1f)))
      lampPoints.add(Pair(Pair((x + 110).toFloat(), 2445f), Pair(0f, -1f)))
    }
    // Along Vinewood Hills Boulevard (y = 450)
    for (x in 1300..3100 step 190) {
      lampPoints.add(Pair(Pair(x.toFloat(), 415f), Pair(0f, 1f)))
    }

    for ((pos, dir) in lampPoints) {
      Gta3DGeometry.addStreetLamp(
        solidBuilder = lampSolidBuilder,
        emissiveBuilder = lampEmissiveBuilder,
        x = pos.first,
        y = pos.second,
        height = 28f,
        armDirX = dir.first,
        armDirY = dir.second
      )
    }

    streetlampsSolidMesh = lampSolidBuilder.build()
    streetlampsEmissiveMesh = lampEmissiveBuilder.build()

    // -------------------------------------------------------------
    // 5. 3D TRAFFIC SIGNALS AT INTERSECTIONS
    // -------------------------------------------------------------
    val signalSolidBuilder = MeshBuilder(2048)
    val signalEmissiveBuilder = MeshBuilder(1024)

    for ((ix, iy) in intersections) {
      // Northeast corner signal
      Gta3DGeometry.addTrafficSignal(signalSolidBuilder, signalEmissiveBuilder, ix + 42f, iy - 42f, -1f, 0f)
      // Southwest corner signal
      Gta3DGeometry.addTrafficSignal(signalSolidBuilder, signalEmissiveBuilder, ix - 42f, iy + 42f, 1f, 0f)
    }

    trafficSignalsSolidMesh = signalSolidBuilder.build()
    trafficSignalsEmissiveMesh = signalEmissiveBuilder.build()

    // -------------------------------------------------------------
    // 5B. REALISTIC URBAN STREET FURNITURE & PROPS
    // -------------------------------------------------------------
    val propSolidBuilder = MeshBuilder(4096)
    val propEmissiveBuilder = MeshBuilder(2048)

    // A. Fire Hydrants along curbs at intersections and midway points
    val hydrantPoints = listOf(
      Pair(1475f, 1075f), Pair(1825f, 1075f), Pair(2125f, 1075f), Pair(2425f, 1075f),
      Pair(1475f, 1575f), Pair(1825f, 1575f), Pair(2125f, 1575f), Pair(2425f, 1575f),
      Pair(1475f, 1975f), Pair(1825f, 1975f), Pair(2125f, 1975f), Pair(2425f, 1975f),
      Pair(1475f, 2375f), Pair(1825f, 2375f), Pair(2125f, 2375f), Pair(2425f, 2375f),
      Pair(1475f, 2875f), Pair(2125f, 2875f), Pair(675f, 1075f), Pair(3375f, 1075f)
    )
    for ((hx, hy) in hydrantPoints) {
      Gta3DGeometry.addFireHydrant(propSolidBuilder, hx, hy)
    }

    // B. Curbside Trash & Recycling Bins
    val binPoints = listOf(
      Pair(Pair(1465f, 1250f), true), Pair(Pair(1535f, 1750f), true),
      Pair(Pair(2065f, 1350f), true), Pair(Pair(2135f, 1850f), true),
      Pair(Pair(1465f, 2980f), true), Pair(Pair(1680f, 2980f), true),
      Pair(Pair(1920f, 1065f), false), Pair(Pair(2250f, 1065f), false),
      Pair(Pair(1650f, 2365f), false), Pair(Pair(2250f, 2365f), false)
    )
    for ((pt, isV) in binPoints) {
      Gta3DGeometry.addTrashAndRecyclingBins(propSolidBuilder, pt.first, pt.second, isV)
    }

    // C. Modern Glass Transit Bus Shelters
    val busShelters = listOf(
      Triple(1462f, 1350f, true),
      Triple(2138f, 1450f, true),
      Triple(1750f, 1062f, false),
      Triple(2280f, 1062f, false),
      Triple(1650f, 2362f, false),
      Triple(2550f, 2362f, false),
      Triple(720f, 1062f, false),
      Triple(3250f, 1062f, false)
    )
    for ((bx, by, isV) in busShelters) {
      Gta3DGeometry.addBusStopShelter(propSolidBuilder, propEmissiveBuilder, bx, by, isV)
    }

    // D. Parking Meters along shopping avenues
    for (y in 1200..1550 step 60) {
      Gta3DGeometry.addParkingMeter(propSolidBuilder, 2065f, y.toFloat())
      Gta3DGeometry.addParkingMeter(propSolidBuilder, 2135f, (y + 30).toFloat())
    }

    // E. US Mail Dropboxes at street corners
    val mailboxes = listOf(
      Pair(1470f, 1625f), Pair(2130f, 1625f),
      Pair(1470f, 2025f), Pair(2130f, 2025f),
      Pair(1775f, 1070f), Pair(2375f, 1070f)
    )
    for ((mx, my) in mailboxes) {
      Gta3DGeometry.addMailbox(propSolidBuilder, mx, my)
    }

    // F. Highway Safety Traffic Cones & Reflective Bollards near construction zone
    Gta3DGeometry.addConstructionBollards(propSolidBuilder, propEmissiveBuilder, 2135f, 2240f)
    Gta3DGeometry.addConstructionBollards(propSolidBuilder, propEmissiveBuilder, 1465f, 2240f)
    Gta3DGeometry.addConstructionBollards(propSolidBuilder, propEmissiveBuilder, 1950f, 2435f)

    streetPropsSolidMesh = propSolidBuilder.build()
    streetPropsEmissiveMesh = propEmissiveBuilder.build()

    // -------------------------------------------------------------
    // 5C. 3D OVERHEAD HIGHWAY GANTRIES
    // -------------------------------------------------------------
    val gantrySolidBuilder = MeshBuilder(2048)
    val gantryEmissiveBuilder = MeshBuilder(1024)

    // Overhead highway gantries spanning the freeways
    Gta3DGeometry.addHighwayGantry(gantrySolidBuilder, gantryEmissiveBuilder, 1200f, 2400f, isVert = false, spanWidth = 90f)
    Gta3DGeometry.addHighwayGantry(gantrySolidBuilder, gantryEmissiveBuilder, 2250f, 2400f, isVert = false, spanWidth = 90f)
    Gta3DGeometry.addHighwayGantry(gantrySolidBuilder, gantryEmissiveBuilder, 3050f, 2400f, isVert = false, spanWidth = 90f)
    Gta3DGeometry.addHighwayGantry(gantrySolidBuilder, gantryEmissiveBuilder, 2100f, 850f, isVert = true, spanWidth = 70f)
    Gta3DGeometry.addHighwayGantry(gantrySolidBuilder, gantryEmissiveBuilder, 2100f, 2650f, isVert = true, spanWidth = 70f)

    highwayGantriesSolidMesh = gantrySolidBuilder.build()
    highwayGantriesEmissiveMesh = gantryEmissiveBuilder.build()

    // -------------------------------------------------------------
    // 5D. 3D COMMERCIAL BILLBOARDS (ECola, Sprunk, Maze Bank)
    // -------------------------------------------------------------
    val bbSolidBuilder = MeshBuilder(2048)
    val bbEmissiveBuilder = MeshBuilder(2048)

    val billboardPoints = listOf(
      Triple(1150f, 2345f, 0), // ECola on Olympic Freeway
      Triple(1950f, 2455f, 1), // Sprunk on Olympic Freeway
      Triple(2850f, 2345f, 2), // Maze Bank on Olympic Freeway
      Triple(2045f, 950f, 0),  // ECola on Downtown North Approach
      Triple(2155f, 1550f, 1), // Sprunk near Legion Square
      Triple(2045f, 2750f, 2), // Maze Bank near South LS
      Triple(620f, 1045f, 0),  // ECola on Del Perro Coast
      Triple(3450f, 1155f, 1)  // Sprunk near Sandy Shores
    )
    for ((bx, by, brand) in billboardPoints) {
      Gta3DGeometry.addBillboard(bbSolidBuilder, bbEmissiveBuilder, bx, by, z = 0f, isVert = (bx > 2000f && bx < 2200f), brandType = brand)
    }

    billboardsSolidMesh = bbSolidBuilder.build()
    billboardsEmissiveMesh = bbEmissiveBuilder.build()

    // -------------------------------------------------------------
    // 6. 3D FOLIAGE: PALM TREES, PINES & OAK TREES
    // -------------------------------------------------------------
    val foliageBuilder = MeshBuilder(8192)

    // California Fan Palms along major avenues
    val palmPositions = listOf(
      Pair(1300f, 400f), Pair(1600f, 400f), Pair(1900f, 400f), Pair(2200f, 400f), Pair(2500f, 400f), Pair(2800f, 400f),
      Pair(680f, 1200f), Pair(680f, 1400f), Pair(680f, 1600f), Pair(680f, 1800f), Pair(680f, 2000f), Pair(680f, 2200f),
      Pair(1640f, 1740f), Pair(1960f, 1740f), Pair(1640f, 2000f), Pair(1960f, 2000f),
      Pair(1700f, 1870f), Pair(1900f, 1870f),
      Pair(1320f, 2950f), Pair(1580f, 2950f), Pair(1850f, 2950f), Pair(2150f, 2950f),
      Pair(1450f, 3350f), Pair(1750f, 3350f)
    )
    for ((px, py) in palmPositions) {
      Gta3DGeometry.addPalmTree(foliageBuilder, px, py, 44f)
    }

    // Lush California Evergreen Pines in Vinewood Hills
    val pinePositions = listOf(
      Pair(1400f, 250f), Pair(1600f, 220f), Pair(1800f, 200f), Pair(2000f, 230f), Pair(2200f, 210f),
      Pair(2400f, 240f), Pair(2600f, 220f), Pair(2800f, 250f), Pair(3000f, 210f),
      Pair(1720f, 460f), Pair(1920f, 470f), Pair(2120f, 460f), Pair(2320f, 480f)
    )
    for ((px, py) in pinePositions) {
      Gta3DGeometry.addOrganicTree(foliageBuilder, px, py, 42f, isPine = true)
    }

    // Sprawling Shaded Oak Trees in Central Parks & South Los Santos
    val oakPositions = listOf(
      Pair(1650f, 1780f), Pair(1950f, 1780f), Pair(1650f, 1960f), Pair(1950f, 1960f),
      Pair(1720f, 1830f), Pair(1880f, 1830f), Pair(1720f, 1910f), Pair(1880f, 1910f),
      Pair(1360f, 3020f), Pair(1560f, 3020f), Pair(1760f, 3020f), Pair(1960f, 3020f),
      Pair(1360f, 3280f), Pair(1560f, 3280f), Pair(1760f, 3280f), Pair(1960f, 3280f)
    )
    for ((ox, oy) in oakPositions) {
      Gta3DGeometry.addOrganicTree(foliageBuilder, ox, oy, 38f, isPine = false)
    }

    // Bushes and hedges around sidewalks and park perimeters
    val bushColors = listOf(
      floatArrayOf(0.14f, 0.48f, 0.20f, 1f),
      floatArrayOf(0.18f, 0.58f, 0.24f, 1f),
      floatArrayOf(0.12f, 0.42f, 0.16f, 1f)
    )
    for (i in 0 until 40) {
      val bx = 1600f + (i % 8) * 55f
      val by = 1680f + (i / 8) * 85f
      val col = bushColors[i % bushColors.size]
      Gta3DGeometry.addBush(foliageBuilder, bx, by, 10f, 10f, 7f, col)
    }
    // Michael's estate hedges
    for (x in 2080..2360 step 35) {
      Gta3DGeometry.addBush(foliageBuilder, x.toFloat(), 430f, 14f, 8f, 9f, bushColors[0])
    }

    foliageMesh = foliageBuilder.build()

    // -------------------------------------------------------------
    // 7. BUILDINGS WITH ARCHITECTURAL DETAILS & STOREFRONTS
    // -------------------------------------------------------------
    buildingMeshes.clear()
    val bEmissiveBuilder = MeshBuilder(4096)
    val glassBandColor = floatArrayOf(0.45f, 0.72f, 0.92f, 1f) // Reflective Cyan glass
    val goldCrownColor = floatArrayOf(0.98f, 0.82f, 0.18f, 1f)

    for (b in LosSantosWorld.buildings) {
      val r = b.rect
      val height = b.height
      val bBuilder = MeshBuilder(2048)

      when {
        b.name.contains("MAZE BANK") -> {
          // Iconic Maze Bank Tower: Cylindrical tapered skyscraper with rooftop helipad
          Gta3DGeometry.addDetailedSkyscraper(
            solidBuilder = bBuilder,
            emissiveBuilder = bEmissiveBuilder,
            minX = r.left, minY = r.top, maxX = r.right, maxY = r.bottom,
            totalHeight = height,
            wallColor = floatArrayOf(0.18f, 0.32f, 0.48f, 1f),
            glassColor = glassBandColor,
            warmLightColor = floatArrayOf(0.98f, 0.88f, 0.45f, 1f)
          )
          // Helipad Pad & White H on rooftop
          val cx = (r.left + r.right) / 2f
          val cy = (r.top + r.bottom) / 2f
          bBuilder.addBox(cx - 42f, cy - 42f, height, cx + 42f, cy + 42f, height + 0.8f, goldCrownColor, goldCrownColor)
          val white = floatArrayOf(0.98f, 0.98f, 0.98f, 1f)
          bBuilder.addGroundQuad(cx - 20f, cy - 18f, cx - 12f, cy - 18f, cx - 12f, cy + 18f, cx - 20f, cy + 18f, height + 1.0f, white)
          bBuilder.addGroundQuad(cx + 12f, cy - 18f, cx + 20f, cy - 18f, cx + 20f, cy + 18f, cx + 12f, cy + 18f, height + 1.0f, white)
          bBuilder.addGroundQuad(cx - 12f, cy - 4f, cx + 12f, cy - 4f, cx + 12f, cy + 4f, cx - 12f, cy + 4f, height + 1.0f, white)
        }
        b.name.contains("FIB") || b.name.contains("IAA") || b.name.contains("UNION DEPOSITORY") || b.name.contains("ARCADIUS") || height > 100f -> {
          // Modernist & High-Rise Downtown Skyscraper
          val wallCol = if (b.name.contains("FIB")) floatArrayOf(0.12f, 0.18f, 0.30f, 1f) else floatArrayOf(0.24f, 0.28f, 0.36f, 1f)
          Gta3DGeometry.addDetailedSkyscraper(
            solidBuilder = bBuilder,
            emissiveBuilder = bEmissiveBuilder,
            minX = r.left, minY = r.top, maxX = r.right, maxY = r.bottom,
            totalHeight = height,
            wallColor = wallCol,
            glassColor = glassBandColor
          )
        }
        b.name.contains("MICHAEL") || b.name.contains("VINEWOOD LUXURY") || b.name.contains("HILLS ESTATE") -> {
          // Spanish Colonial Villa with Terracotta Roof, Pool & Tennis Court
          Gta3DGeometry.addSpanishMansion(bBuilder, r.left, r.top, r.right, r.bottom, height)
        }
        b.name.contains("FRANKLIN") || b.name.contains("GROVE STREET") -> {
          // South Los Santos Residential Bungalows with Porch, Lawn & Driveway
          val siding = if (b.name.contains("FRANKLIN")) floatArrayOf(0.25f, 0.45f, 0.32f, 1f) else floatArrayOf(0.68f, 0.64f, 0.58f, 1f)
          Gta3DGeometry.addResidentialBungalow(bBuilder, r.left, r.top, r.right, r.bottom, height, sidingColor = siding)
        }
        b.name.contains("PAY 'N' SPRAY") -> {
          // Realistic Automotive Custom Garage with Roll-up Bay Doors & Hazard Stripes
          Gta3DGeometry.addPaynSprayGarage(bBuilder, bEmissiveBuilder, r.left, r.top, r.right, r.bottom, height)
        }
        b.name.contains("AMMU-NATION") -> {
          // Realistic Ammu-Nation Gun Store & Range with 3D Crosshair Target Marquee
          Gta3DGeometry.addAmmuNationStore(bBuilder, bEmissiveBuilder, r.left, r.top, r.right, r.bottom, height)
        }
        b.name.contains("GALILEO OBSERVATORY") -> {
          // White Classical Observatory Rotunda & Bronze Dome
          val rotundaCol = floatArrayOf(0.95f, 0.95f, 0.96f, 1f)
          val domeBronze = floatArrayOf(0.48f, 0.62f, 0.52f, 1f) // Oxidized copper/bronze dome
          val cx = (r.left + r.right) * 0.5f
          val cy = (r.top + r.bottom) * 0.5f
          // Base terrace
          bBuilder.addBox(r.left - 15f, r.top - 15f, 0f, r.right + 15f, r.bottom + 15f, 16f, rotundaCol)
          // Central circular rotunda
          bBuilder.addCylinderZ(cx, cy, 16f, height * 0.75f, 32f, 30f, 12, rotundaCol)
          // Central hemisphere dome
          bBuilder.addCylinderZ(cx, cy, height * 0.75f, height, 30f, 2f, 12, domeBronze)
        }
        else -> {
          // Standard commercial/residential building with multi-tier trim
          val wallCol = floatArrayOf(
            ((b.colorHex shr 16) and 0xFF) / 255f,
            ((b.colorHex shr 8) and 0xFF) / 255f,
            (b.colorHex and 0xFF) / 255f,
            1f
          )
          val roofCol = floatArrayOf(
            ((b.roofColorHex shr 16) and 0xFF) / 255f,
            ((b.roofColorHex shr 8) and 0xFF) / 255f,
            (b.roofColorHex and 0xFF) / 255f,
            1f
          )
          bBuilder.addBox(r.left, r.top, 0f, r.right, r.bottom, height, wallCol, roofCol)
          // Window strips
          if (height > 40f) {
            val fz = height * 0.45f
            bBuilder.addBox(r.left - 0.3f, r.top - 0.3f, fz, r.right + 0.3f, r.top + 0.3f, fz + 4f, glassBandColor)
            bBuilder.addBox(r.left - 0.3f, r.bottom - 0.3f, fz, r.right + 0.3f, r.bottom + 0.3f, fz + 4f, glassBandColor)
          }
        }
      }

      buildingMeshes.add(bBuilder.build() to floatArrayOf(r.left, r.top, r.right, r.bottom, height))
    }
    buildingEmissivesMesh = bEmissiveBuilder.build()

    // -------------------------------------------------------------
    // 8. 3D REALISTIC VEHICLES (AERODYNAMIC SOLID + GLOWING EMISSIVES)
    // -------------------------------------------------------------
    // Pegassi Zentorno (Electric Yellow & Carbon)
    val scSolidB = MeshBuilder(4096)
    val scEmissB = MeshBuilder(512)
    Gta3DGeometry.addRealisticSupercar(
      scSolidB, scEmissB,
      bodyColor = floatArrayOf(0.98f, 0.72f, 0.10f, 1f),
      trimColor = floatArrayOf(0.10f, 0.12f, 0.15f, 1f)
    )
    supercarSolidMesh = scSolidB.build()
    supercarEmissiveMesh = scEmissB.build()

    // Bravado Buffalo (Emerald Metallic Green)
    val bufSolidB = MeshBuilder(4096)
    val bufEmissB = MeshBuilder(512)
    Gta3DGeometry.addRealisticSupercar(
      bufSolidB, bufEmissB,
      bodyColor = floatArrayOf(0.08f, 0.65f, 0.35f, 1f),
      trimColor = floatArrayOf(0.12f, 0.14f, 0.16f, 1f)
    )
    buffaloSolidMesh = bufSolidB.build()
    buffaloEmissiveMesh = bufEmissB.build()

    // Police Cruiser (LAPD Black & White Livery + Bullbar + Roof Strobes)
    val polSolidB = MeshBuilder(4096)
    val polEmissB = MeshBuilder(512)
    Gta3DGeometry.addRealisticPoliceCruiser(polSolidB, polEmissB)
    policeSolidMesh = polSolidB.build()
    policeEmissiveMesh = polEmissB.build()

    // Rhino Heavy Armor Tank
    val tankSolidB = MeshBuilder(4096)
    val tankEmissB = MeshBuilder(64)
    Gta3DGeometry.addRealisticTank(tankSolidB, tankEmissB)
    tankSolidMesh = tankSolidB.build()

    // -------------------------------------------------------------
    // 9. 3D REALISTIC HUMAN CHARACTERS & ANIMATED PEDESTRIANS
    // -------------------------------------------------------------
    // Player Protagonists
    val fBuilder = MeshBuilder(1024)
    Gta3DGeometry.addRealisticHumanCharacter(
      fBuilder, 0f, 0f, 0f, 0f, 0f, isMoving = false,
      shirtColor = floatArrayOf(0.12f, 0.38f, 0.22f, 1f), // Franklin Green Jacket
      pantsColor = floatArrayOf(0.18f, 0.20f, 0.26f, 1f), // Dark Cargo
      skinColor = floatArrayOf(0.48f, 0.32f, 0.20f, 1f),
      hairColor = floatArrayOf(0.10f, 0.08f, 0.08f, 1f)
    )
    playerFranklinMesh = fBuilder.build()

    val mBuilder = MeshBuilder(1024)
    Gta3DGeometry.addRealisticHumanCharacter(
      mBuilder, 0f, 0f, 0f, 0f, 0f, isMoving = false,
      shirtColor = floatArrayOf(0.24f, 0.28f, 0.32f, 1f), // Michael Charcoal Suit
      pantsColor = floatArrayOf(0.20f, 0.22f, 0.26f, 1f),
      skinColor = floatArrayOf(0.86f, 0.68f, 0.55f, 1f),
      hairColor = floatArrayOf(0.35f, 0.30f, 0.25f, 1f)
    )
    playerMichaelMesh = mBuilder.build()

    val tBuilder = MeshBuilder(1024)
    Gta3DGeometry.addRealisticHumanCharacter(
      tBuilder, 0f, 0f, 0f, 0f, 0f, isMoving = false,
      shirtColor = floatArrayOf(0.92f, 0.90f, 0.86f, 1f), // Trevor Distressed White Tee
      pantsColor = floatArrayOf(0.55f, 0.45f, 0.32f, 1f), // Desert Khaki
      skinColor = floatArrayOf(0.84f, 0.65f, 0.52f, 1f),
      hairColor = floatArrayOf(0.45f, 0.40f, 0.35f, 1f)
    )
    playerTrevorMesh = tBuilder.build()

    // 4 Walk-Cycle Keyframes for Civilian Styles & Police Officers
    val pedStyles = listOf(
      Triple(floatArrayOf(0.85f, 0.25f, 0.18f, 1f), floatArrayOf(0.15f, 0.22f, 0.38f, 1f), floatArrayOf(0.82f, 0.64f, 0.50f, 1f)),
      Triple(floatArrayOf(0.95f, 0.95f, 0.98f, 1f), floatArrayOf(0.18f, 0.65f, 0.85f, 1f), floatArrayOf(0.78f, 0.58f, 0.44f, 1f)),
      Triple(floatArrayOf(0.15f, 0.32f, 0.55f, 1f), floatArrayOf(0.30f, 0.28f, 0.25f, 1f), floatArrayOf(0.88f, 0.70f, 0.58f, 1f))
    )

    for (styleIdx in 0 until 3) {
      val (shirt, pants, skin) = pedStyles[styleIdx]
      for (frame in 0 until 4) {
        val phase = (frame * PI.toFloat() * 0.5f)
        val pB = MeshBuilder(1024)
        Gta3DGeometry.addRealisticHumanCharacter(
          pB, 0f, 0f, 0f, 0f, phase, isMoving = true,
          shirtColor = shirt,
          pantsColor = pants,
          skinColor = skin,
          hairColor = floatArrayOf(0.12f, 0.10f, 0.08f, 1f)
        )
        pedestrianWalkMeshes[styleIdx][frame] = pB.build()
      }
    }

    // Police Officers on foot
    for (frame in 0 until 4) {
      val phase = (frame * PI.toFloat() * 0.5f)
      val copB = MeshBuilder(1024)
      Gta3DGeometry.addRealisticHumanCharacter(
        copB, 0f, 0f, 0f, 0f, phase, isMoving = true,
        shirtColor = floatArrayOf(0.12f, 0.16f, 0.28f, 1f), // LAPD Navy Uniform
        pantsColor = floatArrayOf(0.10f, 0.12f, 0.20f, 1f),
        skinColor = floatArrayOf(0.80f, 0.62f, 0.48f, 1f),
        hairColor = floatArrayOf(0.10f, 0.08f, 0.08f, 1f),
        shoesColor = floatArrayOf(0.08f, 0.08f, 0.10f, 1f),
        hasSunglasses = true
      )
      policeOfficerWalkMeshes[frame] = copB.build()
    }

    // -------------------------------------------------------------
    // 10. FIRST PERSON COCKPIT & WEAPONS
    // -------------------------------------------------------------
    firstPersonCockpitMesh = Gta3DGeometry.buildVehicleCockpitMesh()
    for (i in 0..6) {
      weaponViewModelMeshes[i] = Gta3DGeometry.buildFirstPersonWeaponMesh(i)
    }
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    viewportWidth = width
    viewportHeight = height
    GLES20.glViewport(0, 0, width, height)

    val aspect = width.toFloat() / height.toFloat()
    // Landscape Panoramic 70° FOV
    Matrix.perspectiveM(mProjectionMatrix, 0, 70.0f, aspect, 0.2f, 2800.0f)
  }

  override fun onDrawFrame(gl: GL10?) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    GLES20.glUseProgram(programId)

    GLES20.glEnableVertexAttribArray(aPositionLoc)
    GLES20.glEnableVertexAttribArray(aNormalLoc)
    GLES20.glEnableVertexAttribArray(aColorLoc)

    // Dynamic Time-of-Day Atmosphere & Lighting (Sunny Noon, Golden Sunset, Neon Night)
    val tod = engine.currentTimeOfDay
    GLES20.glClearColor(tod.skyR, tod.skyG, tod.skyB, 1.0f)
    GLES20.glUniform4f(uFogColorLoc, tod.fogR, tod.fogG, tod.fogB, 1.0f)
    val fogFar = if (tod.isNight) 2400.0f else 3800.0f
    GLES20.glUniform1f(uFogNearLoc, 100.0f)
    GLES20.glUniform1f(uFogFarLoc, fogFar)
    val sunY = if (tod == GameEngine.TimeOfDay.SUNSET) 450f else 1200f
    val sunZ = if (tod == GameEngine.TimeOfDay.SUNSET) 700f else 2200f
    GLES20.glUniform3f(uLightPosLoc, 800f, sunY, sunZ)

    // Flashing Police Emergency Strobe
    if (engine.wantedStars > 0) {
      val isRed = (System.currentTimeMillis() / 180) % 2 == 0L
      val r = if (isRed) 0.95f else 0.10f
      val b = if (isRed) 0.10f else 0.95f
      GLES20.glUniform3f(uPoliceLightColorLoc, r, 0.25f, b)
      GLES20.glUniform1f(uPoliceLightIntensityLoc, 0.45f)
    } else {
      GLES20.glUniform1f(uPoliceLightIntensityLoc, 0.0f)
    }

    setupCamera()

    // -------------------------------------------------------------
    // PASS 1: SOLID ENVIRONMENT (Emissive = 0.0)
    // -------------------------------------------------------------
    GLES20.glUniform1f(uEmissiveLoc, 0f)
    Matrix.setIdentityM(mModelMatrix, 0)
    updateMatrices()

    // A. Distant Mountain Ridge & Terrain
    mountainMesh?.let { drawMesh(it) }
    terrainMesh?.let { drawMesh(it) }
    oceanMesh?.let { drawMesh(it) }
    beachMesh?.let { drawMesh(it) }
    desertMesh?.let { drawMesh(it) }
    parksAndGrassMesh?.let { drawMesh(it) }

    // B. Roads, Sidewalks & Markings
    roadsMesh?.let { drawMesh(it) }
    sidewalksMesh?.let { drawMesh(it) }
    roadStripesMesh?.let { drawMesh(it) }

    // C. Foliage (Palm Trees & Bushes)
    foliageMesh?.let { drawMesh(it) }

    // D. Streetlamp, Traffic Posts, Urban Props, Highway Gantries & Billboards
    streetlampsSolidMesh?.let { drawMesh(it) }
    trafficSignalsSolidMesh?.let { drawMesh(it) }
    streetPropsSolidMesh?.let { drawMesh(it) }
    highwayGantriesSolidMesh?.let { drawMesh(it) }
    billboardsSolidMesh?.let { drawMesh(it) }

    // E. 3D Buildings with Distance Culling
    val px = engine.playerX
    val py = engine.playerY
    for ((mesh, bounds) in buildingMeshes) {
      val cx = (bounds[0] + bounds[2]) / 2f
      val cy = (bounds[1] + bounds[3]) / 2f
      val dx = cx - px
      val dy = cy - py
      if (dx * dx + dy * dy < 2400f * 2400f) {
        Matrix.setIdentityM(mModelMatrix, 0)
        updateMatrices()
        drawMesh(mesh)
      }
    }

    // -------------------------------------------------------------
    // PASS 2: GLOWING EMISSIVE FIXTURES (Streetlamps, Signals & Store Signs)
    // -------------------------------------------------------------
    GLES20.glUniform1f(uEmissiveLoc, 1.0f)
    Matrix.setIdentityM(mModelMatrix, 0)
    updateMatrices()

    streetlampsEmissiveMesh?.let { drawMesh(it) }
    trafficSignalsEmissiveMesh?.let { drawMesh(it) }
    streetPropsEmissiveMesh?.let { drawMesh(it) }
    highwayGantriesEmissiveMesh?.let { drawMesh(it) }
    billboardsEmissiveMesh?.let { drawMesh(it) }
    buildingEmissivesMesh?.let { drawMesh(it) }

    // -------------------------------------------------------------
    // PASS 3: 3D DYNAMIC VEHICLES, PEDESTRIANS & THIRD-PERSON PLAYER
    // -------------------------------------------------------------
    // 1. DYNAMIC VEHICLES (Solid & Emissive Passes)
    GLES20.glUniform1f(uEmissiveLoc, 0.0f)
    for (v in engine.vehicles) {
      if (v == engine.currentVehicle && engine.isFirstPerson) continue

      val dx = v.x - px
      val dy = v.y - py
      if (dx * dx + dy * dy > 1800f * 1800f) continue

      Matrix.setIdentityM(mModelMatrix, 0)
      Matrix.translateM(mModelMatrix, 0, v.x, v.y, 0f)
      Matrix.rotateM(mModelMatrix, 0, v.angleDeg - 90f, 0f, 0f, 1f)
      updateMatrices()

      val (solidMesh, emissiveMesh) = when {
        v.isTank -> Pair(tankSolidMesh, null)
        v.isPolice -> Pair(policeSolidMesh, policeEmissiveMesh)
        v.model == VehicleModel.BUFFALO -> Pair(buffaloSolidMesh, buffaloEmissiveMesh)
        else -> Pair(supercarSolidMesh, supercarEmissiveMesh)
      }
      solidMesh?.let { drawMesh(it) }

      // Draw high-intensity Xenon headlights, red taillights, and police strobes
      if (emissiveMesh != null) {
        GLES20.glUniform1f(uEmissiveLoc, 1.0f)
        drawMesh(emissiveMesh)
        GLES20.glUniform1f(uEmissiveLoc, 0.0f)
      }
    }

    // 2. 3D ARTICULATED PEDESTRIANS (Walking AI Along Sidewalks & Parks)
    GLES20.glUniform1f(uEmissiveLoc, 0.0f)
    for (p in engine.pedestrians) {
      if (!p.isAlive) continue
      val dx = p.x - px
      val dy = p.y - py
      if (dx * dx + dy * dy > 1200f * 1200f) continue

      Matrix.setIdentityM(mModelMatrix, 0)
      Matrix.translateM(mModelMatrix, 0, p.x, p.y, 0f)
      Matrix.rotateM(mModelMatrix, 0, p.angleDeg - 90f, 0f, 0f, 1f)
      updateMatrices()

      val frameIdx = ((p.id + (engine.walkBobPhase * 2.2f).toInt()).mod(4)).coerceIn(0, 3)
      val pedMesh = if (p.isPolice) {
        policeOfficerWalkMeshes[frameIdx]
      } else {
        val style = (p.id.mod(3)).toInt().coerceIn(0, 2)
        pedestrianWalkMeshes[style][frameIdx]
      }
      pedMesh?.let { drawMesh(it) }
    }

    // 3. 3D THIRD-PERSON PLAYER CHARACTER (Rendered when on-foot in 3rd person)
    if (!engine.isFirstPerson && engine.currentVehicle == null) {
      Matrix.setIdentityM(mModelMatrix, 0)
      Matrix.translateM(mModelMatrix, 0, engine.playerX, engine.playerY, 0f)
      Matrix.rotateM(mModelMatrix, 0, engine.playerYaw - 90f, 0f, 0f, 1f)
      updateMatrices()

      val playerMesh = when (engine.currentCharacter) {
        CharacterType.FRANKLIN -> playerFranklinMesh
        CharacterType.MICHAEL -> playerMichaelMesh
        CharacterType.TREVOR -> playerTrevorMesh
      }
      playerMesh?.let { drawMesh(it) }
    }

    // -------------------------------------------------------------
    // PASS 4: FIRST-PERSON WEAPON OR COCKPIT VIEWMODEL
    // -------------------------------------------------------------
    if (engine.isFirstPerson) {
      renderFirstPersonViewmodel()
    }

    GLES20.glDisableVertexAttribArray(aPositionLoc)
    GLES20.glDisableVertexAttribArray(aNormalLoc)
    GLES20.glDisableVertexAttribArray(aColorLoc)
  }

  private fun setupCamera() {
    val yawRad = engine.playerYaw * PI.toFloat() / 180f
    val pitchRad = engine.playerPitch * PI.toFloat() / 180f

    val eyeX: Float
    val eyeY: Float
    val eyeZ: Float
    val targetX: Float
    val targetY: Float
    val targetZ: Float

    if (engine.isFirstPerson) {
      val car = engine.currentVehicle
      if (car != null) {
        // Cockpit view inside car
        val carRad = car.angleDeg * PI.toFloat() / 180f
        eyeX = car.x + cos(carRad) * 6f - sin(carRad) * 6f
        eyeY = car.y + sin(carRad) * 6f + cos(carRad) * 6f
        eyeZ = 12.5f

        targetX = eyeX + cos(carRad) * 100f
        targetY = eyeY + sin(carRad) * 100f
        targetZ = eyeZ + sin(pitchRad) * 30f
      } else {
        // On foot eye level + head bob
        eyeX = engine.playerX
        eyeY = engine.playerY
        val bob = sin(engine.walkBobPhase) * 0.7f
        eyeZ = engine.playerZ + bob

        val cosPitch = cos(pitchRad)
        val dirX = cos(yawRad) * cosPitch
        val dirY = sin(yawRad) * cosPitch
        val dirZ = sin(pitchRad)

        targetX = eyeX + dirX * 100f
        targetY = eyeY + dirY * 100f
        targetZ = eyeZ + dirZ * 100f
      }
    } else {
      // Third-person chase camera
      val dist = if (engine.currentVehicle != null) 80f else 45f
      val height = if (engine.currentVehicle != null) 34f else 26f

      eyeX = engine.playerX - cos(yawRad) * dist
      eyeY = engine.playerY - sin(yawRad) * dist
      eyeZ = engine.playerZ + height

      targetX = engine.playerX
      targetY = engine.playerY
      targetZ = engine.playerZ + 8f
    }

    val finalEyeX: Float
    val finalEyeY: Float
    val finalEyeZ: Float

    if (!camInitialized) {
      camSmoothEyeX = eyeX
      camSmoothEyeY = eyeY
      camSmoothEyeZ = eyeZ
      camInitialized = true
    }

    if (engine.isFirstPerson && engine.currentVehicle == null) {
      // Immediate response for precision on-foot first-person aiming
      finalEyeX = eyeX
      finalEyeY = eyeY
      finalEyeZ = eyeZ
      camSmoothEyeX = eyeX
      camSmoothEyeY = eyeY
      camSmoothEyeZ = eyeZ
    } else {
      // Silky smooth exponential damping for vehicle driving & third-person cinematic chase camera
      val smoothRate = if (engine.currentVehicle != null) 0.20f else 0.32f
      camSmoothEyeX += (eyeX - camSmoothEyeX) * smoothRate
      camSmoothEyeY += (eyeY - camSmoothEyeY) * smoothRate
      camSmoothEyeZ += (eyeZ - camSmoothEyeZ) * smoothRate
      finalEyeX = camSmoothEyeX
      finalEyeY = camSmoothEyeY
      finalEyeZ = camSmoothEyeZ
    }

    Matrix.setLookAtM(
      mViewMatrix, 0,
      finalEyeX, finalEyeY, finalEyeZ,
      targetX, targetY, targetZ,
      0f, 0f, 1f
    )
  }

  private fun renderFirstPersonViewmodel() {
    GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)

    val car = engine.currentVehicle
    if (car != null) {
      Matrix.setIdentityM(mModelMatrix, 0)
      Matrix.setIdentityM(mTempMatrix, 0)
      Matrix.translateM(mTempMatrix, 0, 0f, 0f, -0.2f)
      Matrix.multiplyMM(mModelMatrix, 0, mTempMatrix, 0, mModelMatrix, 0)

      val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
      Matrix.perspectiveM(weaponProjMatrix, 0, 70f, aspect, 0.05f, 50f)
      Matrix.setLookAtM(weaponViewMatrix, 0, 0f, -0.3f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)

      Matrix.multiplyMM(mMVMatrix, 0, weaponViewMatrix, 0, mModelMatrix, 0)
      Matrix.multiplyMM(mMVPMatrix, 0, weaponProjMatrix, 0, mMVMatrix, 0)

      GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mMVPMatrix, 0)
      GLES20.glUniformMatrix4fv(uMVMatrixLoc, 1, false, mMVMatrix, 0)
      GLES20.glUniform1f(uEmissiveLoc, 0f)

      firstPersonCockpitMesh?.let { drawMesh(it) }
    } else {
      val weaponMesh = weaponViewModelMeshes[engine.currentWeaponIndex] ?: return

      Matrix.setIdentityM(mModelMatrix, 0)
      Matrix.setIdentityM(mTempMatrix, 0)

      val swayX = cos(engine.walkBobPhase * 0.5f) * 0.012f
      val swayY = sin(engine.walkBobPhase) * 0.016f

      Matrix.translateM(mTempMatrix, 0, swayX, 0f, swayY)
      Matrix.multiplyMM(mModelMatrix, 0, mTempMatrix, 0, mModelMatrix, 0)

      val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
      Matrix.perspectiveM(weaponProjMatrix, 0, 62f, aspect, 0.05f, 20f)
      Matrix.setLookAtM(weaponViewMatrix, 0, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)

      Matrix.multiplyMM(mMVMatrix, 0, weaponViewMatrix, 0, mModelMatrix, 0)
      Matrix.multiplyMM(mMVPMatrix, 0, weaponProjMatrix, 0, mMVMatrix, 0)

      GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mMVPMatrix, 0)
      GLES20.glUniformMatrix4fv(uMVMatrixLoc, 1, false, mMVMatrix, 0)
      GLES20.glUniform1f(uEmissiveLoc, 0f)

      drawMesh(weaponMesh)
    }
  }

  private fun updateMatrices() {
    Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0)

    GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mMVPMatrix, 0)
    GLES20.glUniformMatrix4fv(uMVMatrixLoc, 1, false, mMVMatrix, 0)
  }

  private fun drawMesh(mesh: MeshData) {
    mesh.vertexBuffer.position(0)
    GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, mesh.vertexBuffer)

    mesh.normalBuffer.position(0)
    GLES20.glVertexAttribPointer(aNormalLoc, 3, GLES20.GL_FLOAT, false, 0, mesh.normalBuffer)

    mesh.colorBuffer.position(0)
    GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, mesh.colorBuffer)

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
  }
}
