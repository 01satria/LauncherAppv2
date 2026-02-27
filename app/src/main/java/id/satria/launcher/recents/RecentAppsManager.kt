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
 * RecentAppsManager
 *
 * Menggunakan UsageStatsManager untuk mendapatkan daftar app yang terakhir
 * digunakan, diurutkan dari yang paling baru. Ini adalah cara yang benar
 * untuk launcher mendapatkan "recent apps" — sama seperti AOSP Recents.
 *
 * Requires: PACKAGE_USAGE_STATS permission (user harus grant manual di
 * Settings > Apps > Special app access > Usage access).
 */
class RecentAppsManager(private val context: Context) {

    private val _recentPackages = MutableStateFlow<List<String>>(emptyList())
    val recentPackages: StateFlow<List<String>> = _recentPackages.asStateFlow()

    /** Cek apakah permission Usage Stats sudah diberikan user */
    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Buka halaman Settings untuk user grant permission */
    fun openPermissionSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Baca recent apps dari UsageStatsManager.
     * Mengambil stats 7 hari terakhir, urutkan berdasarkan lastTimeUsed DESC,
     * filter launcher sendiri, ambil max [limit] package unik.
     */
    suspend fun loadRecentApps(
        excludePackages: Set<String> = emptySet(),
        limit: Int = 10,
    ) = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            _recentPackages.value = emptyList()
            return@withContext
        }

        runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000

            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                sevenDaysAgo,
                now,
            )

            val ownPkg = context.packageName
            val result = stats
                .filter { it.packageName != ownPkg }
                .filter { it.packageName !in excludePackages }
                .filter { it.lastTimeUsed > 0 }
                .sortedByDescending { it.lastTimeUsed }
                .map { it.packageName }
                .distinct()
                .take(limit)

            _recentPackages.value = result
        }.getOrElse {
            _recentPackages.value = emptyList()
        }
    }

    /**
     * Tambahkan package ke depan daftar recent (dipanggil saat user launch app).
     * Ini memastikan recent list langsung update tanpa harus tunggu polling.
     */
    fun onAppLaunched(packageName: String, excludePackages: Set<String> = emptySet()) {
        if (packageName == context.packageName) return
        if (packageName in excludePackages) return
        val current = _recentPackages.value.toMutableList()
        current.remove(packageName)
        current.add(0, packageName)
        _recentPackages.value = current.take(10)
    }
}
