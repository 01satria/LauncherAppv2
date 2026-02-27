package id.satria.launcher.recents

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton event bus untuk tombol Recent Apps.
 *
 * Menggunakan SharedFlow (bukan StateFlow) karena:
 * - SharedFlow adalah one-shot event — setiap emit() diterima oleh collector aktif
 * - StateFlow memiliki bug timing: jika nilai sudah `true` sebelum collector aktif,
 *   dan kemudian `true` lagi, `LaunchedEffect` tidak re-execute karena nilai tidak berubah
 * - replay=1 memastikan event tidak hilang meski launcher baru saja kembali ke foreground
 *   dan collector belum sempat subscribe
 * - Tidak perlu manual `consume()` karena SharedFlow tidak menyimpan state permanen
 */
object RecentAppsEvent {
    private val _events = MutableSharedFlow<Unit>(replay = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    /** Dipanggil AccessibilityService atau MainActivity saat tombol Recent ditekan */
    fun fire() {
        _events.tryEmit(Unit)
    }

    /** Reset replay cache setelah event dikonsumsi agar tidak re-trigger saat recompose */
    fun consume() {
        _events.resetReplayCache()
    }
}
