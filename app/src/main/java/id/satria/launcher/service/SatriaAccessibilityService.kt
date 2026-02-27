package id.satria.launcher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import id.satria.launcher.recents.RecentAppsEvent

class SatriaAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "SatriaA11y"

        // Package name sistem yang menampilkan Overview/Recents screen
        // Berbeda-beda per vendor dan versi Android
        private val RECENTS_PACKAGES = setOf(
            "com.android.systemui",          // AOSP / Pixel / OnePlus / Nothing
            "com.miui.home",                 // Xiaomi MIUI
            "com.miui.recents",              // Xiaomi MIUI (khusus recents)
            "com.samsung.android.app.taskedge", // Samsung
            "com.samsung.android.launcher",  // Samsung One UI
            "com.huawei.android.launcher",   // Huawei
            "com.oppo.launcher",             // OPPO
            "com.vivo.launcher",             // Vivo
            "com.realme.launcher",           // Realme
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo?.apply {
            flags = flags or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            // Pantau semua window state change untuk deteksi Recents screen
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        Log.d(TAG, "Accessibility service connected")
    }

    /**
     * Deteksi Recents/Overview screen via dua cara:
     *
     * 1. TYPE_WINDOW_STATE_CHANGED — fired saat Overview terbuka.
     *    className biasanya "RecentsActivity" atau mengandung "Recents"
     *    packageName adalah paket SystemUI atau launcher default vendor.
     *
     * 2. TYPE_WINDOWS_CHANGED — fired saat window baru muncul.
     *    Lebih reliable di beberapa device.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: return
                val cls = event.className?.toString() ?: ""

                Log.d(TAG, "WINDOW_STATE_CHANGED pkg=$pkg cls=$cls")

                // Deteksi Recents Activity
                val isRecentsClass = cls.contains("Recents", ignoreCase = true) ||
                        cls.contains("Overview", ignoreCase = true) ||
                        cls.contains("RecentsTv", ignoreCase = true)

                val isSystemUi = RECENTS_PACKAGES.any { pkg.startsWith(it) } ||
                        pkg == "com.android.systemui"

                if (isSystemUi && isRecentsClass) {
                    Log.d(TAG, "✅ Recents screen detected via WINDOW_STATE_CHANGED")
                    RecentAppsEvent.fire()
                }
            }

            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Beberapa device tidak fire WINDOW_STATE_CHANGED untuk Recents,
                // tapi fire TYPE_WINDOWS_CHANGED. Cek via rootInActiveWindow.
            }

            else -> { /* ignore */ }
        }
    }

    override fun onInterrupt() {}

    /**
     * Fallback: intercept KEYCODE_APP_SWITCH (3-button nav / device yang masih pakai physical key).
     * Return true = konsumsi event, sistem TIDAK membuka Overview bawaannya.
     */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        if (event.keyCode == KeyEvent.KEYCODE_APP_SWITCH &&
            event.action == KeyEvent.ACTION_UP
        ) {
            Log.d(TAG, "✅ KEYCODE_APP_SWITCH intercepted")
            RecentAppsEvent.fire()
            return true
        }
        return false
    }
}
