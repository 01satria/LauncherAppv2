package id.satria.launcher

import android.graphics.Color
import android.os.Bundle
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

        // Navigation Bar otomatis tersembunyi setelah beberapa detik
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())

        // (Opsional) Hide status bar juga
        // controller.hide(WindowInsetsCompat.Type.statusBars())

        setContent {
            val darkMode by vm.darkMode.collectAsState()

            SatriaTheme(darkMode = darkMode) { HomeScreen(vm = vm) }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshApps()
        vm.resetHabitsIfNewDay()

        // Pastikan tetap immersive saat resume
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}

// Created by Satria Bagus
// Github: 01satria
// Website: https://itssatria.vercel.app
