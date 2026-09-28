package com.paolonata.whatsapptranscriber.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BrandPrimary = Color(0xFF075E54)
val BrandAccent = Color(0xFF25D366)
val BrandBackground = Color(0xFFF6F6F6)
val BrandSurface = Color(0xFFFFFFFF)
val BrandError = Color(0xFFB3261E)

// Font sizes are intentionally larger than Material defaults: the app is designed
// for an elderly reader who needs to read a long transcript comfortably.
private val ElderFriendlyTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 22.sp),
)

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    secondary = BrandAccent,
    background = BrandBackground,
    surface = BrandSurface,
    error = BrandError,
)

private val DarkColors = darkColorScheme(
    primary = BrandAccent,
    secondary = BrandPrimary,
)

@Composable
fun WhatsAppTranscriberTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = ElderFriendlyTypography,
        content = content,
    )
}
