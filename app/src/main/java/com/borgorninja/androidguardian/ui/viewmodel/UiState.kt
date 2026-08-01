package com.borgorninja.androidguardian.ui.viewmodel

import com.borgorninja.androidguardian.data.AppInfo
import com.borgorninja.androidguardian.shizuku.ShizukuState

enum class AppFilter { USER, SYSTEM }

/**
 * Single immutable state object for the whole screen (unidirectional data flow).
 * The ViewModel exposes exactly one StateFlow<MainUiState>; Compose recomposes
 * on any field change and never mutates this object directly.
 */
data class MainUiState(
    val shizukuState: ShizukuState = ShizukuState.ServiceDead,
    val isLoadingApps: Boolean = false,
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val filter: AppFilter = AppFilter.USER,
    val selectedPackage: String? = null,
    val consoleLog: List<ConsoleEntry> = emptyList(),
    val isCompiling: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredApps: List<AppInfo>
        get() = apps
            .filter { if (filter == AppFilter.USER) !it.isSystemApp else it.isSystemApp }
            .filter {
                searchQuery.isBlank() ||
                    it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
}

data class ConsoleEntry(val text: String, val isError: Boolean, val timestampMs: Long)

/** One-shot events the UI should react to exactly once (snackbars, etc.). */
sealed class UiEvent {
    data class ShowMessage(val message: String) : UiEvent()
}
