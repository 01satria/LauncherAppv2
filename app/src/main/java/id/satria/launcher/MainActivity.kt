package id.satria.launcher

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import id.satria.launcher.service.EdgeSwipeService
import id.satria.launcher.ui.screen.HomeScreen
import id.satria.launcher.ui.theme.SatriaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val overlayPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        syncEdgeSwipeService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)
        // Keep screen on as launcher
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        hideNavBar()
        vm.refreshApps()
        vm.resetHabitsIfNewDay()
        vm.checkUsagePermission()
        syncEdgeSwipeService()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavBar()
    }

    override fun onDestroy() {
        EdgeSwipeService.stop(this)
        super.onDestroy()
    }

    private fun hideNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { ctrl ->
                ctrl.hide(WindowInsets.Type.navigationBars())
                ctrl.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

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
        )
        runCatching { overlayPermLauncher.launch(intent) }
    }
}

// Created by Satria Bagus
// Github: 01satria
// Website: https://itssatria.vercel.app
