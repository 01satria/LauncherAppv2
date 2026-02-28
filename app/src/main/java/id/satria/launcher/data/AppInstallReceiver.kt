package id.satria.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AppInstallReceiver — mendengarkan ACTION_PACKAGE_ADDED / REMOVED.
 * Launcher me-refresh daftar app via onResume MainActivity.
 */
class AppInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: MainViewModel.refreshApps() dipanggil dari onResume
    }
}
