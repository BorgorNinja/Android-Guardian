package com.borgorninja.androidguardian.data

import android.graphics.drawable.Drawable

/**
 * Immutable UI-facing representation of an installed package.
 * [icon] is resolved once at load time and cached in memory; we intentionally
 * avoid re-resolving it per-recomposition since PackageManager.getApplicationIcon
 * is a binder call.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val versionName: String?,
    val compileStatus: CompileStatus = CompileStatus.Unknown
)
