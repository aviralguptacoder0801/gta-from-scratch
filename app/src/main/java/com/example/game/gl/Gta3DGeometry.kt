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
    roofColor: FloatArray
  ): MeshData {
    val positions = FloatArray(36 * 3)
    val normals = FloatArray(36 * 3)
    val colors = FloatArray(36 * 4)

    var pIdx = 0
    var nIdx = 0
    var cIdx = 0

    fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, col: FloatArray) {
      positions[pIdx++] = x
      positions[pIdx++] = y
      positions[pIdx++] = z

      normals[nIdx++] = nx
      normals[nIdx++] = ny
      normals[nIdx++] = nz

      colors[cIdx++] = col[0]
      colors[cIdx++] = col[1]
      colors[cIdx++] = col[2]
      colors[cIdx++] = col[3]
    }

    fun addQuad(
      x1: Float, y1: Float, z1: Float,
      x2: Float, y2: Float, z2: Float,
      x3: Float, y3: Float, z3: Float,
      x4: Float, y4: Float, z4: Float,
      nx: Float, ny: Float, nz: Float,
      col: FloatArray
    ) {
      addVertex(x1, y1, z1, nx, ny, nz, col)
      addVertex(x2, y2, z2, nx, ny, nz, col)
      addVertex(x3, y3, z3, nx, ny, nz, col)

      addVertex(x1, y1, z1, nx, ny, nz, col)
      addVertex(x3, y3, z3, nx, ny, nz, col)
      addVertex(x4, y4, z4, nx, ny, nz, col)
    }

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

    return MeshData(
      vertexBuffer = createFloatBuffer(positions),
      normalBuffer = createFloatBuffer(normals),
      colorBuffer = createFloatBuffer(colors),
      vertexCount = 36
    )
  }

  fun buildGroundQuad(
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    x3: Float, y3: Float,
    x4: Float, y4: Float,
    z: Float,
    color: FloatArray
  ): MeshData {
    val positions = floatArrayOf(
      x1, y1, z,  x2, y2, z,  x3, y3, z,
      x1, y1, z,  x3, y3, z,  x4, y4, z
    )
    val normals = floatArrayOf(
      0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
      0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f
    )
    val colors = FloatArray(24).apply {
      for (i in 0 until 6) {
        this[i * 4 + 0] = color[0]
        this[i * 4 + 1] = color[1]
        this[i * 4 + 2] = color[2]
        this[i * 4 + 3] = color[3]
      }
    }

    return MeshData(
      vertexBuffer = createFloatBuffer(positions),
      normalBuffer = createFloatBuffer(normals),
      colorBuffer = createFloatBuffer(colors),
      vertexCount = 6
    )
  }

  /**
   * Generates a 3D Palm Tree (trunk + fronds) for tropical California scenery.
   */
  fun buildPalmTreeMesh(x: Float, y: Float, height: Float = 42f): MeshData {
    val positions = ArrayList<Float>()
    val normals = ArrayList<Float>()
    val colors = ArrayList<Float>()

    val trunkCol = floatArrayOf(0.48f, 0.35f, 0.22f, 1f)
    val frondCol = floatArrayOf(0.12f, 0.65f, 0.24f, 1f)
    val frondDark = floatArrayOf(0.08f, 0.45f, 0.16f, 1f)

    // Trunk Box
    val trunkBox = buildBoxMesh(x - 2f, y - 2f, 0f, x + 2f, y + 2f, height, trunkCol, trunkCol)
    for (i in 0 until trunkBox.vertexCount) {
      positions.add(trunkBox.vertexBuffer.get(i * 3 + 0))
      positions.add(trunkBox.vertexBuffer.get(i * 3 + 1))
      positions.add(trunkBox.vertexBuffer.get(i * 3 + 2))
      normals.add(trunkBox.normalBuffer.get(i * 3 + 0))
      normals.add(trunkBox.normalBuffer.get(i * 3 + 1))
      normals.add(trunkBox.normalBuffer.get(i * 3 + 2))
      colors.add(trunkBox.colorBuffer.get(i * 4 + 0))
      colors.add(trunkBox.colorBuffer.get(i * 4 + 1))
      colors.add(trunkBox.colorBuffer.get(i * 4 + 2))
      colors.add(trunkBox.colorBuffer.get(i * 4 + 3))
    }

    // 6 Radiating Palm Fronds
    for (f in 0 until 6) {
      val angle = (f * 60f) * PI.toFloat() / 180f
      val reach = 22f
      val ex = x + cos(angle) * reach
      val ey = y + sin(angle) * reach
      val ez = height - 4f

      val frondBox = buildBoxMesh(
        minOf(x, ex) - 1.5f, minOf(y, ey) - 1.5f, ez - 1f,
        maxOf(x, ex) + 1.5f, maxOf(y, ey) + 1.5f, height + 3f,
        if (f % 2 == 0) frondCol else frondDark,
        frondCol
      )
      for (i in 0 until frondBox.vertexCount) {
        positions.add(frondBox.vertexBuffer.get(i * 3 + 0))
        positions.add(frondBox.vertexBuffer.get(i * 3 + 1))
        positions.add(frondBox.vertexBuffer.get(i * 3 + 2))
        normals.add(frondBox.normalBuffer.get(i * 3 + 0))
        normals.add(frondBox.normalBuffer.get(i * 3 + 1))
        normals.add(frondBox.normalBuffer.get(i * 3 + 2))
        colors.add(frondBox.colorBuffer.get(i * 4 + 0))
        colors.add(frondBox.colorBuffer.get(i * 4 + 1))
        colors.add(frondBox.colorBuffer.get(i * 4 + 2))
        colors.add(frondBox.colorBuffer.get(i * 4 + 3))
      }
    }

    val pArr = positions.toFloatArray()
    val nArr = normals.toFloatArray()
    val cArr = colors.toFloatArray()

    return MeshData(
      vertexBuffer = createFloatBuffer(pArr),
      normalBuffer = createFloatBuffer(nArr),
      colorBuffer = createFloatBuffer(cArr),
      vertexCount = pArr.size / 3
    )
  }

  fun buildFirstPersonWeaponMesh(weaponIndex: Int): MeshData {
    val positions = ArrayList<Float>()
    val normals = ArrayList<Float>()
    val colors = ArrayList<Float>()

    fun appendBox(
      x1: Float, y1: Float, z1: Float,
      x2: Float, y2: Float, z2: Float,
      col: FloatArray
    ) {
      val box = buildBoxMesh(x1, y1, z1, x2, y2, z2, col, col)
      val pBuf = box.vertexBuffer
      val nBuf = box.normalBuffer
      val cBuf = box.colorBuffer

      for (i in 0 until box.vertexCount) {
        positions.add(pBuf.get(i * 3 + 0))
        positions.add(pBuf.get(i * 3 + 1))
        positions.add(pBuf.get(i * 3 + 2))
        normals.add(nBuf.get(i * 3 + 0))
        normals.add(nBuf.get(i * 3 + 1))
        normals.add(nBuf.get(i * 3 + 2))
        colors.add(cBuf.get(i * 4 + 0))
        colors.add(cBuf.get(i * 4 + 1))
        colors.add(cBuf.get(i * 4 + 2))
        colors.add(cBuf.get(i * 4 + 3))
      }
    }

    val gunMetal = floatArrayOf(0.14f, 0.16f, 0.20f, 1f)
    val darkSteel = floatArrayOf(0.24f, 0.28f, 0.34f, 1f)
    val goldAccent = floatArrayOf(0.95f, 0.78f, 0.20f, 1f)
    val armSkin = floatArrayOf(0.86f, 0.64f, 0.50f, 1f)
    val clothSleeve = floatArrayOf(0.15f, 0.20f, 0.18f, 1f)

    // Forearm & Hand holding weapon
    appendBox(0.20f, 0.35f, -0.34f, 0.34f, 0.62f, -0.22f, clothSleeve)
    appendBox(0.18f, 0.58f, -0.26f, 0.28f, 0.74f, -0.14f, armSkin)

    when (weaponIndex) {
      // PISTOL (.45 Combat Pistol)
      1 -> {
        // Metallic Slide
        appendBox(0.13f, 0.60f, -0.12f, 0.21f, 0.98f, -0.04f, darkSteel)
        // Gold accents along barrel
        appendBox(0.14f, 0.65f, -0.04f, 0.20f, 0.95f, -0.02f, goldAccent)
        // Polymer Grip
        appendBox(0.15f, 0.62f, -0.26f, 0.20f, 0.72f, -0.12f, gunMetal)
      }
      // SMG (Micro SMG)
      2 -> {
        appendBox(0.11f, 0.52f, -0.14f, 0.23f, 1.08f, -0.02f, darkSteel)
        appendBox(0.14f, 0.64f, -0.32f, 0.19f, 0.76f, -0.14f, gunMetal)
        appendBox(0.15f, 0.88f, -0.30f, 0.18f, 0.96f, -0.14f, goldAccent)
      }
      // RPG-7 Rocket Launcher
      5 -> {
        // Heavy tube
        appendBox(0.08f, 0.38f, -0.10f, 0.24f, 1.38f, 0.08f, floatArrayOf(0.18f, 0.26f, 0.16f, 1f))
        // Conical Rocket Tip
        appendBox(0.05f, 1.38f, -0.13f, 0.27f, 1.62f, 0.11f, floatArrayOf(0.92f, 0.22f, 0.14f, 1f))
      }
      // CARBINE ASSAULT RIFLE
      else -> {
        appendBox(0.11f, 0.48f, -0.16f, 0.23f, 1.30f, -0.02f, darkSteel)
        appendBox(0.14f, 0.76f, -0.36f, 0.19f, 0.92f, -0.16f, gunMetal)
        appendBox(0.10f, 0.92f, -0.15f, 0.24f, 1.20f, -0.03f, floatArrayOf(0.12f, 0.12f, 0.12f, 1f))
        appendBox(0.13f, 0.54f, -0.02f, 0.21f, 0.85f, 0.04f, goldAccent)
      }
    }

    val pArr = positions.toFloatArray()
    val nArr = normals.toFloatArray()
    val cArr = colors.toFloatArray()

    return MeshData(
      vertexBuffer = createFloatBuffer(pArr),
      normalBuffer = createFloatBuffer(nArr),
      colorBuffer = createFloatBuffer(cArr),
      vertexCount = pArr.size / 3
    )
  }

  fun buildVehicleCockpitMesh(): MeshData {
    val positions = ArrayList<Float>()
    val normals = ArrayList<Float>()
    val colors = ArrayList<Float>()

    fun appendBox(
      x1: Float, y1: Float, z1: Float,
      x2: Float, y2: Float, z2: Float,
      col: FloatArray
    ) {
      val box = buildBoxMesh(x1, y1, z1, x2, y2, z2, col, col)
      val pBuf = box.vertexBuffer
      val nBuf = box.normalBuffer
      val cBuf = box.colorBuffer

      for (i in 0 until box.vertexCount) {
        positions.add(pBuf.get(i * 3 + 0))
        positions.add(pBuf.get(i * 3 + 1))
        positions.add(pBuf.get(i * 3 + 2))
        normals.add(nBuf.get(i * 3 + 0))
        normals.add(nBuf.get(i * 3 + 1))
        normals.add(nBuf.get(i * 3 + 2))
        colors.add(cBuf.get(i * 4 + 0))
        colors.add(cBuf.get(i * 4 + 1))
        colors.add(cBuf.get(i * 4 + 2))
        colors.add(cBuf.get(i * 4 + 3))
      }
    }

    val dashColor = floatArrayOf(0.12f, 0.14f, 0.18f, 1f)
    val leatherColor = floatArrayOf(0.08f, 0.09f, 0.12f, 1f)
    val dialCyan = floatArrayOf(0.22f, 0.88f, 0.98f, 1f)
    val carHood = floatArrayOf(0.98f, 0.72f, 0.08f, 1f) // Zentorno Yellow

    // Dashboard Shelf
    appendBox(-0.75f, 0.60f, -0.45f, 0.75f, 1.10f, -0.22f, dashColor)
    // Digital Speedometer Instrument Cluster
    appendBox(-0.35f, 0.82f, -0.20f, -0.05f, 0.96f, -0.10f, dialCyan)
    // Steering Column & Wheel Rim
    appendBox(-0.25f, 0.45f, -0.30f, -0.15f, 0.65f, -0.15f, leatherColor)
    appendBox(-0.38f, 0.42f, -0.15f, -0.02f, 0.46f, 0.12f, leatherColor)

    // Windshield Pillars
    appendBox(-0.78f, 0.70f, -0.30f, -0.68f, 1.15f, 0.65f, leatherColor)
    appendBox(0.68f, 0.70f, -0.30f, 0.78f, 1.15f, 0.65f, leatherColor)
    appendBox(-0.75f, 0.65f, 0.55f, 0.75f, 0.85f, 0.68f, leatherColor)

    // Aerodynamic Supercar Hood extending outside windshield
    appendBox(-0.72f, 1.10f, -0.40f, 0.72f, 2.90f, -0.26f, carHood)

    val pArr = positions.toFloatArray()
    val nArr = normals.toFloatArray()
    val cArr = colors.toFloatArray()

    return MeshData(
      vertexBuffer = createFloatBuffer(pArr),
      normalBuffer = createFloatBuffer(nArr),
      colorBuffer = createFloatBuffer(cArr),
      vertexCount = pArr.size / 3
    )
  }
}
