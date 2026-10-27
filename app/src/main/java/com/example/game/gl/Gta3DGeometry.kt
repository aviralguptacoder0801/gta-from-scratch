package com.example.game.gl

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MeshData(
  val vertexBuffer: FloatBuffer,
  val normalBuffer: FloatBuffer,
  val colorBuffer: FloatBuffer,
  val vertexCount: Int
)

/**
 * High-performance, primitive-array backed MeshBuilder with dynamic capacity growth
 * and zero per-frame allocation for high-fidelity 3D open-world rendering.
 */
class MeshBuilder(initialCapacity: Int = 2048) {
  private var positions = FloatArray(initialCapacity * 3)
  private var normals = FloatArray(initialCapacity * 3)
  private var colors = FloatArray(initialCapacity * 4)
  var vertexCount = 0
    private set

  private fun ensureCapacity(additionalVertices: Int) {
    val needed = vertexCount + additionalVertices
    if (needed * 3 > positions.size) {
      val newSize = maxOf(positions.size * 2, needed * 3 + 2048)
      positions = positions.copyOf(newSize)
      normals = normals.copyOf(newSize)
      colors = colors.copyOf(newSize * 4 / 3 + 2048)
    }
  }

  fun addVertex(
    x: Float, y: Float, z: Float,
    nx: Float, ny: Float, nz: Float,
    r: Float, g: Float, b: Float, a: Float = 1f
  ) {
    ensureCapacity(1)
    val vIdx = vertexCount
    val pIdx = vIdx * 3
    val cIdx = vIdx * 4

    positions[pIdx] = x
    positions[pIdx + 1] = y
    positions[pIdx + 2] = z

    normals[pIdx] = nx
    normals[pIdx + 1] = ny
    normals[pIdx + 2] = nz

    colors[cIdx] = r
    colors[cIdx + 1] = g
    colors[cIdx + 2] = b
    colors[cIdx + 3] = a

    vertexCount++
  }

  fun addTriangle(
    x1: Float, y1: Float, z1: Float,
    x2: Float, y2: Float, z2: Float,
    x3: Float, y3: Float, z3: Float,
    nx: Float, ny: Float, nz: Float,
    col: FloatArray
  ) {
    val r = col[0]
    val g = col[1]
    val b = col[2]
    val a = if (col.size > 3) col[3] else 1f
    addVertex(x1, y1, z1, nx, ny, nz, r, g, b, a)
    addVertex(x2, y2, z2, nx, ny, nz, r, g, b, a)
    addVertex(x3, y3, z3, nx, ny, nz, r, g, b, a)
  }

  fun addQuad(
    x1: Float, y1: Float, z1: Float,
    x2: Float, y2: Float, z2: Float,
    x3: Float, y3: Float, z3: Float,
    x4: Float, y4: Float, z4: Float,
    nx: Float, ny: Float, nz: Float,
    col: FloatArray
  ) {
    val r = col[0]
    val g = col[1]
    val b = col[2]
    val a = if (col.size > 3) col[3] else 1f

    addVertex(x1, y1, z1, nx, ny, nz, r, g, b, a)
    addVertex(x2, y2, z2, nx, ny, nz, r, g, b, a)
    addVertex(x3, y3, z3, nx, ny, nz, r, g, b, a)

    addVertex(x1, y1, z1, nx, ny, nz, r, g, b, a)
    addVertex(x3, y3, z3, nx, ny, nz, r, g, b, a)
    addVertex(x4, y4, z4, nx, ny, nz, r, g, b, a)
  }

  fun addGroundQuad(
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    x3: Float, y3: Float,
    x4: Float, y4: Float,
    z: Float,
    col: FloatArray
  ) {
    addQuad(x1, y1, z, x2, y2, z, x3, y3, z, x4, y4, z, 0f, 0f, 1f, col)
  }

  fun addBox(
    minX: Float, minY: Float, minZ: Float,
    maxX: Float, maxY: Float, maxZ: Float,
    wallColor: FloatArray,
    roofColor: FloatArray = wallColor
  ) {
    // Top face (Z max - Roof)
    addQuad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0f, 0f, 1f, roofColor)
    // Bottom face (Z min)
    addQuad(minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, minX, minY, minZ, 0f, 0f, -1f, wallColor)
    // Front face (Y min)
    addQuad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0f, -1f, 0f, wallColor)
    // Back face (Y max)
    addQuad(maxX, maxY, minZ, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, 0f, 1f, 0f, wallColor)
    // Left face (X min)
    addQuad(minX, maxY, minZ, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, -1f, 0f, 0f, wallColor)
    // Right face (X max)
    addQuad(maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, 1f, 0f, 0f, wallColor)
  }

  fun addCylinderZ(
    cx: Float, cy: Float, minZ: Float, maxZ: Float,
    rBottom: Float, rTop: Float,
    segments: Int = 8,
    col: FloatArray
  ) {
    val step = (2.0 * PI / segments).toFloat()
    for (i in 0 until segments) {
      val a1 = i * step
      val a2 = (i + 1) * step
      val c1 = cos(a1)
      val s1 = sin(a1)
      val c2 = cos(a2)
      val s2 = sin(a2)

      val x1b = cx + c1 * rBottom
      val y1b = cy + s1 * rBottom
      val x2b = cx + c2 * rBottom
      val y2b = cy + s2 * rBottom

      val x1t = cx + c1 * rTop
      val y1t = cy + s1 * rTop
      val x2t = cx + c2 * rTop
      val y2t = cy + s2 * rTop

      val midC = (c1 + c2) * 0.5f
      val midS = (s1 + s2) * 0.5f
      addQuad(x1b, y1b, minZ, x2b, y2b, minZ, x2t, y2t, maxZ, x1t, y1t, maxZ, midC, midS, 0f, col)
    }
  }

  fun addWheelX(
    cx: Float, cy: Float, cz: Float,
    radius: Float, width: Float,
    tireColor: FloatArray,
    rimColor: FloatArray,
    segments: Int = 8
  ) {
    val halfW = width * 0.5f
    val step = (2.0 * PI / segments).toFloat()
    val minX = cx - halfW
    val maxX = cx + halfW

    // Tire rubber tread
    for (i in 0 until segments) {
      val a1 = i * step
      val a2 = (i + 1) * step
      val c1 = cos(a1) * radius
      val s1 = sin(a1) * radius
      val c2 = cos(a2) * radius
      val s2 = sin(a2) * radius

      val normY = (sin(a1) + sin(a2)) * 0.5f
      val normZ = (cos(a1) + cos(a2)) * 0.5f
      addQuad(
        minX, cy + s1, cz + c1,
        maxX, cy + s1, cz + c1,
        maxX, cy + s2, cz + c2,
        minX, cy + s2, cz + c2,
        0f, normY, normZ, tireColor
      )
    }

    // Outer & inner rim discs
    val rimR = radius * 0.70f
    for (i in 0 until segments) {
      val a1 = i * step
      val a2 = (i + 1) * step
      val c1 = cos(a1) * rimR
      val s1 = sin(a1) * rimR
      val c2 = cos(a2) * rimR
      val s2 = sin(a2) * rimR

      addTriangle(
        maxX + 0.05f, cy, cz,
        maxX + 0.05f, cy + s1, cz + c1,
        maxX + 0.05f, cy + s2, cz + c2,
        1f, 0f, 0f,
        if (i % 2 == 0) rimColor else floatArrayOf(rimColor[0] * 0.7f, rimColor[1] * 0.7f, rimColor[2] * 0.7f, 1f)
      )
      addTriangle(
        minX - 0.05f, cy, cz,
        minX - 0.05f, cy + s2, cz + c2,
        minX - 0.05f, cy + s1, cz + c1,
        -1f, 0f, 0f,
        rimColor
      )
    }
  }

  fun build(): MeshData {
    val trimmedPos = positions.copyOf(vertexCount * 3)
    val trimmedNorm = normals.copyOf(vertexCount * 3)
    val trimmedCol = colors.copyOf(vertexCount * 4)

    return MeshData(
      vertexBuffer = Gta3DGeometry.createFloatBuffer(trimmedPos),
      normalBuffer = Gta3DGeometry.createFloatBuffer(trimmedNorm),
      colorBuffer = Gta3DGeometry.createFloatBuffer(trimmedCol),
      vertexCount = vertexCount
    )
  }
}

object Gta3DGeometry {

  fun createFloatBuffer(data: FloatArray): FloatBuffer {
    return ByteBuffer.allocateDirect(data.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
      .apply {
        put(data)
        position(0)
      }
  }

  fun buildBoxMesh(
    minX: Float, minY: Float, minZ: Float,
    maxX: Float, maxY: Float, maxZ: Float,
    wallColor: FloatArray,
    roofColor: FloatArray = wallColor
  ): MeshData {
    val b = MeshBuilder(36)
    b.addBox(minX, minY, minZ, maxX, maxY, maxZ, wallColor, roofColor)
    return b.build()
  }

  fun buildGroundQuad(
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    x3: Float, y3: Float,
    x4: Float, y4: Float,
    z: Float,
    color: FloatArray
  ): MeshData {
    val b = MeshBuilder(6)
    b.addGroundQuad(x1, y1, x2, y2, x3, y3, x4, y4, z, color)
    return b.build()
  }

  /**
   * Realistic California Palm Tree:
   * - 6-segment curved tapered trunk with fibrous bark rings
   * - Coconut crown cluster
   * - 10 cascading, feathered fronds arching outward and drooping downward
   */
  fun addPalmTree(builder: MeshBuilder, x: Float, y: Float, height: Float = 44f) {
    val trunk1 = floatArrayOf(0.48f, 0.36f, 0.24f, 1f)
    val trunk2 = floatArrayOf(0.38f, 0.28f, 0.18f, 1f)
    val coconutCol = floatArrayOf(0.30f, 0.20f, 0.12f, 1f)
    val frondTop = floatArrayOf(0.12f, 0.68f, 0.22f, 1f)
    val frondUnder = floatArrayOf(0.18f, 0.52f, 0.16f, 1f)

    // 6-segment curved tapered trunk
    val segH = height / 6f
    var curX = x
    var curY = y
    var curZ = 0f
    var r = 2.4f

    for (s in 0 until 6) {
      val nextZ = curZ + segH
      val nextR = (r - 0.22f).coerceAtLeast(1.1f)
      val nextX = curX + 0.6f
      val nextY = curY + 0.4f
      val col = if (s % 2 == 0) trunk1 else trunk2

      builder.addCylinderZ(
        (curX + nextX) * 0.5f, (curY + nextY) * 0.5f,
        curZ, nextZ, r, nextR, 8, col
      )

      curX = nextX
      curY = nextY
      curZ = nextZ
      r = nextR
    }

    // Coconut cluster at crown
    builder.addBox(curX - 1.8f, curY - 1.8f, curZ - 1.2f, curX + 1.8f, curY + 1.8f, curZ + 1.2f, coconutCol)

    // 10 Cascading curved palm fronds
    val frondCount = 10
    val stepAngle = (2.0 * PI / frondCount).toFloat()
    for (f in 0 until frondCount) {
      val angle = f * stepAngle
      val c = cos(angle)
      val s = sin(angle)

      // Segment 1: Arching up & out
      val x1 = curX + c * 8f
      val y1 = curY + s * 8f
      val z1 = curZ + 4.5f

      // Segment 2: Apex reach
      val x2 = curX + c * 17f
      val y2 = curY + s * 17f
      val z2 = curZ + 2.0f

      // Segment 3: Drooping tip
      val x3 = curX + c * 24f
      val y3 = curY + s * 24f
      val z3 = curZ - 4.5f

      val col = if (f % 2 == 0) frondTop else frondUnder

      // Arching feather quad 1
      builder.addQuad(
        curX - s * 1.5f, curY + c * 1.5f, curZ,
        curX + s * 1.5f, curY - c * 1.5f, curZ,
        x1 + s * 2.2f, y1 - c * 2.2f, z1,
        x1 - s * 2.2f, y1 + c * 2.2f, z1,
        0f, 0f, 1f, col
      )

      // Arching feather quad 2
      builder.addQuad(
        x1 - s * 2.2f, y1 + c * 2.2f, z1,
        x1 + s * 2.2f, y1 - c * 2.2f, z1,
        x2 + s * 1.6f, y2 - c * 1.6f, z2,
        x2 - s * 1.6f, y2 + c * 1.6f, z2,
        0f, 0f, 1f, col
      )

      // Drooping tip triangle
      builder.addTriangle(
        x2 - s * 1.6f, y2 + c * 1.6f, z2,
        x2 + s * 1.6f, y2 - c * 1.6f, z2,
        x3, y3, z3,
        0f, 0f, 1f, col
      )
    }
  }

  fun buildPalmTreeMesh(x: Float, y: Float, height: Float = 44f): MeshData {
    val b = MeshBuilder(512)
    addPalmTree(b, x, y, height)
    return b.build()
  }

  /**
   * Realistic 3D Streetlamp with cast iron base, arched cantilever arm,
   * emissive warm luminaire, and sidewalk illumination pool.
   */
  fun addStreetLamp(
    solidBuilder: MeshBuilder,
    emissiveBuilder: MeshBuilder,
    x: Float, y: Float,
    height: Float = 28f,
    armDirX: Float, armDirY: Float
  ) {
    val baseCol = floatArrayOf(0.18f, 0.20f, 0.24f, 1f)
    val poleCol = floatArrayOf(0.14f, 0.16f, 0.20f, 1f)
    val hoodCol = floatArrayOf(0.10f, 0.11f, 0.14f, 1f)
    val bulbCol = floatArrayOf(1.0f, 0.95f, 0.72f, 1f) // Radiant Warm Amber/White
    val groundGlowCol = floatArrayOf(0.85f, 0.80f, 0.55f, 0.32f)

    // Base pedestal
    solidBuilder.addBox(x - 2.5f, y - 2.5f, 0f, x + 2.5f, y + 2.5f, 2.2f, baseCol, baseCol)

    // Vertical pole
    solidBuilder.addCylinderZ(x, y, 2.2f, height, 1.2f, 0.8f, 6, poleCol)

    // Cantilever arm
    val armLen = 9.5f
    val armEndX = x + armDirX * armLen
    val armEndY = y + armDirY * armLen
    val armZ = height - 0.5f
    solidBuilder.addBox(
      minOf(x, armEndX) - 0.7f, minOf(y, armEndY) - 0.7f, armZ,
      maxOf(x, armEndX) + 0.7f, maxOf(y, armEndY) + 0.7f, height + 0.8f,
      poleCol, poleCol
    )

    // Luminaire housing
    solidBuilder.addBox(
      armEndX - 1.8f, armEndY - 1.8f, height - 1.4f,
      armEndX + 1.8f, armEndY + 1.8f, height + 0.6f,
      hoodCol, hoodCol
    )

    // Emissive bulb
    emissiveBuilder.addBox(
      armEndX - 1.2f, armEndY - 1.2f, height - 2.2f,
      armEndX + 1.2f, armEndY + 1.2f, height - 1.2f,
      bulbCol, bulbCol
    )

    // Ground light halo
    val pSize = 11f
    solidBuilder.addGroundQuad(
      armEndX - pSize, armEndY - pSize,
      armEndX + pSize, armEndY - pSize,
      armEndX + pSize, armEndY + pSize,
      armEndX - pSize, armEndY + pSize,
      z = 0.85f,
      col = groundGlowCol
    )
  }

  /**
   * 3D Bush / Landscaping hedge for parks and curbs
   */
  fun addBush(
    builder: MeshBuilder,
    x: Float, y: Float,
    w: Float = 12f, d: Float = 12f, h: Float = 8f,
    primaryColor: FloatArray = floatArrayOf(0.16f, 0.52f, 0.22f, 1f)
  ) {
    val darkBase = floatArrayOf(primaryColor[0] * 0.75f, primaryColor[1] * 0.75f, primaryColor[2] * 0.75f, 1f)
    val brightTop = floatArrayOf(
      minOf(1f, primaryColor[0] * 1.25f),
      minOf(1f, primaryColor[1] * 1.25f),
      minOf(1f, primaryColor[2] * 1.25f),
      1f
    )
    builder.addBox(x - w / 2f, y - d / 2f, 0.5f, x + w / 2f, y + d / 2f, h * 0.6f, darkBase, primaryColor)
    builder.addBox(
      x - w * 0.35f, y - d * 0.35f, h * 0.5f,
      x + w * 0.35f, y + d * 0.35f, h,
      primaryColor, brightTop
    )
  }

  /**
   * 3D Traffic Signal Mast at intersections with Red, Amber, Green emissive lenses
   */
  fun addTrafficSignal(
    solidBuilder: MeshBuilder,
    emissiveBuilder: MeshBuilder,
    x: Float, y: Float,
    armDirX: Float, armDirY: Float
  ) {
    val poleCol = floatArrayOf(0.22f, 0.24f, 0.26f, 1f)
    val headCol = floatArrayOf(0.92f, 0.75f, 0.12f, 1f) // California Traffic Yellow
    val height = 24f

    solidBuilder.addBox(x - 1.2f, y - 1.2f, 0f, x + 1.2f, y + 1.2f, height, poleCol, poleCol)

    val armLen = 14f
    val endX = x + armDirX * armLen
    val endY = y + armDirY * armLen
    solidBuilder.addBox(
      minOf(x, endX) - 0.8f, minOf(y, endY) - 0.8f, height - 1f,
      maxOf(x, endX) + 0.8f, maxOf(y, endY) + 0.8f, height + 0.8f,
      poleCol, poleCol
    )

    solidBuilder.addBox(
      endX - 1.6f, endY - 1.6f, height - 6.5f,
      endX + 1.6f, endY + 1.6f, height + 0.5f,
      headCol, headCol
    )

    // Emissive Signal Lenses
    emissiveBuilder.addBox(
      endX - 1.1f, endY - 1.1f, height - 1.8f,
      endX + 1.1f, endY + 1.1f, height - 0.2f,
      floatArrayOf(0.98f, 0.18f, 0.18f, 1f),
      floatArrayOf(0.98f, 0.18f, 0.18f, 1f)
    )
    emissiveBuilder.addBox(
      endX - 1.1f, endY - 1.1f, height - 3.8f,
      endX + 1.1f, endY + 1.1f, height - 2.2f,
      floatArrayOf(0.98f, 0.80f, 0.12f, 1f),
      floatArrayOf(0.98f, 0.80f, 0.12f, 1f)
    )
    emissiveBuilder.addBox(
      endX - 1.1f, endY - 1.1f, height - 5.8f,
      endX + 1.1f, endY + 1.1f, height - 4.2f,
      floatArrayOf(0.18f, 0.98f, 0.35f, 1f),
      floatArrayOf(0.18f, 0.98f, 0.35f, 1f)
    )
  }

  /**
   * Realistic 3D Aerodynamic Supercar (GTA 5 Pegassi Zentorno / Bravado Buffalo style):
   * - Tapered low-slung wedge chassis with front air dam & splitter
   * - Sloping hood with dual cooling air vents
   * - Contoured flared wheel arches
   * - 4 true 3D cylindrical wheels with black rubber tires and 5-spoke silver alloy rims
   * - Slanted raked 45-degree front windshield, aerodynamic roof, and fastback sloping rear glass
   * - Dual aerodynamic side mirrors
   * - Rear aerodynamic carbon spoiler / wing
   * - Rear diffuser with chrome dual exhaust pipes
   * - Emissive high-intensity Xenon headlights and glowing red LED taillight bar
   * - Soft ambient ground contact shadow quad
   */
  fun addRealisticSupercar(
    solidBuilder: MeshBuilder,
    emissiveBuilder: MeshBuilder,
    bodyColor: FloatArray,
    trimColor: FloatArray = floatArrayOf(0.10f, 0.12f, 0.15f, 1f)
  ) {
    val tireCol = floatArrayOf(0.12f, 0.13f, 0.15f, 1f)
    val rimCol = floatArrayOf(0.85f, 0.88f, 0.92f, 1f)
    val glassCol = floatArrayOf(0.16f, 0.24f, 0.32f, 1f)
    val chromeCol = floatArrayOf(0.92f, 0.94f, 0.96f, 1f)
    val shadowCol = floatArrayOf(0f, 0f, 0f, 0.45f)
    val xenonCol = floatArrayOf(0.88f, 0.96f, 1.0f, 1f)
    val tailLightCol = floatArrayOf(0.98f, 0.12f, 0.12f, 1f)

    // 0. Ground contact ambient shadow
    solidBuilder.addGroundQuad(
      -22f, -44f, 22f, -44f, 22f, 44f, -22f, 44f,
      z = 0.25f, col = shadowCol
    )

    // 1. Lower chassis / side skirts
    solidBuilder.addBox(-18.5f, -40f, 2.5f, 18.5f, 40f, 5.5f, trimColor)

    // 2. Front aerodynamic splitter & intake grille
    solidBuilder.addBox(-19f, 38f, 1.5f, 19f, 43f, 4.0f, trimColor)
    solidBuilder.addBox(-14f, 41.5f, 4.0f, 14f, 43f, 8.0f, trimColor) // Grille mesh

    // 3. Main wedge body & flared fenders
    solidBuilder.addBox(-19f, -38f, 5.5f, 19f, 38f, 10.5f, bodyColor)

    // 4. Sloping front hood
    solidBuilder.addQuad(
      -17f, 14f, 12.0f,
      17f, 14f, 12.0f,
      16f, 40f, 8.5f,
      -16f, 40f, 8.5f,
      0f, 0.3f, 0.9f, bodyColor
    )
    // Dual hood heat extractor vents
    solidBuilder.addBox(-11f, 22f, 10.2f, -5f, 32f, 10.6f, trimColor)
    solidBuilder.addBox(5f, 22f, 10.2f, 11f, 32f, 10.6f, trimColor)

    // 5. Aerodynamic Cockpit & Glass: Raked Windshield
    solidBuilder.addQuad(
      -15f, 13f, 12.0f,
      15f, 13f, 12.0f,
      13.5f, -2f, 18.8f,
      -13.5f, -2f, 18.8f,
      0f, 0.7f, 0.7f, glassCol
    )
    // Roof
    solidBuilder.addQuad(
      -13.5f, -2f, 18.8f,
      13.5f, -2f, 18.8f,
      13.5f, -16f, 18.8f,
      -13.5f, -16f, 18.8f,
      0f, 0f, 1f, bodyColor
    )
    // Sloping rear fastback window
    solidBuilder.addQuad(
      -13.5f, -16f, 18.8f,
      13.5f, -16f, 18.8f,
      -15f, -33f, 12.5f,
      15f, -33f, 12.5f,
      0f, -0.6f, 0.8f, glassCol
    )
    // Side windows & Pillars
    solidBuilder.addBox(-15.8f, -16f, 12.0f, -13.5f, 11f, 18.5f, glassCol)
    solidBuilder.addBox(13.5f, -16f, 12.0f, 15.8f, 11f, 18.5f, glassCol)

    // 6. Side rearview mirrors
    solidBuilder.addBox(-20.5f, 8f, 13.5f, -16f, 11f, 15.5f, bodyColor)
    solidBuilder.addBox(16f, 8f, 13.5f, 20.5f, 11f, 15.5f, bodyColor)

    // 7. Rear deck & high aerodynamic rear carbon wing
    solidBuilder.addBox(-17f, -40f, 9.5f, 17f, -33f, 12.5f, bodyColor)
    // Spoiler struts
    solidBuilder.addBox(-10f, -38f, 12.5f, -8.5f, -36f, 17.5f, trimColor)
    solidBuilder.addBox(8.5f, -38f, 12.5f, 10f, -36f, 17.5f, trimColor)
    // Carbon wing blade
    solidBuilder.addBox(-18f, -41f, 17.5f, 18f, -34f, 19.0f, trimColor, trimColor)

    // 8. Rear diffuser & dual chrome exhaust tips
    solidBuilder.addBox(-14f, -41.5f, 3.0f, 14f, -39f, 6.0f, trimColor)
    solidBuilder.addCylinderZ(-6f, -41.2f, 3.8f, 5.8f, 1.2f, 1.2f, 6, chromeCol)
    solidBuilder.addCylinderZ(6f, -41.2f, 3.8f, 5.8f, 1.2f, 1.2f, 6, chromeCol)

    // 9. Four 3D Cylindrical Wheels with Alloy Rims and Red Brembo Brake Calipers
    val wR = 5.8f
    val wW = 3.6f
    solidBuilder.addWheelX(18.5f, 23f, 5.8f, wR, wW, tireCol, rimCol, 8)
    solidBuilder.addWheelX(-18.5f, 23f, 5.8f, wR, wW, tireCol, rimCol, 8)
    solidBuilder.addWheelX(19.0f, -22f, 6.0f, wR + 0.3f, wW + 0.8f, tireCol, rimCol, 8)
    solidBuilder.addWheelX(-19.0f, -22f, 6.0f, wR + 0.3f, wW + 0.8f, tireCol, rimCol, 8)

    // Red performance brake calipers behind front and rear alloy spokes
    val caliperRed = floatArrayOf(0.92f, 0.12f, 0.12f, 1f)
    solidBuilder.addBox(19.2f, 21.2f, 6.2f, 19.8f, 24.8f, 8.8f, caliperRed)
    solidBuilder.addBox(-19.8f, 21.2f, 6.2f, -19.2f, 24.8f, 8.8f, caliperRed)
    solidBuilder.addBox(19.4f, -23.8f, 6.4f, 20.0f, -20.2f, 9.0f, caliperRed)
    solidBuilder.addBox(-20.0f, -23.8f, 6.4f, -19.4f, -20.2f, 9.0f, caliperRed)

    // Aerodynamic side cooling radiator scoops behind doors
    val scoopCol = floatArrayOf(0.08f, 0.09f, 0.12f, 1f)
    solidBuilder.addBox(-19.3f, -14f, 6.0f, -18.7f, -4f, 10.0f, scoopCol)
    solidBuilder.addBox(18.7f, -14f, 6.0f, 19.3f, -4f, 10.0f, scoopCol)

    // San Andreas Front & Rear Yellow License Plates
    val plateYellow = floatArrayOf(0.96f, 0.86f, 0.18f, 1f)
    val plateBlueBorder = floatArrayOf(0.12f, 0.22f, 0.55f, 1f)
    // Front Plate
    solidBuilder.addBox(-5.5f, 43.1f, 3.2f, 5.5f, 43.4f, 6.8f, plateBlueBorder)
    solidBuilder.addBox(-5.0f, 43.2f, 3.6f, 5.0f, 43.5f, 6.4f, plateYellow)
    // Rear Plate
    solidBuilder.addBox(-5.5f, -41.6f, 4.2f, 5.5f, -41.3f, 7.8f, plateBlueBorder)
    solidBuilder.addBox(-5.0f, -41.7f, 4.6f, 5.0f, -41.4f, 7.4f, plateYellow)

    // 10. Emissive Xenon Headlights
    emissiveBuilder.addBox(-16.5f, 39.5f, 7.5f, -10.5f, 41.5f, 9.8f, xenonCol)
    emissiveBuilder.addBox(10.5f, 39.5f, 7.5f, 16.5f, 41.5f, 9.8f, xenonCol)

    // 11. Emissive Rear Taillight Bar
    emissiveBuilder.addBox(-16f, -40.6f, 9.8f, 16f, -39.4f, 11.6f, tailLightCol)

    // 12. Headlight Forward Light Cone Puddles (casts illuminated wedge on asphalt in front of car)
    val headlightGroundCol = floatArrayOf(0.85f, 0.92f, 1.0f, 0.26f)
    solidBuilder.addGroundQuad(
      -18f, 42f,
      18f, 42f,
      32f, 120f,
      -32f, 120f,
      z = 0.58f,
      col = headlightGroundCol
    )
  }

  /**
   * Realistic 3D LAPD-Style Police Cruiser (Vapid Stanier):
   * - Black & White authentic two-tone livery (white doors & roof, dark blue/black chassis)
   * - Front steel heavy-duty push bumper ("bullbar")
   * - Police steelie wheels with chrome center hubcaps
   * - Dual flashing emergency lightbar (Police Blue & Police Red emissives)
   * - Xenon headlights and taillights
   */
  fun addRealisticPoliceCruiser(
    solidBuilder: MeshBuilder,
    emissiveBuilder: MeshBuilder
  ) {
    val blackBody = floatArrayOf(0.08f, 0.10f, 0.14f, 1f)
    val whiteLivery = floatArrayOf(0.96f, 0.96f, 0.98f, 1f)
    val pushBumperCol = floatArrayOf(0.15f, 0.16f, 0.18f, 1f)
    val tireCol = floatArrayOf(0.12f, 0.13f, 0.15f, 1f)
    val steelieRim = floatArrayOf(0.20f, 0.22f, 0.26f, 1f)
    val chromeHub = floatArrayOf(0.92f, 0.94f, 0.96f, 1f)
    val glassCol = floatArrayOf(0.16f, 0.22f, 0.28f, 1f)
    val shadowCol = floatArrayOf(0f, 0f, 0f, 0.45f)
    val xenonCol = floatArrayOf(0.92f, 0.96f, 1.0f, 1f)
    val tailLightCol = floatArrayOf(0.98f, 0.12f, 0.12f, 1f)
    val strobeRed = floatArrayOf(1.0f, 0.10f, 0.10f, 1f)
    val strobeBlue = floatArrayOf(0.10f, 0.45f, 1.0f, 1f)

    // Shadow
    solidBuilder.addGroundQuad(
      -22f, -45f, 22f, -45f, 22f, 45f, -22f, 45f,
      z = 0.25f, col = shadowCol
    )

    // Chassis & Fenders (Black)
    solidBuilder.addBox(-19f, -42f, 2.5f, 19f, 42f, 10.5f, blackBody)

    // White Police Front & Rear Doors
    solidBuilder.addBox(-19.2f, -14f, 4.5f, -18.6f, 14f, 11f, whiteLivery)
    solidBuilder.addBox(18.6f, -14f, 4.5f, 19.2f, 14f, 11f, whiteLivery)

    // White Roof
    solidBuilder.addBox(-13.5f, -16f, 18.2f, 13.5f, 4f, 19.2f, whiteLivery)

    // Angled Windshield & Rear Window
    solidBuilder.addQuad(
      -14.5f, 15f, 11.0f,
      14.5f, 15f, 11.0f,
      13.5f, 4f, 18.5f,
      -13.5f, 4f, 18.5f,
      0f, 0.6f, 0.8f, glassCol
    )
    solidBuilder.addQuad(
      -13.5f, -16f, 18.5f,
      13.5f, -16f, 18.5f,
      14.5f, -28f, 11.5f,
      -14.5f, -28f, 11.5f,
      0f, -0.6f, 0.8f, glassCol
    )
    // Side Windows
    solidBuilder.addBox(-15f, -16f, 11f, -13f, 4f, 18f, glassCol)
    solidBuilder.addBox(13f, -16f, 11f, 15f, 4f, 18f, glassCol)

    // Front Steel Heavy-Duty Push Bumper ("Bullbar")
    solidBuilder.addBox(-12f, 43f, 2f, 12f, 45.5f, 9.5f, pushBumperCol)
    solidBuilder.addBox(-10f, 45f, 4f, -7f, 46.5f, 12f, pushBumperCol)
    solidBuilder.addBox(7f, 45f, 4f, 10f, 46.5f, 12f, pushBumperCol)

    // Wheels: Police Steelies with Chrome Center Hubcaps
    val wR = 5.8f
    val wW = 3.6f
    solidBuilder.addWheelX(18.5f, 24f, 5.8f, wR, wW, tireCol, steelieRim, 8)
    solidBuilder.addWheelX(-18.5f, 24f, 5.8f, wR, wW, tireCol, steelieRim, 8)
    solidBuilder.addWheelX(18.5f, -24f, 5.8f, wR, wW, tireCol, steelieRim, 8)
    solidBuilder.addWheelX(-18.5f, -24f, 5.8f, wR, wW, tireCol, steelieRim, 8)
    // Chrome center caps
    solidBuilder.addBox(20.4f, 22.5f, 4.5f, 20.8f, 25.5f, 7.1f, chromeHub)
    solidBuilder.addBox(-20.8f, 22.5f, 4.5f, -20.4f, 25.5f, 7.1f, chromeHub)
    solidBuilder.addBox(20.4f, -25.5f, 4.5f, 20.8f, -22.5f, 7.1f, chromeHub)
    solidBuilder.addBox(-20.8f, -25.5f, 4.5f, -20.4f, -22.5f, 7.1f, chromeHub)

    // Roof Emergency Strobe Lightbar
    solidBuilder.addBox(-10f, -7f, 19.2f, 10f, -3f, 20.2f, pushBumperCol) // Base bar
    emissiveBuilder.addBox(-9.5f, -6.5f, 20.2f, -0.5f, -3.5f, 22.2f, strobeRed)   // Red beacon
    emissiveBuilder.addBox(0.5f, -6.5f, 20.2f, 9.5f, -3.5f, 22.2f, strobeBlue)    // Blue beacon

    // Headlights & Taillights
    emissiveBuilder.addBox(-16f, 41.5f, 6.5f, -10f, 42.8f, 8.8f, xenonCol)
    emissiveBuilder.addBox(10f, 41.5f, 6.5f, 16f, 42.8f, 8.8f, xenonCol)
    emissiveBuilder.addBox(-16f, -42.8f, 7f, -10f, -41.5f, 9.2f, tailLightCol)
    emissiveBuilder.addBox(10f, -42.8f, 7f, 16f, -41.5f, 9.2f, tailLightCol)

    // Grille Red & Blue Flashing Wig-Wag Strobes
    emissiveBuilder.addBox(-5.5f, 43.2f, 7.5f, -2.5f, 44.5f, 9.2f, strobeRed)
    emissiveBuilder.addBox(2.5f, 43.2f, 7.5f, 5.5f, 44.5f, 9.2f, strobeBlue)

    // Police Plate & Star Door Insignia
    val starGold = floatArrayOf(0.95f, 0.85f, 0.20f, 1f)
    val plateYellow = floatArrayOf(0.96f, 0.86f, 0.18f, 1f)
    solidBuilder.addBox(-19.3f, -2f, 6.5f, -19.1f, 2f, 9.5f, starGold) // Left Door Star
    solidBuilder.addBox(19.1f, -2f, 6.5f, 19.3f, 2f, 9.5f, starGold)  // Right Door Star
    // Rear Plate
    solidBuilder.addBox(-5.0f, -43.2f, 4.0f, 5.0f, -42.9f, 6.5f, plateYellow)

    // Police Forward Light Cone Glare
    val policeGroundCone = floatArrayOf(0.85f, 0.92f, 1.0f, 0.28f)
    solidBuilder.addGroundQuad(
      -18f, 44f,
      18f, 44f,
      30f, 120f,
      -30f, 120f,
      z = 0.58f,
      col = policeGroundCone
    )
  }

  /**
   * Realistic 3D Rhino Heavy Armor Tank:
   * - Dual heavy tread assemblies with road wheels
   * - Sloped angular composite armor chassis
   * - Rotatable turret with commander cupola
   * - Long 120mm rifled cannon barrel with muzzle brake
   */
  fun addRealisticTank(
    solidBuilder: MeshBuilder,
    emissiveBuilder: MeshBuilder
  ) {
    val armorCamo = floatArrayOf(0.32f, 0.36f, 0.28f, 1f)
    val armorDark = floatArrayOf(0.20f, 0.24f, 0.18f, 1f)
    val trackCol = floatArrayOf(0.12f, 0.13f, 0.14f, 1f)
    val shadowCol = floatArrayOf(0f, 0f, 0f, 0.5f)

    solidBuilder.addGroundQuad(-28f, -48f, 28f, -48f, 28f, 48f, -28f, 48f, 0.25f, shadowCol)

    // Dual Heavy Track Assemblies
    solidBuilder.addBox(-26f, -46f, 1f, -16f, 46f, 10f, trackCol)
    solidBuilder.addBox(16f, -46f, 1f, 26f, 46f, 10f, trackCol)

    // Sloped composite hull
    solidBuilder.addBox(-16f, -44f, 4f, 16f, 44f, 12f, armorCamo)
    solidBuilder.addQuad(
      -16f, 22f, 12f,
      16f, 22f, 12f,
      15f, 45f, 7f,
      -15f, 45f, 7f,
      0f, 0.5f, 0.8f, armorDark
    )

    // Armored Turret
    solidBuilder.addBox(-14f, -14f, 12f, 14f, 14f, 19f, armorCamo)
    solidBuilder.addBox(-8f, -6f, 19f, -1f, 1f, 21.5f, armorDark) // Commander hatch

    // 120mm Heavy Rifled Cannon Barrel
    solidBuilder.addCylinderZ(0f, 32f, 15f, 16.5f, 1.4f, 1.4f, 8, armorDark)
    solidBuilder.addBox(-1.4f, 14f, 14.5f, 1.4f, 52f, 17.5f, armorDark)
    solidBuilder.addBox(-2.5f, 50f, 13.8f, 2.5f, 55f, 18.2f, armorDark) // Muzzle brake
  }

  /**
   * Anatomical 3D Human Body Generator (GTA 5 Style Character & Pedestrians):
   * - Tapered pelvis & waist with belt
   * - Athletic V-taper sculpted torso (chest depth, shoulder breadth, collar/lapels)
   * - Anatomical cylindrical neck and sculpted head with jawline, hair, and sunglasses
   * - Articulated arms with shoulders, biceps, forearms, and hands in skin tone
   * - Dynamic walking stride: arms swing inversely to legs with natural knee bend
   * - Muscular thighs, calves, and athletic sneakers with contrasting rubber soles
   * - Soft ambient ground shadow beneath feet
   */
  fun addRealisticHumanCharacter(
    builder: MeshBuilder,
    x: Float, y: Float, z: Float,
    yawDeg: Float,
    walkPhase: Float,
    isMoving: Boolean,
    shirtColor: FloatArray,
    pantsColor: FloatArray,
    skinColor: FloatArray,
    hairColor: FloatArray,
    shoesColor: FloatArray = floatArrayOf(0.12f, 0.12f, 0.14f, 1f),
    hasSunglasses: Boolean = true
  ) {
    val beltColor = floatArrayOf(0.12f, 0.10f, 0.08f, 1f)
    val buckleColor = floatArrayOf(0.92f, 0.85f, 0.35f, 1f) // Metallic gold buckle
    val soleColor = floatArrayOf(0.94f, 0.94f, 0.96f, 1f)
    val shadowCol = floatArrayOf(0f, 0f, 0f, 0.40f)
    val watchColor = floatArrayOf(0.85f, 0.75f, 0.25f, 1f)

    // 0. Soft Ground Contact Shadow Oval
    builder.addGroundQuad(
      x - 3.4f, y - 3.4f,
      x + 3.4f, y - 3.4f,
      x + 3.4f, y + 3.4f,
      x - 3.4f, y + 3.4f,
      z + 0.15f, shadowCol
    )

    // Stride calculations
    val legSwing = if (isMoving) sin(walkPhase) * 2.2f else 0f
    val armSwing = if (isMoving) sin(walkPhase) * 2.4f else 0f
    val kneeBend = if (isMoving) (cos(walkPhase) * 0.8f).coerceAtLeast(0f) else 0f

    // 1. Pelvis & Waist with Detailed Belt & Metallic Buckle
    builder.addBox(x - 2.1f, y - 1.3f, z + 8.5f, x + 2.1f, y + 1.3f, z + 10.4f, pantsColor)
    builder.addBox(x - 2.15f, y - 1.35f, z + 10.1f, x + 2.15f, y + 1.35f, z + 10.6f, beltColor)
    builder.addBox(x - 0.5f, y + 1.25f, z + 10.1f, x + 0.5f, y + 1.42f, z + 10.6f, buckleColor)

    // 2. Sculpted Athletic V-Taper Torso with Collar / Lapels
    builder.addBox(x - 2.5f, y - 1.4f, z + 10.6f, x + 2.5f, y + 1.4f, z + 15.2f, shirtColor)
    // Chest muscle depth & jacket lapels
    builder.addBox(x - 2.3f, y - 1.6f, z + 12.2f, x + 2.3f, y + 1.65f, z + 15.0f, shirtColor)
    // Collar
    builder.addBox(x - 1.5f, y - 1.2f, z + 14.8f, x + 1.5f, y + 1.5f, z + 15.6f, shirtColor)

    // 3. Anatomical Neck & Sculpted Head
    builder.addCylinderZ(x, y, z + 15.0f, z + 16.2f, 0.85f, 0.85f, 6, skinColor)
    // Head / Face
    builder.addBox(x - 1.35f, y - 1.35f, z + 16.0f, x + 1.35f, y + 1.45f, z + 18.9f, skinColor)
    // Nose bridge
    builder.addBox(x - 0.3f, y + 1.4f, z + 16.9f, x + 0.3f, y + 1.7f, z + 17.7f, skinColor)
    // Ears
    builder.addBox(x - 1.55f, y - 0.3f, z + 16.8f, x - 1.35f, y + 0.3f, z + 17.8f, skinColor)
    builder.addBox(x + 1.35f, y - 0.3f, z + 16.8f, x + 1.55f, y + 0.3f, z + 17.8f, skinColor)

    // Textured Styled Hair Volume
    builder.addBox(x - 1.45f, y - 1.45f, z + 17.6f, x + 1.45f, y + 1.3f, z + 19.4f, hairColor)
    builder.addBox(x - 1.4f, y - 1.55f, z + 16.6f, x + 1.4f, y - 1.2f, z + 18.2f, hairColor) // Back fade

    // Signature Sunglasses
    if (hasSunglasses) {
      val glassesCol = floatArrayOf(0.06f, 0.08f, 0.10f, 1f)
      val frameCol = floatArrayOf(0.85f, 0.75f, 0.25f, 1f) // Gold aviator frame
      builder.addBox(x - 1.4f, y + 1.0f, z + 17.3f, x + 1.4f, y + 1.5f, z + 18.1f, glassesCol)
      builder.addBox(x - 1.42f, y + 1.48f, z + 17.9f, x + 1.42f, y + 1.52f, z + 18.15f, frameCol)
    }

    // 4. Articulated Left Arm (Rounded Shoulder, Bicep, Forearm, Hand, Gold Watch)
    val leftArmY = y - armSwing
    builder.addBox(x - 3.5f, leftArmY - 1.1f, z + 12.2f, x - 2.5f, leftArmY + 1.1f, z + 15.0f, shirtColor) // Shoulder
    builder.addBox(x - 3.4f, leftArmY - 0.9f, z + 9.8f, x - 2.6f, leftArmY + 0.9f, z + 12.2f, skinColor)  // Forearm
    // Gold watch on left wrist
    builder.addBox(x - 3.45f, leftArmY - 0.85f, z + 9.7f, x - 2.55f, leftArmY + 0.85f, z + 10.1f, watchColor)
    builder.addBox(x - 3.3f, leftArmY - 0.7f, z + 8.5f, x - 2.7f, leftArmY + 0.7f, z + 9.7f, skinColor)   // Hand

    // 5. Articulated Right Arm
    val rightArmY = y + armSwing
    builder.addBox(x + 2.5f, rightArmY - 1.1f, z + 12.2f, x + 3.5f, rightArmY + 1.1f, z + 15.0f, shirtColor)
    builder.addBox(x + 2.6f, rightArmY - 0.9f, z + 9.8f, x + 3.4f, rightArmY + 0.9f, z + 12.2f, skinColor)
    builder.addBox(x + 2.7f, rightArmY - 0.7f, z + 8.5f, x + 3.3f, rightArmY + 0.7f, z + 9.7f, skinColor)

    // 6. Articulated Left Leg & Athletic Sneaker
    val leftLegY = y + legSwing
    builder.addBox(x - 1.9f, leftLegY - 1.1f, z + 4.8f, x - 0.3f, leftLegY + 1.1f, z + 8.5f, pantsColor) // Thigh
    builder.addBox(x - 1.8f, leftLegY - 1.0f + kneeBend, z + 1.4f, x - 0.4f, leftLegY + 1.0f + kneeBend, z + 4.8f, pantsColor) // Calf
    // Sneaker & Cushioned Sole
    builder.addBox(x - 1.85f, leftLegY - 1.2f, z + 0.4f, x - 0.35f, leftLegY + 2.0f, z + 1.6f, shoesColor)
    builder.addBox(x - 1.95f, leftLegY - 1.3f, z, x - 0.25f, leftLegY + 2.1f, z + 0.4f, soleColor)

    // 7. Articulated Right Leg & Sneaker
    val rightLegY = y - legSwing
    builder.addBox(x + 0.3f, rightLegY - 1.1f, z + 4.8f, x + 1.9f, rightLegY + 1.1f, z + 8.5f, pantsColor)
    builder.addBox(x + 0.4f, rightLegY - 1.0f - kneeBend, z + 1.4f, x + 1.8f, rightLegY + 1.0f - kneeBend, z + 4.8f, pantsColor)
    builder.addBox(x + 0.35f, rightLegY - 1.2f, z + 0.4f, x + 1.85f, rightLegY + 2.0f, z + 1.6f, shoesColor)
    builder.addBox(x + 0.25f, rightLegY - 1.3f, z, x + 1.95f, rightLegY + 2.1f, z + 0.4f, soleColor)
  }

  fun buildFirstPersonWeaponMesh(weaponIndex: Int): MeshData {
    val b = MeshBuilder(512)
    val gunMetal = floatArrayOf(0.14f, 0.16f, 0.20f, 1f)
    val darkSteel = floatArrayOf(0.24f, 0.28f, 0.34f, 1f)
    val goldAccent = floatArrayOf(0.95f, 0.78f, 0.20f, 1f)
    val armSkin = floatArrayOf(0.86f, 0.64f, 0.50f, 1f)
    val clothSleeve = floatArrayOf(0.15f, 0.20f, 0.18f, 1f)

    // Forearm & Hand holding weapon
    b.addBox(0.20f, 0.35f, -0.34f, 0.34f, 0.62f, -0.22f, clothSleeve)
    b.addBox(0.18f, 0.58f, -0.26f, 0.28f, 0.74f, -0.14f, armSkin)

    when (weaponIndex) {
      // PISTOL (.45 Combat Pistol)
      1 -> {
        b.addBox(0.13f, 0.60f, -0.12f, 0.21f, 0.98f, -0.04f, darkSteel)
        b.addBox(0.14f, 0.65f, -0.04f, 0.20f, 0.95f, -0.02f, goldAccent)
        b.addBox(0.15f, 0.62f, -0.26f, 0.20f, 0.72f, -0.12f, gunMetal)
      }
      // SMG (Micro SMG)
      2 -> {
        b.addBox(0.11f, 0.52f, -0.14f, 0.23f, 1.08f, -0.02f, darkSteel)
        b.addBox(0.14f, 0.64f, -0.32f, 0.19f, 0.76f, -0.14f, gunMetal)
        b.addBox(0.15f, 0.88f, -0.30f, 0.18f, 0.96f, -0.14f, goldAccent)
      }
      // RPG-7 Rocket Launcher
      5 -> {
        b.addBox(0.08f, 0.38f, -0.10f, 0.24f, 1.38f, 0.08f, floatArrayOf(0.18f, 0.26f, 0.16f, 1f))
        b.addBox(0.05f, 1.38f, -0.13f, 0.27f, 1.62f, 0.11f, floatArrayOf(0.92f, 0.22f, 0.14f, 1f))
      }
      // CARBINE ASSAULT RIFLE & SHOTGUN
      else -> {
        b.addBox(0.11f, 0.48f, -0.16f, 0.23f, 1.30f, -0.02f, darkSteel)
        b.addBox(0.14f, 0.76f, -0.36f, 0.19f, 0.92f, -0.16f, gunMetal)
        b.addBox(0.10f, 0.92f, -0.15f, 0.24f, 1.20f, -0.03f, floatArrayOf(0.12f, 0.12f, 0.12f, 1f))
        b.addBox(0.13f, 0.54f, -0.02f, 0.21f, 0.85f, 0.04f, goldAccent)
      }
    }
    return b.build()
  }

  fun buildVehicleCockpitMesh(): MeshData {
    val b = MeshBuilder(512)
    val dashColor = floatArrayOf(0.12f, 0.14f, 0.18f, 1f)
    val leatherColor = floatArrayOf(0.08f, 0.09f, 0.12f, 1f)
    val dialCyan = floatArrayOf(0.22f, 0.88f, 0.98f, 1f)
    val carHood = floatArrayOf(0.98f, 0.72f, 0.08f, 1f) // Zentorno Yellow

    // Dashboard Shelf
    b.addBox(-0.75f, 0.60f, -0.45f, 0.75f, 1.10f, -0.22f, dashColor)
    // Digital Speedometer Instrument Cluster
    b.addBox(-0.35f, 0.82f, -0.20f, -0.05f, 0.96f, -0.10f, dialCyan)
    // Steering Column & Wheel Rim
    b.addBox(-0.25f, 0.45f, -0.30f, -0.15f, 0.65f, -0.15f, leatherColor)
    b.addBox(-0.38f, 0.42f, -0.15f, -0.02f, 0.46f, 0.12f, leatherColor)
    // Windshield Pillars
    b.addBox(-0.78f, 0.70f, -0.30f, -0.68f, 1.15f, 0.65f, leatherColor)
    b.addBox(0.68f, 0.70f, -0.30f, 0.78f, 1.15f, 0.65f, leatherColor)
    b.addBox(-0.75f, 0.65f, 0.55f, 0.75f, 0.85f, 0.68f, leatherColor)
    // Aerodynamic Supercar Hood extending outside windshield
    b.addBox(-0.72f, 1.10f, -0.40f, 0.72f, 2.90f, -0.26f, carHood)

    return b.build()
  }

  /**
   * Distant Mountain Ridge Backdrop (Vinewood Hills & Mount Chiliad Silhouette):
   * Creates a jagged, multi-layered mountain silhouette with stratified rock colors
   * and atmospheric depth, transforming the environment from a flat void into a real world.
   */
  fun addMountainBackdrop(builder: MeshBuilder) {
    val rockFar = floatArrayOf(0.36f, 0.40f, 0.48f, 1f)
    val rockMid = floatArrayOf(0.48f, 0.46f, 0.40f, 1f)
    val rockSun = floatArrayOf(0.62f, 0.54f, 0.42f, 1f)
    val grassHill = floatArrayOf(0.28f, 0.45f, 0.25f, 1f)

    // North Mountain Ridge (Vinewood / Mount Chiliad)
    val nPeaks = listOf(
      Triple(0f, -400f, 180f),
      Triple(600f, -600f, 260f),
      Triple(1200f, -500f, 320f),
      Triple(1800f, -650f, 410f),
      Triple(2400f, -550f, 360f),
      Triple(3000f, -700f, 480f), // Mount Chiliad summit
      Triple(3600f, -500f, 380f),
      Triple(4200f, -450f, 250f)
    )

    for (i in 0 until nPeaks.size - 1) {
      val p1 = nPeaks[i]
      val p2 = nPeaks[i + 1]
      val midX = (p1.first + p2.first) * 0.5f
      val midY = (p1.second + p2.second) * 0.5f + 180f
      val baseZ = 0f

      // Front mountain face (facing south into Los Santos)
      builder.addTriangle(
        p1.first, p1.second, p1.third,
        p2.first, p2.second, p2.third,
        midX, midY, baseZ,
        0f, 0.8f, 0.6f, if (i % 2 == 0) rockSun else rockMid
      )

      // Base foothills
      builder.addGroundQuad(
        p1.first, 0f, p2.first, 0f,
        p2.first, 200f, p1.first, 200f,
        baseZ, grassHill
      )
    }

    // Colossal 3D White VINEWOOD Hillside Sign
    addVinewoodSign(builder)

    // East Mountain Ridge (Palomino Highlands)
    val ePeaks = listOf(
      Triple(4600f, 0f, 220f),
      Triple(4700f, 800f, 310f),
      Triple(4800f, 1600f, 380f),
      Triple(4750f, 2400f, 340f),
      Triple(4650f, 3200f, 280f),
      Triple(4500f, 4000f, 190f)
    )

    for (i in 0 until ePeaks.size - 1) {
      val p1 = ePeaks[i]
      val p2 = ePeaks[i + 1]
      val midX = (p1.first + p2.first) * 0.5f - 180f
      val midY = (p1.second + p2.second) * 0.5f

      builder.addTriangle(
        p1.first, p1.second, p1.third,
        p2.first, p2.second, p2.third,
        midX, midY, 0f,
        -0.8f, 0f, 0.6f, rockFar
      )
    }
  }

  /**
   * Colossal 3D White VINEWOOD Hillside Sign:
   * Authentic recreation of the legendary Los Santos hillside landmark,
   * fully sculpted with 3D white block typography and rear wooden/steel structural scaffolding.
   */
  fun addVinewoodSign(builder: MeshBuilder) {
    val white = floatArrayOf(0.98f, 0.98f, 0.98f, 1f)
    val strutWood = floatArrayOf(0.28f, 0.20f, 0.14f, 1f)
    val baseZ = 95f
    val topZ = 125f
    val letterH = topZ - baseZ
    val y = 110f

    val letterCenters = listOf(
      1880f to "V",
      1940f to "I",
      2000f to "N",
      2060f to "E",
      2120f to "W",
      2180f to "O",
      2240f to "O",
      2300f to "D"
    )

    for ((cx, char) in letterCenters) {
      // Rear timber scaffolding legs
      builder.addBox(cx - 9f, y + 1.2f, baseZ - 8f, cx - 7.5f, y + 4.5f, topZ - 4f, strutWood)
      builder.addBox(cx + 7.5f, y + 1.2f, baseZ - 8f, cx + 9f, y + 4.5f, topZ - 4f, strutWood)
      builder.addBox(cx - 9f, y + 2.5f, baseZ + 12f, cx + 9f, y + 3.8f, baseZ + 14f, strutWood)

      val halfW = 9.5f
      when (char) {
        "V" -> {
          builder.addQuad(
            cx - halfW, y - 0.9f, topZ,
            cx - halfW + 3.5f, y - 0.9f, topZ,
            cx + 1.5f, y - 0.9f, baseZ,
            cx - 2.0f, y - 0.9f, baseZ,
            0f, -1f, 0f, white
          )
          builder.addQuad(
            cx + halfW - 3.5f, y - 0.9f, topZ,
            cx + halfW, y - 0.9f, topZ,
            cx + 2.0f, y - 0.9f, baseZ,
            cx - 1.5f, y - 0.9f, baseZ,
            0f, -1f, 0f, white
          )
        }
        "I" -> {
          builder.addBox(cx - 2.5f, y - 1f, baseZ, cx + 2.5f, y + 1f, topZ, white)
          builder.addBox(cx - 7f, y - 1f, topZ - 3f, cx + 7f, y + 1f, topZ, white)
          builder.addBox(cx - 7f, y - 1f, baseZ, cx + 7f, y + 1f, baseZ + 3f, white)
        }
        "N" -> {
          builder.addBox(cx - halfW, y - 1f, baseZ, cx - halfW + 3.5f, y + 1f, topZ, white)
          builder.addBox(cx + halfW - 3.5f, y - 1f, baseZ, cx + halfW, y + 1f, topZ, white)
          builder.addQuad(
            cx - halfW + 3.5f, y - 0.9f, topZ,
            cx - halfW, y - 0.9f, topZ,
            cx + halfW - 3.5f, y - 0.9f, baseZ,
            cx + halfW, y - 0.9f, baseZ,
            0f, -1f, 0f, white
          )
        }
        "E" -> {
          builder.addBox(cx - halfW, y - 1f, baseZ, cx - halfW + 3.5f, y + 1f, topZ, white)
          builder.addBox(cx - halfW, y - 1f, topZ - 3.5f, cx + halfW, y + 1f, topZ, white)
          builder.addBox(cx - halfW, y - 1f, baseZ + letterH * 0.45f, cx + halfW - 2f, y + 1f, baseZ + letterH * 0.45f + 3f, white)
          builder.addBox(cx - halfW, y - 1f, baseZ, cx + halfW, y + 1f, baseZ + 3.5f, white)
        }
        "W" -> {
          val wW = 12.5f
          builder.addBox(cx - wW, y - 1f, baseZ, cx - wW + 3.0f, y + 1f, topZ, white)
          builder.addBox(cx + wW - 3.0f, y - 1f, baseZ, cx + wW, y + 1f, topZ, white)
          builder.addBox(cx - 1.5f, y - 1f, baseZ, cx + 1.5f, y + 1f, topZ - 4f, white)
          builder.addBox(cx - wW, y - 1f, baseZ, cx + wW, y + 1f, baseZ + 3.5f, white)
        }
        "O" -> {
          builder.addBox(cx - halfW, y - 1f, baseZ, cx - halfW + 3.5f, y + 1f, topZ, white)
          builder.addBox(cx + halfW - 3.5f, y - 1f, baseZ, cx + halfW, y + 1f, topZ, white)
          builder.addBox(cx - halfW, y - 1f, topZ - 3.5f, cx + halfW, y + 1f, topZ, white)
          builder.addBox(cx - halfW, y - 1f, baseZ, cx + halfW, y + 1f, baseZ + 3.5f, white)
        }
        "D" -> {
          builder.addBox(cx - halfW, y - 1f, baseZ, cx - halfW + 3.5f, y + 1f, topZ, white)
          builder.addBox(cx + halfW - 3.5f, y - 1f, baseZ + 4f, cx + halfW, y + 1f, topZ - 4f, white)
          builder.addBox(cx - halfW, y - 1f, topZ - 3.5f, cx + halfW - 2f, y + 1f, topZ, white)
          builder.addBox(cx - halfW, y - 1f, baseZ, cx + halfW - 2f, y + 1f, baseZ + 3.5f, white)
        }
      }
    }
  }

  /**
   * Realistic 3D Architectural Skyscraper (Maze Bank Tower / FIB / Arcadius):
   * - 3-tier setback silhouette
   * - 3D window grid frames with warm interior lights and cool reflective glass
   * - Grand entrance colonnade with revolving doors and glass canopy
   * - Rooftop mechanical penthouse, water chiller units, and antenna mast
   */
  fun addDetailedSkyscraper(
    solidBuilder: MeshBuilder,
    emissiveBuilder: MeshBuilder,
    minX: Float, minY: Float, maxX: Float, maxY: Float,
    totalHeight: Float,
    wallColor: FloatArray,
    glassColor: FloatArray = floatArrayOf(0.42f, 0.68f, 0.88f, 1f),
    warmLightColor: FloatArray = floatArrayOf(0.98f, 0.86f, 0.55f, 1f)
  ) {
    val cx = (minX + maxX) * 0.5f
    val cy = (minY + maxY) * 0.5f
    val halfW = (maxX - minX) * 0.5f
    val halfD = (maxY - minY) * 0.5f

    val tier1H = totalHeight * 0.45f
    val tier2H = totalHeight * 0.80f
    val tier3H = totalHeight

    val trimCol = floatArrayOf(wallColor[0] * 0.8f, wallColor[1] * 0.8f, wallColor[2] * 0.8f, 1f)
    val concreteDark = floatArrayOf(0.22f, 0.24f, 0.28f, 1f)

    // Tier 1: Base podium & lower tower
    solidBuilder.addBox(cx - halfW, cy - halfD, 0f, cx + halfW, cy + halfD, tier1H, wallColor, trimCol)

    // Ground Floor Grand Entrance Arcade
    val entH = 14f
    solidBuilder.addBox(cx - halfW - 1.5f, cy - halfD - 3f, 0f, cx + halfW + 1.5f, cy - halfD, entH, concreteDark)
    // Dark glass revolving doors
    solidBuilder.addBox(cx - 18f, cy - halfD - 3.2f, 0f, cx + 18f, cy - halfD - 0.5f, 11f, floatArrayOf(0.12f, 0.14f, 0.18f, 1f))
    // Entrance illuminated canopy
    solidBuilder.addBox(cx - 24f, cy - halfD - 12f, 11f, cx + 24f, cy - halfD, 13f, concreteDark)

    // Window Grids Tier 1 (Front & Back)
    val floorSpacing = 16f
    var fz = entH + 4f
    var floorIdx = 0
    while (fz < tier1H - 8f) {
      val isLit = (floorIdx % 3 == 0)
      val curGlass = if (isLit) warmLightColor else glassColor

      // Horizontal window band recessed into facade
      solidBuilder.addBox(cx - halfW + 4f, cy - halfD - 0.3f, fz, cx + halfW - 4f, cy - halfD + 0.3f, fz + 8f, curGlass)
      solidBuilder.addBox(cx - halfW + 4f, cy + halfD - 0.3f, fz, cx + halfW - 4f, cy + halfD + 0.3f, fz + 8f, curGlass)
      // Side window bands
      solidBuilder.addBox(cx - halfW - 0.3f, cy - halfD + 4f, fz, cx - halfW + 0.3f, cy + halfD - 4f, fz + 8f, curGlass)
      solidBuilder.addBox(cx + halfW - 0.3f, cy - halfD + 4f, fz, cx + halfW + 0.3f, cy + halfD - 4f, fz + 8f, curGlass)

      fz += floorSpacing
      floorIdx++
    }

    // Tier 2: Mid Tower Setback
    val w2 = halfW * 0.85f
    val d2 = halfD * 0.85f
    solidBuilder.addBox(cx - w2, cy - d2, tier1H, cx + w2, cy + d2, tier2H, wallColor, trimCol)

    while (fz < tier2H - 8f) {
      val isLit = (floorIdx % 2 == 0)
      val curGlass = if (isLit) warmLightColor else glassColor

      solidBuilder.addBox(cx - w2 + 3f, cy - d2 - 0.3f, fz, cx + w2 - 3f, cy - d2 + 0.3f, fz + 8f, curGlass)
      solidBuilder.addBox(cx - w2 + 3f, cy + d2 - 0.3f, fz, cx + w2 - 3f, cy + d2 + 0.3f, fz + 8f, curGlass)
      solidBuilder.addBox(cx - w2 - 0.3f, cy - d2 + 3f, fz, cx - w2 + 0.3f, cy + d2 - 3f, fz + 8f, curGlass)
      solidBuilder.addBox(cx + w2 - 0.3f, cy - d2 + 3f, fz, cx + w2 + 0.3f, cy + d2 - 3f, fz + 8f, curGlass)

      fz += floorSpacing
      floorIdx++
    }

    // Tier 3: Crown Penthouse Setback
    val w3 = halfW * 0.68f
    val d3 = halfD * 0.68f
    solidBuilder.addBox(cx - w3, cy - d3, tier2H, cx + w3, cy + d3, tier3H, wallColor, concreteDark)

    // Rooftop Mechanical Equipment (HVAC chillers, water tank, broadcast antenna)
    solidBuilder.addBox(cx - 16f, cy - 16f, tier3H, cx + 4f, cy + 4f, tier3H + 12f, concreteDark)
    // Water tank cylinder
    solidBuilder.addCylinderZ(cx + 14f, cy + 14f, tier3H, tier3H + 16f, 6.5f, 6.5f, 8, floatArrayOf(0.48f, 0.40f, 0.32f, 1f))
    // Conical water tank roof
    solidBuilder.addCylinderZ(cx + 14f, cy + 14f, tier3H + 16f, tier3H + 20f, 7.2f, 0.5f, 8, floatArrayOf(0.35f, 0.28f, 0.22f, 1f))

    // Broadcast Communication Antenna Mast
    solidBuilder.addCylinderZ(cx, cy, tier3H, tier3H + 55f, 1.4f, 0.4f, 6, floatArrayOf(0.85f, 0.88f, 0.92f, 1f))
    // Blinking red aviation warning beacon at tip
    emissiveBuilder.addBox(cx - 1.2f, cy - 1.2f, tier3H + 54f, cx + 1.2f, cy + 1.2f, tier3H + 57f, floatArrayOf(1.0f, 0.15f, 0.15f, 1f))
  }

  /**
   * Realistic California Spanish Colonial Mansion (Michael's Rockford Hills Estate):
   * Terracotta tile roof, arched loggias, sparkling swimming pool, and sun loungers.
   */
  fun addSpanishMansion(
    builder: MeshBuilder,
    minX: Float, minY: Float, maxX: Float, maxY: Float, height: Float
  ) {
    val stuccoWhite = floatArrayOf(0.95f, 0.93f, 0.88f, 1f)
    val terracottaRoof = floatArrayOf(0.78f, 0.36f, 0.18f, 1f)
    val woodBeams = floatArrayOf(0.35f, 0.22f, 0.14f, 1f)
    val poolWater = floatArrayOf(0.18f, 0.78f, 0.92f, 1f)
    val poolCoping = floatArrayOf(0.92f, 0.92f, 0.94f, 1f)
    val tennisCourtGreen = floatArrayOf(0.18f, 0.52f, 0.28f, 1f)
    val whiteLine = floatArrayOf(0.98f, 0.98f, 0.98f, 1f)

    val cx = (minX + maxX) * 0.5f
    val cy = (minY + maxY) * 0.5f

    // Main 2-story Villa Wing
    builder.addBox(minX + 20f, minY + 15f, 0f, maxX - 20f, maxY - 25f, height * 0.7f, stuccoWhite)
    // Terracotta hipped roof with eaves
    builder.addBox(minX + 16f, minY + 11f, height * 0.7f, maxX - 16f, maxY - 21f, height, terracottaRoof, terracottaRoof)

    // Balcony with decorative wrought-iron railings
    builder.addBox(minX + 35f, minY + 8f, height * 0.42f, maxX - 35f, minY + 15f, height * 0.45f, woodBeams)

    // Backyard Luxury Swimming Pool
    val poolX1 = minX + 30f
    val poolY1 = maxY - 15f
    val poolX2 = minX + 110f
    val poolY2 = maxY + 40f
    // Pool coping border
    builder.addBox(poolX1 - 3f, poolY1 - 3f, 0.2f, poolX2 + 3f, poolY2 + 3f, 0.8f, poolCoping)
    // Water surface
    builder.addGroundQuad(poolX1, poolY1, poolX2, poolY1, poolX2, poolY2, poolX1, poolY2, 0.7f, poolWater)

    // Private Tennis Court
    val tcX1 = maxX - 100f
    val tcY1 = maxY - 10f
    val tcX2 = maxX + 10f
    val tcY2 = maxY + 65f
    builder.addGroundQuad(tcX1, tcY1, tcX2, tcY1, tcX2, tcY2, tcX1, tcY2, 0.55f, tennisCourtGreen)
    // White court boundary lines
    builder.addGroundQuad(tcX1 + 4f, tcY1 + 4f, tcX2 - 4f, tcY1 + 4f, tcX2 - 4f, tcY1 + 6f, tcX1 + 4f, tcY1 + 6f, 0.6f, whiteLine)
    builder.addGroundQuad(tcX1 + 4f, tcY2 - 6f, tcX2 - 4f, tcY2 - 6f, tcX2 - 4f, tcY2 - 4f, tcX1 + 4f, tcY2 - 4f, 0.6f, whiteLine)
  }

  /**
   * Realistic South Los Santos Residential Bungalow (Franklin's House & Grove Street):
   * Gabled porch roof, siding panels, front walkway, mailbox, and carport.
   */
  fun addResidentialBungalow(
    builder: MeshBuilder,
    minX: Float, minY: Float, maxX: Float, maxY: Float, height: Float,
    sidingColor: FloatArray = floatArrayOf(0.72f, 0.68f, 0.60f, 1f),
    roofColor: FloatArray = floatArrayOf(0.28f, 0.30f, 0.34f, 1f)
  ) {
    val trimWhite = floatArrayOf(0.92f, 0.92f, 0.94f, 1f)
    val lawnGreen = floatArrayOf(0.24f, 0.54f, 0.24f, 1f)
    val driveGray = floatArrayOf(0.45f, 0.47f, 0.50f, 1f)

    // Front yard lawn
    builder.addGroundQuad(minX - 15f, minY - 20f, maxX + 25f, minY - 20f, maxX + 25f, maxY + 15f, minX - 15f, maxY + 15f, 0.4f, lawnGreen)
    // Concrete driveway
    builder.addGroundQuad(maxX - 20f, minY - 20f, maxX + 15f, minY - 20f, maxX + 15f, maxY, maxX - 20f, maxY, 0.5f, driveGray)

    // Main house body
    builder.addBox(minX, minY, 0f, maxX - 20f, maxY, height * 0.65f, sidingColor)
    // Pitched gabled roof
    builder.addBox(minX - 3f, minY - 3f, height * 0.65f, maxX - 17f, maxY + 3f, height, roofColor, roofColor)

    // Front Covered Porch
    builder.addBox(minX + 8f, minY - 10f, 0f, minX + 38f, minY, 1.2f, driveGray)
    builder.addBox(minX + 8f, minY - 10f, 1.2f, minX + 11f, minY - 7f, height * 0.60f, trimWhite)
    builder.addBox(minX + 35f, minY - 10f, 1.2f, minX + 38f, minY - 7f, height * 0.60f, trimWhite)
    builder.addBox(minX + 6f, minY - 12f, height * 0.60f, minX + 40f, minY, height * 0.70f, roofColor)
  }

  /**
   * Realistic Organic Tree with branching trunk and multi-tiered foliage canopies
   */
  fun addOrganicTree(builder: MeshBuilder, x: Float, y: Float, height: Float = 36f, isPine: Boolean = false) {
    val woodDark = floatArrayOf(0.32f, 0.24f, 0.16f, 1f)
    val leafBottom = floatArrayOf(0.14f, 0.42f, 0.18f, 1f)
    val leafMid = floatArrayOf(0.18f, 0.54f, 0.24f, 1f)
    val leafTop = floatArrayOf(0.24f, 0.66f, 0.30f, 1f)

    // Trunk
    val trunkH = height * 0.38f
    builder.addCylinderZ(x, y, 0f, trunkH, 2.2f, 1.4f, 8, woodDark)

    if (isPine) {
      // 3 Conical tiered tiers
      val r1 = 14f
      val r2 = 10f
      val r3 = 5f
      builder.addCylinderZ(x, y, trunkH * 0.8f, trunkH + 12f, r1, 2f, 8, leafBottom)
      builder.addCylinderZ(x, y, trunkH + 10f, trunkH + 22f, r2, 1.5f, 8, leafMid)
      builder.addCylinderZ(x, y, trunkH + 20f, height + 6f, r3, 0.4f, 8, leafTop)
    } else {
      // Lush Deciduous Oak Canopy (3 overlapping organic clusters)
      builder.addBox(x - 14f, y - 14f, trunkH, x + 14f, y + 14f, trunkH + 15f, leafBottom, leafMid)
      builder.addBox(x - 10f, y - 10f, trunkH + 12f, x + 10f, y + 10f, trunkH + 24f, leafMid, leafTop)
      builder.addBox(x - 6f, y - 6f, trunkH + 22f, x + 6f, y + 6f, height + 4f, leafTop, leafTop)
    }
  }

  /**
   * Road Surface Markings: Manholes, Storm Drains & White Turn Arrows
   */
  fun addRoadSurfaceDetails(builder: MeshBuilder, x: Float, y: Float, isVert: Boolean) {
    val ironManhole = floatArrayOf(0.15f, 0.16f, 0.18f, 1f)
    val whiteArrow = floatArrayOf(0.96f, 0.96f, 0.98f, 1f)

    // Embedded cast-iron circular manhole cover
    builder.addCylinderZ(x, y, 0.56f, 0.60f, 4.2f, 4.2f, 8, ironManhole)

    // Painted straight arrow on road lane
    val off = if (isVert) Pair(0f, 32f) else Pair(32f, 0f)
    val ax = x + off.first
    val ay = y + off.second
    if (isVert) {
      // Shaft
      builder.addGroundQuad(ax - 1.2f, ay - 10f, ax + 1.2f, ay - 10f, ax + 1.2f, ay + 6f, ax - 1.2f, ay + 6f, 0.62f, whiteArrow)
      // Head
      builder.addTriangle(ax - 4.5f, ay + 4f, 0.62f, ax + 4.5f, ay + 4f, 0.62f, ax, ay + 14f, 0.62f, 0f, 0f, 1f, whiteArrow)
    } else {
      builder.addGroundQuad(ax - 10f, ay - 1.2f, ax + 6f, ay - 1.2f, ax + 6f, ay + 1.2f, ax - 10f, ay + 1.2f, 0.62f, whiteArrow)
      builder.addTriangle(ax + 4f, ay - 4.5f, 0.62f, ax + 4f, ay + 4.5f, 0.62f, ax + 14f, ay, 0.62f, 0f, 0f, 1f, whiteArrow)
    }
  }

  /**
   * 3D California Curbside Fire Hydrant:
   * Red cast-iron cylindrical barrel, yellow domed bonnet cap with hex nut,
   * side hose connection pumper nozzles, and circular ground flange.
   */
  fun addFireHydrant(builder: MeshBuilder, x: Float, y: Float) {
    val bodyRed = floatArrayOf(0.88f, 0.14f, 0.14f, 1f)
    val capYellow = floatArrayOf(0.96f, 0.85f, 0.15f, 1f)
    val silverNozzle = floatArrayOf(0.85f, 0.86f, 0.90f, 1f)

    // Ground flange ring
    builder.addCylinderZ(x, y, 0.85f, 1.35f, 1.8f, 1.8f, 6, bodyRed)
    // Main vertical barrel
    builder.addCylinderZ(x, y, 1.35f, 5.5f, 1.25f, 1.25f, 6, bodyRed)
    // Yellow bonnet domed cap
    builder.addCylinderZ(x, y, 5.5f, 6.8f, 1.35f, 0.7f, 6, capYellow)
    // Hex operating nut on top
    builder.addBox(x - 0.35f, y - 0.35f, 6.8f, x + 0.35f, y + 0.35f, 7.3f, capYellow)
    // Left & Right threaded discharge nozzles
    builder.addBox(x - 2.2f, y - 0.45f, 3.8f, x - 1.2f, y + 0.45f, 4.8f, silverNozzle)
    builder.addBox(x + 1.2f, y - 0.45f, 3.8f, x + 2.2f, y + 0.45f, 4.8f, silverNozzle)
  }

  /**
   * 3D Municipal Curbside Trash & Blue Recycling Bins (Los Santos City):
   * Plastic molded bins with wheeled axles and hinged lids.
   */
  fun addTrashAndRecyclingBins(builder: MeshBuilder, x: Float, y: Float, isVert: Boolean) {
    val trashGreen = floatArrayOf(0.18f, 0.42f, 0.22f, 1f)
    val recycleBlue = floatArrayOf(0.14f, 0.48f, 0.85f, 1f)
    val lidDark = floatArrayOf(0.12f, 0.13f, 0.15f, 1f)
    val wheelBlack = floatArrayOf(0.08f, 0.09f, 0.10f, 1f)

    val (tx, ty, rx, ry) = if (isVert) {
      listOf(x, y - 2.8f, x, y + 2.8f)
    } else {
      listOf(x - 2.8f, y, x + 2.8f, y)
    }

    // Green Trash Can
    builder.addBox(tx - 1.8f, ty - 1.8f, 0.85f, tx + 1.8f, ty + 1.8f, 6.2f, trashGreen)
    builder.addBox(tx - 2.0f, ty - 2.0f, 6.2f, tx + 2.0f, ty + 2.0f, 6.9f, lidDark)
    builder.addBox(tx - 1.9f, ty - 2.2f, 0.85f, tx + 1.9f, ty - 1.7f, 2.0f, wheelBlack)

    // Blue Recycling Can
    builder.addBox(rx - 1.8f, ry - 1.8f, 0.85f, rx + 1.8f, ry + 1.8f, 6.2f, recycleBlue)
    builder.addBox(rx - 2.0f, ry - 2.0f, 6.2f, rx + 2.0f, ry + 2.0f, 6.9f, recycleBlue)
    builder.addBox(rx - 1.9f, ry - 2.2f, 0.85f, rx + 1.9f, ry - 1.7f, 2.0f, wheelBlack)
  }

  /**
   * 3D Modern Glass Transit Bus Shelter:
   * Dark powder-coated steel posts, curved clear glass canopy roof,
   * brushed aluminum passenger bench, and illuminated transit map display.
   */
  fun addBusStopShelter(
    solidBuilder: MeshBuilder,
    emissiveBuilder: MeshBuilder,
    x: Float, y: Float, isVert: Boolean
  ) {
    val steelCol = floatArrayOf(0.15f, 0.17f, 0.20f, 1f)
    val glassCol = floatArrayOf(0.35f, 0.65f, 0.85f, 0.65f)
    val benchWood = floatArrayOf(0.65f, 0.45f, 0.28f, 1f)
    val mapIllum = floatArrayOf(0.95f, 0.95f, 0.85f, 1f)

    val halfL = 12f
    val halfW = 4.5f
    val h = 14f

    if (isVert) {
      // 4 Steel support posts
      solidBuilder.addBox(x - halfW, y - halfL, 0.85f, x - halfW + 0.9f, y - halfL + 0.9f, h, steelCol)
      solidBuilder.addBox(x - halfW, y + halfL - 0.9f, 0.85f, x - halfW + 0.9f, y + halfL, h, steelCol)
      solidBuilder.addBox(x + halfW - 0.9f, y - halfL, 0.85f, x + halfW, y - halfL + 0.9f, h, steelCol)
      solidBuilder.addBox(x + halfW - 0.9f, y + halfL - 0.9f, 0.85f, x + halfW, y + halfL, h, steelCol)
      // Roof canopy
      solidBuilder.addBox(x - halfW - 1.5f, y - halfL - 1.5f, h, x + halfW + 1.5f, y + halfL + 1.5f, h + 1.0f, steelCol, glassCol)
      // Back glass panel
      solidBuilder.addBox(x - halfW, y - halfL, 1.2f, x - halfW + 0.4f, y + halfL, h - 0.5f, glassCol)
      // Wooden bench
      solidBuilder.addBox(x - halfW + 1.2f, y - halfL + 3f, 3.2f, x - halfW + 3.8f, y + halfL - 3f, 4.0f, benchWood)
      // Illuminated transit map timetable kiosk
      emissiveBuilder.addBox(x - halfW - 0.2f, y + halfL - 6.5f, 3.0f, x - halfW + 0.6f, y + halfL - 1.5f, 11.5f, mapIllum)
    } else {
      solidBuilder.addBox(x - halfL, y - halfW, 0.85f, x - halfL + 0.9f, y - halfW + 0.9f, h, steelCol)
      solidBuilder.addBox(x + halfL - 0.9f, y - halfW, 0.85f, x + halfL, y - halfW + 0.9f, h, steelCol)
      solidBuilder.addBox(x - halfL, y + halfW - 0.9f, 0.85f, x - halfL + 0.9f, y + halfW, h, steelCol)
      solidBuilder.addBox(x + halfL - 0.9f, y + halfW - 0.9f, 0.85f, x + halfL, y + halfW, h, steelCol)
      solidBuilder.addBox(x - halfL - 1.5f, y - halfW - 1.5f, h, x + halfL + 1.5f, y + halfW + 1.5f, h + 1.0f, steelCol, glassCol)
      solidBuilder.addBox(x - halfL, y - halfW, 1.2f, x + halfL, y - halfW + 0.4f, h - 0.5f, glassCol)
      solidBuilder.addBox(x - halfL + 3f, y - halfW + 1.2f, 3.2f, x + halfL - 3f, y - halfW + 3.8f, 4.0f, benchWood)
      emissiveBuilder.addBox(x + halfL - 6.5f, y - halfW - 0.2f, 3.0f, x + halfL - 1.5f, y - halfW + 0.6f, 11.5f, mapIllum)
    }
  }

  /**
   * 3D Overhead Highway Directional Gantry:
   * Spans across multi-lane highways with overhead steel truss and green reflective route signs.
   */
  fun addHighwayGantry(
    solidBuilder: MeshBuilder,
    emissiveBuilder: MeshBuilder,
    cx: Float, cy: Float,
    isVert: Boolean, spanWidth: Float
  ) {
    val steelTruss = floatArrayOf(0.40f, 0.44f, 0.48f, 1f)
    val signGreen = floatArrayOf(0.08f, 0.48f, 0.24f, 1f)
    val whiteText = floatArrayOf(0.98f, 0.98f, 0.98f, 1f)
    val clearH = 26f
    val topH = 36f

    val halfSpan = spanWidth * 0.5f + 8f

    if (isVert) {
      // Left and right steel lattice uprights
      solidBuilder.addBox(cx - halfSpan - 2.5f, cy - 2f, 0.85f, cx - halfSpan + 2.5f, cy + 2f, topH, steelTruss)
      solidBuilder.addBox(cx + halfSpan - 2.5f, cy - 2f, 0.85f, cx + halfSpan + 2.5f, cy + 2f, topH, steelTruss)
      // Overhead cross-truss
      solidBuilder.addBox(cx - halfSpan, cy - 1.8f, clearH, cx + halfSpan, cy + 1.8f, topH, steelTruss)

      // Green Highway Signboard (e.g. "LOS SANTOS FWY / DOWNTOWN")
      solidBuilder.addBox(cx - halfSpan * 0.7f, cy - 2.2f, clearH + 1.5f, cx - 4f, cy - 1.6f, topH - 1.5f, signGreen)
      emissiveBuilder.addBox(cx - halfSpan * 0.65f, cy - 2.3f, clearH + 3.5f, cx - 8f, cy - 2.1f, topH - 3.5f, whiteText)

      // Second Signboard ("VINEWOOD / DEL PERRO")
      solidBuilder.addBox(cx + 4f, cy - 2.2f, clearH + 1.5f, cx + halfSpan * 0.7f, cy - 1.6f, topH - 1.5f, signGreen)
      emissiveBuilder.addBox(cx + 8f, cy - 2.3f, clearH + 3.5f, cx + halfSpan * 0.65f, cy - 2.1f, topH - 3.5f, whiteText)
    } else {
      solidBuilder.addBox(cx - 2f, cy - halfSpan - 2.5f, 0.85f, cx + 2f, cy - halfSpan + 2.5f, topH, steelTruss)
      solidBuilder.addBox(cx - 2f, cy + halfSpan - 2.5f, 0.85f, cx + 2f, cy + halfSpan + 2.5f, topH, steelTruss)
      solidBuilder.addBox(cx - 1.8f, cy - halfSpan, clearH, cx + 1.8f, cy + halfSpan, topH, steelTruss)

      solidBuilder.addBox(cx - 2.2f, cy - halfSpan * 0.7f, clearH + 1.5f, cx - 1.6f, cy - 4f, topH - 1.5f, signGreen)
      emissiveBuilder.addBox(cx - 2.3f, cy - halfSpan * 0.65f, clearH + 3.5f, cx - 2.1f, cy - 8f, topH - 3.5f, whiteText)

      solidBuilder.addBox(cx - 2.2f, cy + 4f, clearH + 1.5f, cx - 1.6f, cy + halfSpan * 0.7f, topH - 1.5f, signGreen)
      emissiveBuilder.addBox(cx - 2.3f, cy + 8f, clearH + 3.5f, cx - 2.1f, cy + halfSpan * 0.65f, topH - 3.5f, whiteText)
    }
  }

  /**
   * 3D Curbside Parking Meter & Blue US Mail Drop Box
   */
  fun addParkingMeter(builder: MeshBuilder, x: Float, y: Float) {
    val poleSteel = floatArrayOf(0.65f, 0.68f, 0.72f, 1f)
    val headBlack = floatArrayOf(0.12f, 0.14f, 0.16f, 1f)
    val screenCyan = floatArrayOf(0.20f, 0.85f, 0.95f, 1f)

    builder.addCylinderZ(x, y, 0.85f, 5.8f, 0.45f, 0.45f, 6, poleSteel)
    builder.addBox(x - 0.7f, y - 0.7f, 5.8f, x + 0.7f, y + 0.7f, 8.2f, headBlack)
    builder.addBox(x - 0.45f, y + 0.65f, 6.8f, x + 0.45f, y + 0.75f, 7.8f, screenCyan)
  }

  fun addMailbox(builder: MeshBuilder, x: Float, y: Float) {
    val uspsBlue = floatArrayOf(0.08f, 0.35f, 0.75f, 1f)
    val slotWhite = floatArrayOf(0.92f, 0.92f, 0.94f, 1f)

    builder.addBox(x - 1.6f, y - 1.6f, 0.85f, x + 1.6f, y + 1.6f, 6.2f, uspsBlue)
    builder.addCylinderZ(x, y, 6.2f, 7.4f, 1.6f, 0.4f, 6, uspsBlue)
    builder.addBox(x - 1.1f, y + 1.55f, 5.2f, x + 1.1f, y + 1.65f, 5.8f, slotWhite)
  }

  /**
   * 3D Highway Safety Traffic Cones & Reflective Bollards
   */
  fun addConstructionBollards(solidBuilder: MeshBuilder, emissiveBuilder: MeshBuilder, x: Float, y: Float) {
    val coneOrange = floatArrayOf(0.98f, 0.45f, 0.05f, 1f)
    val stripeWhite = floatArrayOf(0.98f, 0.98f, 0.98f, 1f)
    val baseBlack = floatArrayOf(0.12f, 0.12f, 0.14f, 1f)

    for (i in 0 until 3) {
      val cx = x + i * 8.5f
      val cy = y
      // Heavy rubber square base
      solidBuilder.addBox(cx - 1.4f, cy - 1.4f, 0.85f, cx + 1.4f, cy + 1.4f, 1.25f, baseBlack)
      // Tapered orange cone
      solidBuilder.addCylinderZ(cx, cy, 1.25f, 4.8f, 1.1f, 0.3f, 6, coneOrange)
      // Reflective white center collar band
      emissiveBuilder.addCylinderZ(cx, cy, 2.6f, 3.4f, 0.82f, 0.62f, 6, stripeWhite)
    }
  }

  /**
   * 3D Los Santos Billboards (ECola, Sprunk, Maze Bank):
   * Massive dual steel pillar foundation with illuminated vibrant commercial advertisement panels.
   */
  fun addBillboard(
    solidBuilder: MeshBuilder,
    emissiveBuilder: MeshBuilder,
    x: Float, y: Float, z: Float = 0f,
    isVert: Boolean, brandType: Int
  ) {
    val steelPillar = floatArrayOf(0.24f, 0.26f, 0.30f, 1f)
    val frameMetal = floatArrayOf(0.18f, 0.20f, 0.22f, 1f)

    // Vibrant Brand Colors
    val (brandBg, brandAccent) = when (brandType % 3) {
      0 -> Pair(floatArrayOf(0.85f, 0.10f, 0.12f, 1f), floatArrayOf(1.0f, 0.95f, 0.90f, 1f)) // ECola Red
      1 -> Pair(floatArrayOf(0.10f, 0.75f, 0.22f, 1f), floatArrayOf(0.98f, 0.92f, 0.15f, 1f)) // Sprunk Green & Lemon
      else -> Pair(floatArrayOf(0.82f, 0.15f, 0.18f, 1f), floatArrayOf(0.98f, 0.85f, 0.20f, 1f)) // Maze Bank Red & Gold
    }

    val poleH = 34f
    val boardW = 28f
    val boardH = 14f

    if (isVert) {
      // Dual cylindrical support columns
      solidBuilder.addCylinderZ(x, y - 8f, z, z + poleH, 1.4f, 1.4f, 6, steelPillar)
      solidBuilder.addCylinderZ(x, y + 8f, z, z + poleH, 1.4f, 1.4f, 6, steelPillar)
      // Frame
      solidBuilder.addBox(x - 1.2f, y - boardW / 2f, z + poleH - 2f, x + 1.2f, y + boardW / 2f, z + poleH + boardH, frameMetal)
      // Emissive Billboard Artwork Panel (Front face)
      emissiveBuilder.addBox(x + 1.2f, y - boardW / 2f + 1f, z + poleH, x + 1.5f, y + boardW / 2f - 1f, z + poleH + boardH - 2f, brandBg)
      // Logo stripe/text graphic
      emissiveBuilder.addBox(x + 1.52f, y - boardW / 2f + 4f, z + poleH + 4f, x + 1.55f, y + boardW / 2f - 4f, z + poleH + 8f, brandAccent)
    } else {
      solidBuilder.addCylinderZ(x - 8f, y, z, z + poleH, 1.4f, 1.4f, 6, steelPillar)
      solidBuilder.addCylinderZ(x + 8f, y, z, z + poleH, 1.4f, 1.4f, 6, steelPillar)
      solidBuilder.addBox(x - boardW / 2f, y - 1.2f, z + poleH - 2f, x + boardW / 2f, y + 1.2f, z + poleH + boardH, frameMetal)
      emissiveBuilder.addBox(x - boardW / 2f + 1f, y + 1.2f, z + poleH, x + boardW / 2f - 1f, y + 1.5f, z + poleH + boardH - 2f, brandBg)
      emissiveBuilder.addBox(x - boardW / 2f + 4f, y + 1.52f, z + poleH + 4f, x + boardW / 2f - 4f, y + 1.55f, z + poleH + 8f, brandAccent)
    }
  }

  /**
   * 3D Ammu-Nation Gun Store & Shooting Range:
   * Red-white-blue patriotic exterior, corrugated metal roof, security bars,
   * and glowing emissive 3D target crosshair emblem & signage.
   */
  fun addAmmuNationStore(
    solidBuilder: MeshBuilder,
    emissiveBuilder: MeshBuilder,
    minX: Float, minY: Float, maxX: Float, maxY: Float, height: Float
  ) {
    val wallBeige = floatArrayOf(0.85f, 0.80f, 0.72f, 1f)
    val trimNavy = floatArrayOf(0.10f, 0.18f, 0.35f, 1f)
    val roofCorrugated = floatArrayOf(0.35f, 0.38f, 0.42f, 1f)
    val signRed = floatArrayOf(0.92f, 0.12f, 0.15f, 1f)
    val signWhite = floatArrayOf(0.98f, 0.98f, 0.98f, 1f)

    val cx = (minX + maxX) * 0.5f

    // Main reinforced retail building
    solidBuilder.addBox(minX, minY, 0f, maxX, maxY, height, wallBeige, roofCorrugated)
    // Navy blue trim base along bottom
    solidBuilder.addBox(minX - 0.5f, minY - 0.5f, 0f, maxX + 0.5f, maxY + 0.5f, 4.5f, trimNavy)

    // Entrance security doors & protective steel grill
    solidBuilder.addBox(cx - 10f, minY - 1.2f, 0f, cx + 10f, minY, 11f, floatArrayOf(0.12f, 0.14f, 0.16f, 1f))

    // Glowing 3D "AMMU-NATION" Marquee Signboard
    solidBuilder.addBox(cx - 24f, minY - 3.5f, height - 7.5f, cx + 24f, minY - 0.5f, height + 1.5f, trimNavy)
    emissiveBuilder.addBox(cx - 22f, minY - 3.8f, height - 6.5f, cx + 22f, minY - 3.3f, height + 0.5f, signRed)
    // Crosshair target logo in white
    emissiveBuilder.addBox(cx - 4f, minY - 4.0f, height - 5.5f, cx + 4f, minY - 3.6f, height - 0.5f, signWhite)
  }

  /**
   * 3D Pay 'n' Spray Custom Automotive Repair Shop:
   * Industrial masonry garage with dual roll-up corrugated steel bay doors,
   * high-visibility yellow & black safety hazard stripes, and spray gun signage.
   */
  fun addPaynSprayGarage(
    solidBuilder: MeshBuilder,
    emissiveBuilder: MeshBuilder,
    minX: Float, minY: Float, maxX: Float, maxY: Float, height: Float
  ) {
    val wallStucco = floatArrayOf(0.78f, 0.74f, 0.68f, 1f)
    val doorRoller = floatArrayOf(0.55f, 0.58f, 0.62f, 1f)
    val roofAsphalt = floatArrayOf(0.22f, 0.24f, 0.26f, 1f)
    val hazardYellow = floatArrayOf(0.96f, 0.82f, 0.10f, 1f)
    val hazardBlack = floatArrayOf(0.10f, 0.12f, 0.14f, 1f)
    val sprayBlue = floatArrayOf(0.12f, 0.55f, 0.92f, 1f)

    solidBuilder.addBox(minX, minY, 0f, maxX, maxY, height, wallStucco, roofAsphalt)

    // Bay 1: Roll-up Corrugated Shutter Door (Half Open)
    solidBuilder.addBox(minX + 8f, minY - 0.8f, 0f, minX + 28f, minY, 14f, doorRoller)
    // Bay 2: Roll-up Corrugated Shutter Door (Closed)
    solidBuilder.addBox(maxX - 28f, minY - 0.8f, 0f, maxX - 8f, minY, 18f, doorRoller)

    // Yellow & Black Diagonal Hazard Caution Stripes above bays
    solidBuilder.addBox(minX + 6f, minY - 1.2f, 18f, maxX - 6f, minY - 0.2f, 21f, hazardYellow)
    for (i in 0 until 8) {
      val bx = minX + 8f + i * 6.5f
      solidBuilder.addBox(bx, minY - 1.3f, 18f, bx + 3.2f, minY - 0.1f, 21f, hazardBlack)
    }

    // Glowing "PAY 'N' SPRAY" Neon Sign
    emissiveBuilder.addBox(minX + 16f, minY - 2.5f, height - 6.5f, maxX - 16f, minY - 1.5f, height + 1.0f, sprayBlue)
  }
}


