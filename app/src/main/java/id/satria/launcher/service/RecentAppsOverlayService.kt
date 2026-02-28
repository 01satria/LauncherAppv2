package id.satria.launcher.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.*
import android.animation.*
import androidx.compose.ui.graphics.asAndroidBitmap
import id.satria.launcher.data.AppData
import id.satria.launcher.data.LauncherRepository
import id.satria.launcher.recents.RecentAppsManager
import id.satria.launcher.ui.component.iconCache
import kotlinx.coroutines.*

class RecentAppsOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var rootView: FrameLayout? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var recentsManager: RecentAppsManager
    private lateinit var repo: LauncherRepository

    // Theme colors — loaded synchronously from SharedPreferences (no DataStore conflict)
    private var colorSurface       = Color.parseColor("#1C1C1E")
    private var colorSurfaceMid    = Color.parseColor("#2C2C2E")
    private var colorAccent        = Color.parseColor("#27AE60")
    private var colorTextPrimary   = Color.WHITE
    private var colorTextSecondary = Color.parseColor("#8E8E93")
    private var colorScrimAlpha    = 160
    private var colorHandle        = Color.argb(80, 200, 200, 200)
    private var colorBorder        = Color.parseColor("#2C2C2E")

    companion object {
        fun show(context: Context) {
            context.startService(Intent(context, RecentAppsOverlayService::class.java))
        }

        // Reference to EdgeSwipeService view — used to reset the cooldown flag on dismiss
        @Volatile var edgeView: Any? = null
    }

    private var isMounted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        recentsManager = RecentAppsManager(applicationContext)
        repo = LauncherRepository(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Guard: if overlay already showing, ignore repeated startService() calls
        if (isMounted) return START_NOT_STICKY
        isMounted = true
        loadDarkModeAndShow()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        removeOverlay()
        // Reset EdgeSwipeService cooldown so next swipe works
        try {
            val v = edgeView
            if (v != null) {
                val f = v.javaClass.getDeclaredField("overlayShowing")
                f.isAccessible = true
                f.setBoolean(v, false)
            }
        } catch (_: Exception) {}
        edgeView = null
        super.onDestroy()
    }

    // Read dark_mode from the DataStore-backed SharedPreferences file directly.
    // We intentionally avoid creating a new DataStore instance here — that would
    // conflict with the one in Prefs.kt and cause a crash.
    // DataStore writes preferences to: datastore/satria_prefs.preferences_pb
    // We use the DataStore API on the application context (same singleton).
    private fun loadDarkModeAndShow() {
        scope.launch {
            val isDark = withContext(Dispatchers.IO) { readDarkModePref() }
            applyTheme(isDark)
            val allApps = async(Dispatchers.IO) { repo.getInstalledApps() }
            recentsManager.loadRecentApps(limit = 10)
            val apps = allApps.await()
            val recentPkgs = recentsManager.recentPackages.value
            val recentApps = recentPkgs
                .mapNotNull { pkg -> apps.find { it.packageName == pkg } }
                .take(10)
            mountOverlay(recentApps, recentsManager.hasPermission())
        }
    }

    // Read dark mode synchronously from SharedPreferences mirror.
    // Prefs.kt writes here whenever dark mode changes (see setDarkMode).
    // This avoids any DataStore import conflict.
    private fun readDarkModePref(): Boolean =
        id.satria.launcher.LauncherApp.get()
            ?.uiPrefs()
            ?.getBoolean(id.satria.launcher.LauncherApp.KEY_DARK_MODE, true)
            ?: true

    private fun applyTheme(isDark: Boolean) {
        if (isDark) {
            colorSurface       = Color.parseColor("#1C1C1E")
            colorSurfaceMid    = Color.parseColor("#2C2C2E")
            colorAccent        = Color.parseColor("#27AE60")
            colorTextPrimary   = Color.WHITE
            colorTextSecondary = Color.parseColor("#8E8E93")
            colorScrimAlpha    = 160
            colorHandle        = Color.argb(80, 200, 200, 200)
            colorBorder        = Color.parseColor("#2C2C2E")
        } else {
            colorSurface       = Color.parseColor("#FFFFFF")
            colorSurfaceMid    = Color.parseColor("#E5E5EA")
            colorAccent        = Color.parseColor("#1E8449")
            colorTextPrimary   = Color.parseColor("#1C1C1E")
            colorTextSecondary = Color.parseColor("#6D6D72")
            colorScrimAlpha    = 120
            colorHandle        = Color.argb(80, 60, 60, 60)
            colorBorder        = Color.parseColor("#D1D1D6")
        }
    }

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
    private fun sp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, v.toFloat(), resources.displayMetrics)

    private fun mountOverlay(recentApps: List<AppData>, hasPermission: Boolean) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.TRANSPARENT)

        val panel = buildPanel(recentApps, hasPermission)
        root.addView(panel, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        ))

        // Dismiss on tap outside panel (above panel top)
        root.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_DOWN) {
                val panelTop = root.height - panel.height
                if (ev.y < panelTop) { animateDismiss(); true } else false
            } else false
        }

        rootView = root
        wm.addView(root, WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ))

        root.post { animateIn(root, panel) }
    }

    private fun animateIn(root: FrameLayout, panel: View) {
        val h = panel.height.toFloat().coerceAtLeast(dp(160).toFloat())
        panel.translationY = h
        ValueAnimator.ofInt(0, colorScrimAlpha).apply {
            duration = 220
            addUpdateListener { runCatching { root.setBackgroundColor(Color.argb(it.animatedValue as Int, 0, 0, 0)) } }
            start()
        }
        ObjectAnimator.ofFloat(panel, View.TRANSLATION_Y, h, 0f).apply {
            duration = 280; interpolator = DecelerateInterpolator(2.2f); start()
        }
    }

    private fun animateDismiss() {
        val root = rootView ?: run { stopSelf(); return }
        val panel = root.getChildAt(0)
        val h = panel?.height?.toFloat()?.coerceAtLeast(dp(160).toFloat()) ?: dp(160).toFloat()

        AnimatorSet().apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { stopSelf() }
            })
            val fade = ValueAnimator.ofInt(colorScrimAlpha, 0).apply {
                duration = 200
                addUpdateListener { runCatching { root.setBackgroundColor(Color.argb(it.animatedValue as Int, 0, 0, 0)) } }
            }
            val slide = panel?.let {
                ObjectAnimator.ofFloat(it, View.TRANSLATION_Y, 0f, h + dp(40)).apply {
                    duration = 220; interpolator = AccelerateInterpolator(1.8f)
                }
            }
            if (slide != null) playTogether(fade, slide) else play(fade)
            start()
        }
    }

    private fun buildPanel(recentApps: List<AppData>, hasPermission: Boolean): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                val r = dp(22).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                setColor(colorSurface)
            }
            setPadding(0, 0, 0, getNavBarHeight() + dp(10))
        }

        // Handle
        panel.addView(View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(2).toFloat()
                setColor(colorHandle)
            }
        }, LinearLayout.LayoutParams(dp(36), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(10); bottomMargin = dp(6)
        })

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        header.addView(TextView(this).apply {
            text = "Recent Apps"
            setTextColor(colorTextPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(16))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        if (recentApps.isNotEmpty()) {
            header.addView(buildClearAllButton())
        }
        panel.addView(header, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Divider
        panel.addView(View(this).apply { setBackgroundColor(colorBorder) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))

        when {
            !hasPermission -> panel.addView(buildPermissionView())
            recentApps.isEmpty() -> panel.addView(buildEmptyView())
            else -> panel.addView(buildScrollerView(recentApps))
        }
        return panel
    }

    private fun buildClearAllButton(): TextView {
        val btn = TextView(this)
        btn.text = "Clear All"
        btn.setTextColor(colorAccent)
        btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14))
        btn.typeface = Typeface.DEFAULT_BOLD
        btn.setPadding(dp(12), dp(8), dp(4), dp(8))
        btn.isClickable = true
        btn.isFocusable = true
        btn.setOnClickListener {
            recentsManager.clearAll()
            animateDismiss()
        }
        return btn
    }

    private fun buildScrollerView(apps: List<AppData>): HorizontalScrollView {
        val scroller = HorizontalScrollView(this)
        scroller.isHorizontalScrollBarEnabled = false
        scroller.overScrollMode = View.OVER_SCROLL_NEVER
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(dp(16), dp(16), dp(16), dp(16))
        apps.forEach { app ->
            val lp = LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = dp(12)
            row.addView(buildAppCard(app), lp)
        }
        scroller.addView(row)
        return scroller
    }

    private fun buildAppCard(app: AppData): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.isClickable = true
        card.isFocusable = true

        val iconSize = dp(56)
        val iconBox = FrameLayout(this)
        iconBox.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(14).toFloat()
            setColor(colorSurfaceMid)
        }
        val imgView = ImageView(this)
        imgView.scaleType = ImageView.ScaleType.FIT_CENTER
        val bmp = iconCache.get(app.packageName)
        if (bmp != null) imgView.setImageBitmap(bmp.asAndroidBitmap())
        val p = dp(6)
        imgView.setPadding(p, p, p, p)
        iconBox.addView(imgView, FrameLayout.LayoutParams(iconSize, iconSize))
        card.addView(iconBox, LinearLayout.LayoutParams(iconSize, iconSize))
        card.addView(View(this), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6)))

        val label = TextView(this)
        label.text = app.label
        label.setTextColor(colorTextPrimary)
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(11))
        label.maxLines = 1
        label.ellipsize = android.text.TextUtils.TruncateAt.END
        label.gravity = Gravity.CENTER_HORIZONTAL
        card.addView(label, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        card.setOnClickListener {
            animateDismiss()
            repo.launchApp(app.packageName)
            recentsManager.onAppLaunched(app.packageName)
        }
        card.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
            false
        }
        return card
    }

    private fun buildPermissionView(): LinearLayout {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER_HORIZONTAL
        layout.setPadding(dp(32), dp(20), dp(32), dp(20))

        val title = TextView(this)
        title.text = "🔒 Permission Required"
        title.setTextColor(colorTextPrimary)
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(15))
        title.typeface = Typeface.DEFAULT_BOLD
        title.gravity = Gravity.CENTER
        layout.addView(title, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })

        val desc = TextView(this)
        desc.text = "Grant Usage Statistics access to load recent apps."
        desc.setTextColor(colorTextSecondary)
        desc.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(13))
        desc.gravity = Gravity.CENTER
        layout.addView(desc, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(16) })

        val btn = TextView(this)
        btn.text = "Open Settings"
        btn.setTextColor(Color.WHITE)
        btn.typeface = Typeface.DEFAULT_BOLD
        btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14))
        btn.gravity = Gravity.CENTER
        btn.setPadding(dp(24), dp(12), dp(24), dp(12))
        btn.isClickable = true
        btn.isFocusable = true
        btn.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            setColor(colorAccent)
        }
        btn.setOnClickListener { recentsManager.openPermissionSettings(); stopSelf() }
        layout.addView(btn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return layout
    }

    private fun buildEmptyView(): TextView {
        val tv = TextView(this)
        tv.text = "No recent apps"
        tv.setTextColor(colorTextSecondary)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14))
        tv.gravity = Gravity.CENTER
        tv.setPadding(dp(32), dp(32), dp(32), dp(32))
        return tv
    }

    private fun removeOverlay() {
        rootView?.let { v -> runCatching { windowManager?.removeView(v) }; rootView = null }
        windowManager = null
    }

    private fun getNavBarHeight(): Int {
        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(48)
    }
}
