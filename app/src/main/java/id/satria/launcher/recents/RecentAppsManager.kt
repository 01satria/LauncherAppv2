package id.satria.launcher.recents

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

/**
 * RecentAppsManager — RAM-minimal edition
 *
 * Strategi:
 * - List recent dijaga in-memory (MutableStateFlow) — tidak ada polling, tidak ada timer
 * - Sumber data utama: onAppLaunched() dipanggil setiap kali user buka app dari launcher
 * - Jika Usage Stats permission ada, loadRecentApps() dipanggil on-demand (saat panel
 *   dibuka) untuk melengkapi list dengan app yang dibuka di luar launcher
 * - clearAll() benar-benar kosongkan list in-memory
 * - Tidak ada background thread yang berjalan terus-menerus
 */
class RecentAppsManager(private val context: Context) {

    private val _recentPackages = MutableStateFlow<List<String>>(emptyList())
    val recentPackages: StateFlow<List<String>> = _recentPackages.asStateFlow()

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
     * Load on-demand dari UsageStatsManager, merge dengan in-memory list.
     * In-memory (lebih fresh) diprioritaskan di atas. Hanya query 3 hari.
     */
    suspend fun loadRecentApps(
        excludePackages: Set<String> = emptySet(),
        limit: Int = 10,
    ) = withContext(Dispatchers.IO) {
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

            // Merge: in-memory di depan, usage stats melengkapi
            val merged = (_recentPackages.value + fromUsage)
                .distinct()
                .filter { it !in excludePackages }
                .take(limit)

            _recentPackages.value = merged
        }
        // Jika gagal, biarkan in-memory list tetap seperti sebelumnya
    }

    /** Panggil saat user launch app — update in-memory instantly, no IO */
    fun onAppLaunched(packageName: String, excludePackages: Set<String> = emptySet()) {
        if (packageName == context.packageName) return
        if (packageName in excludePackages) return
        val current = _recentPackages.value.toMutableList()
        current.remove(packageName)
        current.add(0, packageName)
        _recentPackages.value = current.take(10)
    }

    /** Clear All — kosongkan list in-memory sepenuhnya */
    fun clearAll() {
        _recentPackages.value = emptyList()
    }
}
