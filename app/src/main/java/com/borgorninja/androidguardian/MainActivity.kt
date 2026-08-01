package com.borgorninja.androidguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.borgorninja.androidguardian.shizuku.ShizukuManager
import com.borgorninja.androidguardian.ui.navigation.AndroidGuardianNavGraph
import com.borgorninja.androidguardian.ui.theme.AndroidGuardianTheme
import com.borgorninja.androidguardian.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AndroidGuardianTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AndroidGuardianNavGraph(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Shizuku permission grants happen in a separate system dialog process;
        // re-check on resume in case the user granted/revoked it while backgrounded.
        ShizukuManager.refresh()
    }
}
