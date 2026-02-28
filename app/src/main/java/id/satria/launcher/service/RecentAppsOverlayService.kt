package id.satria.launcher.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.*
import android.widget.*
import android.animation.*
import android.graphics.Typeface
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.util.TypedValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import id.satria.launcher.data.AppData
import id.satria.launcher.data.LauncherRepository
import id.satria.launcher.recents.RecentAppsManager
import id.satria.launcher.ui.component.iconCache
import kotlinx.coroutines.*

/**
 * RecentAppsOverlayService
 *
 * Menampilkan panel Recent Apps sebagai system overlay menggunakan
 * pure Android View — BUKAN Compose. Tidak butuh LifecycleOwner,
 * SavedStateRegistry, atau dependency Compose apapun.
 *
 * Flow:
 * EdgeSwipeService deteksi swipe → RecentAppsOverlayService.show()
 * → tampil di atas semua aplikasi via TYPE_APPLICATION_OVERLAY
 * → user pilih app / tap scrim / Clear All → stopSelf()
 */
class RecentAppsOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var rootView: FrameLayout? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var recentsManager: RecentAppsManager
    private lateinit var repo: LauncherRepository

    companion object {
        fun show(context: Context) {
            context.startService(Intent(context, RecentAppsOverlayService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        recentsManager = RecentAppsManager(applicationContext)
        repo = LauncherRepository(applicationContext)
        loadThenShow()
    }

    override fun onDestroy() {
        scope.cancel()
        removeOverlay()
        super.onDestroy()
    }

    private fun loadThenShow() = scope.launch {
        val allApps = async(Dispatchers.IO) { repo.getInstalledApps() }
        recentsManager.loadRecentApps(limit = 10)
        val apps = allApps.await()
        val recentPkgs = recentsManager.recentPackages.value
        val recentApps = recentPkgs.mapNotNull { pkg -> apps.find { it.packageName == pkg } }.take(10)
        mountOverlay(recentApps, recentsManager.hasPermission())
    }

    // ── dp / sp helpers ───────────────────────────────────────────────────────
    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
    private fun sp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, v.toFloat(), resources.displayMetrics)

    // ── Mount overlay to WindowManager ────────────────────────────────────────
    private fun mountOverlay(recentApps: List<AppData>, hasPermission: Boolean) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        // Fullscreen transparent root — tap → dismiss
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            setOnClickListener { animateDismiss() }
        }

        // Bottom sheet panel
        val panel = buildPanel(recentApps, hasPermission)
        panel.isClickable = true
        panel.setOnClickListener { /* intercept — jangan dismiss */ }

        root.addView(panel, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        ))

        rootView = root

        wm.addView(root, WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ))

        // Animate in after layout
        root.post { animateIn(root, panel) }
    }

    // ── Animations ────────────────────────────────────────────────────────────
    private fun animateIn(root: FrameLayout, panel: View) {
        val panelH = panel.height.toFloat().coerceAtLeast(dp(200).toFloat())
        panel.translationY = panelH

        ValueAnimator.ofInt(0, 153).apply {
            duration = 220
            addUpdateListener {
                runCatching { root.setBackgroundColor(Color.argb(it.animatedValue as Int, 0, 0, 0)) }
            }
            start()
        }
        ObjectAnimator.ofFloat(panel, View.TRANSLATION_Y, panelH, 0f).apply {
            duration = 260
            interpolator = DecelerateInterpolator(2f)
            start()
        }
    }

    private fun animateDismiss() {
        val root = rootView ?: run { stopSelf(); return }
        val panel = if (root.childCount > 0) root.getChildAt(0) else null
        val panelH = panel?.height?.toFloat()?.coerceAtLeast(dp(200).toFloat()) ?: dp(200).toFloat()

        val set = AnimatorSet()
        val fade = ValueAnimator.ofInt(153, 0).apply {
            duration = 200
            addUpdateListener { runCatching { root.setBackgroundColor(Color.argb(it.animatedValue as Int, 0, 0, 0)) } }
        }
        val slide = panel?.let {
            ObjectAnimator.ofFloat(it, View.TRANSLATION_Y, 0f, panelH + dp(40)).apply {
                duration = 200
                interpolator = AccelerateInterpolator(1.5f)
            }
        }
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) { stopSelf() }
        })
        if (slide != null) set.playTogether(fade, slide) else set.play(fade)
        set.start()
    }

    // ── Panel ─────────────────────────────────────────────────────────────────
    private fun buildPanel(recentApps: List<AppData>, hasPermission: Boolean): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.TRANSPARENT, Color.argb(245, 13, 13, 13)),
            )
            val navH = getNavBarHeight()
            setPadding(0, 0, 0, navH + dp(16))
        }

        // Handle bar
        panel.addView(View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(2).toFloat()
                setColor(Color.argb(76, 255, 255, 255))
            }
        }, LinearLayout.LayoutParams(dp(36), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(14); bottomMargin = dp(10)
        })

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), 0, dp(24), dp(14))
        }
        header.addView(TextView(this).apply {
            text = "Recent Apps"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(17))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        if (recentApps.isNotEmpty()) {
            header.addView(TextView(this).apply {
                text = "Clear All"
                setTextColor(Color.argb(128, 255, 255, 255))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(13))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    recentsManager.clearAll()
                    animateDismiss()
                }
            })
        }
        panel.addView(header, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Content
        when {
            !hasPermission -> panel.addView(buildPermissionView())
            recentApps.isEmpty() -> panel.addView(buildEmptyView())
            else -> panel.addView(buildScrollerView(recentApps))
        }

        return panel
    }

    // ── App scroller ──────────────────────────────────────────────────────────
    private fun buildScrollerView(apps: List<AppData>): HorizontalScrollView {
        val scroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), 0, dp(20), 0)
        }
        apps.forEach { app ->
            row.addView(buildAppCard(app), LinearLayout.LayoutParams(dp(76), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(14)
            })
        }
        scroller.addView(row)
        return scroller
    }

    // ── App card ──────────────────────────────────────────────────────────────
    private fun buildAppCard(app: AppData): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Icon
        val iconSize = dp(64)
        val iconBox = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(Color.argb(18, 255, 255, 255))
            }
        }
        iconBox.addView(ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            // iconCache berisi ImageBitmap (Compose) — konversi ke android Bitmap
            val imgBitmap: ImageBitmap? = iconCache.get(app.packageName)
            if (imgBitmap != null) {
                setImageBitmap(imgBitmap.asAndroidBitmap())
            }
        }, FrameLayout.LayoutParams(iconSize, iconSize))
        card.addView(iconBox, LinearLayout.LayoutParams(iconSize, iconSize))

        // Spacer
        card.addView(View(this), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7)))

        // Label
        card.addView(TextView(this).apply {
            text = app.label
            setTextColor(Color.argb(224, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(11))
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_HORIZONTAL
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Click
        card.setOnClickListener {
            animateDismiss()
            repo.launchApp(app.packageName)
            recentsManager.onAppLaunched(app.packageName)
        }
        // Touch scale feedback
        card.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }
        return card
    }

    // ── Permission prompt ─────────────────────────────────────────────────────
    private fun buildPermissionView(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), dp(8), dp(32), dp(8))
        }
        layout.addView(TextView(this).apply {
            text = "🔒 Izin Diperlukan"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(15))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
        layout.addView(TextView(this).apply {
            text = "Izinkan Usage Statistics agar recent apps bisa dimuat."
            setTextColor(Color.argb(153, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(13))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) })
        layout.addView(TextView(this).apply {
            text = "Buka Settings"
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(13))
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(12), dp(24), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.argb(255, 39, 174, 96))
            }
            setOnClickListener { recentsManager.openPermissionSettings(); stopSelf() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return layout
    }

    // ── Empty state ───────────────────────────────────────────────────────────
    private fun buildEmptyView() = TextView(this).apply {
        text = "Belum ada app yang digunakan"
        setTextColor(Color.argb(102, 255, 255, 255))
        setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14))
        gravity = Gravity.CENTER
        setPadding(dp(32), dp(28), dp(32), dp(28))
    }

    // ── Remove from WindowManager ─────────────────────────────────────────────
    private fun removeOverlay() {
        rootView?.let { v ->
            runCatching { windowManager?.removeView(v) }
            rootView = null
        }
        windowManager = null
    }

    private fun getNavBarHeight(): Int {
        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(48)
    }
}
