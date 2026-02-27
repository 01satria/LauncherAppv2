package id.satria.launcher.recents

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * EdgeSwipeEvent — one-shot event dari EdgeSwipeService ke HomeScreen.
 *
 * SharedFlow replay=1 + extraBufferCapacity=1:
 * - replay=1: event tidak hilang jika launcher baru foreground
 * - tryEmit() non-blocking, aman dipanggil dari main thread TouchEvent
 * - consume() reset replay agar tidak re-trigger saat recompose
 */
object EdgeSwipeEvent {
    private val _flow = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val flow: SharedFlow<Unit> = _flow.asSharedFlow()

    fun fire() { _flow.tryEmit(Unit) }
    fun consume() { _flow.resetReplayCache() }
}
