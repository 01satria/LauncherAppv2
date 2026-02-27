package id.satria.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import id.satria.launcher.recents.CustomRecentsPanelActivity
import id.satria.launcher.ui.screen.HomeScreen
import id.satria.launcher.ui.theme.SatriaTheme
import kotlin.math.abs

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

        // 🔥 Navigation Bar otomatis tersembunyi setelah beberapa detik
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

    // Gesture Recents Panel logic
    private var touchStartX = 0f
    private var touchStartY = 0f
    private val SWIPE_THRESHOLD = 50f
    private val EDGE_WIDTH_RATIO = 0.15f // Trigger gesture only on right 15% edge
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Only trigger if enabled
        if (!vm.enableCustomRecents.value) return super.dispatchTouchEvent(ev)

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = ev.rawX
                touchStartY = ev.rawY
            }
            MotionEvent.ACTION_UP -> {
                val touchEndX = ev.rawX
                val touchEndY = ev.rawY
                val deltaX = touchStartX - touchEndX
                val deltaY = touchStartY - touchEndY

                val screenWidth = resources.displayMetrics.widthPixels
                val edgeWidth = screenWidth * EDGE_WIDTH_RATIO

                // Check swipe from right edge towards left
                if (touchStartX >= (screenWidth - edgeWidth) &&
                                deltaX > SWIPE_THRESHOLD &&
                                abs(deltaX) > abs(deltaY)
                ) {
                    val intent = Intent(this, CustomRecentsPanelActivity::class.java)
                    startActivity(intent)
                    // Let super handle it too, but we successfully intercepted our custom gesture
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}

// Created by Satria Bagus
// Github: 01satria
// Website: https://itssatria.vercel.app
