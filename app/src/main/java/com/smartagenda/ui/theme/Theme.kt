package com.smartagenda.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightScheme = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentSoft,
    onPrimaryContainer = OnAccentContainer,
    secondary = InkSecondary,
    onSecondary = PaperSurface,
    secondaryContainer = AccentSoft,
    onSecondaryContainer = OnAccentContainer,
    background = PaperBase,
    onBackground = InkPrimary,
    surface = PaperSurface,
    onSurface = InkPrimary,
    surfaceVariant = PaperElevated,
    onSurfaceVariant = InkSecondary,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Danger,
    onError = PaperSurface,
)

private val DarkScheme = darkColorScheme(
    primary = NightAccent,
    onPrimary = NightOnAccent,
    primaryContainer = NightAccentSoft,
    onPrimaryContainer = NightInk,
    secondary = NightInkSecondary,
    secondaryContainer = NightAccentSoft,
    onSecondaryContainer = NightAccent,
    background = NightBase,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightElevated,
    onSurfaceVariant = NightInkSecondary,
    outline = NightHairline,
    outlineVariant = NightHairline,
    error = NightDanger,
)

private val DayExtras = AgendaExtraColors(
    textSecondary = InkSecondary,
    textTertiary = InkTertiary,
    hairline = Hairline,
    accentSoft = AccentSoft,
    amber = Amber,
    amberSoft = AmberSoft,
    amberLine = AmberLine,
    elevated = PaperElevated,
)

private val NightExtras = AgendaExtraColors(
    textSecondary = NightInkSecondary,
    textTertiary = NightInkTertiary,
    hairline = NightHairline,
    accentSoft = NightAccentSoft,
    amber = NightAmber,
    amberSoft = NightAmberSoft,
    amberLine = NightAmberLine,
    elevated = NightElevated,
)

private val AgendaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp,
    ),
)

private val AgendaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun SmartAgendaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val extras = if (darkTheme) NightExtras else DayExtras

    CompositionLocalProvider(LocalAgendaColors provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AgendaTypography,
            shapes = AgendaShapes,
            content = content,
        )
    }
}