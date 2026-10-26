package com.example.game.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.engine.GameEngine
import com.example.game.model.CharacterType
import com.example.ui.theme.GtaGold

@Composable
fun CharacterWheelDialog(
  engine: GameEngine,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .width(340.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(Color(0xFA0F172A))
        .border(2.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
        .padding(16.dp)
        .testTag("character_wheel_modal")
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "SWITCH PROTAGONIST",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.2.sp
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3 Protagonist Cards
        for (char in CharacterType.values()) {
          val isSelected = engine.currentCharacter == char
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 5.dp)
              .clickable {
                engine.switchCharacter(char)
                onDismiss()
              }
              .testTag("char_card_${char.name.lowercase()}"),
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) char.themeColor.copy(alpha = 0.2f) else Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(14.dp),
            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, char.themeColor) else null
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              // Character Portrait
              Image(
                painter = painterResource(id = char.portraitRes),
                contentDescription = char.heroName,
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .border(2.dp, char.themeColor, CircleShape),
                contentScale = ContentScale.Crop
              )

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = char.heroName.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                  )
                  if (isSelected) {
                    Text(
                      text = "ACTIVE",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Black,
                      color = char.themeColor
                    )
                  }
                }
                Text(
                  text = char.nickname,
                  fontSize = 11.sp,
                  color = GtaGold
                )
                Text(
                  text = "Special: ${char.abilityName}",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = char.themeColor
                )
              }
            }
          }
        }
      }
    }
  }
}
