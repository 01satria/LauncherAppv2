package id.satria.launcher.recents

import android.content.Context
import androidx.compose.runtime.mutableStateListOf

/**
 * RecentAppsStore — singleton in-memory store.
 * Diisi dari launcher setiap kali user membuka app.
 * RAM minimal: hanya menyimpan packageName + label (max 8 entri, ~few KB).
 */
object RecentAppsStore {

    val recentPackages = mutableStateListOf<RecentAppEntry>()
    private const val MAX = 8

    fun add(pkg: String, label: String, context: Context) {
        if (pkg == context.packageName) return
        recentPackages.removeAll { it.packageName == pkg }
        recentPackages.add(0, RecentAppEntry(pkg, label))
        if (recentPackages.size > MAX) recentPackages.removeAt(recentPackages.size - 1)
    }
}

data class RecentAppEntry(
    val packageName: String,
    val label: String,
)
