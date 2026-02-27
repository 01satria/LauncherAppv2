package id.satria.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * SatriaAccessibilityService
 *
 * Mendeteksi tombol Recent Apps via performGlobalAction(GLOBAL_ACTION_RECENTS).
 * Service ini TIDAK memblokir Recent — ia mengirim broadcast ke launcher
 * sebelum sistem membuka Overview, sehingga launcher bisa intercept dan
 * menampilkan panel recent-nya sendiri.
 *
 * Cara aktifkan: Settings > Accessibility > Installed apps > Satria Launcher > ON
 */
class SatriaAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_RECENT_APPS = "id.satria.launcher.RECENT_APPS_BUTTON"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Tidak perlu handle event biasa
    }

    override fun onInterrupt() {}

    /**
     * Dipanggil saat user menekan tombol Recent (gesture atau button).
     * Di sinilah kita intercept dan kirim broadcast ke launcher.
     */
    override fun onKeyEvent(event: android.view.KeyEvent?): Boolean {
        if (event?.keyCode == android.view.KeyEvent.KEYCODE_APP_SWITCH &&
            event.action == android.view.KeyEvent.ACTION_UP) {
            sendRecentBroadcast()
            return false // false = tidak konsumsi, biarkan sistem juga handle
        }
        return super.onKeyEvent(event)
    }

    private fun sendRecentBroadcast() {
        val intent = Intent(ACTION_RECENT_APPS).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
}
