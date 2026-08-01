package com.borgorninja.androidguardian.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.borgorninja.androidguardian.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    /**
     * Lists installed applications. Runs on Dispatchers.IO since
     * PackageManager.getInstalledApplications is a binder call that can take
     * tens of milliseconds with hundreds of packages installed.
     */
    suspend fun listInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    label = runCatching { pm.getApplicationLabel(appInfo).toString() }
                        .getOrDefault(appInfo.packageName),
                    icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull(),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    versionName = runCatching {
                        pm.getPackageInfo(appInfo.packageName, 0).versionName
                    }.getOrNull()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Runs `dumpsys package <pkg> | grep -E "dexopt"` via Shizuku and parses the
     * first matching status line. We grep server-side rather than filtering the
     * full dumpsys output in-process because a single package's dumpsys block
     * can run to hundreds of lines on system packages with multiple splits.
     */
    suspend fun queryCompileStatus(packageName: String): CompileStatus {
        val output = StringBuilder()
        var sawError = false
        ShizukuManager.runCommand(
            arrayOf("sh", "-c", "dumpsys package $packageName | grep -E \"dexopt|status=\"")
        ).collect { line ->
            if (line.isError) sawError = true
            output.appendLine(line.text)
        }
        if (sawError && output.isBlank()) {
            return CompileStatus.Error("dumpsys query failed")
        }
        return CompileStatus.parse(output.toString())
    }
}
