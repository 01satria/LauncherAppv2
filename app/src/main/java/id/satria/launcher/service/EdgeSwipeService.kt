package id.satria.launcher.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

/**
 * EdgeSwipeService
 *
 * View invisible 20dp di tepi kiri layar.
 * Saat swipe terdeteksi → langsung show RecentAppsOverlayService
 * (tidak perlu bawa launcher ke foreground).
 */
class EdgeSwipeService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    companion object {
        const val EDGE_WIDTH_DP = 20

        fun start(context: Context) {
            context.startService(Intent(context, EdgeSwipeService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, EdgeSwipeService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Called on every startService() — only do setup once (onCreate handles first call)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        setupOverlay()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun setupOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val density = resources.displayMetrics.density
        val edgeWidthPx = (EDGE_WIDTH_DP * density).toInt()
        val minSwipePx  = (55 * density).toInt()
        val maxDriftPx  = (80 * density).toInt()

        val params = WindowManager.LayoutParams(
            edgeWidthPx,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.START or Gravity.TOP
        }

        val view = object : View(this) {
            private var startX = 0f
            private var startY = 0f
            // Per-gesture trigger flag — reset only on ACTION_DOWN (new finger down)
            private var triggeredThisGesture = false
            // Cooldown flag — prevents re-trigger while overlay is animating/showing
            // Reset by RecentAppsOverlayService calling notifyDismissed()
            @Volatile var overlayShowing = false

            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        triggeredThisGesture = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Guard: only trigger once per gesture AND only if no overlay showing
                        if (!triggeredThisGesture && !overlayShowing) {
                            val dx = event.rawX - startX
                            val dy = event.rawY - startY
                            if (dx >= minSwipePx && abs(dy) < maxDriftPx) {
                                triggeredThisGesture = true
                                overlayShowing = true
                                RecentAppsOverlayService.show(applicationContext)
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // Do NOT reset triggeredThisGesture here — it resets on next DOWN.
                        // This prevents double-fire if user lifts finger slowly after trigger.
                        return true
                    }
                }
                return false
            }
        }

        // Give RecentAppsOverlayService a reference to clear the cooldown on dismiss
        RecentAppsOverlayService.edgeView = view

        overlayView = view
        wm.addView(view, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            runCatching { windowManager?.removeView(it) }
            overlayView = null
        }
        windowManager = null
    }
}
