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

    // Pengganti InstalledApps.getSortedApps()
    // Optimasi RAM: setiap Drawable dimuat sekali, langsung di-cache sebagai
    // RGB_565 Bitmap (50% lebih ringan dari ARGB_8888), lalu Drawable dibuang.
    // AppData hanya menyimpan label+packageName (ringan), bukan Drawable besar.
    suspend fun getInstalledApps(): List<AppData> = withContext(Dispatchers.IO) {
        val mainIntent = Intent(Intent.ACTION_MAIN).also { it.addCategory(Intent.CATEGORY_LAUNCHER) }
        pm.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA)
            .mapNotNull { ri ->
                runCatching {
                    val pkg = ri.activityInfo.packageName
                    val label = ri.loadLabel(pm).toString()
                    // Pre-cache icon sebagai RGB_565 bitmap agar Drawable tidak tertahan di RAM
                    // iconCache sudah memakai LruCache — otomatis evict jika penuh
                    if (id.satria.launcher.ui.component.iconCache.get(pkg) == null) {
                        runCatching {
                            val px  = 88 // cukup tajam s/d hdpi, hemat RAM vs 96+
                            val bmp = ri.loadIcon(pm)
                                .toBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
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
