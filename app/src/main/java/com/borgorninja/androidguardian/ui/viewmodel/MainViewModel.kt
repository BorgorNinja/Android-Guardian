package com.borgorninja.androidguardian.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.borgorninja.androidguardian.data.AppRepository
import com.borgorninja.androidguardian.data.CompileStatus
import com.borgorninja.androidguardian.shizuku.ShizukuManager
import com.borgorninja.androidguardian.shizuku.ShizukuState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<UiEvent> = _events

    private var compileJob: Job? = null

    init {
        ShizukuManager.attachListeners()
        observeShizukuState()
    }

    private fun observeShizukuState() {
        viewModelScope.launch {
            // ShizukuManager.state already reflects live listener callbacks; we just
            // mirror it into our own UDF state so screens only ever read one StateFlow.
            ShizukuManager.state.collect { shizukuState ->
                _uiState.update { it.copy(shizukuState = shizukuState) }
                if (shizukuState == ShizukuState.Granted && _uiState.value.apps.isEmpty()) {
                    loadInstalledApps()
                }
            }
        }
    }

    fun onRequestPermission() = ShizukuManager.requestPermission()

    fun onRefreshShizukuState() = ShizukuManager.refresh()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterChanged(filter: AppFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun onAppSelected(packageName: String) {
        _uiState.update { it.copy(selectedPackage = packageName, consoleLog = emptyList()) }
        refreshCompileStatus(packageName)
    }

    fun loadInstalledApps() {
        if (_uiState.value.shizukuState != ShizukuState.Granted) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true, errorMessage = null) }
            try {
                val apps = repository.listInstalledApps()
                _uiState.update { it.copy(apps = apps, isLoadingApps = false) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isLoadingApps = false, errorMessage = "Failed to list apps: ${t.message}")
                }
                _events.tryEmit(UiEvent.ShowMessage("Failed to list apps: ${t.message}"))
            }
        }
    }

    private fun refreshCompileStatus(packageName: String) {
        viewModelScope.launch {
            val status = runCatching { repository.queryCompileStatus(packageName) }
                .getOrElse { CompileStatus.Error(it.message ?: "unknown") }
            updateAppStatus(packageName, status)
        }
    }

    private fun updateAppStatus(packageName: String, status: CompileStatus) {
        _uiState.update { state ->
            state.copy(apps = state.apps.map {
                if (it.packageName == packageName) it.copy(compileStatus = status) else it
            })
        }
    }

    /** `cmd package compile -m speed -f <pkg>` — force full AOT at the `speed` filter. */
    fun optimizeSelectedApp() {
        val pkg = _uiState.value.selectedPackage ?: return
        if (_uiState.value.isCompiling) return
        runCommandAsCompileJob(
            command = arrayOf("sh", "-c", "cmd package compile -m speed -f $pkg"),
            packageName = pkg,
            startMessage = "$ cmd package compile -m speed -f $pkg"
        )
    }

    /** `cmd package compile --reset <pkg>` — discard current compilation, revert to JIT/interpreted. */
    fun resetCompilation() {
        val pkg = _uiState.value.selectedPackage ?: return
        if (_uiState.value.isCompiling) return
        runCommandAsCompileJob(
            command = arrayOf("sh", "-c", "cmd package compile --reset $pkg"),
            packageName = pkg,
            startMessage = "$ cmd package compile --reset $pkg"
        )
    }

    private fun runCommandAsCompileJob(command: Array<String>, packageName: String, startMessage: String) {
        if (_uiState.value.shizukuState != ShizukuState.Granted) {
            _events.tryEmit(UiEvent.ShowMessage("Shizuku permission not granted"))
            return
        }
        appendConsole(startMessage, isError = false)
        _uiState.update { it.copy(isCompiling = true) }
        updateAppStatus(packageName, CompileStatus.InProgress)

        compileJob = viewModelScope.launch {
            try {
                ShizukuManager.runCommand(command).collect { line ->
                    appendConsole(line.text, line.isError)
                }
            } catch (t: Throwable) {
                appendConsole("Process interrupted: ${t.message}", isError = true)
            } finally {
                _uiState.update { it.copy(isCompiling = false) }
                refreshCompileStatus(packageName)
            }
        }
    }

    fun cancelRunningCommand() {
        compileJob?.cancel()
        compileJob = null
        _uiState.update { it.copy(isCompiling = false) }
        appendConsole("Cancelled by user", isError = true)
    }

    private fun appendConsole(text: String, isError: Boolean) {
        _uiState.update {
            it.copy(
                consoleLog = it.consoleLog + ConsoleEntry(text, isError, System.currentTimeMillis())
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        ShizukuManager.detachListeners()
    }
}
