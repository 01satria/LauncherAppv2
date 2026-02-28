package id.satria.launcher.service

import android.animation.*
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.util.TypedValue
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.*
import androidx.compose.ui.graphics.asAndroidBitmap
import id.satria.launcher.data.AppData
import id.satria.launcher.data.LauncherRepository
import id.satria.launcher.recents.RecentAppsManager
import id.satria.launcher.ui.component.iconCache
import kotlinx.coroutines.*
import kotlin.math.abs

/**
 * RecentAppsOverlayService — OHO+ Task Switcher style
 *
 * Dirender sebagai TYPE_APPLICATION_OVERLAY window (pure Android Views).
 * Tampil sebagai kartu-kartu portrait besar yang bisa di-swipe ke atas untuk ditutup.
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

    private var isMounted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        recentsManager = RecentAppsManager(applicationContext)
        repo = LauncherRepository(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isMounted) return START_NOT_STICKY
        isMounted = true
        loadAndShow()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        removeOverlay()
        EdgeSwipeService.notifyDismissed()
        super.onDestroy()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
    private fun sp(v: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)
    private fun navBarHeight(): Int {
        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(48)
    }

    // ── Load data & mount ─────────────────────────────────────────────────────

    private fun loadAndShow() {
        scope.launch {
            val allApps = async(Dispatchers.IO) { repo.getInstalledApps() }
            recentsManager.loadRecentApps(limit = 15)
            val apps = allApps.await()
            val recentPkgs = recentsManager.recentPackages.value
            val recentApps = recentPkgs.mapNotNull { pkg -> apps.find { it.packageName == pkg } }.take(15)
            mountOverlay(recentApps, recentsManager.hasPermission())
        }
    }

    // ── Mount overlay window ──────────────────────────────────────────────────

    private fun mountOverlay(recentApps: List<AppData>, hasPermission: Boolean) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        // Full-screen root — dark scrim + tap-outside-to-dismiss
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.TRANSPARENT)
        root.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_DOWN) {
                val panelView = rootView?.getChildAt(0) ?: return@setOnTouchListener false
                val panelTop = root.height - panelView.height
                if (ev.y < panelTop) { animateDismiss(); true } else false
            } else false
        }

        // Build the bottom panel
        val panel = buildPanel(recentApps, hasPermission)
        root.addView(panel, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        ))

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

    // ── Animate in / out ──────────────────────────────────────────────────────

    private fun animateIn(root: FrameLayout, panel: View) {
        panel.post {
            val h = panel.height.toFloat().coerceAtLeast(dp(200).toFloat())
            panel.translationY = h
            ValueAnimator.ofInt(0, 184).apply {
                duration = 220
                addUpdateListener {
                    runCatching { root.setBackgroundColor(Color.argb(it.animatedValue as Int, 0, 0, 0)) }
                }
                start()
            }
            ObjectAnimator.ofFloat(panel, View.TRANSLATION_Y, h, 0f).apply {
                duration = 300; interpolator = DecelerateInterpolator(2.4f); start()
            }
        }
    }

    private fun animateDismiss() {
        val root = rootView ?: run { stopSelf(); return }
        val panel = root.getChildAt(0) ?: run { stopSelf(); return }
        val h = panel.height.toFloat().coerceAtLeast(dp(200).toFloat())
        val scrimAlpha = 184
        AnimatorSet().apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) { stopSelf() }
            })
            val fade = ValueAnimator.ofInt(scrimAlpha, 0).apply {
                duration = 200
                addUpdateListener {
                    runCatching { root.setBackgroundColor(Color.argb(it.animatedValue as Int, 0, 0, 0)) }
                }
            }
            val slide = ObjectAnimator.ofFloat(panel, View.TRANSLATION_Y, 0f, h + dp(40)).apply {
                duration = 240; interpolator = AccelerateInterpolator(1.8f)
            }
            playTogether(fade, slide)
            start()
        }
    }

    private fun removeOverlay() {
        rootView?.let { v -> runCatching { windowManager?.removeView(v) }; rootView = null }
        windowManager = null
    }

    // ── Build panel (bottom sheet) ────────────────────────────────────────────

    private fun buildPanel(recentApps: List<AppData>, hasPermission: Boolean): RelativeLayout {
        val rl = RelativeLayout(this)
        rl.setBackgroundColor(Color.TRANSPARENT)

        val bgParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(420))
        bgParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        val gradBg = object : View(this) {
            override fun onDraw(canvas: Canvas) {
                val w = width.toFloat(); val h2 = height.toFloat()
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val shader = LinearGradient(0f, 0f, 0f, h2,
                    intArrayOf(Color.TRANSPARENT, Color.argb(235, 10, 10, 12), Color.argb(255, 8, 8, 10)),
                    floatArrayOf(0f, 0.30f, 1f), Shader.TileMode.CLAMP)
                paint.shader = shader
                canvas.drawRect(0f, 0f, w, h2, paint)
            }
        }
        gradBg.setWillNotDraw(false)
        rl.addView(gradBg, bgParams)

        val contentParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        contentParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

        val contentInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, navBarHeight() + dp(20))
        }

        when {
            !hasPermission -> contentInner.addView(buildPermissionView())
            recentApps.isEmpty() -> contentInner.addView(buildEmptyView())
            else -> {
                contentInner.addView(buildCardsScroller(recentApps))
                contentInner.addView(buildClearAllPill(), LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(16) })
            }
        }

        rl.addView(contentInner, contentParams)
        return rl
    }

    // ── Cards horizontal scroller ─────────────────────────────────────────────

    private fun buildCardsScroller(apps: List<AppData>): HorizontalScrollView {
        val scroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        apps.forEachIndexed { index, app ->
            val lp = LinearLayout.LayoutParams(dp(140), dp(252))
            lp.marginEnd = dp(14)
            val card = buildTaskCard(app)
            row.addView(card, lp)
            // Stagger entrance animation
            card.alpha = 0f
            card.translationY = dp(40).toFloat()
            card.animate()
                .alpha(1f).translationY(0f)
                .setDuration(280)
                .setStartDelay((index * 45L).coerceAtMost(320L))
                .setInterpolator(DecelerateInterpolator(1.8f))
                .start()
        }
        scroller.addView(row)
        return scroller
    }

    // ── Single task card ──────────────────────────────────────────────────────

    private fun buildTaskCard(app: AppData): FrameLayout {
        val card = FrameLayout(this)
        card.clipToOutline = true
        card.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dp(22).toFloat())
            }
        }

        // ── Preview area (top 75%) ─────────────────────────────────────────────
        val cardHue = (abs(app.packageName.hashCode()) % 360).toFloat()
        val topCol  = hslToColor(cardHue, 0.28f, 0.14f)
        val botCol  = hslToColor(cardHue, 0.22f, 0.10f)

        val previewBg = object : View(this) {
            override fun onDraw(canvas: Canvas) {
                val sh = LinearGradient(0f, 0f, 0f, height.toFloat(),
                    topCol, botCol, Shader.TileMode.CLAMP)
                val p = Paint(); p.shader = sh
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
            }
        }
        previewBg.setWillNotDraw(false)
        val previewH = (252 * 0.74f).toInt()
        card.addView(previewBg, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(previewH)))

        // App icon centered in preview
        val iconFrame = FrameLayout(this)
        val iconBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(Color.argb(15, 255, 255, 255))
        }
        iconFrame.background = iconBg

        val iv = ImageView(this)
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        val bmp = iconCache.get(app.packageName)
        if (bmp != null) iv.setImageBitmap(bmp.asAndroidBitmap())
        val iconPad = dp(8)
        iv.setPadding(iconPad, iconPad, iconPad, iconPad)
        iconFrame.addView(iv, FrameLayout.LayoutParams(dp(70), dp(70)))
        val iconFrameParams = FrameLayout.LayoutParams(dp(70), dp(70)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(previewH / 2) - dp(70) / 2 - dp(16) // vertically centered in preview
        }
        card.addView(iconFrame, iconFrameParams)

        // Fake skeleton lines below icon
        val skeletonLL = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listOf(0.85f, 0.60f, 0.42f).forEachIndexed { i, w ->
            val line = View(this)
            val lineBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(2).toFloat()
                setColor(hslToColor(cardHue, 0.20f, 0.28f))
            }
            line.background = lineBg
            val lineLp = LinearLayout.LayoutParams((dp(80) * w).toInt(), dp(4))
            lineLp.topMargin = if (i == 0) 0 else dp(5)
            skeletonLL.addView(line, lineLp)
        }
        val skeletonParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(previewH / 2) + dp(70) / 2 + dp(4)
        }
        card.addView(skeletonLL, skeletonParams)

        // ── Bottom name bar ────────────────────────────────────────────────────
        val nameBar = object : LinearLayout(this) {
            override fun onDraw(canvas: Canvas) {
                val p = Paint()
                val sh = LinearGradient(0f, 0f, 0f, height.toFloat(),
                    Color.argb(255, 17, 17, 20), Color.argb(255, 13, 13, 16),
                    Shader.TileMode.CLAMP)
                p.shader = sh
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
            }
        }
        nameBar.setWillNotDraw(false)
        nameBar.orientation = LinearLayout.HORIZONTAL
        nameBar.gravity = Gravity.CENTER_VERTICAL
        nameBar.setPadding(dp(10), 0, dp(10), 0)

        // Small icon in name bar
        val smallIv = ImageView(this)
        smallIv.scaleType = ImageView.ScaleType.FIT_CENTER
        val smallBmp = iconCache.get(app.packageName)
        if (smallBmp != null) smallIv.setImageBitmap(smallBmp.asAndroidBitmap())
        val r = dp(7).toFloat()
        smallIv.clipToOutline = true
        smallIv.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, r) }
        }
        nameBar.addView(smallIv, LinearLayout.LayoutParams(dp(26), dp(26)).apply { marginEnd = dp(8) })

        val tv = TextView(this)
        tv.text = app.label
        tv.setTextColor(Color.argb(224, 255, 255, 255))
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(11f))
        tv.maxLines = 1
        tv.ellipsize = android.text.TextUtils.TruncateAt.END
        nameBar.addView(tv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val nameBarH = (252 * 0.26f).toInt()
        val nameBarParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(nameBarH), Gravity.BOTTOM)
        card.addView(nameBar, nameBarParams)

        // Thin divider between preview and name bar
        val divider = View(this)
        divider.setBackgroundColor(Color.argb(18, 255, 255, 255))
        val divParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1).apply {
            bottomMargin = dp(nameBarH)
        }
        card.addView(divider, divParams)

        // ── X close button ─────────────────────────────────────────────────────
        val closeBtn = buildCloseButton(app)
        val closeLp = FrameLayout.LayoutParams(dp(28), dp(28)).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = dp(8); marginEnd = dp(8)
        }
        card.addView(closeBtn, closeLp)

        // ── Touch: tap to launch, swipe up to dismiss ──────────────────────────
        var downX = 0f; var downY = 0f; var dragStart = 0f
        var isDragging = false

        card.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX; downY = ev.rawY; dragStart = v.translationY; isDragging = false
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = ev.rawY - downY
                    val dx = ev.rawX - downX
                    if (!isDragging && abs(dy) > dp(8) && abs(dy) > abs(dx)) {
                        isDragging = true
                        v.animate().cancel()
                    }
                    if (isDragging && dy < 0) {
                        v.translationY = dragStart + dy
                        val prog = (-dy / dp(200).toFloat()).coerceIn(0f, 1f)
                        v.alpha = 1f - prog * 0.7f
                        v.scaleX = 1f - prog * 0.18f
                        v.scaleY = 1f - prog * 0.18f
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    if (isDragging) {
                        val dy = ev.rawY - downY
                        if (dy < -dp(70)) {
                            dismissCard(v, app)
                        } else {
                            v.animate().translationY(dragStart).alpha(1f)
                                .scaleX(1f).scaleY(1f).setDuration(200)
                                .setInterpolator(DecelerateInterpolator()).start()
                        }
                    } else {
                        // Tap → launch
                        animateDismiss()
                        repo.launchApp(app.packageName)
                        recentsManager.onAppLaunched(app.packageName)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().translationY(dragStart).alpha(1f)
                        .scaleX(1f).scaleY(1f).setDuration(150).start()
                    true
                }
                else -> false
            }
        }

        return card
    }

    // ── Dismiss single card via swipe ─────────────────────────────────────────

    private fun dismissCard(cardView: View, app: AppData) {
        scope.launch { recentsManager.killAndClearAll(listOf(app.packageName)) }
        val parent = cardView.parent as? LinearLayout ?: return
        cardView.animate()
            .translationY(-dp(500).toFloat())
            .alpha(0f)
            .scaleX(0.6f).scaleY(0.6f)
            .setDuration(260)
            .setInterpolator(AccelerateInterpolator(1.6f))
            .withEndAction {
                // Animate width collapse
                val startW = cardView.width + dp(14)
                ValueAnimator.ofInt(startW, 0).apply {
                    duration = 200
                    addUpdateListener {
                        val lp = cardView.layoutParams as LinearLayout.LayoutParams
                        lp.width = it.animatedValue as Int
                        lp.marginEnd = 0
                        cardView.layoutParams = lp
                    }
                    withEndAction { parent.removeView(cardView) }
                    start()
                }
            }
            .start()
    }

    private fun Animator.withEndAction(block: () -> Unit) {
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) { block() }
        })
    }

    // ── X close button ────────────────────────────────────────────────────────

    private fun buildCloseButton(app: AppData): View {
        val btn = object : View(this) {
            override fun onDraw(canvas: Canvas) {
                val r = width / 2f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color = Color.argb(140, 0, 0, 0)
                canvas.drawCircle(r, r, r, paint)
                paint.color = Color.argb(220, 255, 255, 255)
                paint.strokeWidth = dp(1).toFloat().coerceAtLeast(1.5f)
                paint.strokeCap = Paint.Cap.ROUND
                val off = r * 0.30f
                canvas.drawLine(r - off, r - off, r + off, r + off, paint)
                canvas.drawLine(r + off, r - off, r - off, r + off, paint)
            }
        }
        btn.setWillNotDraw(false)
        btn.isClickable = true
        btn.isFocusable = true
        btn.setOnClickListener {
            val cardView = btn.parent as? FrameLayout ?: return@setOnClickListener
            val parent = cardView.parent as? LinearLayout ?: return@setOnClickListener
            dismissCard(cardView, app)
        }
        return btn
    }

    // ── Clear All pill ────────────────────────────────────────────────────────

    private fun buildClearAllPill(): View {
        val pill = object : TextView(this) {
            override fun onDraw(canvas: Canvas) {
                val r = height / 2f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color = Color.argb(26, 255, 255, 255)
                canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), r, r, paint)
                paint.color = Color.argb(40, 255, 255, 255)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(1).toFloat()
                canvas.drawRoundRect(0.5f, 0.5f, width - 0.5f, height - 0.5f, r, r, paint)
                super.onDraw(canvas)
            }
        }
        pill.setWillNotDraw(false)
        pill.text = "✕  Clear All"
        pill.setTextColor(Color.argb(210, 255, 255, 255))
        pill.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14f))
        pill.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        pill.gravity = Gravity.CENTER
        pill.letterSpacing = 0.02f
        pill.setPadding(dp(32), dp(13), dp(32), dp(13))
        pill.isClickable = true
        pill.isFocusable = true
        pill.setOnClickListener {
            scope.launch { recentsManager.killAndClearAll() }
            animateDismiss()
        }
        pill.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
            false
        }
        return pill
    }

    // ── Permission / Empty views ───────────────────────────────────────────────

    private fun buildPermissionView(): LinearLayout {
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(40), dp(40), dp(40), dp(40))
        }
        ll.addView(TextView(this).apply {
            text = "🔒 Usage Stats Required"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(15f))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) })
        ll.addView(TextView(this).apply {
            text = "Allow Usage Statistics access to enable Recent Apps."
            setTextColor(Color.argb(128, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(13f))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(20) })
        val btn = TextView(this).apply {
            text = "Open Settings"
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14f))
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(12), dp(28), dp(12))
            isClickable = true; isFocusable = true
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(50).toFloat()
                setColor(Color.parseColor("#27AE60"))
            }
            setOnClickListener { recentsManager.openPermissionSettings(); stopSelf() }
        }
        ll.addView(btn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return ll
    }

    private fun buildEmptyView(): LinearLayout {
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(40), dp(60), dp(40), dp(60))
        }
        ll.addView(TextView(this).apply {
            text = "✓"
            setTextColor(Color.argb(46, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(44f))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) })
        ll.addView(TextView(this).apply {
            text = "No recent apps"
            setTextColor(Color.argb(80, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(14f))
            gravity = Gravity.CENTER
        })
        return ll
    }

    // ── HSL color helper ──────────────────────────────────────────────────────

    private fun hslToColor(hDeg: Float, s: Float, l: Float): Int {
        // HSL → RGB
        val h = hDeg / 360f
        val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
        val p = 2 * l - q
        fun hue2rgb(t0: Float): Float {
            var t = t0
            if (t < 0) t += 1f
            if (t > 1) t -= 1f
            return when {
                t < 1/6f -> p + (q - p) * 6 * t
                t < 1/2f -> q
                t < 2/3f -> p + (q - p) * (2/3f - t) * 6
                else      -> p
            }
        }
        val r = (hue2rgb(h + 1/3f) * 255).toInt()
        val g = (hue2rgb(h)         * 255).toInt()
        val b = (hue2rgb(h - 1/3f) * 255).toInt()
        return Color.rgb(r, g, b)
    }
}
