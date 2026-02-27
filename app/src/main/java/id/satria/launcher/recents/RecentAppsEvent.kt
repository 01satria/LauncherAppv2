package id.satria.launcher.recents

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton state untuk Recent Apps button.
 *
 * Pakai StateFlow (bukan SharedFlow) karena:
 * - StateFlow menyimpan nilai terakhir — tidak hilang meski collector mati sementara
 * - Saat launcher kembali ke foreground, composable langsung baca state = true
 * - Setelah ditampilkan, HomeScreen reset ke false
 */
object RecentAppsEvent {
    private val _pending = MutableStateFlow(false)
    val pending: StateFlow<Boolean> = _pending.asStateFlow()

    /** Dipanggil AccessibilityService saat tombol Recent ditekan */
    fun fire() {
        _pending.value = true
    }

    /** Dipanggil HomeScreen setelah panel ditampilkan */
    fun consume() {
        _pending.value = false
    }
}
