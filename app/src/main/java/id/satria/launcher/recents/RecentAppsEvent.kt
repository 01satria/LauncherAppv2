package id.satria.launcher.recents

// RecentAppsEvent dihapus sepenuhnya.
// Trigger recent apps kini murni via swipe gesture dari tepi kiri layar
// yang dideteksi langsung di HomeScreen — tidak perlu event bus, tidak perlu
// accessibility service, tidak ada race condition, RAM minimal.
