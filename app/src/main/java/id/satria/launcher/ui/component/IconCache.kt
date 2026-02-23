package id.satria.launcher.ui.component

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

// ── LruCache dengan batas ketat 2 MB ─────────────────────────────────────────
// Setiap icon ARGB_8888 96px = 36 KB → 2 MB cukup ±55 icon
// Icon yang tidak terlihat otomatis di-evict (least-recently-used)
// Batas diturunkan dari 4 MB → 2 MB karena bitmap sudah pakai RGB_565 (50% lebih hemat)
private const val MAX_CACHE_BYTES = 2 * 1024 * 1024 // 2 MB

val iconCache = object : LruCache<String, ImageBitmap>(MAX_CACHE_BYTES) {
    override fun sizeOf(key: String, value: ImageBitmap): Int =
        // RGB_565 = 2 byte/pixel (vs ARGB_8888 = 4 byte/pixel)
        value.width * value.height * 2
}

/** Panggil saat app di-uninstall untuk bebaskan memori */
fun clearIconCache() = iconCache.evictAll()

/** Hapus icon satu package — dipanggil saat app di-uninstall */
fun removeIconFromCache(pkg: String) = iconCache.remove(pkg)
