package id.satria.launcher.recents

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton event bus untuk Recent Apps button.
 *
 * Diakses langsung oleh SatriaAccessibilityService (fire)
 * dan HomeScreen (collect) — tanpa broadcast, tanpa Channel,
 * tanpa ketergantungan pada ViewModel instance.
 *
 * Karena AccessibilityService dan Activity berjalan dalam
 * proses yang sama, singleton ini selalu tersedia untuk keduanya.
 */
object RecentAppsEvent {
    private val _flow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val flow = _flow.asSharedFlow()

    /** Dipanggil AccessibilityService saat tombol Recent ditekan */
    fun fire() {
        _flow.tryEmit(Unit)
    }
}
