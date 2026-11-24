package com.studyflow.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Cores fora do esquema Material: sucesso/alerta/erro "soft", sidebar escura e textos sutis.
 * Espelham os tokens `--color-*` do front web.
 */
@Immutable
data class StudyFlowColors(
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val sidebar: Color,
    val sidebarTop: Color,
    val sidebarText: Color,
    val sidebarAccent: Color,
    val sidebarOnAccent: Color,
    val subtle: Color,
    val primarySoft: Color,
    val border: Color,
    val borderStrong: Color,
)

private val LightExtended = StudyFlowColors(
    success = LightSuccess,
    successSoft = LightSuccessSoft,
    warning = LightWarning,
    warningSoft = LightWarningSoft,
    danger = LightDanger,
    dangerSoft = LightDangerSoft,
    sidebar = LightSidebar,
    sidebarTop = LightSidebarTop,
    sidebarText = LightSidebarText,
    sidebarAccent = LightSidebarAccent,
    sidebarOnAccent = Color(0xFF102017),
    subtle = LightSubtle,
    primarySoft = LightPrimarySoft,
    border = LightBorder,
    borderStrong = LightBorderStrong,
)

private val DarkExtended = StudyFlowColors(
    success = DarkSuccess,
    successSoft = DarkSuccessSoft,
    warning = DarkWarning,
    warningSoft = DarkWarningSoft,
    danger = DarkDanger,
    dangerSoft = DarkDangerSoft,
    sidebar = DarkSidebar,
    sidebarTop = DarkSidebarTop,
    sidebarText = DarkSidebarText,
    sidebarAccent = DarkSidebarAccent,
    sidebarOnAccent = Color(0xFF102017),
    subtle = DarkSubtle,
    primarySoft = DarkPrimarySoft,
    border = DarkBorder,
    borderStrong = DarkBorderStrong,
)

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimarySoft,
    onPrimaryContainer = LightPrimaryStrong,
    secondary = LightSuccess,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LightSuccessSoft,
    onSecondaryContainer = Color(0xFF1C4A35),
    tertiary = LightWarning,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = LightWarningSoft,
    onTertiaryContainer = Color(0xFF5B420F),
    error = LightDanger,
    onError = Color(0xFFFFFFFF),
    errorContainer = LightDangerSoft,
    onErrorContainer = Color(0xFF7C2F1D),
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMuted,
    outline = LightBorderStrong,
    outlineVariant = LightBorder,
    inverseSurface = LightSidebar,
    inverseOnSurface = Color(0xFFE9E5D9),
)

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimarySoft,
    onPrimaryContainer = Color(0xFFCFE9DA),
    secondary = DarkSuccess,
    onSecondary = Color(0xFF10231B),
    secondaryContainer = DarkSuccessSoft,
    onSecondaryContainer = Color(0xFFCFE9DA),
    tertiary = DarkWarning,
    onTertiary = Color(0xFF2E2408),
    tertiaryContainer = DarkWarningSoft,
    onTertiaryContainer = Color(0xFFF3DFB6),
    error = DarkDanger,
    onError = Color(0xFF3B1207),
    errorContainer = DarkDangerSoft,
    onErrorContainer = Color(0xFFFFD9CE),
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorderStrong,
    outlineVariant = DarkBorder,
    inverseSurface = DarkText,
    inverseOnSurface = DarkBackground,
)

val LocalStudyFlowColors = staticCompositionLocalOf { LightExtended }

@Composable
fun StudyFlowTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkExtended else LightExtended
    CompositionLocalProvider(LocalStudyFlowColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = StudyFlowTypography,
            content = content,
        )
    }
}

object StudyFlowTheme {
    val colors: StudyFlowColors
        @Composable get() = LocalStudyFlowColors.current
}
