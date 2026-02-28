package id.satria.launcher

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.satria.launcher.data.*
import id.satria.launcher.recents.RecentAppsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)
    val repo  = LauncherRepository(app)
    val recentAppsManager = RecentAppsManager(app)

    // Recent apps — list package names, paling baru di depan
    val recentApps: StateFlow<List<String>> = recentAppsManager.recentPackages

    // Permission Usage Stats
    private val _hasUsagePermission = MutableStateFlow(recentAppsManager.hasPermission())
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppData>>(emptyList())
    val allApps: StateFlow<List<AppData>> = _allApps.asStateFlow()

    // ── Flow kritis (HomeScreen selalu butuh) — SharingStarted.Eagerly ──────
    val showNames         = prefs.showNames.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val layoutMode        = prefs.layoutMode.stateIn(viewModelScope, SharingStarted.Eagerly, "grid")
    val iconSize          = prefs.iconSize.stateIn(viewModelScope, SharingStarted.Eagerly, 54)
    val dockIconSize      = prefs.dockIconSize.stateIn(viewModelScope, SharingStarted.Eagerly, 56)
    val hiddenPackages    = prefs.hiddenPackages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dockPackages      = prefs.dockPackages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val darkMode          = prefs.darkMode.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val recentAppsEnabled = prefs.recentAppsEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val gridCols          = prefs.gridCols.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_GRID_COLS)
    val gridRows          = prefs.gridRows.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_GRID_ROWS)
    val showHidden        = prefs.showHidden.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Flow sekunder — WhileSubscribed(5s) hemat RAM saat layar home ────────
    private val WS = SharingStarted.WhileSubscribed(5_000)
    val userName         = prefs.userName.stateIn(viewModelScope, WS, "User")
    val assistantName    = prefs.assistantName.stateIn(viewModelScope, WS, "Assistant")
    val avatarPath       = prefs.avatarPath.stateIn(viewModelScope, WS, null)
    val todos            = prefs.todos.stateIn(viewModelScope, WS, emptyList())
    val countdowns       = prefs.countdowns.stateIn(viewModelScope, WS, emptyList())
    val weatherLocations = prefs.weatherLocations.stateIn(viewModelScope, WS, emptyList())
    val prayerCities     = prefs.prayerCities.stateIn(viewModelScope, WS, emptyList())
    val prayerCache      = prefs.prayerCache.stateIn(viewModelScope, WS, "{}")
    val habits           = prefs.habits.stateIn(viewModelScope, WS, emptyList())
    val moneyWallets     = prefs.moneyWallets.stateIn(viewModelScope, WS, emptyList())
    val moneyTransactions = prefs.moneyTransactions.stateIn(viewModelScope, WS, emptyList())

    private val _avatarVersion = MutableStateFlow(0)
    val avatarVersion: StateFlow<Int> = _avatarVersion.asStateFlow()

    val filteredApps = combine(allApps, hiddenPackages, dockPackages, showHidden) {
        apps, hidden, dock, showH ->
        apps.filter { a ->
            !dock.contains(a.packageName) && (showH || !hidden.contains(a.packageName))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dockApps = combine(allApps, dockPackages) { apps, dock ->
        dock.mapNotNull { pkg -> apps.find { it.packageName == pkg } }.take(5)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refreshApps()
        // Tidak ada refreshRecentApps() di init — recent dimuat on-demand saat panel dibuka
    }

    fun refreshApps() = viewModelScope.launch {
        val current = _allApps.value
        val fresh   = repo.getInstalledApps()
        if (current.isEmpty() || current.size != fresh.size ||
            current.map { it.packageName }.toSet() != fresh.map { it.packageName }.toSet()) {
            _allApps.value = fresh
        }
        if (current.isNotEmpty()) {
            val freshPkgs = fresh.map { it.packageName }.toSet()
            current.filter { it.packageName !in freshPkgs }
                   .forEach { id.satria.launcher.ui.component.iconCache.remove(it.packageName) }
        }
    }

    /**
     * Launch app + update recent list in-memory (no IO, instant).
     * Menggunakan FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
     * agar app yang sedang background di-bring ke foreground, bukan dibuat instance baru.
     */
    fun launchApp(pkg: String) {
        repo.launchApp(pkg)
        if (recentAppsEnabled.value) {
            recentAppsManager.onAppLaunched(pkg, dockPackages.value.toSet())
        }
    }

    fun uninstallApp(pkg: String) {
        _allApps.value = _allApps.value.filter { it.packageName != pkg }
        id.satria.launcher.ui.component.removeIconFromCache(pkg)
        repo.uninstallApp(pkg)
    }

    fun hideApp(pkg: String) = viewModelScope.launch {
        val newHidden = hiddenPackages.value.toMutableList()
        if (!newHidden.contains(pkg)) newHidden.add(pkg)
        prefs.setHiddenPackages(newHidden)
        if (dockPackages.value.contains(pkg)) prefs.setDockPackages(dockPackages.value.filter { it != pkg })
    }
    fun unhideApp(pkg: String)  = viewModelScope.launch { prefs.setHiddenPackages(hiddenPackages.value.filter { it != pkg }) }
    fun toggleDock(pkg: String) = viewModelScope.launch {
        val dock = dockPackages.value.toMutableList()
        if (dock.contains(pkg)) { dock.remove(pkg) }
        else {
            if (dock.size >= 5) return@launch
            dock.add(pkg)
            if (hiddenPackages.value.contains(pkg)) prefs.setHiddenPackages(hiddenPackages.value.filter { it != pkg })
        }
        prefs.setDockPackages(dock)
    }

    fun setDarkMode(v: Boolean) = viewModelScope.launch { prefs.setDarkMode(v) }

    /**
     * Muat recent apps dari UsageStatsManager — on-demand saja,
     * dipanggil saat panel recent dibuka (bukan polling).
     */
    fun refreshRecentApps() = viewModelScope.launch {
        recentAppsManager.loadRecentApps(excludePackages = dockPackages.value.toSet())
    }

    /** Kill semua background process dari recent apps, lalu kosongkan list */
    fun clearRecentApps() = viewModelScope.launch {
        recentAppsManager.killAndClearAll()
    }

    /** Cek ulang status Usage Stats permission */
    fun checkUsagePermission() {
        _hasUsagePermission.value = recentAppsManager.hasPermission()
        // Tidak auto-refresh recent di sini — hemat CPU/RAM
    }

    fun openUsagePermissionSettings() = recentAppsManager.openPermissionSettings()

    fun setRecentAppsEnabled(v: Boolean) = viewModelScope.launch {
        prefs.setRecentAppsEnabled(v)
        // Sync EdgeSwipeService di background thread setelah pref tersimpan
        // Dipanggil dari MainActivity.syncEdgeSwipeService() saat onResume juga
    }
    fun setGridCols(v: Int)       = viewModelScope.launch { prefs.setGridCols(v) }
    fun setGridRows(v: Int)       = viewModelScope.launch { prefs.setGridRows(v) }

    fun saveUserName(v: String)      = viewModelScope.launch { prefs.setUserName(v) }
    fun saveAssistantName(v: String) = viewModelScope.launch { prefs.setAssistantName(v) }
    fun setShowHidden(v: Boolean)    = viewModelScope.launch { prefs.setShowHidden(v) }
    fun setShowNames(v: Boolean)     = viewModelScope.launch { prefs.setShowNames(v) }
    fun setLayoutMode(v: String)     = viewModelScope.launch { prefs.setLayoutMode(v) }
    fun setIconSize(v: Int)          = viewModelScope.launch { prefs.setIconSize(v) }
    fun setDockIconSize(v: Int)      = viewModelScope.launch { prefs.setDockIconSize(v) }

    fun saveAvatar(bitmap: Bitmap) = viewModelScope.launch {
        val file = File(getApplication<Application>().filesDir, "avatar.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        prefs.setAvatarPath(file.absolutePath)
        _avatarVersion.value++
    }

    // ── Todo ───────────────────────────────────────────────────────────────
    fun addTodo(text: String)   = viewModelScope.launch { prefs.setTodos(todos.value + TodoItem(makeId(), text, false)) }
    fun toggleTodo(id: String)  = viewModelScope.launch { prefs.setTodos(todos.value.map { if (it.id == id) it.copy(done = !it.done) else it }) }
    fun removeTodo(id: String)  = viewModelScope.launch { prefs.setTodos(todos.value.filter { it.id != id }) }

    // ── Countdown ──────────────────────────────────────────────────────────
    fun addCountdown(name: String, isoDate: String) = viewModelScope.launch { prefs.setCountdowns(countdowns.value + CountdownItem(makeId(), name, isoDate)) }
    fun removeCountdown(id: String) = viewModelScope.launch { prefs.setCountdowns(countdowns.value.filter { it.id != id }) }

    // ── Weather ────────────────────────────────────────────────────────────
    fun addWeatherLocation(loc: String) = viewModelScope.launch {
        if (weatherLocations.value.contains(loc)) return@launch
        if (weatherLocations.value.size >= 8) return@launch
        prefs.setWeatherLocations(weatherLocations.value + loc)
    }
    fun removeWeatherLocation(loc: String) = viewModelScope.launch { prefs.setWeatherLocations(weatherLocations.value.filter { it != loc }) }

    // ── Prayer ─────────────────────────────────────────────────────────────
    fun addPrayerCity(city: String) = viewModelScope.launch {
        if (prayerCities.value.contains(city)) return@launch
        if (prayerCities.value.size >= 8) return@launch
        prefs.setPrayerCities(prayerCities.value + city)
    }
    fun removePrayerCity(city: String) = viewModelScope.launch {
        prefs.setPrayerCities(prayerCities.value.filter { it != city })
        val cache = org.json.JSONObject(prayerCache.value)
        cache.remove(city)
        prefs.setPrayerCache(cache.toString())
    }
    fun updatePrayerCache(city: String, timingsJson: String) = viewModelScope.launch {
        if (!prayerCities.value.contains(city)) return@launch
        val cache = org.json.JSONObject(prayerCache.value)
        cache.put(city, timingsJson)
        prefs.setPrayerCache(cache.toString())
    }

    // ── Habits ─────────────────────────────────────────────────────────────
    fun addHabit(name: String, emoji: String) = viewModelScope.launch {
        prefs.setHabits(habits.value + HabitItem(makeId(), name, emoji, emptyList(), 0, System.currentTimeMillis()))
    }
    fun deleteHabit(id: String) = viewModelScope.launch { prefs.setHabits(habits.value.filter { it.id != id }) }
    fun checkHabit(id: String) = viewModelScope.launch {
        val today = todayKey()
        prefs.setHabits(habits.value.map { habit ->
            if (habit.id != id) return@map habit
            val alreadyDone = habit.doneToday(today)
            if (alreadyDone) {
                val newDates = habit.doneDates.filter { it != today }
                habit.copy(doneDates = newDates, streak = calcStreak(newDates))
            } else {
                val newDates = habit.doneDates + today
                habit.copy(doneDates = newDates.takeLast(90), streak = calcStreak(newDates))
            }
        })
    }

    private fun calcStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        var streak = 0
        while (true) {
            val key = fmt.format(cal.time)
            if (dates.contains(key)) { streak++; cal.add(Calendar.DAY_OF_YEAR, -1) }
            else break
        }
        return streak
    }

    fun resetHabitsIfNewDay() { /* streak auto-calc dari doneDates */ }

    // ── Money Management ───────────────────────────────────────────────────
    fun addMoneyWallet(name: String, emoji: String, color: String, currency: String) = viewModelScope.launch {
        val wallet = MoneyWallet(makeId(), name, emoji, currency, color)
        prefs.setMoneyWallets(prefs.moneyWallets.first() + wallet)
    }
    fun deleteMoneyWallet(walletId: String) = viewModelScope.launch {
        val current  = prefs.moneyWallets.first()
        val currentTx = prefs.moneyTransactions.first()
        prefs.setMoneyWallets(current.filter { it.id != walletId })
        prefs.setMoneyTransactions(currentTx.filter { it.walletId != walletId && it.toWalletId != walletId })
    }
    fun addMoneyTransaction(
        walletId: String, type: String, amount: Double,
        categoryKey: String, note: String, date: String, toWalletId: String = ""
    ) = viewModelScope.launch {
        val tx = MoneyTransaction(makeId(), walletId, type, amount, categoryKey, note, date, toWalletId)
        val current = prefs.moneyTransactions.first()
        prefs.setMoneyTransactions((current + tx).takeLast(1000))
    }
    fun deleteMoneyTransaction(txId: String) = viewModelScope.launch {
        prefs.setMoneyTransactions(prefs.moneyTransactions.first().filter { it.id != txId })
    }
    fun exportMoneyDataJson(): String {
        val json = Json { prettyPrint = true }
        return """{"wallets":${json.encodeToString(moneyWallets.value)},"transactions":${json.encodeToString(moneyTransactions.value)}}"""
    }
    fun importMoneyDataJson(jsonStr: String): Boolean {
        return try {
            val j   = Json { ignoreUnknownKeys = true }
            val obj = j.parseToJsonElement(jsonStr).jsonObject
            val wallets = j.decodeFromString<List<MoneyWallet>>(obj["wallets"]?.toString() ?: return false)
            val txs     = j.decodeFromString<List<MoneyTransaction>>(obj["transactions"]?.toString() ?: return false)
            viewModelScope.launch {
                prefs.setMoneyWallets(wallets)
                prefs.setMoneyTransactions(txs)
            }
            true
        } catch (e: Exception) { false }
    }

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private fun makeId()   = "${System.currentTimeMillis()}-${(Math.random() * 100000).toInt()}"
}
