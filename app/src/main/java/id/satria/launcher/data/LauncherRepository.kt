package id.satria.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LauncherRepository(private val context: Context) {

    private val pm = context.packageManager

    // Pengganti InstalledApps.getSortedApps()
    suspend fun getInstalledApps(): List<AppData> = withContext(Dispatchers.IO) {
        val mainIntent = Intent(Intent.ACTION_MAIN).also { it.addCategory(Intent.CATEGORY_LAUNCHER) }
        pm.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA)
            .mapNotNull { ri ->
                runCatching {
                    AppData(
                        label       = ri.loadLabel(pm).toString(),
                        packageName = ri.activityInfo.packageName,
                        icon        = ri.loadIcon(pm),
                    )
                }.getOrNull()
            }
            .filter { it.packageName != context.packageName } // exclude launcher itself
            .sortedBy { it.label.lowercase() }
    }

    // Pengganti RNLauncherKitHelper.launchApplication()
    fun launchApp(packageName: String) {
        runCatching {
            pm.getLaunchIntentForPackage(packageName)?.also {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        }
    }

    // Pengganti UninstallModule.uninstallApp()
    fun uninstallApp(packageName: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
