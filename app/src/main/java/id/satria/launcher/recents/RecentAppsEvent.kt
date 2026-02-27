package id.satria.launcher.recents

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton event bus untuk tombol Recent Apps.
 *
 * SharedFlow dengan replay=1:
 * - Setiap fire() adalah one-shot event yang diterima collector aktif
 * - replay=1 menjamin event tidak hilang jika launcher baru foreground
 * - consume() reset replay cache agar tidak re-trigger di recompose berikutnya
 */
object RecentAppsEvent {
    private val _events = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    /** Dipanggil AccessibilityService atau MainActivity saat Recents dideteksi */
    fun fire() {
        _events.tryEmit(Unit)
    }

    /** Dipanggil HomeScreen segera setelah event dikonsumsi */
    fun consume() {
        _events.resetReplayCache()
    }
}
