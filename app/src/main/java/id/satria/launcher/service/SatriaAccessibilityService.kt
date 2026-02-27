package id.satria.launcher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import id.satria.launcher.recents.RecentAppsEvent

class SatriaAccessibilityService : AccessibilityService() {

    /**
     * onServiceConnected WAJIB di-override untuk set FLAG_REQUEST_FILTER_KEY_EVENTS
     * secara programatik. Hanya deklarasi di XML tidak cukup pada Android 9+.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    /**
     * Intercept KEYCODE_APP_SWITCH.
     * Return true = event dikonsumsi, sistem TIDAK membuka Overview.
     */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_APP_SWITCH
            && event.action == KeyEvent.ACTION_UP
        ) {
            RecentAppsEvent.fire()
            return true
        }
        return false
    }
}
