package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

data class AccentPalette(
    val name: String,
    val hex: String,
    val primaryLight: Color,
    val onPrimaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val secondaryLight: Color,
    val secondaryContainerLight: Color,
    val tertiaryLight: Color,
    val tertiaryContainerLight: Color,
    val primaryDark: Color,
    val onPrimaryDark: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
    val secondaryDark: Color,
    val secondaryContainerDark: Color,
    val tertiaryDark: Color,
    val tertiaryContainerDark: Color
)

val ExpressiveAccents = listOf(
    AccentPalette(
        name = "Indigo Pulse",
        hex = "#6366f1",
        primaryLight = Color(0xFF4F46E5),
        onPrimaryLight = Color(0xFFFFFFFF),
        primaryContainerLight = Color(0xFFE0E7FF),
        onPrimaryContainerLight = Color(0xFF1E1B4B),
        secondaryLight = Color(0xFF0D9488),
        secondaryContainerLight = Color(0xFFCCFBF1),
        tertiaryLight = Color(0xFFE11D48),
        tertiaryContainerLight = Color(0xFFFFE4E6),
        primaryDark = Color(0xFFA5B4FC),
        onPrimaryDark = Color(0xFF1E1B4B),
        primaryContainerDark = Color(0xFF3730A3),
        onPrimaryContainerDark = Color(0xFFE0E7FF),
        secondaryDark = Color(0xFF5EEAD4),
        secondaryContainerDark = Color(0xFF115E59),
        tertiaryDark = Color(0xFFFDA4AF),
        tertiaryContainerDark = Color(0xFF881337)
    ),
    AccentPalette(
        name = "Lagoon Teal",
        hex = "#0d9488",
        primaryLight = Color(0xFF0F766E),
        onPrimaryLight = Color(0xFFFFFFFF),
        primaryContainerLight = Color(0xFFCCFBF1),
        onPrimaryContainerLight = Color(0xFF042F2E),
        secondaryLight = Color(0xFF4F46E5),
        secondaryContainerLight = Color(0xFFE0E7FF),
        tertiaryLight = Color(0xFFD97706),
        tertiaryContainerLight = Color(0xFFFEF3C7),
        primaryDark = Color(0xFF5EEAD4),
        onPrimaryDark = Color(0xFF042F2E),
        primaryContainerDark = Color(0xFF115E59),
        onPrimaryContainerDark = Color(0xFFCCFBF1),
        secondaryDark = Color(0xFFA5B4FC),
        secondaryContainerDark = Color(0xFF312E81),
        tertiaryDark = Color(0xFFFCD34D),
        tertiaryContainerDark = Color(0xFF78350F)
    ),
    AccentPalette(
        name = "Coral Bloom",
        hex = "#f43f5e",
        primaryLight = Color(0xFFE11D48),
        onPrimaryLight = Color(0xFFFFFFFF),
        primaryContainerLight = Color(0xFFFFE4E6),
        onPrimaryContainerLight = Color(0xFF4C0519),
        secondaryLight = Color(0xFF7C3AED),
        secondaryContainerLight = Color(0xFFEDE9FE),
        tertiaryLight = Color(0xFF0D9488),
        tertiaryContainerLight = Color(0xFFCCFBF1),
        primaryDark = Color(0xFFFDA4AF),
        onPrimaryDark = Color(0xFF4C0519),
        primaryContainerDark = Color(0xFF9F1239),
        onPrimaryContainerDark = Color(0xFFFFE4E6),
        secondaryDark = Color(0xFFC4B5FD),
        secondaryContainerDark = Color(0xFF4C1D95),
        tertiaryDark = Color(0xFF5EEAD4),
        tertiaryContainerDark = Color(0xFF115E59)
    ),
    AccentPalette(
        name = "Botanical Mint",
        hex = "#16a34a",
        primaryLight = Color(0xFF15803D),
        onPrimaryLight = Color(0xFFFFFFFF),
        primaryContainerLight = Color(0xFFDCFCE7),
        onPrimaryContainerLight = Color(0xFF052E16),
        secondaryLight = Color(0xFF0284C7),
        secondaryContainerLight = Color(0xFFE0F2FE),
        tertiaryLight = Color(0xFFD97706),
        tertiaryContainerLight = Color(0xFFFEF3C7),
        primaryDark = Color(0xFF86EFAC),
        onPrimaryDark = Color(0xFF052E16),
        primaryContainerDark = Color(0xFF166534),
        onPrimaryContainerDark = Color(0xFFDCFCE7),
        secondaryDark = Color(0xFF7DD3FC),
        secondaryContainerDark = Color(0xFF075985),
        tertiaryDark = Color(0xFFFCD34D),
        tertiaryContainerDark = Color(0xFF78350F)
    ),
    AccentPalette(
        name = "Plum Velvet",
        hex = "#9333ea",
        primaryLight = Color(0xFF7E22CE),
        onPrimaryLight = Color(0xFFFFFFFF),
        primaryContainerLight = Color(0xFFF3E8FF),
        onPrimaryContainerLight = Color(0xFF3B0764),
        secondaryLight = Color(0xFFEC4899),
        secondaryContainerLight = Color(0xFFFCE7F3),
        tertiaryLight = Color(0xFF0D9488),
        tertiaryContainerLight = Color(0xFFCCFBF1),
        primaryDark = Color(0xFFD8B4FE),
        onPrimaryDark = Color(0xFF3B0764),
        primaryContainerDark = Color(0xFF6B21A8),
        onPrimaryContainerDark = Color(0xFFF3E8FF),
        secondaryDark = Color(0xFFF9A8D4),
        secondaryContainerDark = Color(0xFF831843),
        tertiaryDark = Color(0xFF5EEAD4),
        tertiaryContainerDark = Color(0xFF115E59)
    ),
    AccentPalette(
        name = "Amber Glow",
        hex = "#d97706",
        primaryLight = Color(0xFFB45309),
        onPrimaryLight = Color(0xFFFFFFFF),
        primaryContainerLight = Color(0xFFFEF3C7),
        onPrimaryContainerLight = Color(0xFF451A03),
        secondaryLight = Color(0xFF0F766E),
        secondaryContainerLight = Color(0xFFCCFBF1),
        tertiaryLight = Color(0xFF4F46E5),
        tertiaryContainerLight = Color(0xFFE0E7FF),
        primaryDark = Color(0xFFFCD34D),
        onPrimaryDark = Color(0xFF451A03),
        primaryContainerDark = Color(0xFF92400E),
        onPrimaryContainerDark = Color(0xFFFEF3C7),
        secondaryDark = Color(0xFF5EEAD4),
        secondaryContainerDark = Color(0xFF115E59),
        tertiaryDark = Color(0xFFA5B4FC),
        tertiaryContainerDark = Color(0xFF312E81)
    )
)

object SemanticColors {
    val PriorityHighLight = Color(0xFFDC2626)
    val PriorityHighContainerLight = Color(0xFFFEE2E2)
    val PriorityHighDark = Color(0xFFF87171)
    val PriorityHighContainerDark = Color(0xFF450A0A)

    val PriorityMedLight = Color(0xFFD97706)
    val PriorityMedContainerLight = Color(0xFFFEF3C7)
    val PriorityMedDark = Color(0xFFFBBF24)
    val PriorityMedContainerDark = Color(0xFF451A03)

    val PriorityLowLight = Color(0xFF16A34A)
    val PriorityLowContainerLight = Color(0xFFDCFCE7)
    val PriorityLowDark = Color(0xFF4ADE80)
    val PriorityLowContainerDark = Color(0xFF052E16)
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFF6366F1)): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> Color(clean.toLong(16) or 0xFF000000)
            8 -> Color(clean.toLong(16))
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

fun buildExpressiveColorScheme(accentHex: String, darkTheme: Boolean): ColorScheme {
    val preset = ExpressiveAccents.find { it.hex.equals(accentHex.trim(), ignoreCase = true) }
    if (preset != null) {
        return if (darkTheme) {
            darkColorScheme(
                primary = preset.primaryDark,
                onPrimary = preset.onPrimaryDark,
                primaryContainer = preset.primaryContainerDark,
                onPrimaryContainer = preset.onPrimaryContainerDark,
                secondary = preset.secondaryDark,
                onSecondary = Color(0xFF082F2C),
                secondaryContainer = preset.secondaryContainerDark,
                onSecondaryContainer = Color(0xFFCCFBF1),
                tertiary = preset.tertiaryDark,
                onTertiary = Color(0xFF3F0D1D),
                tertiaryContainer = preset.tertiaryContainerDark,
                onTertiaryContainer = Color(0xFFFFE4E6),
                background = Color(0xFF0F111A),
                onBackground = Color(0xFFECEEF8),
                surface = Color(0xFF151824),
                onSurface = Color(0xFFECEEF8),
                surfaceVariant = Color(0xFF222638),
                onSurfaceVariant = Color(0xFF9EA4BE),
                surfaceContainerLowest = Color(0xFF0B0D14),
                surfaceContainerLow = Color(0xFF171A26),
                surfaceContainer = Color(0xFF1D2130),
                surfaceContainerHigh = Color(0xFF252A3C),
                surfaceContainerHighest = Color(0xFF2F354B),
                outline = Color(0xFF3E445B),
                outlineVariant = Color(0xFF2A2F42),
                error = Color(0xFFF87171),
                onError = Color(0xFF450A0A),
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFFEE2E2)
            )
        } else {
            lightColorScheme(
                primary = preset.primaryLight,
                onPrimary = preset.onPrimaryLight,
                primaryContainer = preset.primaryContainerLight,
                onPrimaryContainer = preset.onPrimaryContainerLight,
                secondary = preset.secondaryLight,
                onSecondary = Color.White,
                secondaryContainer = preset.secondaryContainerLight,
                onSecondaryContainer = Color(0xFF042F2E),
                tertiary = preset.tertiaryLight,
                onTertiary = Color.White,
                tertiaryContainer = preset.tertiaryContainerLight,
                onTertiaryContainer = Color(0xFF4C0519),
                background = Color(0xFFF6F7FC),
                onBackground = Color(0xFF181B28),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF181B28),
                surfaceVariant = Color(0xFFE9ECF6),
                onSurfaceVariant = Color(0xFF565B73),
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow = Color(0xFFF1F3FA),
                surfaceContainer = Color(0xFFEBEEF7),
                surfaceContainerHigh = Color(0xFFE4E7F2),
                surfaceContainerHighest = Color(0xFFDCE0EE),
                outline = Color(0xFFC5CADB),
                outlineVariant = Color(0xFFE1E4F0),
                error = Color(0xFFDC2626),
                onError = Color.White,
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFF7F1D1D)
            )
        }
    }

    // Custom hex fallback
    val base = parseHexColor(accentHex)
    val onBase = if (base.luminance() > 0.5f) Color(0xFF12141C) else Color.White
    return if (darkTheme) {
        darkColorScheme(
            primary = base,
            onPrimary = onBase,
            primaryContainer = base.copy(alpha = 0.28f),
            onPrimaryContainer = Color(0xFFECEEF8),
            background = Color(0xFF0F111A),
            onBackground = Color(0xFFECEEF8),
            surface = Color(0xFF151824),
            onSurface = Color(0xFFECEEF8),
            surfaceVariant = Color(0xFF222638),
            onSurfaceVariant = Color(0xFF9EA4BE)
        )
    } else {
        lightColorScheme(
            primary = base,
            onPrimary = onBase,
            primaryContainer = base.copy(alpha = 0.16f),
            onPrimaryContainer = Color(0xFF181B28),
            background = Color(0xFFF6F7FC),
            onBackground = Color(0xFF181B28),
            surface = Color.White,
            onSurface = Color(0xFF181B28),
            surfaceVariant = Color(0xFFE9ECF6),
            onSurfaceVariant = Color(0xFF565B73)
        )
    }
}
