package com.alquran.noor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.alquran.noor.ui.AppRoot
import com.alquran.noor.ui.AppViewModel

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels { AppViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // أبقِ شاشة البداية حتى تُقرأ الإعدادات وحالة المصحف لتجنّب وميض المظهر
        splash.setKeepOnScreenCondition {
            appViewModel.settings.value == null || appViewModel.ready.value == null
        }

        setContent { AppRoot(appViewModel) }
    }

    override fun onStart() {
        super.onStart()
        (application as QuranApp).container.player.connect()
    }
}
