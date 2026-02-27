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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import id.satria.launcher.ui.screen.HomeScreen
import id.satria.launcher.ui.theme.SatriaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge + transparent bar
        enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Navigation bar tetap tampil (transparent) agar tombol Back/Home/Recent
        // bisa dipakai user. Launcher intercept tombol Recent via onKeyDown().
        // Gunakan edge-to-edge sehingga konten tetap full-screen di balik nav bar.
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            val darkMode by vm.darkMode.collectAsState()
            SatriaTheme(darkMode = darkMode) { HomeScreen(vm = vm) }
        }
    }

    /**
     * Intercept tombol Recent Apps (KEYCODE_APP_SWITCH).
     * Android hanya mengirim event ini ke app yang terdaftar sebagai HOME launcher.
     * Tanpa override ini, Android akan membuka sistem Overview screen sendiri.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            vm.onRecentAppsButtonPressed()
            return true  // konsumsi event — sistem tidak buka Overview-nya sendiri
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        vm.refreshApps()
        vm.resetHabitsIfNewDay()
        // Cek ulang usage stats permission (user mungkin baru saja grant dari Settings)
        vm.checkUsagePermission()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
    }
}

// Created by Satria Bagus
// Github: 01satria
// Website: https://itssatria.vercel.app
