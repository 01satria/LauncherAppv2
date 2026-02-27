package id.satria.launcher

import android.content.Intent
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
import id.satria.launcher.recents.RecentAppsEvent
import id.satria.launcher.ui.screen.HomeScreen
import id.satria.launcher.ui.theme.SatriaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private val TAG = "MainActivity"

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

    /**
     * onNewIntent dipanggil saat launcher sudah running dan dibawa ke foreground.
     * Ini terjadi ketika user menekan tombol Home atau Recent dari app lain.
     *
     * Jika intent ACTION_MAIN + CATEGORY_HOME, berarti tombol Home ditekan.
     * Jika ada extra "show_recents" = true, berarti dari tombol Recent.
     *
     * Catatan: AccessibilityService sudah handle deteksi via WINDOW_STATE_CHANGED.
     * onNewIntent ini adalah fallback tambahan.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent action=${intent.action} extras=${intent.extras}")

        // Beberapa launcher custom menggunakan intent extra untuk signal recents
        if (intent.getBooleanExtra("show_recents", false)) {
            RecentAppsEvent.fire()
        }
    }

    /**
     * Fallback untuk 3-button navigation: tangkap KEYCODE_APP_SWITCH
     * saat launcher sedang di foreground (misal user klik Recent dari launcher itu sendiri).
     * Untuk kasus user sedang di app lain, AccessibilityService yang handle.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH && event?.repeatCount == 0) {
            Log.d(TAG, "KEYCODE_APP_SWITCH via onKeyDown (launcher di foreground)")
            RecentAppsEvent.fire()
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
