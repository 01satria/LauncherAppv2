package id.satria.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import id.satria.launcher.data.Prefs
import id.satria.launcher.ui.overlay.RecentAppsOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RecentAppsService : AccessibilityService() {

    companion object {
        val recentPackages = mutableStateListOf<RecentAppEntry>()
        private const val MAX_RECENT = 8

        var isOverlayVisible = false
            private set

        fun addRecent(pkg: String, label: String, context: Context) {
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

    private val lifecycleOwner = SimpleLifecycleOwner()
    private val savedStateOwner = SimpleSavedStateOwner(lifecycleOwner)

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.start()

        serviceScope.launch {
            val prefs = Prefs(applicationContext)
            prefs.recentAppsEnabled.collect { enabled ->
                isEnabled = enabled
                if (!enabled && isOverlayVisible) hideOverlay()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (!isEnabled) return false
        if (event == null) return false
        if (event.action != KeyEvent.ACTION_UP) return false

        return if (event.keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            if (isOverlayVisible) hideOverlay() else showOverlay()
            true
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

        val pm = packageManager
        val composeView = ComposeView(this)
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(savedStateOwner)
        composeView.setContent {
            RecentAppsOverlay(
                recentPackages = recentPackages,
                onDismiss = { hideOverlay() },
                onLaunch = { pkg: String ->
                    hideOverlay()
                    val intent = pm.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                },
                onClearAll = {
                    recentPackages.clear()
                    hideOverlay()
                },
                packageManager = pm,
            )
        }

        overlayView = composeView
        try {
            windowManager?.addView(composeView, params)
        } catch (e: Exception) {
            overlayView = null
            isOverlayVisible = false
        }
    }

    private fun hideOverlay() {
        val view = overlayView ?: return
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {}
        overlayView = null
        isOverlayVisible = false
    }
}

data class RecentAppEntry(
    val packageName: String,
    val label: String,
)

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
    init {
        controller.performAttach()
        controller.performRestore(null)
    }
}
