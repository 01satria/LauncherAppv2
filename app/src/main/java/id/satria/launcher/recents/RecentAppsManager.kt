package id.satria.launcher.recents

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf

/**
 * RecentAppsManager
 *
 * Mengambil daftar app yang baru dipakai dari UsageStatsManager (data sistem).
 * Tidak perlu tracking manual — data selalu akurat karena langsung dari OS.
 *
 * Permission yang dibutuhkan: PACKAGE_USAGE_STATS
 * (user grant sekali di Settings → Privacy → Usage access)
 */
object RecentAppsManager {

    data class RecentApp(val packageName: String, val label: String)

    /**
     * Ambil daftar recent apps dari sistem (max [limit] item).
     * Filter: buang launcher sendiri, buang app yang tidak bisa dibuka.
     */
    fun getRecents(context: Context, limit: Int = 8): List<RecentApp> {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val pm  = context.packageManager
            val now = System.currentTimeMillis()

            // Ambil usage stats 3 hari terakhir
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 3L * 24 * 60 * 60 * 1000,
                now
            ) ?: return emptyList()

            stats
                .filter { it.packageName != context.packageName }   // buang launcher
                .filter { it.lastTimeUsed > 0 }
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null } // hanya app yang bisa dibuka
                .sortedByDescending { it.lastTimeUsed }
                .take(limit)
                .mapNotNull { stat ->
                    runCatching {
                        val label = pm.getApplicationLabel(
                            pm.getApplicationInfo(stat.packageName, 0)
                        ).toString()
                        RecentApp(stat.packageName, label)
                    }.getOrNull()
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Cek apakah permission PACKAGE_USAGE_STATS sudah di-grant user.
     */
    fun hasPermission(context: Context): Boolean {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 1000 * 60,
                now
            )
            !stats.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
