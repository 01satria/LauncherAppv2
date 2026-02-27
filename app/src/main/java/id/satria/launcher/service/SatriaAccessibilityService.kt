package id.satria.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import id.satria.launcher.data.Prefs
import id.satria.launcher.recents.CustomRecentsPanelActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SatriaAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var prefs: Prefs
    private var isCustomRecentsEnabled = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        scope.launch {
            prefs.enableCustomRecents.collect { enabled -> isCustomRecentsEnabled = enabled }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, but required to override
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isCustomRecentsEnabled) {
            return super.onKeyEvent(event)
        }

        val action = event.action
        val keyCode = event.keyCode

        // Intercept Recent Apps key
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH && action == KeyEvent.ACTION_UP) {
            openCustomRecentsPanel()
            return true // Consume the event so the default system panel doesn't show
        }

        return super.onKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun openCustomRecentsPanel() {
        val intent =
                Intent(this, CustomRecentsPanelActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
        startActivity(intent)
    }
}
