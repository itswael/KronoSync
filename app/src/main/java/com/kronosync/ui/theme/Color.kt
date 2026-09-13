package com.kronosync.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Calm, encouraging palette (teal/indigo) — deliberately not the stock M3 seed purple.
// Never mapped to task status here: status color roles for Done/Partial/Skipped live in
// StatusColors below and stay off the `error` role per the tone principle (see spec §1, §7.2).

private val Teal10 = Color(0xFF00201C)
private val Teal20 = Color(0xFF00382F)
private val Teal30 = Color(0xFF005143)
private val Teal40 = Color(0xFF006C58)
private val Teal80 = Color(0xFF5CDBBA)
private val Teal90 = Color(0xFF78F8D6)

private val Indigo10 = Color(0xFF0B1D33)
private val Indigo20 = Color(0xFF1B324D)
private val Indigo30 = Color(0xFF2C4966)
private val Indigo40 = Color(0xFF3F6180)
private val Indigo80 = Color(0xFFA9CBEC)
private val Indigo90 = Color(0xFFCEE5FF)

private val Amber40 = Color(0xFF8A5A00)
private val Amber80 = Color(0xFFFFC26B)
private val Amber90 = Color(0xFFFFDDB0)
private val Amber10 = Color(0xFF2A1800)
private val Amber20 = Color(0xFF472A00)

private val Neutral10 = Color(0xFF191C1B)
private val Neutral90 = Color(0xFFE1E3E0)
private val Neutral95 = Color(0xFFF0F2EF)
private val Neutral99 = Color(0xFFFBFDFA)
private val NeutralVariant30 = Color(0xFF404944)
private val NeutralVariant50 = Color(0xFF707973)
private val NeutralVariant80 = Color(0xFFBFC9C2)
private val NeutralVariant90 = Color(0xFFDBE5DD)

val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Indigo40,
    onSecondary = Color.White,
    secondaryContainer = Indigo90,
    onSecondaryContainer = Indigo10,
    tertiary = Amber40,
    onTertiary = Color.White,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F5F1),
    surfaceContainer = Color(0xFFEDEFEB),
    surfaceContainerHigh = Color(0xFFE7E9E5),
    surfaceContainerHighest = Color(0xFFE1E4DF)
)

val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Indigo80,
    onSecondary = Indigo20,
    secondaryContainer = Indigo30,
    onSecondaryContainer = Indigo90,
    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber20,
    onTertiaryContainer = Amber90,
    background = Color(0xFF101312),
    onBackground = Neutral90,
    surface = Color(0xFF101312),
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant50,
    surfaceContainerLowest = Color(0xFF0B0E0D),
    surfaceContainerLow = Color(0xFF181B1A),
    surfaceContainer = Color(0xFF1C1F1E),
    surfaceContainerHigh = Color(0xFF272A29),
    surfaceContainerHighest = Color(0xFF323533)
)

/** True-black variant of [DarkColors] for the AMOLED setting — same roles, near-black surfaces. */
val AmoledColors = DarkColors.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0C0B),
    surfaceContainer = Color(0xFF0F1211),
    surfaceContainerHigh = Color(0xFF161918),
    surfaceContainerHighest = Color(0xFF1E2120)
)

/**
 * Status colors kept deliberately off the M3 `error` role (reserved for real app errors)
 * per the tone principle: no red/alarming treatment for a skipped or partial task.
 */
object StatusColors {
    val doneLight = Teal40
    val doneContainerLight = Teal90
    val partialLight = Amber40
    val partialContainerLight = Amber90
    val skippedLight = NeutralVariant50
    val skippedContainerLight = NeutralVariant90

    val doneDark = Teal80
    val doneContainerDark = Teal30
    val partialDark = Amber80
    val partialContainerDark = Amber20
    val skippedDark = NeutralVariant80
    val skippedContainerDark = NeutralVariant30
}
