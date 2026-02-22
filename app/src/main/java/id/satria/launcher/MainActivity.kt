package id.satria.launcher

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
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
        enableEdgeToEdge()

        window.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            statusBarColor    = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }

        setContent {
            val accentHex by vm.themeAccent.collectAsState()
            val bgHex     by vm.themeBg.collectAsState()
            val borderHex by vm.themeBorder.collectAsState()
            val fontHex   by vm.themeFont.collectAsState()

            SatriaTheme(
                accentHex = accentHex,
                bgHex     = bgHex,
                borderHex = borderHex,
                fontHex   = fontHex,
            ) {
                HomeScreen(vm = vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshApps()
        vm.resetHabitsIfNewDay()
    }
}
