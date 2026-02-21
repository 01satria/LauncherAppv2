package id.satria.launcher.ui.component

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

// ── LruCache menggantikan HashMap biasa ────────────────────────────────────
// LruCache otomatis membuang entry paling lama saat mendekati batas memori,
// sehingga RAM tidak terus naik seiring banyaknya app yang di-scroll.
//
// Batas: 4 MB — cukup untuk ~55 icon @96px (96*96*4 bytes ≈ 36KB per icon)
private const val MAX_CACHE_BYTES = 4 * 1024 * 1024 // 4 MB

val iconCache = object : LruCache<String, ImageBitmap>(MAX_CACHE_BYTES) {
    override fun sizeOf(key: String, value: ImageBitmap): Int {
        // Ukuran dalam bytes: width * height * 4 (ARGB_8888)
        return value.width * value.height * 4
    }
}

/** Panggil saat app di-uninstall atau list di-refresh untuk bebaskan memori */
fun clearIconCache() = iconCache.evictAll()