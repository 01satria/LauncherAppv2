package id.satria.launcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
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

/**
 * RecentAppsService — Foreground Service + SYSTEM_ALERT_WINDOW.
 *
 * TIDAK butuh Accessibility Service → tidak ada "Restricted Setting".
 * Hanya butuh izin "Tampilkan di atas aplikasi lain" yang bisa
 * diaktifkan langsung tanpa batasan di semua Android.
 *
 * Cara kerja (mirip One Hand Operation+):
 *  - Strip transparan tipis dipasang di tepi BAWAH layar via WindowManager
 *  - Swipe UP dari strip → panel recent apps muncul
 *  - RAM minimal: strip hanya View kosong, tidak ada polling/timer
 */
class RecentAppsService : Service() {

    companion object {
        const val ACTION_START = "id.satria.launcher.RECENT_START"
        const val ACTION_STOP  = "id.satria.launcher.RECENT_STOP"
        private const val CHANNEL_ID  = "satria_recent_channel"
        private const val NOTIF_ID    = 7788
        private const val MAX_RECENT  = 8

        // List recent apps — diisi dari luar saat user launch app
        val recentPackages = mutableStateListOf<RecentAppEntry>()

        fun addRecent(pkg: String, label: String, context: Context) {
            if (pkg == context.packageName) return
            recentPackages.removeAll { it.packageName == pkg }
            recentPackages.add(0, RecentAppEntry(pkg, label))
            if (recentPackages.size > MAX_RECENT) {
                recentPackages.removeAt(recentPackages.size - 1)
            }
        }
    }

    private val serviceScope  = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wm: WindowManager? = null
    private var stripView: View? = null
    private var panelView: View? = null
    private var touchStartY = 0f

    // Minimal LifecycleOwner agar ComposeView bisa render di luar Activity
    private val lifecycleOwner = SimpleLifecycleOwner()
    private val savedStateOwner = SimpleSavedStateOwner(lifecycleOwner)

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.start()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> addStrip()
            ACTION_STOP  -> { removeStrip(); removePanel(); stopSelf() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeStrip()
        removePanel()
        lifecycleOwner.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Gesture Strip ─────────────────────────────────────────────────────

    private fun addStrip() {
        if (stripView != null) return
        val density  = resources.displayMetrics.density
        val swipePx  = 50 * density   // minimal 50dp swipe ke atas untuk trigger

        val strip = View(this)
        strip.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { touchStartY = event.rawY; true }
                MotionEvent.ACTION_UP   -> {
                    if ((touchStartY - event.rawY) >= swipePx) showPanel()
                    true
                }
                else -> false
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (8 * density).toInt(),          // strip 8dp, hampir tidak terlihat
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.START

        try { wm?.addView(strip, params); stripView = strip }
        catch (_: Exception) {}
    }

    private fun removeStrip() {
        stripView?.let { runCatching { wm?.removeView(it) } }
        stripView = null
    }

    // ── Overlay Panel ─────────────────────────────────────────────────────

    private fun showPanel() {
        if (panelView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM

        val pm = packageManager
        val composeView = ComposeView(this)
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(savedStateOwner)
        composeView.setContent {
            RecentAppsOverlay(
                recentPackages = recentPackages,
                onDismiss      = { removePanel() },
                onLaunch       = { pkg: String ->
                    removePanel()
                    pm.getLaunchIntentForPackage(pkg)?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(it)
                    }
                },
                onClearAll     = { recentPackages.clear(); removePanel() },
                packageManager = pm,
            )
        }

        try { wm?.addView(composeView, params); panelView = composeView }
        catch (_: Exception) {}
    }

    private fun removePanel() {
        panelView?.let { runCatching { wm?.removeView(it) } }
        panelView = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun overlayType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Recent Apps", NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recent Apps aktif")
            .setContentText("Swipe atas dari tepi bawah layar")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
}

// ── Data ──────────────────────────────────────────────────────────────────────

data class RecentAppEntry(val packageName: String, val label: String)

// ── Minimal LifecycleOwner untuk ComposeView di luar Activity ─────────────────

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

private class SimpleSavedStateOwner(lo: LifecycleOwner) : SavedStateRegistryOwner {
    override val lifecycle: Lifecycle = lo.lifecycle
    private val ctrl = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = ctrl.savedStateRegistry
    init { ctrl.performAttach(); ctrl.performRestore(null) }
}
