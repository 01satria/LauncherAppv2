package id.satria.launcher

import android.app.Application
import android.content.SharedPreferences

/**
 * Application class — provides a single SharedPreferences instance
 * that services can use to read preferences without touching DataStore.
 *
 * Prefs.kt uses DataStore (proto-backed). Services that need a quick
 * synchronous read use this SharedPreferences mirror instead.
 * The launcher writes key "dark_mode" here whenever darkMode changes.
 */
class LauncherApp : Application() {
    companion object {
        const val PREFS_NAME = "satria_ui_prefs"
        const val KEY_DARK_MODE = "dark_mode"

        @Volatile
        private var instance: LauncherApp? = null
        fun get(): LauncherApp? = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    fun uiPrefs(): SharedPreferences =
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
}
