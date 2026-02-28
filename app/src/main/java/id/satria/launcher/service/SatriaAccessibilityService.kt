package id.satria.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

/**
 * SatriaAccessibilityService
 *
 * Digunakan untuk "Close All recent tasks" yang sesungguhnya:
 *   1. Terima broadcast ACTION_CLOSE_ALL_TASKS dari RecentAppsOverlayService
 *   2. Buka system recents via performGlobalAction(GLOBAL_ACTION_RECENTS)
 *   3. Cari node "Close all" / "Clear all" di accessibility tree system UI
 *   4. Klik node tersebut
 *   5. Kembali ke launcher via GLOBAL_ACTION_HOME
 *
 * Untuk close per-app: gunakan swipe/dismiss gesture pada node task card.
 */
class SatriaAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_CLOSE_ALL_TASKS = "id.satria.launcher.ACTION_CLOSE_ALL_TASKS"
        const val ACTION_CLOSE_ONE_TASK  = "id.satria.launcher.ACTION_CLOSE_ONE_TASK"
        const val EXTRA_PACKAGE          = "pkg"

        /** Kirim broadcast agar service tutup semua tasks */
        fun sendCloseAll(context: Context) {
            val intent = Intent(ACTION_CLOSE_ALL_TASKS).apply {
                `package` = context.packageName
            }
            context.sendBroadcast(intent)
        }

        /** Kirim broadcast agar service tutup satu task berdasarkan package */
        fun sendCloseOne(context: Context, packageName: String) {
            val intent = Intent(ACTION_CLOSE_ONE_TASK).apply {
                `package` = context.packageName
                putExtra(EXTRA_PACKAGE, packageName)
            }
            context.sendBroadcast(intent)
        }

        @Volatile
        var instance: SatriaAccessibilityService? = null

        fun isEnabled(): Boolean = instance != null
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_CLOSE_ALL_TASKS -> handleCloseAll()
                ACTION_CLOSE_ONE_TASK  -> {
                    val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return
                    handleCloseOne(pkg)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val filter = IntentFilter().apply {
            addAction(ACTION_CLOSE_ALL_TASKS)
            addAction(ACTION_CLOSE_ONE_TASK)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op — kita hanya pakai explicit performGlobalAction
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        scope.cancel()
        runCatching { unregisterReceiver(receiver) }
        super.onDestroy()
    }

    // ── Close All ──────────────────────────────────────────────────────────────

    private fun handleCloseAll() {
        scope.launch {
            // 1. Buka system recents
            performGlobalAction(GLOBAL_ACTION_RECENTS)
            // 2. Tunggu recents UI muncul
            delay(600)
            // 3. Cari dan klik tombol "Close all" / "Clear all"
            if (!tryClickCloseAll()) {
                // Fallback: swipe semua card satu per satu
                swipeAllCards()
            }
            // 4. Kembali ke home
            delay(200)
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun handleCloseOne(packageName: String) {
        scope.launch {
            performGlobalAction(GLOBAL_ACTION_RECENTS)
            delay(600)
            dismissTaskCard(packageName)
            delay(200)
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    // ── Cari tombol "Close all" di system UI ───────────────────────────────────

    private fun tryClickCloseAll(): Boolean {
        val root = rootInActiveWindow ?: return false
        // Kata kunci berbagai OEM: "Close all", "Clear all", "Dismiss all", "閉じる"
        val keywords = listOf("close all", "clear all", "dismiss all", "hapus semua",
            "tutup semua", "close_all", "btn_close_all", "dismissAll")
        return findAndClick(root, keywords)
    }

    private fun findAndClick(node: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        // Cek node ini
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val id   = node.viewIdResourceName?.lowercase() ?: ""
        if (keywords.any { text.contains(it) || desc.contains(it) || id.contains(it) }) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            // Coba parent yang clickable
            var parent = node.parent
            repeat(3) {
                if (parent?.isClickable == true) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                parent = parent?.parent
            }
        }
        // Rekursif ke children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClick(child, keywords)) return true
        }
        return false
    }

    // ── Swipe semua task card satu per satu (fallback) ─────────────────────────

    private suspend fun swipeAllCards() {
        repeat(20) {
            val root = rootInActiveWindow ?: return
            val scrollable = findScrollableList(root) ?: return
            // Ambil card pertama yang visible
            val card = getFirstTaskCard(scrollable) ?: return
            card.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
            delay(150)
        }
    }

    private fun findScrollableList(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findScrollableList(node.getChild(i) ?: continue)
            if (found != null) return found
        }
        return null
    }

    private fun getFirstTaskCard(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isDismissable) return node
        for (i in 0 until node.childCount) {
            val found = getFirstTaskCard(node.getChild(i) ?: continue)
            if (found != null) return found
        }
        return null
    }

    // ── Dismiss satu task card berdasarkan package ─────────────────────────────

    private fun dismissTaskCard(packageName: String) {
        val root = rootInActiveWindow ?: return
        findNodeByPackage(root, packageName)?.let { node ->
            node.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
        }
    }

    private fun findNodeByPackage(node: AccessibilityNodeInfo, pkg: String): AccessibilityNodeInfo? {
        if (node.packageName?.toString() == pkg && node.isDismissable) return node
        for (i in 0 until node.childCount) {
            val found = findNodeByPackage(node.getChild(i) ?: continue, pkg)
            if (found != null) return found
        }
        return null
    }
}
