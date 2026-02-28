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
     * Set package yang sudah di-kill oleh user via "Close All".
     * Package ini di-exclude dari UsageStats load sampai user membuka app tersebut lagi.
     * Ini perlu karena UsageStatsManager menyimpan *riwayat pemakaian*, bukan status proses —
     * app yang sudah di-kill masih muncul di UsageStats sampai kita explicitly exclude.
     */
    private val killedPackages = mutableSetOf<String>()

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
     * Load recent apps dari UsageStatsManager.
     * Package yang ada di killedPackages di-exclude — tetap tidak muncul
     * sampai user launch sendiri (onAppLaunched dipanggil).
     */
    suspend fun loadRecentApps(
        excludePackages: Set<String> = emptySet(),
        limit: Int = 10,
    ) = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext

        // Gabungkan exclusion eksplisit + package yang sudah di-kill
        val effectiveExclude = excludePackages + killedPackages

        runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val threeDaysAgo = now - 3L * 24 * 60 * 60 * 1000

            val fromUsage = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, threeDaysAgo, now)
                .filter { it.packageName != context.packageName }
                .filter { it.packageName !in effectiveExclude }
                .filter { it.lastTimeUsed > 0 }
                .sortedByDescending { it.lastTimeUsed }
                .map { it.packageName }
                .distinct()

            _recentPackages.value = fromUsage.take(limit)
        }
    }

    /**
     * Panggil saat user launch app.
     * Hapus dari killedPackages → app muncul kembali di recent saat berikutnya.
     */
    fun onAppLaunched(packageName: String, excludePackages: Set<String> = emptySet()) {
        if (packageName == context.packageName) return
        if (packageName in excludePackages) return

        // App ini sudah dipakai lagi → tidak perlu di-exclude lagi
        killedPackages.remove(packageName)

        val current = _recentPackages.value.toMutableList()
        current.remove(packageName)
        current.add(0, packageName)
        _recentPackages.value = current.take(10)
    }

    /**
     * Kill semua background process dari recent apps, lalu kosongkan list.
     * Tambahkan ke killedPackages agar tidak muncul lagi di UsageStats load berikutnya.
     *
     * KILL_BACKGROUND_PROCESSES permission (normal, auto-granted) diperlukan.
     * Proses foreground tidak terpengaruh — ini adalah batasan Android.
     */
    suspend fun killAndClearAll(
        packages: List<String> = _recentPackages.value.toList(),
    ) = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        packages.forEach { pkg ->
            runCatching { am.killBackgroundProcesses(pkg) }
            // Tambahkan ke exclusion set agar tidak muncul lagi dari UsageStats
            killedPackages.add(pkg)
        }

        // Langsung kosongkan list yang ditampilkan
        _recentPackages.value = emptyList()
    }
}
