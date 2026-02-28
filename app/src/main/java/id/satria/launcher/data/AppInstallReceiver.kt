package id.satria.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * AppInstallReceiver — mendengarkan ACTION_PACKAGE_ADDED.
 *
 * Classifier HANYA berjalan saat app baru diinstall,
 * TIDAK berjalan periodik — nol overhead saat idle.
 *
 * Flow:
 *   PACKAGE_ADDED → baca overrides → classify 1 app → simpan kategori → done
 */
class AppInstallReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json  = Json { ignoreUnknownKeys = true }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return

        // Abaikan update (replace) — hanya proses install baru
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

        val packageName = intent.data?.schemeSpecificPart ?: return

        val pm    = context.packageManager
        val label = runCatching {
            pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                .loadLabel(pm).toString()
        }.getOrDefault(packageName)

        // Jalankan di IO thread — tidak blocking receiver
        scope.launch {
            val prefs = Prefs(context)

            // Baca overrides user yang sudah ada
            val overrides: Map<String, String> = runCatching {
                json.decodeFromString<Map<String, String>>(
                    prefs.categoryOverrides.first()
                )
            }.getOrDefault(emptyMap())

            // Classify satu app baru
            val category = AppClassifier.classify(packageName, label, overrides)

            // Gabungkan ke map kategori yang sudah ada
            val existing: MutableMap<String, String> = runCatching {
                json.decodeFromString<Map<String, String>>(
                    prefs.appCategories.first()
                ).toMutableMap()
            }.getOrDefault(mutableMapOf())

            existing[packageName] = category.name
            prefs.setAppCategories(json.encodeToString(existing as Map<String, String>))
        }
    }
}
