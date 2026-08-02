package com.borgorninja.androidguardian.shizuku

import android.content.pm.PackageManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/** Coarse-grained state machine driving all permission-gated UI in the app. */
sealed class ShizukuState {
    data object ServiceDead : ShizukuState()
    data object Pending : ShizukuState()
    data object Granted : ShizukuState()
    data object Denied : ShizukuState()
}

/** A single line emitted by a running shell process, tagged by stream. */
data class ProcessLine(val text: String, val isError: Boolean)

/**
 * Thin, coroutine-friendly wrapper around the Shizuku singleton API.
 *
 * Shizuku itself is a process-global singleton (it's backed by static JNI/Binder
 * state, not something you instantiate), so this class holds no mutable Shizuku
 * state of its own — it only adapts callback-based listeners into Kotlin Flow
 * and coroutines, and centralizes the permission request code.
 */
object ShizukuManager {

    private const val REQUEST_CODE = 0x5A11 // arbitrary, must match the listener below

    private val _state = MutableStateFlow(currentState())
    val state: StateFlow<ShizukuState> = _state

    private val binderListener = Shizuku.OnBinderReceivedListener {
        refresh()
    }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _state.value = ShizukuState.ServiceDead
    }
    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != REQUEST_CODE) return@OnRequestPermissionResultListener
            _state.value = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                ShizukuState.Granted
            } else {
                ShizukuState.Denied
            }
        }

    private var listenersAttached = false

    /** Call once, e.g. from Activity.onCreate, before reading [state]. */
    fun attachListeners() {
        if (listenersAttached) return
        listenersAttached = true
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        refresh()
    }

    fun detachListeners() {
        if (!listenersAttached) return
        listenersAttached = false
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }

    /** Re-evaluates the state machine from scratch. Safe to call any time. */
    fun refresh() {
        _state.value = currentState()
    }

    private fun currentState(): ShizukuState {
        return try {
            if (!Shizuku.pingBinder()) {
                ShizukuState.ServiceDead
            } else if (Shizuku.isPreV11()) {
                // Pre-v11 Shizuku predates the runtime permission model entirely;
                // treat it as denied rather than crash on checkSelfPermission.
                ShizukuState.Denied
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ShizukuState.Granted
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                ShizukuState.Denied
            } else {
                ShizukuState.Pending
            }
        } catch (_: Throwable) {
            // Shizuku not installed at all — pingBinder / static init can throw.
            ShizukuState.ServiceDead
        }
    }

    /** Triggers the system permission dialog. Result arrives via [permissionListener]. */
    fun requestPermission() {
        if (Shizuku.isPreV11()) {
            _state.value = ShizukuState.Denied
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            _state.value = ShizukuState.Granted
            return
        }
        Shizuku.requestPermission(REQUEST_CODE)
        _state.value = ShizukuState.Pending
    }

    /**
     * Runs a shell command via `Shizuku.newProcess`, streaming stdout/stderr line-by-line.
     *
     * The blocking work (starting the process, reading streams, `waitFor()`) runs
     * in a child coroutine launched on [Dispatchers.IO]. Deliberately *not* wrapped
     * in `withContext` here: `awaitClose` must be the direct last statement of the
     * `callbackFlow` producer scope, or it throws "awaitClose() can only be invoked
     * from the producer context" — `withContext` installs a different child context,
     * which broke that contract. `launch` instead creates a proper child coroutine
     * of the same producer scope, leaving `awaitClose` untouched as the true last call.
     */
    fun runCommand(command: Array<String>): Flow<ProcessLine> = callbackFlow {
        var process: Process? = null

        val job = launch(Dispatchers.IO) {
            process = try {
                // newProcess(cmd, env, dir) is not declared public in the Shizuku-API
                // library, so Class.getMethod() (public members only) throws
                // NoSuchMethodException here. getDeclaredMethod() finds it regardless
                // of visibility; isAccessible = true bypasses the Java access check
                // at invoke time. This is safe: it's our own bundled library class,
                // not a restricted Android platform API subject to hidden-API rules.
                Shizuku::class.java
                    .getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    .apply { isAccessible = true }
                    .invoke(null, command, null, null) as Process
            } catch (t: Throwable) {
                trySend(ProcessLine("Failed to start process: ${t.message}", isError = true))
                close()
                return@launch
            }

            val activeProcess = process!!
            val stdoutReader = BufferedReader(InputStreamReader(activeProcess.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(activeProcess.errorStream))

            val stdoutThread = Thread {
                try {
                    stdoutReader.forEachLine { line ->
                        trySend(ProcessLine(line, isError = false))
                    }
                } catch (_: Exception) { /* stream closed on process exit — expected */ }
            }.apply { isDaemon = true; start() }

            val stderrThread = Thread {
                try {
                    stderrReader.forEachLine { line ->
                        trySend(ProcessLine(line, isError = true))
                    }
                } catch (_: Exception) { /* stream closed on process exit — expected */ }
            }.apply { isDaemon = true; start() }

            val exitCode = try {
                activeProcess.waitFor()
            } catch (_: InterruptedException) {
                activeProcess.destroy()
                -1
            }

            stdoutThread.join(2_000)
            stderrThread.join(2_000)
            trySend(ProcessLine("Process exited with code $exitCode", isError = exitCode != 0))
            close()
        }

        awaitClose {
            // Collector cancelled (e.g. screen left) before natural completion,
            // or the flow closed normally above — either way, stop the child
            // coroutine and make sure the underlying process isn't left running.
            job.cancel()
            process?.takeIf { it.isAlive }?.destroy()
        }
    }

    /** True if the Shizuku manager/server app is present on the device at all. */
    fun isShizukuInstalled(pm: PackageManager): Boolean {
        return try {
            pm.getPackageInfo("moe.shizuku.privileged.api", 0) != null
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}

/** Thrown when a command is attempted without an active Granted permission state. */
class ShizukuPermissionException(message: String) : CancellationException(message)
