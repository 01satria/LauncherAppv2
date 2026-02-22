package id.satria.launcher

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.satria.launcher.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)
    val repo  = LauncherRepository(app)

    private val _allApps = MutableStateFlow<List<AppData>>(emptyList())
    val allApps: StateFlow<List<AppData>> = _allApps.asStateFlow()

    val userName         = prefs.userName.stateIn(viewModelScope, SharingStarted.Eagerly, "User")
    val assistantName    = prefs.assistantName.stateIn(viewModelScope, SharingStarted.Eagerly, "Assistant")
    val showHidden       = prefs.showHidden.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val showNames        = prefs.showNames.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val layoutMode       = prefs.layoutMode.stateIn(viewModelScope, SharingStarted.Eagerly, "grid")
    val avatarPath       = prefs.avatarPath.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val iconSize         = prefs.iconSize.stateIn(viewModelScope, SharingStarted.Eagerly, 54)
    val dockIconSize     = prefs.dockIconSize.stateIn(viewModelScope, SharingStarted.Eagerly, 56)
    val hiddenPackages   = prefs.hiddenPackages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dockPackages     = prefs.dockPackages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val todos            = prefs.todos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val countdowns       = prefs.countdowns.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val weatherLocations = prefs.weatherLocations.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val notes            = prefs.notes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val habits           = prefs.habits.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val themeAccent = prefs.themeAccent.stateIn(viewModelScope, SharingStarted.Eagerly, "FF27AE60")
    val themeBg     = prefs.themeBg.stateIn(viewModelScope, SharingStarted.Eagerly, "FF000000")
    val themeBorder = prefs.themeBorder.stateIn(viewModelScope, SharingStarted.Eagerly, "FF1A1A1A")
    val themeFont   = prefs.themeFont.stateIn(viewModelScope, SharingStarted.Eagerly, "FFFFFFFF")

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

    init { refreshApps() }

    fun refreshApps() = viewModelScope.launch { _allApps.value = repo.getInstalledApps() }
    fun launchApp(pkg: String) = repo.launchApp(pkg)
    fun uninstallApp(pkg: String) { _allApps.value = _allApps.value.filter { it.packageName != pkg }; repo.uninstallApp(pkg) }

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

    fun setThemeAccent(v: String) = viewModelScope.launch { prefs.setThemeAccent(v) }
    fun setThemeBg(v: String)     = viewModelScope.launch { prefs.setThemeBg(v) }
    fun setThemeBorder(v: String) = viewModelScope.launch { prefs.setThemeBorder(v) }
    fun setThemeFont(v: String)   = viewModelScope.launch { prefs.setThemeFont(v) }

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

    // ── Notes ──────────────────────────────────────────────────────────────
    fun addNote(text: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        prefs.setNotes(listOf(NoteItem(makeId(), text, now, now)) + notes.value)
    }
    fun updateNote(id: String, text: String) = viewModelScope.launch {
        prefs.setNotes(notes.value.map { if (it.id == id) it.copy(text = text, updatedAt = System.currentTimeMillis()) else it })
    }
    fun deleteNote(id: String) = viewModelScope.launch { prefs.setNotes(notes.value.filter { it.id != id }) }

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
                // Uncheck
                val newDates = habit.doneDates.filter { it != today }
                habit.copy(doneDates = newDates, streak = calcStreak(newDates))
            } else {
                val newDates = habit.doneDates + today
                habit.copy(doneDates = newDates.takeLast(365), streak = calcStreak(newDates))
            }
        })
    }

    private fun calcStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        val fmt   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal   = Calendar.getInstance()
        var streak = 0
        while (true) {
            val key = fmt.format(cal.time)
            if (dates.contains(key)) { streak++; cal.add(Calendar.DAY_OF_YEAR, -1) }
            else break
        }
        return streak
    }

    fun resetHabitsIfNewDay() { /* Streaks auto-calc from doneDates — no reset needed */ }

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private fun makeId()   = "${System.currentTimeMillis()}-${(Math.random() * 100000).toInt()}"
}
