package id.satria.launcher.recents

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageEvents
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

    companion object {
        /**
         * killedPackages sebagai companion object (static) — TETAP ADA selama proses hidup.
         * Ini kritis karena RecentAppsManager dibuat ulang setiap kali service restart,
         * tapi exclusion set tidak boleh hilang antar-sesi overlay.
         */
        private val killedPackages = mutableSetOf<String>()
    }

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
     * Load recent apps berdasarkan UsageEvents (app-switch events) — jauh lebih akurat
     * daripada UsageStats aggregate. Mengambil app yang berpindah ke foreground dalam
     * 6 jam terakhir, diurutkan dari yang paling baru.
     */
    suspend fun loadRecentApps(
        excludePackages: Set<String> = emptySet(),
        limit: Int = 15,
    ) = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext

        val effectiveExclude = excludePackages + killedPackages + context.packageName

        runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val sixHoursAgo = now - 6L * 60 * 60 * 1000

            // Query event-by-event — ACTIVITY_RESUMED = user benar-benar membuka app tersebut
            val events = usm.queryEvents(sixHoursAgo, now)
            val event = UsageEvents.Event()

            // Linked map: packageName → timestamp terakhir foreground
            val lastSeen = LinkedHashMap<String, Long>()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    val pkg = event.packageName ?: continue
                    if (pkg !in effectiveExclude) {
                        lastSeen[pkg] = event.timeStamp
                    }
                }
            }

            // Sort by most recent, deduplicate, take limit
            val sorted = lastSeen.entries
                .sortedByDescending { it.value }
                .map { it.key }
                .distinct()
                .take(limit)

            _recentPackages.value = sorted
        }
    }

    /**
     * Panggil saat user launch app.
     * Hapus dari killedPackages → app muncul kembali di recent saat dibuka lagi.
     */
    fun onAppLaunched(packageName: String, excludePackages: Set<String> = emptySet()) {
        if (packageName == context.packageName) return
        killedPackages.remove(packageName)
        val current = _recentPackages.value.toMutableList()
        current.remove(packageName)
        current.add(0, packageName)
        _recentPackages.value = current.take(15)
    }

    /**
     * Kill packages dan exclude dari tampilan.
     * - killBackgroundProcesses: membunuh proses background
     * - killedPackages: exclude dari query berikutnya (persists sepanjang hidup proses)
     */
    suspend fun killAndClearAll(
        packages: List<String> = _recentPackages.value.toList(),
    ) = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        packages.forEach { pkg ->
            runCatching { am.killBackgroundProcesses(pkg) }
            killedPackages.add(pkg)
        }
        // Update list secara atomik
        _recentPackages.value = _recentPackages.value.filter { it !in killedPackages }
    }

    /** Singkir satu package dari tampilan tanpa kill (karena sudah di-handle ActivityManager) */
    fun removeFromList(packageName: String) {
        killedPackages.add(packageName)
        _recentPackages.value = _recentPackages.value.filter { it != packageName }
    }
}
