package id.satria.launcher.ui.component

import androidx.compose.ui.graphics.ImageBitmap

// Shared icon cache — bisa diakses AppGridItem, AppListItem, dan Dock
val iconCache = HashMap<String, ImageBitmap>(64)

/** Panggil saat app di-uninstall atau list di-refresh untuk bebaskan memori */
fun clearIconCache() = iconCache.clear()