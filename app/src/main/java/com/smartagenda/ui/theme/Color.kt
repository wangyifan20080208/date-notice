package com.smartagenda.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val PaperBase = Color(0xFFFBFAF7)
val PaperSurface = Color(0xFFFFFFFF)
val PaperElevated = Color(0xFFF4F2ED)
val InkPrimary = Color(0xFF191A1C)
val InkSecondary = Color(0xFF5E6165)
val InkTertiary = Color(0xFF8B8E92)
val Hairline = Color(0xFFE7E4DD)

val Accent = Color(0xFF0E6B5B)
val AccentSoft = Color(0xFFE4EFEA)
val OnAccent = Color(0xFFFFFFFF)
val OnAccentContainer = Color(0xFF05231D)

val Amber = Color(0xFF95560A)
val AmberSoft = Color(0xFFFBF1DE)
val AmberLine = Color(0xFFE3C28A)

val Danger = Color(0xFFB3261E)

val NightBase = Color(0xFF101112)
val NightSurface = Color(0xFF191A1C)
val NightElevated = Color(0xFF212326)
val NightInk = Color(0xFFEDEDEA)
val NightInkSecondary = Color(0xFFA7AAAD)
val NightInkTertiary = Color(0xFF7D8083)
val NightHairline = Color(0xFF2C2E31)

val NightAccent = Color(0xFF74D6C0)
val NightAccentSoft = Color(0xFF173A33)
val NightOnAccent = Color(0xFF04231D)
val NightAmber = Color(0xFFE8AE68)
val NightAmberSoft = Color(0xFF31260F)
val NightAmberLine = Color(0xFF5A4620)
val NightDanger = Color(0xFFF2B8B5)

data class AgendaExtraColors(
    val textSecondary: Color,
    val textTertiary: Color,
    val hairline: Color,
    val accentSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val amberLine: Color,
    val elevated: Color,
)

val LocalAgendaColors = compositionLocalOf {
    AgendaExtraColors(
        textSecondary = InkSecondary,
        textTertiary = InkTertiary,
        hairline = Hairline,
        accentSoft = AccentSoft,
        amber = Amber,
        amberSoft = AmberSoft,
        amberLine = AmberLine,
        elevated = PaperElevated,
    )
}