package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Liquid Glass Accent Presets
val IosCyan = Color(0xFF38BDF8)
val IosCobalt = Color(0xFF2563EB)
val IosViolet = Color(0xFF8B5CF6)
val IosRose = Color(0xFFF43F5E)
val IosEmerald = Color(0xFF10B981)
val IosAmber = Color(0xFFF59E0B)

// Mode 1: iOS Translucent Frost (Light/Airy Liquid Glass)
val FrostBackgroundTop = Color(0xFFEAF2FF)
val FrostBackgroundMid = Color(0xFFF5EEFF)
val FrostBackgroundBottom = Color(0xFFE4F8FA)
val FrostSurface = Color(0xFFFFFFFF)
val FrostOnSurface = Color(0xFF0F172A)
val FrostSecondaryText = Color(0xFF475569)

// Mode 2: Midnight Neon Liquid Glass (Dark Glass)
val MidnightBackgroundTop = Color(0xFF060913)
val MidnightBackgroundMid = Color(0xFF0E1326)
val MidnightBackgroundBottom = Color(0xFF130B24)
val MidnightSurface = Color(0xFF12182B)
val MidnightOnSurface = Color(0xFFF8FAFC)
val MidnightSecondaryText = Color(0xFF94A3B8)

// Streak Milestone Spectrum Colors (3d ignition, 10, 20, 50, 100, 200, 300, 400+)
val StreakInactive = Color(0xFF64748B)
val Streak3Flame = Color(0xFFFF6B35)
val Streak10Azure = Color(0xFF00D2FF)
val Streak20Emerald = Color(0xFF00E676)
val Streak50Violet = Color(0xFF9D4EDD)
val Streak100Gold = Color(0xFFFFD166)
val Streak200Crimson = Color(0xFFFF2A6D)
val Streak300Aurora = Color(0xFF05FFA1)
val Streak400Prism = Color(0xFF70D6FF)

enum class GlassMode(val label: String, val subtitle: String) {
    IOS_FROST("iOS Translucent Frost", "Kaca terang transparan berkilau ala iOS 26"),
    MIDNIGHT_NEON("Midnight Neon Glass", "Kaca gelap obsidian dengan aura neon album")
}

data class GlassThemeConfig(
    val glassMode: GlassMode = GlassMode.MIDNIGHT_NEON,
    val frostOpacity: Float = 0.24f,
    val cornerSoftnessDp: Float = 26f,
    val accentHex: String = "#38BDF8",
    val adaptiveAlbumColor: Boolean = true
)

fun streakMilestoneColors(streakDays: Int): List<Color> = when {
    streakDays >= 400 -> listOf(Streak400Prism, Streak200Crimson, Streak100Gold, Streak300Aurora)
    streakDays >= 300 -> listOf(Streak300Aurora, Streak10Azure, Streak50Violet)
    streakDays >= 200 -> listOf(Streak200Crimson, Streak3Flame, Streak100Gold)
    streakDays >= 100 -> listOf(Streak100Gold, Streak3Flame, Color(0xFFFFF3B0))
    streakDays >= 50 -> listOf(Streak50Violet, IosRose, IosCyan)
    streakDays >= 20 -> listOf(Streak20Emerald, IosCyan, Color(0xFFA7F3D0))
    streakDays >= 10 -> listOf(Streak10Azure, IosCobalt, Color(0xFFBAE6FD))
    streakDays >= 3 -> listOf(Streak3Flame, Streak100Gold, Color(0xFFFF9F1C))
    else -> listOf(StreakInactive, Color(0xFF475569))
}

fun streakMilestoneLabel(streakDays: Int): String = when {
    streakDays >= 400 -> "400+ Prism Mythic"
    streakDays >= 300 -> "300+ Aurora Legend"
    streakDays >= 200 -> "200+ Crimson Nova"
    streakDays >= 100 -> "100+ Golden Crown"
    streakDays >= 50 -> "50+ Velvet Pulse"
    streakDays >= 20 -> "20+ Emerald Flow"
    streakDays >= 10 -> "10+ Azure Spark"
    streakDays >= 3 -> "Streak Menyala!"
    else -> "Pakai 3 hari untuk nyalakan"
}

fun parseColorHex(hex: String, fallback: Color = IosCyan): Color {
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        if (clean.length == 6) Color(0xFF000000L or value) else Color(value)
    } catch (_: Exception) {
        fallback
    }
}
