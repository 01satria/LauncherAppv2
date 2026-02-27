package id.satria.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import id.satria.launcher.ui.screen.HomeScreen
import id.satria.launcher.ui.theme.SatriaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    /**
     * BroadcastReceiver untuk menangkap tombol Recent Apps.
     *
     * Ini adalah cara yang dipakai semua launcher (AOSP Launcher3, Nova, dll).
     * Android mengirim ACTION_CLOSE_SYSTEM_DIALOGS dengan reason="recentapps"
     * ke semua app saat tombol Recent ditekan — jauh lebih reliable daripada
     * KEYCODE_APP_SWITCH yang sering dicegat sistem sebelum sampai ke Activity.
     */
    private val recentAppsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                val reason = intent.getStringExtra("reason") ?: ""
                Log.d("Launcher", "CLOSE_SYSTEM_DIALOGS reason=$reason")
                if (reason == "recentapps" || reason == "assist") {
                    vm.onRecentAppsButtonPressed()
                }
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
        @Suppress("UnspecifiedRegisterReceiverFlag")
        registerReceiver(recentAppsReceiver, IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(recentAppsReceiver) }
    }

    // Fallback untuk device yang memang mengirim KEYCODE_APP_SWITCH ke Activity
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            // Hanya trigger di repeatCount 0 — BroadcastReceiver sudah jadi primary handler
            // ini hanya backup untuk ROM yang tidak kirim CLOSE_SYSTEM_DIALOGS
            if (event?.repeatCount == 0) vm.onRecentAppsButtonPressed()
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
