package com.borgorninja.androidguardian

import android.app.Application
import rikka.shizuku.Shizuku

class AndroidGuardianApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Suppress the "app doesn't have permission" toast Shizuku shows by default
        // when we deliberately probe permission state ourselves at startup.
        Shizuku.addBinderReceivedListenerSticky(Shizuku.OnBinderReceivedListener {
            // Binder alive — ShizukuManager listeners (registered per-Activity) react to this.
        })
    }
}
