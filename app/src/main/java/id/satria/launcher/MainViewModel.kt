package id.satria.launcher

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.satria.launcher.data.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)
    val repo = LauncherRepository(app)

    private val _allApps = MutableStateFlow<List<AppData>>(emptyList())
    val allApps: StateFlow<List<AppData>> = _allApps.asStateFlow()

    // Flow kritis (selalu dibutuhkan HomeScreen) → Eagerly agar tidak ada delay
    val showNames = prefs.showNames.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val layoutMode = prefs.layoutMode.stateIn(viewModelScope, SharingStarted.Eagerly, "grid")
    val iconSize = prefs.iconSize.stateIn(viewModelScope, SharingStarted.Eagerly, 54)
    val dockIconSize = prefs.dockIconSize.stateIn(viewModelScope, SharingStarted.Eagerly, 56)
    val hiddenPackages =
            prefs.hiddenPackages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dockPackages =
            prefs.dockPackages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val darkMode = prefs.darkMode.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val gridCols =
            prefs.gridCols.stateIn(
                    viewModelScope,
                    SharingStarted.Eagerly,
                    id.satria.launcher.data.DEFAULT_GRID_COLS
            )
    val gridRows =
            prefs.gridRows.stateIn(
                    viewModelScope,
                    SharingStarted.Eagerly,
                    id.satria.launcher.data.DEFAULT_GRID_ROWS
            )
    val showHidden = prefs.showHidden.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val enableCustomRecents =
            prefs.enableCustomRecents.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // Flow sekunder (hanya dibutuhkan saat Dashboard/tool terbuka)
    // WhileSubscribed(5_000): flow berhenti 5 detik setelah tidak ada subscriber,
    // lalu mulai lagi saat dibutuhkan — hemat coroutine & RAM saat layar home
    private val WS = SharingStarted.WhileSubscribed(5_000)
    val userName = prefs.userName.stateIn(viewModelScope, WS, "User")
    val assistantName = prefs.assistantName.stateIn(viewModelScope, WS, "Assistant")
    val avatarPath = prefs.avatarPath.stateIn(viewModelScope, WS, null)
    val todos = prefs.todos.stateIn(viewModelScope, WS, emptyList())
    val countdowns = prefs.countdowns.stateIn(viewModelScope, WS, emptyList())
    val weatherLocations = prefs.weatherLocations.stateIn(viewModelScope, WS, emptyList())
    val prayerCities = prefs.prayerCities.stateIn(viewModelScope, WS, emptyList())
    val prayerCache = prefs.prayerCache.stateIn(viewModelScope, WS, "{}")
    val habits = prefs.habits.stateIn(viewModelScope, WS, emptyList())
    val moneyWallets = prefs.moneyWallets.stateIn(viewModelScope, WS, emptyList())
    val moneyTransactions = prefs.moneyTransactions.stateIn(viewModelScope, WS, emptyList())

    private val _avatarVersion = MutableStateFlow(0)
    val avatarVersion: StateFlow<Int> = _avatarVersion.asStateFlow()

    val filteredApps =
            combine(allApps, hiddenPackages, dockPackages, showHidden) { apps, hidden, dock, showH
                        ->
                        apps.filter { a ->
                            !dock.contains(a.packageName) &&
                                    (showH || !hidden.contains(a.packageName))
                        }
                    }
                    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dockApps =
            combine(allApps, dockPackages) { apps, dock ->
                        dock.mapNotNull { pkg -> apps.find { it.packageName == pkg } }.take(5)
                    }
                    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refreshApps()
    }

    fun refreshApps() =
            viewModelScope.launch {
                // Jika sudah ada data, hanya refresh jika jumlah app berubah
                // Ini mencegah reload iconCache penuh setiap kali onResume
                val current = _allApps.value
                val fresh = repo.getInstalledApps()
                if (current.isEmpty() ||
                                current.size != fresh.size ||
                                current.map { it.packageName }.toSet() !=
                                        fresh.map { it.packageName }.toSet()
                ) {
                    _allApps.value = fresh
                }
                // Bersihkan icon yang packageName-nya tidak ada lagi
                if (current.isNotEmpty()) {
                    val freshPkgs = fresh.map { it.packageName }.toSet()
                    current.filter { it.packageName !in freshPkgs }.forEach {
                        id.satria.launcher.ui.component.iconCache.remove(it.packageName)
                    }
                }
            }
    fun launchApp(pkg: String) = repo.launchApp(pkg)
    fun uninstallApp(pkg: String) {
        _allApps.value = _allApps.value.filter { it.packageName != pkg }
        id.satria.launcher.ui.component.removeIconFromCache(pkg) // bebaskan bitmap dari LruCache
        repo.uninstallApp(pkg)
    }

    fun hideApp(pkg: String) =
            viewModelScope.launch {
                val newHidden = hiddenPackages.value.toMutableList()
                if (!newHidden.contains(pkg)) newHidden.add(pkg)
                prefs.setHiddenPackages(newHidden)
                if (dockPackages.value.contains(pkg))
                        prefs.setDockPackages(dockPackages.value.filter { it != pkg })
            }
    fun unhideApp(pkg: String) =
            viewModelScope.launch {
                prefs.setHiddenPackages(hiddenPackages.value.filter { it != pkg })
            }
    fun toggleDock(pkg: String) =
            viewModelScope.launch {
                val dock = dockPackages.value.toMutableList()
                if (dock.contains(pkg)) {
                    dock.remove(pkg)
                } else {
                    if (dock.size >= 5) return@launch
                    dock.add(pkg)
                    if (hiddenPackages.value.contains(pkg))
                            prefs.setHiddenPackages(hiddenPackages.value.filter { it != pkg })
                }
                prefs.setDockPackages(dock)
            }

    fun setDarkMode(v: Boolean) = viewModelScope.launch { prefs.setDarkMode(v) }
    fun setGridCols(v: Int) = viewModelScope.launch { prefs.setGridCols(v) }
    fun setGridRows(v: Int) = viewModelScope.launch { prefs.setGridRows(v) }
    fun setEnableCustomRecents(v: Boolean) =
            viewModelScope.launch { prefs.setEnableCustomRecents(v) }

    fun saveUserName(v: String) = viewModelScope.launch { prefs.setUserName(v) }
    fun saveAssistantName(v: String) = viewModelScope.launch { prefs.setAssistantName(v) }
    fun setShowHidden(v: Boolean) = viewModelScope.launch { prefs.setShowHidden(v) }
    fun setShowNames(v: Boolean) = viewModelScope.launch { prefs.setShowNames(v) }
    fun setLayoutMode(v: String) = viewModelScope.launch { prefs.setLayoutMode(v) }
    fun setIconSize(v: Int) = viewModelScope.launch { prefs.setIconSize(v) }
    fun setDockIconSize(v: Int) = viewModelScope.launch { prefs.setDockIconSize(v) }

    fun saveAvatar(bitmap: Bitmap) =
            viewModelScope.launch {
                val file = File(getApplication<Application>().filesDir, "avatar.jpg")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                prefs.setAvatarPath(file.absolutePath)
                _avatarVersion.value++
            }

    // ── Todo ───────────────────────────────────────────────────────────────
    fun addTodo(text: String) =
            viewModelScope.launch { prefs.setTodos(todos.value + TodoItem(makeId(), text, false)) }
    fun toggleTodo(id: String) =
            viewModelScope.launch {
                prefs.setTodos(
                        todos.value.map { if (it.id == id) it.copy(done = !it.done) else it }
                )
            }
    fun removeTodo(id: String) =
            viewModelScope.launch { prefs.setTodos(todos.value.filter { it.id != id }) }

    // ── Countdown ──────────────────────────────────────────────────────────
    fun addCountdown(name: String, isoDate: String) =
            viewModelScope.launch {
                prefs.setCountdowns(countdowns.value + CountdownItem(makeId(), name, isoDate))
            }
    fun removeCountdown(id: String) =
            viewModelScope.launch { prefs.setCountdowns(countdowns.value.filter { it.id != id }) }

    // ── Weather ────────────────────────────────────────────────────────────
    fun addWeatherLocation(loc: String) =
            viewModelScope.launch {
                if (weatherLocations.value.contains(loc)) return@launch
                if (weatherLocations.value.size >= 8) return@launch
                prefs.setWeatherLocations(weatherLocations.value + loc)
            }
    fun removeWeatherLocation(loc: String) =
            viewModelScope.launch {
                prefs.setWeatherLocations(weatherLocations.value.filter { it != loc })
            }

    // ── Prayer ─────────────────────────────────────────────────────────────
    fun addPrayerCity(city: String) =
            viewModelScope.launch {
                if (prayerCities.value.contains(city)) return@launch
                if (prayerCities.value.size >= 8) return@launch
                prefs.setPrayerCities(prayerCities.value + city)
            }
    fun removePrayerCity(city: String) =
            viewModelScope.launch {
                prefs.setPrayerCities(prayerCities.value.filter { it != city })
                // Hapus cache kota yang dihapus
                val cache = org.json.JSONObject(prayerCache.value)
                cache.remove(city)
                prefs.setPrayerCache(cache.toString())
            }
    fun updatePrayerCache(city: String, timingsJson: String) =
            viewModelScope.launch {
                if (!prayerCities.value.contains(city)) return@launch // hanya simpan jika disave
                val cache = org.json.JSONObject(prayerCache.value)
                cache.put(city, timingsJson)
                prefs.setPrayerCache(cache.toString())
            }

    // ── Habits ─────────────────────────────────────────────────────────────
    fun addHabit(name: String, emoji: String) =
            viewModelScope.launch {
                prefs.setHabits(
                        habits.value +
                                HabitItem(
                                        makeId(),
                                        name,
                                        emoji,
                                        emptyList(),
                                        0,
                                        System.currentTimeMillis()
                                )
                )
            }
    fun deleteHabit(id: String) =
            viewModelScope.launch { prefs.setHabits(habits.value.filter { it.id != id }) }
    fun checkHabit(id: String) =
            viewModelScope.launch {
                val today = todayKey()
                prefs.setHabits(
                        habits.value.map { habit ->
                            if (habit.id != id) return@map habit
                            val alreadyDone = habit.doneToday(today)
                            if (alreadyDone) {
                                // Uncheck
                                val newDates = habit.doneDates.filter { it != today }
                                habit.copy(doneDates = newDates, streak = calcStreak(newDates))
                            } else {
                                val newDates = habit.doneDates + today
                                habit.copy(
                                        doneDates = newDates.takeLast(90),
                                        streak = calcStreak(newDates)
                                )
                            }
                        }
                )
            }

    private fun calcStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        var streak = 0
        while (true) {
            val key = fmt.format(cal.time)
            if (dates.contains(key)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        return streak
    }

    fun resetHabitsIfNewDay() {
        /* Streaks auto-calc from doneDates — no reset needed */
    }

    // ── Money Management ────────────────────────────────────────────────────
    // CATATAN: Semua fungsi baca state pakai .value dari StateFlow yang sudah
    // terhubung ke DataStore. WhileSubscribed memastikan flow aktif selama
    // DashboardScreen terbuka, sehingga .value selalu fresh.
    // Data disimpan ke DataStore (file proto di /data/data/…/datastore/) —
    // persist melewati restart HP, clear cache, bahkan app update.

    fun addMoneyWallet(name: String, emoji: String, color: String, currency: String) =
            viewModelScope.launch {
                val wallet =
                        id.satria.launcher.data.MoneyWallet(makeId(), name, emoji, currency, color)
                // Baca langsung dari prefs flow untuk hindari race condition
                prefs.setMoneyWallets(prefs.moneyWallets.first() + wallet)
            }
    fun deleteMoneyWallet(walletId: String) =
            viewModelScope.launch {
                val current = prefs.moneyWallets.first()
                val currentTx = prefs.moneyTransactions.first()
                prefs.setMoneyWallets(current.filter { it.id != walletId })
                prefs.setMoneyTransactions(
                        currentTx.filter { it.walletId != walletId && it.toWalletId != walletId }
                )
            }
    fun addMoneyTransaction(
            walletId: String,
            type: String,
            amount: Double,
            categoryKey: String,
            note: String,
            date: String,
            toWalletId: String = ""
    ) =
            viewModelScope.launch {
                val tx =
                        id.satria.launcher.data.MoneyTransaction(
                                makeId(),
                                walletId,
                                type,
                                amount,
                                categoryKey,
                                note,
                                date,
                                toWalletId
                        )
                val current = prefs.moneyTransactions.first()
                // Simpan max 1000 transaksi terbaru agar DataStore tidak terlalu besar
                prefs.setMoneyTransactions((current + tx).takeLast(1000))
            }
    fun deleteMoneyTransaction(txId: String) =
            viewModelScope.launch {
                prefs.setMoneyTransactions(prefs.moneyTransactions.first().filter { it.id != txId })
            }
    fun exportMoneyDataJson(): String {
        val json = Json { prettyPrint = true }
        // Encode wallets dan transactions sebagai JSON string terpisah,
        // lalu wrap dalam object JSON — format kompatibel untuk import balik
        val walletsJson = json.encodeToString(moneyWallets.value)
        val txJson = json.encodeToString(moneyTransactions.value)
        return """{"wallets":$walletsJson,"transactions":$txJson}"""
    }
    fun importMoneyDataJson(jsonStr: String): Boolean {
        return try {
            val j = Json { ignoreUnknownKeys = true }
            val obj = j.parseToJsonElement(jsonStr).jsonObject
            val wallets =
                    j.decodeFromString<List<id.satria.launcher.data.MoneyWallet>>(
                            obj["wallets"]?.toString() ?: return false
                    )
            val txs =
                    j.decodeFromString<List<id.satria.launcher.data.MoneyTransaction>>(
                            obj["transactions"]?.toString() ?: return false
                    )
            viewModelScope.launch {
                prefs.setMoneyWallets(wallets)
                prefs.setMoneyTransactions(txs)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun todayKey(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private fun makeId() = "${System.currentTimeMillis()}-${(Math.random() * 100000).toInt()}"
}
