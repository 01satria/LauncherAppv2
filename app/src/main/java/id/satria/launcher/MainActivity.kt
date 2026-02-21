package id.satria.launcher

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
        setContent {
            SatriaTheme {
                HomeScreen(vm = vm)
            }
        }
    }

    // Refresh list setiap kali launcher kembali ke foreground
    // (menangkap install/uninstall yang terjadi di background)
    override fun onResume() {
        super.onResume()
        vm.refreshApps()
    }
}
