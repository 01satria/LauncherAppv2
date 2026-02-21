package id.satria.launcher

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import id.satria.launcher.ui.screen.HomeScreen
import id.satria.launcher.ui.theme.SatriaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Transparent window agar wallpaper terlihat di belakang launcher ──
        window.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            // Pastikan status bar & nav bar juga transparan
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }

        setContent {
            SatriaTheme {
                HomeScreen(vm = vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshApps()
    }
}