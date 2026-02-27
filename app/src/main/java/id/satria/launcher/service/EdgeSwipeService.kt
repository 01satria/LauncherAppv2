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
 * Pasang View invisible 20dp di tepi kiri layar via TYPE_APPLICATION_OVERLAY.
 * Saat swipe terdeteksi, langsung buka MainActivity dengan action SHOW_RECENTS
 * — ini mem-bring launcher ke foreground SEBELUM menampilkan RecentAppsOverlay,
 * sehingga overlay Compose bisa tampil dengan benar.
 */
class EdgeSwipeService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    companion object {
        const val EDGE_WIDTH_DP = 20
        const val ACTION_SHOW_RECENTS = "id.satria.launcher.SHOW_RECENTS"

        fun start(context: Context) {
            context.startService(Intent(context, EdgeSwipeService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, EdgeSwipeService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
            x = 0
            y = 0
        }

        val view = object : View(this) {
            private var startX = 0f
            private var startY = 0f
            private var triggered = false

            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        triggered = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!triggered) {
                            val dx = event.rawX - startX
                            val dy = event.rawY - startY
                            if (dx >= minSwipePx && abs(dy) < maxDriftPx) {
                                triggered = true
                                launchMainActivityWithRecents()
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        triggered = false
                        return true
                    }
                }
                return false
            }
        }

        overlayView = view
        wm.addView(view, params)
    }

    /**
     * Bawa MainActivity ke foreground dengan membawa extra SHOW_RECENTS=true.
     * MainActivity.onNewIntent() akan membaca extra ini dan set showRecents=true.
     *
     * FLAG_ACTIVITY_REORDER_TO_FRONT: jika launcher sudah running, bring to front
     * FLAG_ACTIVITY_SINGLE_TOP: tidak buat instance baru jika sudah di top
     * FLAG_ACTIVITY_NEW_TASK: wajib saat startActivity dari non-Activity context
     */
    private fun launchMainActivityWithRecents() {
        val intent = Intent(this, Class.forName("id.satria.launcher.MainActivity")).apply {
            action = ACTION_SHOW_RECENTS
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        runCatching { startActivity(intent) }
    }

    private fun removeOverlay() {
        overlayView?.let {
            runCatching { windowManager?.removeView(it) }
            overlayView = null
        }
        windowManager = null
    }
}
