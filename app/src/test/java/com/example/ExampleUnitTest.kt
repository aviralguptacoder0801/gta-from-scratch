package com.example

import com.example.game.model.CharacterType
import com.example.game.model.VehicleModel
import com.example.game.model.WeaponCategory
import com.example.game.world.LosSantosWorld
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testLosSantosWorldDistricts() {
    val downtown = LosSantosWorld.getDistrictFor(2100f, 1750f)
    assertEquals("DOWNTOWN LOS SANTOS", downtown)

    val vinewood = LosSantosWorld.getDistrictFor(2200f, 450f)
    assertEquals("VINEWOOD HILLS", vinewood)

    val sandyShores = LosSantosWorld.getDistrictFor(3600f, 1200f)
    assertEquals("SANDY SHORES", sandyShores)
  }

  @Test
  fun testCharacterTypesAndAbilities() {
    val characters = CharacterType.values()
    assertEquals(3, characters.size)

    val franklin = CharacterType.FRANKLIN
    assertEquals("Franklin Clinton", franklin.heroName)
    assertEquals("DRIVING FOCUS", franklin.abilityName)

    val michael = CharacterType.MICHAEL
    assertEquals("Michael De Santa", michael.heroName)
    assertEquals("BULLET TIME", michael.abilityName)

    val trevor = CharacterType.TREVOR
    assertEquals("Trevor Philips", trevor.heroName)
    assertEquals("RED MIST RAGE", trevor.abilityName)
  }

  @Test
  fun testVehicleModels() {
    val zentorno = VehicleModel.ZENTORNO
    assertTrue(zentorno.topSpeed > 15f)

    val tank = VehicleModel.RHINO_TANK
    assertTrue(tank.maxHealth >= 2000f)

    val police = VehicleModel.POLICE_CRUISER
    assertNotNull(police)
  }

  @Test
  fun testWeaponArsenal() {
    val weapons = WeaponCategory.values()
    assertTrue(weapons.contains(WeaponCategory.RPG))
    assertTrue(weapons.contains(WeaponCategory.CARBINE))
    assertTrue(weapons.contains(WeaponCategory.PISTOL))

    val rpg = WeaponCategory.RPG
    assertTrue(rpg.isExplosive)
    assertTrue(rpg.damage > 200f)
  }
}
