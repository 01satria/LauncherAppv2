package id.satria.launcher.recents

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import id.satria.launcher.R
import java.util.concurrent.Executors

class CustomRecentsPanelActivity : AppCompatActivity() {

    private lateinit var adapter: RecentAdapter
    private lateinit var panelView: View
    private val bgExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Blur effect (Android 12+) atau fallback ke Dim
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = 50 // Blur yg kuat
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes.dimAmount = 0.6f
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_custom_recents_panel)

        panelView = findViewById(R.id.recentPanel)
        setupViews()
        animatePanelIn()
        loadRecentTasks()
    }

    private fun setupViews() {
        findViewById<View>(R.id.viewDismissDim).setOnClickListener { animatePanelOut() }
        findViewById<View>(R.id.tvClearAll).setOnClickListener { clearAllTasks() }

        val rv = findViewById<RecyclerView>(R.id.rvRecentApps)
        rv.layoutManager = LinearLayoutManager(this)

        adapter =
                RecentAdapter(
                        onTaskClick = { task -> bringTaskToFront(task) },
                        onTaskDismiss = { task -> removeTaskFromSystem(task.persistentId) }
                )
        rv.adapter = adapter

        // Swipe Left to dismiss
        val swipeHandler =
                object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    override fun onMove(
                            r: RecyclerView,
                            v: RecyclerView.ViewHolder,
                            t: RecyclerView.ViewHolder
                    ) = false
                    override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                        val pos = vh.bindingAdapterPosition
                        val task = adapter.getTaskAt(pos)
                        adapter.removeItem(pos)
                        removeTaskFromSystem(task.persistentId)
                    }
                }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(rv)
    }

    private fun loadRecentTasks() {
        bgExecutor.execute {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            // Ambil task terbaru, filter out aplikasi ini sendiri
            val recentTasks =
                    am.getRecentTasks(20, ActivityManager.RECENT_IGNORE_UNAVAILABLE).filter {
                        it.baseIntent?.component?.packageName != packageName
                    }

            mainHandler.post { adapter.submitList(recentTasks) }
        }
    }

    private fun bringTaskToFront(task: ActivityManager.RecentTaskInfo) {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val opts =
                ActivityOptions.makeCustomAnimation(
                        this,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
        am.moveTaskToFront(task.persistentId, ActivityManager.MOVE_TASK_WITH_HOME, opts.toBundle())
        finish()
    }

    private fun removeTaskFromSystem(taskId: Int) {
        bgExecutor.execute {
            try {
                // Hapus task. Perlu permission REMOVE_TASKS & system/root level utk efek native
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val removeMethod =
                        ActivityManager::class.java.getMethod(
                                "removeTask",
                                Int::class.javaPrimitiveType
                        )
                removeMethod.invoke(am, taskId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun clearAllTasks() {
        val tasks = adapter.getAllTasks()
        adapter.clearAll()
        bgExecutor.execute { tasks.forEach { removeTaskFromSystem(it.persistentId) } }
        animatePanelOut()
    }

    private fun animatePanelIn() {
        panelView.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        panelView.viewTreeObserver.removeOnPreDrawListener(this)
                        panelView.translationX = panelView.width.toFloat()
                        panelView
                                .animate()
                                .translationX(0f)
                                .setDuration(400)
                                .setInterpolator(OvershootInterpolator(0.8f)) // Spring / Overshoot
                                .start()
                        return true
                    }
                }
        )
    }

    private fun animatePanelOut() {
        panelView
                .animate()
                .translationX(panelView.width.toFloat())
                .setDuration(250)
                .setInterpolator(AnticipateInterpolator(0.8f))
                .withEndAction {
                    finish()
                    overridePendingTransition(0, 0)
                }
                .start()
    }

    @Deprecated("Deprecated in Java", ReplaceWith("animatePanelOut()"))
    override fun onBackPressed() = animatePanelOut()
}
