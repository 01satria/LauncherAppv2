package id.satria.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap

class LauncherRepository(private val context: Context) {

    private val pm = context.packageManager

    suspend fun getInstalledApps(): List<AppData> = withContext(Dispatchers.IO) {
        val mainIntent = Intent(Intent.ACTION_MAIN).also { it.addCategory(Intent.CATEGORY_LAUNCHER) }
        pm.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA)
            .mapNotNull { ri ->
                runCatching {
                    val pkg   = ri.activityInfo.packageName
                    val label = ri.loadLabel(pm).toString()
                    if (id.satria.launcher.ui.component.iconCache.get(pkg) == null) {
                        runCatching {
                            val bmp = ri.loadIcon(pm)
                                .toBitmap(88, 88, android.graphics.Bitmap.Config.ARGB_8888)
                                .asImageBitmap()
                            id.satria.launcher.ui.component.iconCache.put(pkg, bmp)
                        }
                    }
                    AppData(label = label, packageName = pkg)
                }.getOrNull()
            }
            .filter { it.packageName != context.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Launch app dengan benar:
     * FLAG_ACTIVITY_NEW_TASK            — wajib dari non-Activity context
     * FLAG_ACTIVITY_RESET_TASK_IF_NEEDED — bring to front jika sudah berjalan
     *
     * Jika getLaunchIntent null (misal app system), coba buka via package manager
     * fallback agar recent tetap bisa launch app apapun.
     */
    fun launchApp(packageName: String) {
        runCatching {
            val intent = pm.getLaunchIntentForPackage(packageName) ?: run {
                // Fallback: buka halaman info app jika tidak ada launch intent
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
        }
    }

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
