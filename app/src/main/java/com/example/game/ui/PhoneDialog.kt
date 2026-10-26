package com.example.game.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.engine.GameEngine
import com.example.game.model.VehicleEntity
import com.example.game.model.VehicleModel
import com.example.game.model.WeaponCategory
import com.example.ui.theme.GtaGold
import com.example.ui.theme.GtaGreen
import com.example.ui.theme.GtaPoliceBlue
import com.example.ui.theme.GtaPoliceRed
import java.text.NumberFormat
import java.util.Locale

private enum class PhoneAppScreen {
  HOME, CONTACTS, MAZE_BANK, AMMUNATION, CHEATS, MISSIONS
}

@Composable
fun PhoneDialog(
  engine: GameEngine,
  onDismiss: () -> Unit
) {
  var currentApp by remember { mutableStateOf(PhoneAppScreen.HOME) }
  var phoneToast by remember { mutableStateOf<String?>(null) }
  val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
    maximumFractionDigits = 0
  }

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .width(440.dp)
        .height(330.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(Color(0xFF0F172A))
        .border(3.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
        .padding(12.dp)
        .testTag("ifruit_phone_modal")
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Status Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "iFruit 9",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8)
          )
          Text(
            text = "12:00 PM  4G LTE 100%",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF94A3B8)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // App Header / Navigation
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (currentApp != PhoneAppScreen.HOME) {
            IconButton(
              onClick = { currentApp = PhoneAppScreen.HOME },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
              )
            }
          } else {
            Spacer(modifier = Modifier.size(32.dp))
          }

          Text(
            text = when (currentApp) {
              PhoneAppScreen.HOME -> "iFRUIT"
              PhoneAppScreen.CONTACTS -> "CONTACTS"
              PhoneAppScreen.MAZE_BANK -> "MAZE BANK"
              PhoneAppScreen.AMMUNATION -> "AMMU-NATION"
              PhoneAppScreen.CHEATS -> "CHEATS"
              PhoneAppScreen.MISSIONS -> "JOBS"
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp
          )

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Phone",
              tint = Color.White
            )
          }
        }

        phoneToast?.let { toast ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(GtaGreen.copy(alpha = 0.2f))
              .border(1.dp, GtaGreen, RoundedCornerShape(6.dp))
              .padding(6.dp)
          ) {
            Text(
              text = toast,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = GtaGreen
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content Body
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          when (currentApp) {
            PhoneAppScreen.HOME -> {
              PhoneHomeScreen(onOpenApp = { currentApp = it })
            }
            PhoneAppScreen.CONTACTS -> {
              PhoneContactsScreen(
                engine = engine,
                onToast = { phoneToast = it }
              )
            }
            PhoneAppScreen.MAZE_BANK -> {
              PhoneMazeBankScreen(
                engine = engine,
                currencyFormatter = currencyFormatter,
                onToast = { phoneToast = it }
              )
            }
            PhoneAppScreen.AMMUNATION -> {
              PhoneAmmuNationScreen(
                engine = engine,
                currencyFormatter = currencyFormatter,
                onToast = { phoneToast = it }
              )
            }
            PhoneAppScreen.CHEATS -> {
              PhoneCheatsScreen(
                engine = engine,
                onToast = { phoneToast = it }
              )
            }
            PhoneAppScreen.MISSIONS -> {
              PhoneMissionsScreen(
                engine = engine,
                currencyFormatter = currencyFormatter,
                onToast = { phoneToast = it }
              )
            }
          }
        }

        // Bottom Home Pill
        Box(
          modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 10.dp)
            .size(width = 80.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF64748B))
            .clickable { currentApp = PhoneAppScreen.HOME }
        )
      }
    }
  }
}

@Composable
private fun PhoneHomeScreen(onOpenApp: (PhoneAppScreen) -> Unit) {
  val apps = listOf(
    Triple("Contacts", Icons.Default.ContactPhone, PhoneAppScreen.CONTACTS),
    Triple("Maze Bank", Icons.Default.AccountBalance, PhoneAppScreen.MAZE_BANK),
    Triple("Ammu-Nation", Icons.Default.Security, PhoneAppScreen.AMMUNATION),
    Triple("Cheats", Icons.Default.Gamepad, PhoneAppScreen.CHEATS),
    Triple("Jobs", Icons.Default.ListAlt, PhoneAppScreen.MISSIONS)
  )

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      for (app in apps.take(3)) {
        PhoneAppIcon(title = app.first, icon = app.second) {
          onOpenApp(app.third)
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      for (app in apps.drop(3)) {
        PhoneAppIcon(title = app.first, icon = app.second) {
          onOpenApp(app.third)
        }
      }
    }
  }
}

@Composable
private fun PhoneAppIcon(
  title: String,
  icon: ImageVector,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable { onClick() }
  ) {
    Box(
      modifier = Modifier
        .size(56.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xFF1E293B))
        .border(1.dp, Color(0xFF475569), RoundedCornerShape(16.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = GtaGold,
        modifier = Modifier.size(28.dp)
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = title,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      color = Color.White
    )
  }
}

@Composable
private fun PhoneContactsScreen(
  engine: GameEngine,
  onToast: (String) -> Unit
) {
  val contacts = listOf(
    "Lester Crest" to "Remove Wanted Stars ($1,000)",
    "Mechanic" to "Deliver Pegassi Zentorno ($500)",
    "Simeon Yetarian" to "High-Value Car Request",
    "Lamar Davis" to "Call Grove St Street Backup ($400)"
  )

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    items(contacts) { (name, action) ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            when (name) {
              "Lester Crest" -> {
                if (engine.playerCash >= 1000) {
                  engine.playerCash -= 1000
                  engine.clearWantedLevel()
                  engine.soundManager.playCashChime()
                  onToast("Lester: Police paid off. Stars cleared.")
                } else {
                  onToast("Not enough cash to bribe cops!")
                }
              }
              "Mechanic" -> {
                if (engine.playerCash >= 500) {
                  engine.playerCash -= 500
                  engine.vehicles.add(
                    VehicleEntity(
                      id = System.currentTimeMillis(),
                      model = VehicleModel.ZENTORNO,
                      x = engine.playerX + 45f,
                      y = engine.playerY + 45f,
                      angleDeg = engine.playerAngle,
                      color = 0xFFFFB300
                    )
                  )
                  engine.soundManager.playCashChime()
                  onToast("Mechanic: Your Zentorno is delivered right beside you.")
                } else {
                  onToast("Need $500 for mechanic delivery!")
                }
              }
              "Lamar Davis" -> {
                engine.soundManager.playCashChime()
                onToast("Lamar: Holler if you need backup in Strawberry!")
              }
              else -> {
                onToast("$name: Checking jobs for you.")
              }
            }
          },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Call",
            tint = GtaGreen,
            modifier = Modifier.size(20.dp)
          )
          Column {
            Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Text(action, color = Color(0xFF94A3B8), fontSize = 10.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun PhoneMazeBankScreen(
  engine: GameEngine,
  currencyFormatter: NumberFormat,
  onToast: (String) -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text("MAZE BANK STATEMENT", fontSize = 11.sp, color = Color(0xFF94A3B8))
        Text(
          currencyFormatter.format(engine.playerCash),
          fontSize = 22.sp,
          fontWeight = FontWeight.Black,
          color = GtaGreen
        )
      }
    }

    Button(
      onClick = {
        engine.playerCash += 100000
        engine.soundManager.playCashChime()
        onToast("Maze Bank: Wired $100,000 Heist Dividends.")
      },
      colors = ButtonDefaults.buttonColors(containerColor = GtaGreen),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("WIRE TRANSFER (+$100K)", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun PhoneAmmuNationScreen(
  engine: GameEngine,
  currencyFormatter: NumberFormat,
  onToast: (String) -> Unit
) {
  val items = listOf(
    Triple("Super Heavy Body Armor", 800L, {
      if (engine.playerCash >= 800) {
        engine.playerCash -= 800
        engine.playerArmor = 100f
        engine.soundManager.playCashChime()
        onToast("Body Armor equipped (100% Armor).")
      } else onToast("Insufficient funds!")
    }),
    Triple("Carbine Rifle Ammo Pack (+120)", 400L, {
      if (engine.playerCash >= 400) {
        engine.playerCash -= 400
        engine.inventory.firstOrNull { it.type == WeaponCategory.CARBINE }?.let {
          it.reserveAmmo = (it.reserveAmmo + 120).coerceAtMost(it.type.maxAmmo)
        }
        engine.soundManager.playCashChime()
        onToast("Bought Carbine Rifle ammunition.")
      } else onToast("Insufficient funds!")
    }),
    Triple("RPG Rockets (+4)", 1500L, {
      if (engine.playerCash >= 1500) {
        engine.playerCash -= 1500
        engine.inventory.firstOrNull { it.type == WeaponCategory.RPG }?.let {
          it.reserveAmmo = (it.reserveAmmo + 4).coerceAtMost(it.type.maxAmmo)
        }
        engine.soundManager.playCashChime()
        onToast("RPG Rockets delivered!")
      } else onToast("Insufficient funds!")
    })
  )

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    items(items) { (title, cost, buyAction) ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { buyAction() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            Text(currencyFormatter.format(cost), color = GtaGold, fontSize = 11.sp)
          }
          Text("BUY", fontWeight = FontWeight.Black, color = GtaGreen, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun PhoneCheatsScreen(
  engine: GameEngine,
  onToast: (String) -> Unit
) {
  val cheats = listOf(
    "LOWER_WANTED" to "Clear All Police Stars",
    "MAX_WANTED" to "5-Star Military Siege",
    "GOD_MODE" to "Invincibility (Toggle)",
    "EXPLOSIVE_AMMO" to "Explosive Bullets (Toggle)",
    "SPAWN_TANK" to "Spawn Military Rhino Tank",
    "SPAWN_SUPERCAR" to "Spawn Pegassi Zentorno",
    "GIVE_CASH" to "Add $250,000 Cash"
  )

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    items(cheats) { (cheatCode, desc) ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            when (cheatCode) {
              "LOWER_WANTED" -> {
                engine.clearWantedLevel()
                engine.soundManager.playMissionFanfare()
                onToast("CHEAT ACTIVATED: Wanted level cleared!")
              }
              "MAX_WANTED" -> {
                engine.wantedStars = 5
                engine.addWantedStars(0)
                onToast("CHEAT ACTIVATED: 5-Star wanted level!")
              }
              "GOD_MODE" -> {
                engine.isGodMode = !engine.isGodMode
                onToast("CHEAT ACTIVATED: God Mode ${if (engine.isGodMode) "ON" else "OFF"}")
              }
              "EXPLOSIVE_AMMO" -> {
                engine.hasExplosiveAmmo = !engine.hasExplosiveAmmo
                onToast("CHEAT ACTIVATED: Explosive Ammo ${if (engine.hasExplosiveAmmo) "ON" else "OFF"}")
              }
              "SPAWN_TANK" -> {
                engine.vehicles.add(
                  VehicleEntity(
                    id = System.currentTimeMillis(),
                    model = VehicleModel.RHINO_TANK,
                    x = engine.playerX + 50f,
                    y = engine.playerY + 50f,
                    angleDeg = engine.playerAngle,
                    isTank = true
                  )
                )
                engine.soundManager.playMissionFanfare()
                onToast("CHEAT ACTIVATED: Rhino Tank spawned!")
              }
              "SPAWN_SUPERCAR" -> {
                engine.vehicles.add(
                  VehicleEntity(
                    id = System.currentTimeMillis(),
                    model = VehicleModel.ZENTORNO,
                    x = engine.playerX + 45f,
                    y = engine.playerY + 45f,
                    angleDeg = engine.playerAngle,
                    color = 0xFFFFB300
                  )
                )
                engine.soundManager.playMissionFanfare()
                onToast("CHEAT ACTIVATED: Pegassi Zentorno spawned!")
              }
              "GIVE_CASH" -> {
                engine.playerCash += 250000
                engine.soundManager.playCashChime()
                onToast("CHEAT ACTIVATED: +$250,000 added!")
              }
            }
          },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text(cheatCode, fontWeight = FontWeight.Bold, color = GtaGold, fontSize = 12.sp)
          Text(desc, color = Color(0xFF94A3B8), fontSize = 10.sp)
        }
      }
    }
  }
}

@Composable
private fun PhoneMissionsScreen(
  engine: GameEngine,
  currencyFormatter: NumberFormat,
  onToast: (String) -> Unit
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    items(engine.availableMissions) { mission ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            engine.activeMission = mission
            onToast("GPS set to: ${mission.title}")
          },
        colors = CardDefaults.cardColors(
          containerColor = if (mission == engine.activeMission) Color(0xFF155E75) else Color(0xFF1E293B)
        )
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(mission.title, fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
          Text("Client: ${mission.employer}", color = GtaGold, fontSize = 10.sp)
          Text(mission.description, color = Color(0xFF94A3B8), fontSize = 10.sp)
          Text(
            "Reward: ${currencyFormatter.format(mission.rewardCash)}",
            fontWeight = FontWeight.Bold,
            color = GtaGreen,
            fontSize = 11.sp
          )
        }
      }
    }
  }
}
