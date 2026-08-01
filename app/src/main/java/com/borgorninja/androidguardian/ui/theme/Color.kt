package com.borgorninja.androidguardian.ui.theme

import androidx.compose.ui.graphics.Color

// Dark scheme — primary brand accent is a cool mint-teal to read as "system/technical" rather
// than playful; console text uses a true monospace-green for terminal familiarity.
val AccentTeal = Color(0xFF4CE0B3)
val AccentTealDark = Color(0xFF1E7A5F)
val SurfaceDark = Color(0xFF121316)
val SurfaceDarkElevated = Color(0xFF1B1D21)
val OnSurfaceDark = Color(0xFFE3E5E8)

// Light scheme
val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceLightElevated = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1B1D21)
val AccentTealLight = Color(0xFF00896B)

// Console / terminal palette (fixed regardless of theme — matches user expectations of a shell).
val ConsoleBackground = Color(0xFF0B0C0E)
val ConsoleStdout = Color(0xFF7FE8A4)
val ConsoleStderr = Color(0xFFFF6E6E)
val ConsoleMuted = Color(0xFF6B6F76)

// Status colors
val StatusSpeed = Color(0xFF4CE0B3)
val StatusSpeedProfile = Color(0xFFE0C24C)
val StatusVerifyOrOther = Color(0xFF8C9099)
val StatusError = Color(0xFFFF6E6E)
