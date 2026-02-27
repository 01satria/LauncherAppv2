package id.satria.launcher

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import id.satria.launcher.recents.EdgeSwipeEvent
import id.satria.launcher.service.EdgeSwipeService
import id.satria.launcher.ui.screen.HomeScreen
import id.satria.launcher.ui.theme.SatriaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    /**
     * Launcher untuk membuka Settings overlay permission.
     * Menggunakan ActivityResultLauncher agar properly track result
     * dan sync service setelah user kembali dari Settings.
     */
    private val overlayPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // User kembali dari Settings — sync service sesuai state permission terbaru
        syncEdgeSwipeService()
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
            SatriaTheme(darkMode = darkMode) {
                HomeScreen(
                    vm = vm,
                    onRequestOverlayPermission = { openOverlayPermissionSettings() },
                )
            }
        }

        // Handle intent dari onCreate (app cold start dengan intent SHOW_RECENTS)
        handleIntent(intent)
    }

    /**
     * onNewIntent dipanggil saat MainActivity sudah running dan menerima intent baru.
     * Kasus utama: EdgeSwipeService mengirim ACTION_SHOW_RECENTS saat swipe terdeteksi
     * di atas app lain → launcher di-bring ke foreground → overlay ditampilkan.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == EdgeSwipeService.ACTION_SHOW_RECENTS) {
            // Kirim event ke HomeScreen untuk tampilkan RecentAppsOverlay
            EdgeSwipeEvent.fire()
            // Bersihkan action agar tidak re-trigger saat Activity recreated
            setIntent(intent.apply { action = null })
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshApps()
        vm.resetHabitsIfNewDay()
        vm.checkUsagePermission()
        syncEdgeSwipeService()
    }

    override fun onDestroy() {
        EdgeSwipeService.stop(this)
        super.onDestroy()
    }

    fun syncEdgeSwipeService() {
        if (vm.recentAppsEnabled.value && hasOverlayPermission()) {
            EdgeSwipeService.start(this)
        } else {
            EdgeSwipeService.stop(this)
        }
    }

    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(this)

    /**
     * Buka Settings overlay permission dengan ActivityResultLauncher.
     * Ini memastikan Intent benar-benar terbuka (tidak silent fail seperti startActivity biasa
     * dari Compose context) dan sync service otomatis saat user kembali.
     */
    private fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        runCatching { overlayPermLauncher.launch(intent) }
    }
}

// Created by Satria Bagus
// Github: 01satria
// Website: https://itssatria.vercel.app
