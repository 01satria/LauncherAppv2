package id.satria.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import id.satria.launcher.service.SatriaAccessibilityService
import id.satria.launcher.ui.screen.HomeScreen
import id.satria.launcher.ui.theme.SatriaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    /**
     * Receiver lokal (dalam package sendiri) — menerima broadcast dari
     * SatriaAccessibilityService saat tombol Recent ditekan.
     * Tidak crash karena ini internal broadcast, bukan sistem broadcast.
     */
    private val recentAppsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == SatriaAccessibilityService.ACTION_RECENT_APPS) {
                vm.onRecentAppsButtonPressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            val darkMode by vm.darkMode.collectAsState()
            SatriaTheme(darkMode = darkMode) { HomeScreen(vm = vm) }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(SatriaAccessibilityService.ACTION_RECENT_APPS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(recentAppsReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(recentAppsReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(recentAppsReceiver) }
    }

    // Fallback: beberapa ROM mengirim KEYCODE_APP_SWITCH langsung ke Activity
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH && event?.repeatCount == 0) {
            vm.onRecentAppsButtonPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        vm.refreshApps()
        vm.resetHabitsIfNewDay()
        vm.checkUsagePermission()
    }
}

// Created by Satria Bagus
// Github: 01satria
// Website: https://itssatria.vercel.app
