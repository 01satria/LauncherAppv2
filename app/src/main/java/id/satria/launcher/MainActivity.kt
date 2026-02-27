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
<<<<<<< HEAD
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import id.satria.launcher.service.RecentAppsService
=======
>>>>>>> 104b10745aab3a5e701c78931971f864ac06cce6
import id.satria.launcher.ui.screen.HomeScreen
import id.satria.launcher.ui.theme.SatriaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            val darkMode by vm.darkMode.collectAsState()

            SatriaTheme(darkMode = darkMode) {
<<<<<<< HEAD
                HomeScreen(
                    vm = vm,
                    onAppLaunched = { pkg, label ->
                        RecentAppsService.addRecent(pkg, label, this)
                    },
                )
=======
                HomeScreen(vm = vm)
>>>>>>> 104b10745aab3a5e701c78931971f864ac06cce6
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshApps()
        vm.resetHabitsIfNewDay()
    }
}

// Created by Satria Bagus
// Github: 01satria
// Website: https://itssatria.vercel.app