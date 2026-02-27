package id.satria.launcher.service

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import id.satria.launcher.recents.RecentAppsEvent

/**
 * SatriaAccessibilityService
 *
 * Mendeteksi tombol Recent Apps dan memanggil RecentAppsEvent.fire()
 * secara langsung — tidak pakai broadcast, tidak bisa gagal.
 *
 * Cara aktifkan: Settings → Accessibility → Installed apps
 *                → Cloudys Launcher → ON
 */
class SatriaAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_APP_SWITCH
            && event.action == KeyEvent.ACTION_UP
        ) {
            RecentAppsEvent.fire()
            // return true = konsumsi event, sistem TIDAK buka Overview sendiri
            return true
        }
        return false
    }
}
