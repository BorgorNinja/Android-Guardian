package com.borgorninja.androidguardian.data

/**
 * Parsed representation of the `dexopt` block from
 * `dumpsys package <pkg> | grep -E "dexopt"`.
 *
 * ART reports the *filter* actually applied, which can silently differ from
 * what was requested (e.g. requesting `speed` on a package with no dex code
 * to compile falls back to `run-from-apk` or is skipped entirely). We surface
 * the raw filter string alongside a coarse enum so the UI can show both a
 * quick badge and the exact value for debugging.
 */
sealed class CompileStatus(val label: String) {
    data object Unknown : CompileStatus("Unknown")
    data object InProgress : CompileStatus("Optimizing…")
    data object NotCompiled : CompileStatus("run-from-apk")
    data class Speed(val raw: String) : CompileStatus("speed")
    data class SpeedProfile(val raw: String) : CompileStatus("speed-profile")
    data class Verify(val raw: String) : CompileStatus("verify")
    data class Other(val raw: String) : CompileStatus(raw)
    data class Error(val message: String) : CompileStatus("Error")

    companion object {
        private val filterRegex = Regex("""\[status=([a-zA-Z0-9_\-]+)""")

        /**
         * Parses a single `dexopt` line, e.g.:
         *   arm64: [status=speed] [reason=install] [primary-abi]
         */
        fun parse(dumpsysBlock: String): CompileStatus {
            if (dumpsysBlock.isBlank()) return NotCompiled
            val match = filterRegex.find(dumpsysBlock) ?: return Other(dumpsysBlock.trim().take(64))
            return when (val filter = match.groupValues[1]) {
                "speed" -> Speed(filter)
                "speed-profile" -> SpeedProfile(filter)
                "verify" -> Verify(filter)
                "run-from-apk", "unknown" -> NotCompiled
                else -> Other(filter)
            }
        }
    }
}
