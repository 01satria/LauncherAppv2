package id.satria.launcher.recents

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class RecentAppsManager(private val context: Context) {

    private val _recentPackages = MutableStateFlow<List<String>>(emptyList())
    val recentPackages: StateFlow<List<String>> = _recentPackages.asStateFlow()

    /**
     * Flag: user sudah tekan "Clear All".
     * Saat flag ini true, loadRecentApps() TIDAK akan mengisi list dari UsageStats.
     * Flag di-reset hanya saat user launch app (berarti mulai pakai lagi).
     */
    private var userCleared = false

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openPermissionSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Load on-demand dari UsageStatsManager.
     * userCleared direset di sini agar setiap kali panel dibuka, data fresh dari UsageStats.
     */
    suspend fun loadRecentApps(
        excludePackages: Set<String> = emptySet(),
        limit: Int = 10,
    ) = withContext(Dispatchers.IO) {
        // Reset flag — kalau user sudah clear, biarkan UsageStats isi ulang saat dibuka lagi
        userCleared = false
        if (!hasPermission()) return@withContext

        runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val threeDaysAgo = now - 3L * 24 * 60 * 60 * 1000

            val fromUsage = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, threeDaysAgo, now)
                .filter { it.packageName != context.packageName }
                .filter { it.packageName !in excludePackages }
                .filter { it.lastTimeUsed > 0 }
                .sortedByDescending { it.lastTimeUsed }
                .map { it.packageName }
                .distinct()

            val merged = (_recentPackages.value + fromUsage)
                .distinct()
                .filter { it !in excludePackages }
                .take(limit)

            _recentPackages.value = merged
        }
    }

    /** Panggil saat user launch app — reset flag cleared, update list in-memory */
    fun onAppLaunched(packageName: String, excludePackages: Set<String> = emptySet()) {
        if (packageName == context.packageName) return
        if (packageName in excludePackages) return
        // User mulai pakai app lagi → boleh load dari UsageStats berikutnya
        userCleared = false
        val current = _recentPackages.value.toMutableList()
        current.remove(packageName)
        current.add(0, packageName)
        _recentPackages.value = current.take(10)
    }

    /**
     * Kill background processes untuk semua package di list recents,
     * lalu kosongkan list.
     *
     * Menggunakan ActivityManager.killBackgroundProcesses() yang memerlukan
     * permission KILL_BACKGROUND_PROCESSES (normal permission, tidak perlu root).
     *
     * Catatan: ini setara dengan swipe-dismiss di sistem task switcher Android —
     * process yang sedang di foreground tidak akan terkena.
     */
    suspend fun killAndClearAll(
        packages: List<String> = _recentPackages.value,
    ) = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        packages.forEach { pkg ->
            runCatching { am.killBackgroundProcesses(pkg) }
        }
        // Kosongkan list setelah kill
        _recentPackages.value = emptyList()
    }

    /** Hanya kosongkan list tanpa kill (internal use) */
    fun clearAll() {
        _recentPackages.value = emptyList()
    }
}
