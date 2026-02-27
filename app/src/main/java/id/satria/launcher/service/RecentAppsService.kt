package id.satria.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.view.View
import androidx.compose.ui.platform.ComposeView
import id.satria.launcher.data.Prefs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * RecentAppsService — AccessibilityService ringan untuk fitur Recent Apps.
 * Hanya aktif jika user mengaktifkan toggle di Settings.
 * RAM minimal: hanya daftar recent packages disimpan di memori (max 8 entri).
 * Overlay ditampilkan di atas semua aplikasi via WindowManager + ComposeView.
 */
class RecentAppsService : AccessibilityService() {

    companion object {
        // Singleton state agar launcher bisa update list recent apps
        val recentPackages = mutableStateListOf<RecentAppEntry>()
        private const val MAX_RECENT = 8

        var isOverlayVisible = false
            private set

        // Dipanggil dari MainActivity/HomeScreen setiap kali user launch app
        fun addRecent(pkg: String, label: String, context: Context) {
            // Jangan tambahkan launcher sendiri
            if (pkg == context.packageName) return
            recentPackages.removeAll { it.packageName == pkg }
            recentPackages.add(0, RecentAppEntry(pkg, label))
            if (recentPackages.size > MAX_RECENT) {
                recentPackages.removeAt(recentPackages.size - 1)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isEnabled = false

    // LifecycleOwner minimal agar ComposeView bisa render
    private val lifecycleOwner = SimpleLifecycleOwner()
    private val savedStateOwner = SimpleSavedStateOwner(lifecycleOwner)

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.start()

        // Cek apakah fitur aktif di preferences
        serviceScope.launch {
            val prefs = Prefs(applicationContext)
            prefs.recentAppsEnabled.collect { enabled ->
                isEnabled = enabled
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isEnabled) return
        val type = event?.eventType ?: return

        // Deteksi tombol recent atau gesture recent
        if (type == AccessibilityEvent.TYPE_GESTURE_DETECTION_START) {
            // Gesture recents — pada beberapa gesture nav, ini adalah swipe up & hold
            // Kita handle via ACTION_RECENTS di bawah
        }
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onGesture(gestureId: Int): Boolean {
        if (!isEnabled) return false
        // Override gesture recents — gesture ID 15 = GESTURE_2_FINGER_SWIPE_UP (varies by OEM)
        // Cara paling reliable: kita override di onKeyEvent lewat AccessibilityService.onKeyEvent
        return false
    }

    override fun onKeyEvent(event: android.view.KeyEvent?): Boolean {
        if (!isEnabled) return false
        if (event == null) return false
        if (event.action != android.view.KeyEvent.ACTION_UP) return false

        return if (event.keyCode == android.view.KeyEvent.KEYCODE_APP_SWITCH) {
            // Tombol recent ditekan!
            if (isOverlayVisible) {
                hideOverlay()
            } else {
                showOverlay()
            }
            true // consume event agar sistem tidak buka recents bawaan
        } else {
            false
        }
    }

    override fun onDestroy() {
        hideOverlay()
        lifecycleOwner.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Overlay ──────────────────────────────────────────────────────────────

    private fun showOverlay() {
        if (overlayView != null) return
        isOverlayVisible = true

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(savedStateOwner)
            setContent {
                id.satria.launcher.ui.overlay.RecentAppsOverlay(
                    recentPackages = recentPackages,
                    onDismiss = { hideOverlay() },
                    onLaunch = { pkg ->
                        hideOverlay()
                        val intent = packageManager.getLaunchIntentForPackage(pkg)
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    },
                    onClearAll = {
                        recentPackages.clear()
                        hideOverlay()
                    },
                    packageManager = packageManager,
                )
            }
        }

        overlayView = composeView
        try {
            windowManager?.addView(composeView, params)
        } catch (e: Exception) {
            overlayView = null
            isOverlayVisible = false
        }
    }

    fun hideOverlay() {
        val view = overlayView ?: return
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {}
        overlayView = null
        isOverlayVisible = false
    }
}

// ── Data class ───────────────────────────────────────────────────────────────

data class RecentAppEntry(
    val packageName: String,
    val label: String,
)

// ── Minimal LifecycleOwner untuk ComposeView di luar Activity ────────────────

private class SimpleLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    fun start() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

private class SimpleSavedStateOwner(lifecycleOwner: LifecycleOwner) : SavedStateRegistryOwner {
    override val lifecycle: Lifecycle = lifecycleOwner.lifecycle
    private val controller = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
    init { controller.performAttach(); controller.performRestore(null) }
}
