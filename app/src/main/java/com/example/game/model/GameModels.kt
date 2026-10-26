package com.example.game.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.FranklinGreen
import com.example.ui.theme.MichaelBlue
import com.example.ui.theme.TrevorOrange

enum class CharacterType(
  val heroName: String,
  val nickname: String,
  val abilityName: String,
  val abilityDesc: String,
  val themeColor: Color,
  @DrawableRes val portraitRes: Int
) {
  FRANKLIN(
    heroName = "Franklin Clinton",
    nickname = "The Street Prodigy",
    abilityName = "DRIVING FOCUS",
    abilityDesc = "Hyper-reflexes that slow down time while driving with laser handling.",
    themeColor = FranklinGreen,
    portraitRes = R.drawable.img_franklin
  ),
  MICHAEL(
    heroName = "Michael De Santa",
    nickname = "The Mastermind",
    abilityName = "BULLET TIME",
    abilityDesc = "Adrenaline surge slowing down combat for pinpoint headshot accuracy.",
    themeColor = MichaelBlue,
    portraitRes = R.drawable.img_michael
  ),
  TREVOR(
    heroName = "Trevor Philips",
    nickname = "The Wild Outlaw",
    abilityName = "RED MIST RAGE",
    abilityDesc = "Take 70% reduced damage and deal double devastation.",
    themeColor = TrevorOrange,
    portraitRes = R.drawable.img_trevor
  )
}

enum class WeaponCategory(
  val displayName: String,
  val damage: Float,
  val fireRateMs: Long,
  val range: Float,
  val isExplosive: Boolean,
  val defaultAmmo: Int,
  val maxAmmo: Int,
  val clipSize: Int,
  val bulletSpeed: Float = 22f
) {
  FISTS("Fists", 25f, 350L, 55f, false, -1, -1, -1, 0f),
  PISTOL("Combat Pistol", 35f, 240L, 500f, false, 90, 300, 15, 24f),
  SMG("Micro SMG", 26f, 105L, 460f, false, 180, 600, 30, 26f),
  SHOTGUN("Pump Shotgun", 90f, 650L, 320f, false, 40, 120, 8, 20f),
  CARBINE("Carbine Rifle", 48f, 135L, 620f, false, 150, 600, 30, 28f),
  RPG("RPG-7 Rocket", 280f, 1400L, 800f, true, 8, 20, 1, 14f),
  SNIPER("Heavy Sniper", 200f, 950L, 1000f, false, 25, 80, 5, 34f)
}

data class Weapon(
  val type: WeaponCategory,
  var currentClip: Int,
  var reserveAmmo: Int
)

enum class VehicleModel(
  val modelName: String,
  val topSpeed: Float,
  val acceleration: Float,
  val handling: Float,
  val maxHealth: Float,
  val length: Float,
  val width: Float,
  val defaultColor: Long
) {
  ZENTORNO("Pegassi Zentorno", 16.5f, 0.48f, 5.0f, 380f, 86f, 42f, 0xFFFFB300),
  BUFFALO("Bravado Buffalo S", 13.5f, 0.38f, 4.6f, 440f, 84f, 41f, 0xFF10B981),
  SABRE("Declasse Sabre Turbo", 13.0f, 0.42f, 4.2f, 460f, 88f, 43f, 0xFFDC2626),
  BODHI("Canis Bodhi", 11.5f, 0.34f, 4.4f, 550f, 92f, 45f, 0xFFEA580C),
  POLICE_CRUISER("Vapid Police Interceptor", 14.0f, 0.40f, 4.7f, 480f, 85f, 41f, 0xFF1E293B),
  SWAT_ENFORCER("SWAT Enforcer", 10.5f, 0.28f, 3.5f, 900f, 102f, 48f, 0xFF0F172A),
  HELICOPTER("Police Maverick", 12.0f, 0.32f, 3.8f, 650f, 110f, 55f, 0xFF1D4ED8),
  RHINO_TANK("Rhino Tank", 7.5f, 0.22f, 2.6f, 2200f, 115f, 60f, 0xFF3F6212)
}

data class VehicleEntity(
  val id: Long,
  val model: VehicleModel,
  var x: Float,
  var y: Float,
  var angleDeg: Float,
  var speed: Float = 0f,
  var health: Float = model.maxHealth,
  var color: Long = model.defaultColor,
  var isPolice: Boolean = false,
  var isHelicopter: Boolean = false,
  var isTank: Boolean = false,
  var sirenOn: Boolean = false,
  var isDestroyed: Boolean = false
)

data class PedestrianEntity(
  val id: Long,
  var x: Float,
  var y: Float,
  var angleDeg: Float,
  var speed: Float = 1.2f,
  var health: Float = 60f,
  var isFleeing: Boolean = false,
  var isPolice: Boolean = false,
  var isHostile: Boolean = false,
  var dropsCash: Int = 120,
  var isAlive: Boolean = true
)

data class BulletEntity(
  val id: Long,
  var x: Float,
  var y: Float,
  val vx: Float,
  val vy: Float,
  val damage: Float,
  val isPlayer: Boolean,
  val isRocket: Boolean,
  val maxDistance: Float,
  var traveled: Float = 0f
)

enum class ParticleType {
  SMOKE, FIRE, EXPLOSION_SHOCKWAVE, BLOOD, SPARK, SKID_MARK, POLICE_LIGHT
}

data class ParticleEntity(
  var x: Float,
  var y: Float,
  val vx: Float,
  val vy: Float,
  val color: Long,
  var life: Float,
  val maxLife: Float,
  val radius: Float,
  val type: ParticleType
)

data class Mission(
  val id: String,
  val title: String,
  val employer: String,
  val description: String,
  val rewardCash: Int,
  val targetDistrict: String,
  val targetX: Float,
  val targetY: Float,
  var isCompleted: Boolean = false,
  var isActive: Boolean = false,
  var stage: Int = 0,
  val maxStages: Int = 3
)

data class DistrictArea(
  val name: String,
  val minX: Float,
  val maxX: Float,
  val minY: Float,
  val maxY: Float,
  val colorTag: String
)
