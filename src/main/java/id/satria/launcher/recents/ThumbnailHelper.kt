package id.satria.launcher.recents

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.util.concurrent.Executors

object ThumbnailHelper {
    private val threadPool = Executors.newFixedThreadPool(3) // Batasi 3 pool agar RAM hemat
    private val mainHandler = Handler(Looper.getMainLooper())

    fun loadThumbnailAsync(
            context: Context,
            imageView: ImageView,
            taskId: Int,
            td: ActivityManager.TaskDescription?
    ) {
        imageView.tag = taskId // Pengaman agar recycled view tidak tumpang tindih
        threadPool.execute {
            val bitmap = getTaskThumbnail(context, taskId, td)
            mainHandler.post { if (imageView.tag == taskId) imageView.setImageBitmap(bitmap) }
        }
    }

    private fun getTaskThumbnail(
            context: Context,
            taskId: Int,
            td: ActivityManager.TaskDescription?
    ): Bitmap? {
        // Mode 1: TaskSnapshot via Reflection (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val atmClass = Class.forName("android.app.ActivityTaskManager")
                val iAtm = atmClass.getMethod("getService").invoke(null)
                val snapshot =
                        iAtm.javaClass
                                .getMethod(
                                        "getTaskSnapshot",
                                        Int::class.javaPrimitiveType,
                                        Boolean::class.javaPrimitiveType
                                )
                                .invoke(iAtm, taskId, false)

                if (snapshot != null) {
                    val hwBuffer =
                            snapshot.javaClass.getMethod("getHardwareBuffer").invoke(snapshot) as?
                                    HardwareBuffer
                    val colorSpace =
                            snapshot.javaClass.getMethod("getColorSpace").invoke(snapshot) as?
                                    android.graphics.ColorSpace
                    if (hwBuffer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        return Bitmap.wrapHardwareBuffer(hwBuffer, colorSpace)
                    }
                }
            } catch (e: Exception) {
                /* Fallback */
            }
        }

        // Mode 2: Fallback ke warna utama task (Mirip Lawnchair fallback)
        var primaryColor = td?.primaryColor ?: Color.parseColor("#303030")
        if (primaryColor == 0) primaryColor = Color.parseColor("#303030")

        val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val gradient =
                GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(primaryColor, Color.parseColor("#121212"))
                )
        gradient.setBounds(0, 0, 400, 300)
        gradient.draw(canvas)
        return bitmap
    }
}
