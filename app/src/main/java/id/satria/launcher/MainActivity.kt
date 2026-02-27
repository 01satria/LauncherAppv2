package id.satria.launcher

import android.graphics.Color
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle  = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            val darkMode by vm.darkMode.collectAsState()
            SatriaTheme(darkMode = darkMode) { HomeScreen(vm = vm) }
        }
    }

    // ── Intercept tombol Recent (KEYCODE_APP_SWITCH) ──────────────────────────
    // Launcher (HOME category) adalah satu-satunya app yang bisa intercept key ini.
    // Perlu override KEDUANYA — onKeyDown dan onKeyUp — karena device yang berbeda
    // mengirim event di momen yang berbeda. Juga override dispatchKeyEvent sebagai
    // fallback untuk device yang bypass onKeyDown/Up.

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            // Jangan trigger dua kali jika ini repeat event (tombol ditahan)
            if (event?.repeatCount == 0) {
                vm.onRecentAppsButtonPressed()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true  // Konsumsi event agar sistem tidak buka Overview-nya
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            if (event.action == KeyEvent.ACTION_UP) {
                vm.onRecentAppsButtonPressed()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
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
