package com.example.game.engine

import androidx.compose.ui.geometry.Rect
import com.example.game.audio.SoundManager
import com.example.game.model.BulletEntity
import com.example.game.model.CharacterType
import com.example.game.model.Mission
import com.example.game.model.ParticleEntity
import com.example.game.model.ParticleType
import com.example.game.model.PedestrianEntity
import com.example.game.model.VehicleEntity
import com.example.game.model.VehicleModel
import com.example.game.model.Weapon
import com.example.game.model.WeaponCategory
import com.example.game.world.LosSantosWorld
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(val soundManager: SoundManager) {
  enum class TimeOfDay(
    val label: String,
    val skyR: Float, val skyG: Float, val skyB: Float,
    val sunR: Float, val sunG: Float, val sunB: Float,
    val fogR: Float, val fogG: Float, val fogB: Float,
    val isNight: Boolean
  ) {
    NOON("☀️ NOON", 0.38f, 0.65f, 0.95f, 1.0f, 0.96f, 0.86f, 0.70f, 0.82f, 0.94f, false),
    SUNSET("🌅 SUNSET", 0.92f, 0.44f, 0.18f, 1.0f, 0.62f, 0.22f, 0.88f, 0.42f, 0.28f, false),
    NIGHT("🌙 NIGHT", 0.05f, 0.07f, 0.16f, 0.35f, 0.45f, 0.65f, 0.06f, 0.08f, 0.18f, true)
  }
  var currentTimeOfDay = TimeOfDay.NOON

  fun cycleTimeOfDay() {
    currentTimeOfDay = when (currentTimeOfDay) {
      TimeOfDay.NOON -> TimeOfDay.SUNSET
      TimeOfDay.SUNSET -> TimeOfDay.NIGHT
      TimeOfDay.NIGHT -> TimeOfDay.NOON
    }
    soundManager.vibrate(30, 80)
    triggerStateUpdate()
  }

  // Player state: Spawn right in Downtown Los Santos in front of the colossal Maze Bank Tower & Legion Square
  var playerX = 2100f
  var playerY = 1950f
  var playerAngle = 90f
  var playerYaw = 90f // Looking North toward Maze Bank Skyscraper & Vinewood Hills
  var playerPitch = 6f
  var playerZ = 18f
  var walkBobPhase = 0f
  var isFirstPerson = false // Third-person cinematic view by default so character and supercars are immediately visible
  var isAimingDownSights = false
  var playerHealth = 100f
  var playerArmor = 100f
  var playerCash = 250000L
  var playerAbility = 100f
  var isAbilityActive = false

  var currentCharacter = CharacterType.FRANKLIN
  var currentDistrict = "DOWNTOWN LOS SANTOS"

  // Weapons
  val inventory = mutableListOf(
    Weapon(WeaponCategory.FISTS, -1, -1),
    Weapon(WeaponCategory.PISTOL, 15, 120),
    Weapon(WeaponCategory.SMG, 30, 240),
    Weapon(WeaponCategory.SHOTGUN, 8, 48),
    Weapon(WeaponCategory.CARBINE, 30, 180),
    Weapon(WeaponCategory.RPG, 1, 8),
    Weapon(WeaponCategory.SNIPER, 5, 25)
  )
  var currentWeaponIndex = 1

  // Current Vehicle
  var currentVehicle: VehicleEntity? = null

  // Police & Wanted System
  var wantedStars = 0
  var evasionProgress = 0f // 0f to 1f
  var isEvading = false
  private var crimeHeat = 0f
  private var lastPoliceSpawnTime = 0L

  // Active Mission
  var activeMission: Mission? = null
  val availableMissions = mutableListOf<Mission>()

  // Entities
  val vehicles = mutableListOf<VehicleEntity>()
  val pedestrians = mutableListOf<PedestrianEntity>()
  val bullets = mutableListOf<BulletEntity>()
  val particles = mutableListOf<ParticleEntity>()

  // Input states
  var joystickX = 0f
  var joystickY = 0f
  var smoothJoystickX = 0f
  var smoothJoystickY = 0f
  var invertJoystickY = false
  var invertJoystickX = false
  var isPedalGas = false
  var isPedalBrake = false
  var isHandbrake = false
  var isAimingShooting = false
  var aimAngle = 0f

  // Cheats / perks
  var isGodMode = false
  var hasExplosiveAmmo = false

  fun toggleJoystickInvert() {
    invertJoystickY = !invertJoystickY
    soundManager.vibrate(40, 100)
    triggerStateUpdate()
  }

  // UI state flow for recomposition triggers
  data class EngineState(
    val playerX: Float,
    val playerY: Float,
    val playerHealth: Float,
    val playerArmor: Float,
    val playerCash: Long,
    val playerAbility: Float,
    val isAbilityActive: Boolean,
    val wantedStars: Int,
    val isEvading: Boolean,
    val evasionProgress: Float,
    val currentDistrict: String,
    val currentVehicleSpeed: Float,
    val isInVehicle: Boolean,
    val currentWeapon: Weapon,
    val activeMission: Mission?,
    val character: CharacterType,
    val isFirstPerson: Boolean,
    val invertJoystickY: Boolean = false,
    val timeOfDay: TimeOfDay = TimeOfDay.NOON
  )

  fun togglePOV() {
    isFirstPerson = !isFirstPerson
    soundManager.vibrate(35, 100)
    triggerStateUpdate()
  }

  fun spawnZentorno() {
    val rad = playerYaw * PI.toFloat() / 180f
    val car = VehicleEntity(
      id = nextEntityId++,
      model = VehicleModel.ZENTORNO,
      x = playerX + cos(rad) * 26f + sin(rad) * 16f,
      y = playerY + sin(rad) * 26f - cos(rad) * 16f,
      angleDeg = playerYaw,
      color = 0xFFF59E0B
    )
    vehicles.add(car)
    soundManager.playHorn()
    soundManager.vibrate(50, 150)
    triggerStateUpdate()
  }

  fun spawnTank() {
    val rad = playerYaw * PI.toFloat() / 180f
    val tank = VehicleEntity(
      id = nextEntityId++,
      model = VehicleModel.RHINO_TANK,
      x = playerX + cos(rad) * 44f,
      y = playerY + sin(rad) * 44f,
      angleDeg = playerYaw,
      isTank = true,
      color = 0xFF365314
    )
    vehicles.add(tank)
    soundManager.playExplosion()
    soundManager.vibrate(100, 250)
    triggerStateUpdate()
  }

  fun triggerMaxWanted() {
    wantedStars = if (wantedStars == 5) 0 else 5
    if (wantedStars > 0) {
      soundManager.setPoliceSirenActive(true)
      soundManager.playTireScreech()
    } else {
      soundManager.setPoliceSirenActive(false)
    }
    soundManager.vibrate(80, 200)
    triggerStateUpdate()
  }

  var lastStateEmitTime: Long = 0L

  fun triggerStateUpdate() {
    _stateFlow.value = buildState()
  }

  private val _stateFlow = MutableStateFlow(buildState())
  val stateFlow = _stateFlow.asStateFlow()

  private var nextEntityId = 1L
  private var lastShotTime = 0L
  private val random = Random.Default

  init {
    initWorldEntities()
    initMissions()
  }

  private fun initWorldEntities() {
    // Spawn initial civilian vehicles along roads
    val carModels = listOf(
      VehicleModel.ZENTORNO, VehicleModel.BUFFALO, VehicleModel.SABRE,
      VehicleModel.BODHI, VehicleModel.POLICE_CRUISER
    )

    for (road in LosSantosWorld.roads.take(12)) {
      val midX = (road.startX + road.endX) / 2f
      val midY = (road.startY + road.endY) / 2f
      val angle = if (road.startX == road.endX) 90f else 0f
      val model = carModels.random()

      vehicles.add(
        VehicleEntity(
          id = nextEntityId++,
          model = model,
          x = midX + random.nextInt(-80, 80),
          y = midY + random.nextInt(-80, 80),
          angleDeg = angle,
          speed = random.nextFloat() * 3f + 1f,
          isPolice = model == VehicleModel.POLICE_CRUISER
        )
      )
    }

    // Spawn player's starting golden Zentorno supercar right beside player in Downtown Los Santos
    vehicles.add(
      VehicleEntity(
        id = nextEntityId++,
        model = VehicleModel.ZENTORNO,
        x = 2125f,
        y = 1960f,
        angleDeg = 90f,
        color = 0xFFF59E0B
      )
    )

    // Spawn pedestrians around sidewalks
    for (i in 0 until 24) {
      val px = random.nextFloat() * 2000f + 1000f
      val py = random.nextFloat() * 2000f + 1200f
      pedestrians.add(
        PedestrianEntity(
          id = nextEntityId++,
          x = px,
          y = py,
          angleDeg = random.nextFloat() * 360f,
          speed = random.nextFloat() * 0.8f + 0.6f
        )
      )
    }
  }

  private fun initMissions() {
    availableMissions.clear()
    availableMissions.add(
      Mission(
        id = "M1_REPO",
        title = "Repo Run: Vinewood Gold",
        employer = "Simeon Yetarian",
        description = "Repossess the yellow Zentorno parked at Vinewood Luxury Villa and deliver it to Simeon's dealership.",
        rewardCash = 35000,
        targetDistrict = "VINEWOOD HILLS",
        targetX = 2600f,
        targetY = 340f,
        isActive = true
      )
    )
    availableMissions.add(
      Mission(
        id = "M2_TURF",
        title = "Turf Lockdown",
        employer = "Lamar Davis",
        description = "Take back the Grove Street contraband stash from rival street crews in South Los Santos.",
        rewardCash = 50000,
        targetDistrict = "SOUTH LOS SANTOS",
        targetX = 1820f,
        targetY = 3160f
      )
    )
    availableMissions.add(
      Mission(
        id = "M3_PALETO",
        title = "The Paleto Score",
        employer = "Michael & Trevor",
        description = "Breach the fortified vault at Maze Bank Tower, grab the loot bags, and evade 4-star SWAT forces.",
        rewardCash = 250000,
        targetDistrict = "DOWNTOWN LOS SANTOS",
        targetX = 2100f,
        targetY = 1750f
      )
    )
    availableMissions.add(
      Mission(
        id = "M4_UNION",
        title = "The Big One: Union Depository",
        employer = "Lester Crest",
        description = "The ultimate heist. Seize $1,000,000 in bullion and execute a high-speed getaway to Sandy Shores.",
        rewardCash = 1000000,
        targetDistrict = "DOWNTOWN LOS SANTOS",
        targetX = 2290f,
        targetY = 1360f
      )
    )

    activeMission = availableMissions.firstOrNull()
  }

  fun switchCharacter(character: CharacterType) {
    if (currentCharacter == character) return
    currentCharacter = character
    soundManager.playCashChime()
    soundManager.vibrate(80, 200)

    // Teleport/spawn to character home if requested or switch immediately
    when (character) {
      CharacterType.FRANKLIN -> {
        // Buff driving
      }
      CharacterType.MICHAEL -> {
        // Reset weapon to pistol/carbine
        currentWeaponIndex = 4.coerceAtMost(inventory.size - 1)
      }
      CharacterType.TREVOR -> {
        // Give shotgun / RPG
        currentWeaponIndex = 5.coerceAtMost(inventory.size - 1)
      }
    }
  }

  fun toggleSpecialAbility() {
    isAbilityActive = !isAbilityActive
    if (isAbilityActive && playerAbility < 15f) {
      isAbilityActive = false
    }
    if (isAbilityActive) {
      soundManager.vibrate(100, 220)
    }
  }

  fun enterOrExitVehicle() {
    val car = currentVehicle
    if (car != null) {
      // Exit vehicle
      playerX = car.x + cos((car.angleDeg + 90) * PI.toFloat() / 180f) * 45f
      playerY = car.y + sin((car.angleDeg + 90) * PI.toFloat() / 180f) * 45f
      car.speed = 0f
      currentVehicle = null
      soundManager.stopRadio()
      soundManager.vibrate(50, 120)
    } else {
      // Find nearest vehicle within 95 units
      val nearest = vehicles.filter { !it.isDestroyed }.minByOrNull {
        hypot(it.x - playerX, it.y - playerY)
      }
      if (nearest != null && hypot(nearest.x - playerX, nearest.y - playerY) < 110f) {
        currentVehicle = nearest
        playerX = nearest.x
        playerY = nearest.y
        soundManager.vibrate(70, 180)
        soundManager.startRadio()

        if (nearest.isPolice && wantedStars == 0) {
          addWantedStars(1)
        }
      }
    }
  }

  fun shootWeapon() {
    val now = System.currentTimeMillis()
    val weapon = inventory[currentWeaponIndex]
    val cat = weapon.type

    if (cat == WeaponCategory.FISTS) {
      // Melee punch
      if (now - lastShotTime < cat.fireRateMs) return
      lastShotTime = now
      soundManager.vibrate(40, 150)
      meleeAttack()
      return
    }

    if (weapon.currentClip <= 0) {
      reloadWeapon()
      return
    }

    if (now - lastShotTime < cat.fireRateMs) return
    lastShotTime = now

    weapon.currentClip--
    aimAngle = playerYaw

    // Spawn bullet / rocket
    val rad = aimAngle * PI.toFloat() / 180f
    val spawnX = playerX + cos(rad) * 35f
    val spawnY = playerY + sin(rad) * 35f
    val speed = cat.bulletSpeed

    val isRocket = cat.isExplosive || hasExplosiveAmmo
    bullets.add(
      BulletEntity(
        id = nextEntityId++,
        x = spawnX,
        y = spawnY,
        vx = cos(rad) * speed,
        vy = sin(rad) * speed,
        damage = if (currentCharacter == CharacterType.TREVOR && isAbilityActive) cat.damage * 2.2f else cat.damage,
        isPlayer = true,
        isRocket = isRocket,
        maxDistance = cat.range
      )
    )

    // SFX
    soundManager.playGunshot(cat == WeaponCategory.RPG || cat == WeaponCategory.SHOTGUN || cat == WeaponCategory.SNIPER)

    // Shotgun extra pellets
    if (cat == WeaponCategory.SHOTGUN) {
      for (i in -2..2) {
        if (i == 0) continue
        val spreadRad = (aimAngle + i * 5f) * PI.toFloat() / 180f
        bullets.add(
          BulletEntity(
            id = nextEntityId++,
            x = spawnX,
            y = spawnY,
            vx = cos(spreadRad) * speed,
            vy = sin(spreadRad) * speed,
            damage = cat.damage * 0.4f,
            isPlayer = true,
            isRocket = false,
            maxDistance = cat.range * 0.9f
          )
        )
      }
    }

    // Muzzle flash particle
    particles.add(
      ParticleEntity(
        x = spawnX,
        y = spawnY,
        vx = cos(rad) * 2f,
        vy = sin(rad) * 2f,
        color = 0xFFFDE047,
        life = 0.08f,
        maxLife = 0.08f,
        radius = 12f,
        type = ParticleType.FIRE
      )
    )

    // Firing in public triggers crime heat if near pedestrians/cops
    if (wantedStars == 0 && (pedestrians.any { hypot(it.x - playerX, it.y - playerY) < 300f } || vehicles.any { it.isPolice && hypot(it.x - playerX, it.y - playerY) < 500f })) {
      addWantedStars(1)
    }
  }

  private fun meleeAttack() {
    val reach = 60f
    for (p in pedestrians.filter { it.isAlive }) {
      if (hypot(p.x - playerX, p.y - playerY) < reach) {
        p.health -= 35f
        p.isFleeing = true
        soundManager.playCarCrash()
        spawnBloodSparks(p.x, p.y, ParticleType.BLOOD)
        if (p.health <= 0) {
          p.isAlive = false
          playerCash += p.dropsCash
          soundManager.playCashChime()
          addWantedStars(1)
        }
        break
      }
    }
  }

  fun reloadWeapon() {
    val weapon = inventory[currentWeaponIndex]
    val cat = weapon.type
    if (weapon.reserveAmmo <= 0 || weapon.currentClip >= cat.clipSize) return

    val needed = cat.clipSize - weapon.currentClip
    val take = needed.coerceAtMost(weapon.reserveAmmo)
    weapon.currentClip += take
    weapon.reserveAmmo -= take
    soundManager.vibrate(30, 80)
  }

  fun cycleWeapon() {
    currentWeaponIndex = (currentWeaponIndex + 1) % inventory.size
    soundManager.vibrate(25, 70)
  }

  fun addWantedStars(stars: Int) {
    val old = wantedStars
    wantedStars = (wantedStars + stars).coerceIn(0, 5)
    evasionProgress = 0f
    isEvading = false

    if (wantedStars > old) {
      soundManager.vibrate(180, 240)
      soundManager.setPoliceSirenActive(true)
      spawnPoliceReinforcements()
    } else if (wantedStars == 0) {
      soundManager.setPoliceSirenActive(false)
    }
  }

  fun clearWantedLevel() {
    wantedStars = 0
    evasionProgress = 0f
    isEvading = false
    soundManager.setPoliceSirenActive(false)
  }

  private fun spawnPoliceReinforcements() {
    val count = when (wantedStars) {
      1 -> 1
      2 -> 2
      3 -> 3
      4 -> 4
      5 -> 5
      else -> 0
    }

    for (i in 0 until count) {
      val angle = random.nextFloat() * 360f
      val dist = random.nextFloat() * 350f + 400f
      val sx = (playerX + cos(angle * PI.toFloat() / 180f) * dist).coerceIn(100f, LosSantosWorld.WORLD_WIDTH - 100f)
      val sy = (playerY + sin(angle * PI.toFloat() / 180f) * dist).coerceIn(100f, LosSantosWorld.WORLD_HEIGHT - 100f)

      val model = when {
        wantedStars >= 5 && i == 0 -> VehicleModel.RHINO_TANK
        wantedStars >= 4 && i == 1 -> VehicleModel.HELICOPTER
        wantedStars >= 3 && i % 2 == 0 -> VehicleModel.SWAT_ENFORCER
        else -> VehicleModel.POLICE_CRUISER
      }

      vehicles.add(
        VehicleEntity(
          id = nextEntityId++,
          model = model,
          x = sx,
          y = sy,
          angleDeg = angle + 180f,
          speed = 4f,
          isPolice = true,
          isHelicopter = model == VehicleModel.HELICOPTER,
          isTank = model == VehicleModel.RHINO_TANK,
          sirenOn = true
        )
      )
    }
  }

  fun update(deltaSec: Float) {
    // Time scale for Franklin/Michael abilities
    val timeScale = if (isAbilityActive) {
      when (currentCharacter) {
        CharacterType.FRANKLIN -> if (currentVehicle != null) 0.45f else 1.0f
        CharacterType.MICHAEL -> 0.40f
        CharacterType.TREVOR -> 1.0f
      }
    } else 1.0f

    val dt = deltaSec * timeScale

    // Ability meter depletion/regeneration
    if (isAbilityActive) {
      playerAbility = (playerAbility - deltaSec * 15f).coerceAtLeast(0f)
      if (playerAbility <= 0f) isAbilityActive = false
    } else {
      playerAbility = (playerAbility + deltaSec * 8f).coerceAtMost(100f)
    }

    updatePlayerAndVehicles(dt)
    updateBullets(dt)
    updatePedestrians(dt)
    updatePoliceAI(dt)
    updateParticles(dt)
    checkMissionProgress()

    // Update current district and publish state at throttled interval (reduces Compose recomposition)
    val now = System.currentTimeMillis()
    if (now - lastStateEmitTime >= 65L) {
      lastStateEmitTime = now
      currentDistrict = LosSantosWorld.getDistrictFor(playerX, playerY)
      _stateFlow.value = buildState()
    }
  }

  private fun updatePlayerAndVehicles(dt: Float) {
    // Smooth analog joystick inputs with exponential damping filter
    val rawTargetX = if (invertJoystickX) -joystickX else joystickX
    val rawTargetY = if (invertJoystickY) -joystickY else joystickY
    val smoothFactor = (1f - kotlin.math.exp(-20f * dt)).coerceIn(0.08f, 1f)
    smoothJoystickX += (rawTargetX - smoothJoystickX) * smoothFactor
    smoothJoystickY += (rawTargetY - smoothJoystickY) * smoothFactor

    val forward = smoothJoystickY
    val strafe = smoothJoystickX

    val car = currentVehicle
    if (car != null) {
      // VEHICLE DRIVING PHYSICS
      val model = car.model
      val accel = model.acceleration * (if (currentCharacter == CharacterType.FRANKLIN && isAbilityActive) 1.5f else 1.0f)
      val topSpeed = model.topSpeed * (if (currentCharacter == CharacterType.FRANKLIN && isAbilityActive) 1.25f else 1.0f)
      val turnSpeed = model.handling * (if (isHandbrake) 1.8f else 1.0f)

      // Throttle from gas pedal OR joystick forward (smooth analog acceleration)
      if (isPedalGas || forward > 0.12f) {
        val throttleMult = if (isPedalGas) 1f else forward.coerceIn(0f, 1f)
        car.speed = (car.speed + accel * 60f * dt * throttleMult).coerceAtMost(topSpeed)
      } else if (isPedalBrake || forward < -0.12f) {
        val brakeMult = if (isPedalBrake) 1f else (-forward).coerceIn(0f, 1f)
        car.speed = (car.speed - accel * 75f * dt * brakeMult).coerceAtLeast(-topSpeed * 0.45f)
      } else {
        // Natural coasting & friction deceleration
        car.speed *= (1f - 0.9f * dt).coerceIn(0f, 1f)
      }

      if (isHandbrake && kotlin.math.abs(car.speed) > 3f) {
        car.speed *= (1f - 1.5f * dt).coerceIn(0f, 1f)
        soundManager.playTireScreech()
        spawnSkidMarks(car.x, car.y, car.angleDeg)
      }

      // Smooth steering with joystick X (pushing right turns right, pushing left turns left)
      if (kotlin.math.abs(strafe) > 0.06f) {
        val dir = if (car.speed >= 0) 1f else -1f
        car.angleDeg -= strafe * turnSpeed * 60f * dt * dir
      }

      // Move vehicle
      val rad = car.angleDeg * PI.toFloat() / 180f
      val vx = cos(rad) * car.speed * 40f * dt
      val vy = sin(rad) * car.speed * 40f * dt

      val nextX = (car.x + vx).coerceIn(50f, LosSantosWorld.WORLD_WIDTH - 50f)
      val nextY = (car.y + vy).coerceIn(50f, LosSantosWorld.WORLD_HEIGHT - 50f)

      // Building collisions for vehicle
      val carRect = Rect(nextX - 25f, nextY - 25f, nextX + 25f, nextY + 25f)
      val hitBuilding = LosSantosWorld.buildings.any { it.rect.overlaps(carRect) }

      if (hitBuilding) {
        car.speed = -car.speed * 0.4f
        soundManager.playCarCrash()
        car.health -= 15f
        spawnBloodSparks(nextX, nextY, ParticleType.SPARK)
        if (car.health <= 0f) {
          explode(car.x, car.y)
          car.isDestroyed = true
          enterOrExitVehicle()
        }
      } else {
        car.x = nextX
        car.y = nextY
      }

      playerX = car.x
      playerY = car.y
      playerAngle = car.angleDeg
      playerYaw = car.angleDeg

      // Pay 'n' Spray trigger check
      if (hypot(car.x - 1620f, car.y - 2735f) < 80f) {
        car.health = model.maxHealth
        clearWantedLevel()
        soundManager.playMissionFanfare()
      }

      // Car hitting pedestrians
      if (kotlin.math.abs(car.speed) > 4f) {
        for (p in pedestrians.filter { it.isAlive }) {
          if (hypot(p.x - car.x, p.y - car.y) < 35f) {
            p.health = 0f
            p.isAlive = false
            playerCash += p.dropsCash
            spawnBloodSparks(p.x, p.y, ParticleType.BLOOD)
            soundManager.playCarCrash()
            addWantedStars(1)
          }
        }
      }
    } else {
      // ON FOOT MOVEMENT (First-Person Strafe & Directional Walking)
      val moveSpeed = if (isPedalGas || isHandbrake) 4.2f else 2.4f // sprint
      val inputDist = hypot(strafe, forward)
      if (inputDist > 0.08f) {
        walkBobPhase += dt * moveSpeed * 8f * inputDist
        val radYaw = playerYaw * PI.toFloat() / 180f

        // Natural FPS movement: forward moves along camera look direction, strafe moves perpendicular
        val vx = (cos(radYaw) * forward + sin(radYaw) * strafe) * moveSpeed * 55f * dt
        val vy = (sin(radYaw) * forward - cos(radYaw) * strafe) * moveSpeed * 55f * dt

        val nextX = (playerX + vx).coerceIn(30f, LosSantosWorld.WORLD_WIDTH - 30f)
        val nextY = (playerY + vy).coerceIn(30f, LosSantosWorld.WORLD_HEIGHT - 30f)

        val playerRect = Rect(nextX - 15f, nextY - 15f, nextX + 15f, nextY + 15f)
        val hitBuilding = LosSantosWorld.buildings.any { it.rect.overlaps(playerRect) }
        if (!hitBuilding) {
          playerX = nextX
          playerY = nextY
        }
        playerAngle = playerYaw
      }

      aimAngle = if (hypot(strafe, forward) > 0.15f) playerAngle else aimAngle

      // Ammu-Nation trigger on foot
      if (hypot(playerX - 2000f, playerY - 2735f) < 70f) {
        for (w in inventory) {
          if (w.type.maxAmmo > 0) {
            w.reserveAmmo = w.type.maxAmmo
            w.currentClip = w.type.clipSize
          }
        }
        playerArmor = 100f
      }
    }
  }

  private fun updateBullets(dt: Float) {
    val it = bullets.iterator()
    while (it.hasNext()) {
      val b = it.next()
      b.x += b.vx * 60f * dt
      b.y += b.vy * 60f * dt
      b.traveled += hypot(b.vx, b.vy) * 60f * dt

      // Wall hit check
      val bRect = Rect(b.x - 4f, b.y - 4f, b.x + 4f, b.y + 4f)
      val hitBuilding = LosSantosWorld.buildings.any { it.rect.overlaps(bRect) }

      if (hitBuilding || b.traveled >= b.maxDistance) {
        if (b.isRocket) {
          explode(b.x, b.y)
        } else {
          spawnBloodSparks(b.x, b.y, ParticleType.SPARK)
        }
        it.remove()
        continue
      }

      // Hit pedestrians
      var bulletHit = false
      for (p in pedestrians.filter { it.isAlive }) {
        if (hypot(p.x - b.x, p.y - b.y) < 22f) {
          p.health -= b.damage
          p.isFleeing = true
          spawnBloodSparks(p.x, p.y, ParticleType.BLOOD)
          if (p.health <= 0) {
            p.isAlive = false
            playerCash += p.dropsCash
            soundManager.playCashChime()
            if (p.isPolice) addWantedStars(1)
          }
          bulletHit = true
          break
        }
      }

      // Hit vehicles
      if (!bulletHit) {
        for (v in vehicles.filter { !it.isDestroyed && it != currentVehicle }) {
          if (hypot(v.x - b.x, v.y - b.y) < 38f) {
            v.health -= b.damage
            spawnBloodSparks(b.x, b.y, ParticleType.SPARK)
            if (v.isPolice) addWantedStars(1)
            if (v.health <= 0) {
              v.isDestroyed = true
              explode(v.x, v.y)
            }
            bulletHit = true
            break
          }
        }
      }

      // Enemy bullet hitting player
      if (!bulletHit && !b.isPlayer) {
        if (hypot(playerX - b.x, playerY - b.y) < 25f) {
          damagePlayer(b.damage)
          spawnBloodSparks(playerX, playerY, ParticleType.BLOOD)
          bulletHit = true
        }
      }

      if (bulletHit) {
        if (b.isRocket) explode(b.x, b.y)
        it.remove()
      }
    }
  }

  fun damagePlayer(amount: Float) {
    if (isGodMode) return
    var dmg = amount
    if (currentCharacter == CharacterType.TREVOR && isAbilityActive) {
      dmg *= 0.3f // 70% damage reduction
    }

    soundManager.vibrate(60, 200)

    if (playerArmor > 0) {
      val armorAbsorb = (dmg * 0.7f).coerceAtMost(playerArmor)
      playerArmor -= armorAbsorb
      dmg -= armorAbsorb
    }

    playerHealth = (playerHealth - dmg).coerceAtLeast(0f)
    if (playerHealth <= 0f) {
      // BUSTED / WASTED
      soundManager.playExplosion()
      playerHealth = 100f
      playerArmor = 50f
      playerCash = (playerCash - 2000L).coerceAtLeast(0L)
      clearWantedLevel()
      // Respawn at Hospital in South LS
      playerX = 1520f
      playerY = 2700f
      currentVehicle = null
    }
  }

  fun explode(x: Float, y: Float) {
    soundManager.playExplosion()
    // Shockwave particle
    particles.add(
      ParticleEntity(
        x = x,
        y = y,
        vx = 0f,
        vy = 0f,
        color = 0xFFF97316,
        life = 0.5f,
        maxLife = 0.5f,
        radius = 80f,
        type = ParticleType.EXPLOSION_SHOCKWAVE
      )
    )

    // Fire & smoke bursts
    for (i in 0 until 18) {
      val ang = random.nextFloat() * 360f
      val spd = random.nextFloat() * 4f + 2f
      val rad = ang * PI.toFloat() / 180f
      particles.add(
        ParticleEntity(
          x = x,
          y = y,
          vx = cos(rad) * spd,
          vy = sin(rad) * spd,
          color = if (i % 2 == 0) 0xFFEF4444 else 0xFFF59E0B,
          life = random.nextFloat() * 0.4f + 0.3f,
          maxLife = 0.7f,
          radius = random.nextFloat() * 18f + 12f,
          type = ParticleType.FIRE
        )
      )
    }

    // Damage nearby entities
    val blastRadius = 140f
    for (v in vehicles.filter { !it.isDestroyed }) {
      val d = hypot(v.x - x, v.y - y)
      if (d < blastRadius) {
        v.health -= (1f - d / blastRadius) * 400f
        if (v.health <= 0) v.isDestroyed = true
      }
    }

    for (p in pedestrians.filter { it.isAlive }) {
      val d = hypot(p.x - x, p.y - y)
      if (d < blastRadius) {
        p.health = 0f
        p.isAlive = false
      }
    }

    val distToPlayer = hypot(playerX - x, playerY - y)
    if (distToPlayer < blastRadius) {
      damagePlayer((1f - distToPlayer / blastRadius) * 120f)
    }

    addWantedStars(2)
  }

  private fun updatePedestrians(dt: Float) {
    for (p in pedestrians.filter { it.isAlive }) {
      if (p.isFleeing) {
        val fleeAngle = atan2(p.y - playerY, p.x - playerX)
        p.angleDeg = (fleeAngle * 180f / PI.toFloat())
        p.speed = 3.2f
      }
      val rad = p.angleDeg * PI.toFloat() / 180f
      p.x = (p.x + cos(rad) * p.speed * 40f * dt).coerceIn(50f, LosSantosWorld.WORLD_WIDTH - 50f)
      p.y = (p.y + sin(rad) * p.speed * 40f * dt).coerceIn(50f, LosSantosWorld.WORLD_HEIGHT - 50f)
    }
  }

  private fun updatePoliceAI(dt: Float) {
    if (wantedStars <= 0) {
      isEvading = false
      evasionProgress = 0f
      return
    }

    val policeUnits = vehicles.filter { it.isPolice && !it.isDestroyed }
    var anyCopInSight = false

    for (cop in policeUnits) {
      cop.sirenOn = true
      val dist = hypot(cop.x - playerX, cop.y - playerY)
      if (dist < 650f) {
        anyCopInSight = true
      }

      // Steer toward player
      val targetAngle = (atan2(playerY - cop.y, playerX - cop.x) * 180f / PI.toFloat())
      var diff = targetAngle - cop.angleDeg
      while (diff < -180f) diff += 360f
      while (diff > 180f) diff -= 360f

      cop.angleDeg += diff.coerceIn(-cop.model.handling * 30f * dt, cop.model.handling * 30f * dt)
      cop.speed = (cop.speed + cop.model.acceleration * 50f * dt).coerceAtMost(cop.model.topSpeed)

      val rad = cop.angleDeg * PI.toFloat() / 180f
      cop.x += cos(rad) * cop.speed * 40f * dt
      cop.y += sin(rad) * cop.speed * 40f * dt

      // Cops shoot at 3+ stars
      if (wantedStars >= 3 && dist < 380f && random.nextFloat() < 0.05f) {
        bullets.add(
          BulletEntity(
            id = nextEntityId++,
            x = cop.x,
            y = cop.y,
            vx = cos(rad) * 22f,
            vy = sin(rad) * 22f,
            damage = 15f,
            isPlayer = false,
            isRocket = false,
            maxDistance = 450f
          )
        )
      }
    }

    // Evasion logic
    if (!anyCopInSight) {
      isEvading = true
      evasionProgress = (evasionProgress + dt / 12f).coerceAtMost(1f)
      if (evasionProgress >= 1f) {
        clearWantedLevel()
        soundManager.playMissionFanfare()
      }
    } else {
      isEvading = false
      evasionProgress = 0f
    }
  }

  private fun updateParticles(dt: Float) {
    val it = particles.iterator()
    while (it.hasNext()) {
      val p = it.next()
      p.x += p.vx * 60f * dt
      p.y += p.vy * 60f * dt
      p.life -= dt
      if (p.life <= 0) it.remove()
    }
  }

  private fun spawnBloodSparks(x: Float, y: Float, type: ParticleType) {
    for (i in 0 until 5) {
      val ang = random.nextFloat() * 360f
      val spd = random.nextFloat() * 3f + 1f
      val rad = ang * PI.toFloat() / 180f
      particles.add(
        ParticleEntity(
          x = x,
          y = y,
          vx = cos(rad) * spd,
          vy = sin(rad) * spd,
          color = if (type == ParticleType.BLOOD) 0xFFDC2626 else 0xFFFBBF24,
          life = 0.25f,
          maxLife = 0.25f,
          radius = 5f,
          type = type
        )
      )
    }
  }

  private fun spawnSkidMarks(x: Float, y: Float, angleDeg: Float) {
    particles.add(
      ParticleEntity(
        x = x,
        y = y,
        vx = 0f,
        vy = 0f,
        color = 0xFF1F2937,
        life = 4.0f,
        maxLife = 4.0f,
        radius = 8f,
        type = ParticleType.SKID_MARK
      )
    )
  }

  private fun checkMissionProgress() {
    val mission = activeMission ?: return
    val dist = hypot(playerX - mission.targetX, playerY - mission.targetY)

    if (dist < 90f && !mission.isCompleted) {
      mission.stage++
      if (mission.stage >= mission.maxStages) {
        mission.isCompleted = true
        playerCash += mission.rewardCash
        soundManager.playMissionFanfare()
        soundManager.vibrate(200, 255)

        // Advance to next mission
        val next = availableMissions.firstOrNull { !it.isCompleted && it != mission }
        if (next != null) {
          next.isActive = true
          activeMission = next
        }
      } else {
        soundManager.playCashChime()
      }
    }
  }

  private fun buildState() = EngineState(
    playerX = playerX,
    playerY = playerY,
    playerHealth = playerHealth,
    playerArmor = playerArmor,
    playerCash = playerCash,
    playerAbility = playerAbility,
    isAbilityActive = isAbilityActive,
    wantedStars = wantedStars,
    isEvading = isEvading,
    evasionProgress = evasionProgress,
    currentDistrict = currentDistrict,
    currentVehicleSpeed = currentVehicle?.speed ?: 0f,
    isInVehicle = currentVehicle != null,
    currentWeapon = inventory[currentWeaponIndex],
    activeMission = activeMission,
    character = currentCharacter,
    isFirstPerson = isFirstPerson,
    invertJoystickY = invertJoystickY,
    timeOfDay = currentTimeOfDay
  )
}
