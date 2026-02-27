package id.satria.launcher.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import id.satria.launcher.data.Prefs
import id.satria.launcher.recents.CustomRecentsPanelActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SatriaAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var prefs: Prefs
    private var isCustomRecentsEnabled = true

    private lateinit var windowManager: WindowManager
    private lateinit var edgeView: View

    @SuppressLint("ClickableViewAccessibility")
    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        setupEdgeView()

        scope.launch {
            prefs.enableCustomRecents.collect { enabled ->
                isCustomRecentsEnabled = enabled
                updateEdgeViewVisibility()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupEdgeView() {
        edgeView = FrameLayout(this)
        // Background transparan, hanya sebagai trigger
        edgeView.setBackgroundColor(0x00000000)

        val widthPx = (24 * resources.displayMetrics.density).toInt()
        val params =
                WindowManager.LayoutParams(
                                widthPx,
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                                PixelFormat.TRANSLUCENT
                        )
                        .apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }

        var startX = 0f
        var isSwiping = false

        edgeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    isSwiping = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dX = event.rawX - startX
                    // Deteksi swipe ke kiri (misal threshold 40px)
                    if (!isSwiping && dX < -40) {
                        isSwiping = true
                        android.util.Log.d("SatriaAccessibility", "Swipe gesture detected, opening custom panel")
                        openCustomRecentsPanel()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isSwiping = false
                    true
                }
                else -> false
            }
        }

        // Add view awal
        try {
            windowManager.addView(edgeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateEdgeViewVisibility() {
        if (!::edgeView.isInitialized) return

        if (isCustomRecentsEnabled) {
            edgeView.visibility = View.VISIBLE
        } else {
            edgeView.visibility = View.GONE
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, but required to override
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isCustomRecentsEnabled) {
            return super.onKeyEvent(event)
        }

        val action = event.action
        val keyCode = event.keyCode

        // Intercept Recent Apps key (Jika device menggunakan 3-button navigation)
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH && action == KeyEvent.ACTION_UP) {
            android.util.Log.d("SatriaAccessibility", "Recent apps button pressed, opening custom panel")
            openCustomRecentsPanel()
            return true // Consume the event so the default system panel doesn't show
        }

        return super.onKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        if (::edgeView.isInitialized && edgeView.isAttachedToWindow) {
            windowManager.removeView(edgeView)
        }
    }

    private fun openCustomRecentsPanel() {
        val intent =
                Intent(this, CustomRecentsPanelActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        startActivity(intent)
    }
}
