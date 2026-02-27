package id.satria.launcher

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import id.satria.launcher.service.EdgeSwipeService
import id.satria.launcher.ui.screen.HomeScreen
import id.satria.launcher.ui.theme.SatriaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContent {
            val darkMode by vm.darkMode.collectAsState()
            SatriaTheme(darkMode = darkMode) {
                HomeScreen(
                    vm = vm,
                    onRequestOverlayPermission = { openOverlayPermissionSettings() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshApps()
        vm.resetHabitsIfNewDay()
        vm.checkUsagePermission()
        // Coba start EdgeSwipeService jika fitur aktif dan permission ada
        syncEdgeSwipeService()
    }

    override fun onPause() {
        super.onPause()
        // Jangan stop service saat pause — justru kita butuh service berjalan
        // saat launcher di background agar swipe bekerja di atas app lain
    }

    override fun onDestroy() {
        // Stop service hanya saat launcher benar-benar destroyed
        if (vm.recentAppsEnabled.value) {
            EdgeSwipeService.stop(this)
        }
        super.onDestroy()
    }

    /** Start/stop EdgeSwipeService berdasarkan state recentAppsEnabled + overlay permission */
    fun syncEdgeSwipeService() {
        if (vm.recentAppsEnabled.value && hasOverlayPermission()) {
            EdgeSwipeService.start(this)
        } else {
            EdgeSwipeService.stop(this)
        }
    }

    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(this)

    private fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { startActivity(intent) }
    }
}

// Created by Satria Bagus
// Github: 01satria
// Website: https://itssatria.vercel.app
