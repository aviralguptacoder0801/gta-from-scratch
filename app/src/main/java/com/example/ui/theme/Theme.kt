package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GtaDarkScheme = darkColorScheme(
  primary = GtaGreen,
  secondary = GtaGold,
  tertiary = GtaPoliceBlue,
  background = GtaDarkBg,
  surface = GtaSurface,
  surfaceVariant = GtaElevated,
  onPrimary = GtaDarkBg,
  onSecondary = GtaDarkBg,
  onBackground = TextLight,
  onSurface = TextLight,
  error = AlertRed
)

@Composable
fun GtaTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = GtaDarkScheme,
    typography = Typography,
    content = content
  )
}
