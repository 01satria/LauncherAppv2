package id.satria.launcher

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.satria.launcher.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)
    val repo  = LauncherRepository(app)

    // ── App list ───────────────────────────────────────────────────────────
    private val _allApps = MutableStateFlow<List<AppData>>(emptyList())
    val allApps: StateFlow<List<AppData>> = _allApps.asStateFlow()

    // ── Prefs as StateFlow ─────────────────────────────────────────────────
    val userName        = prefs.userName.stateIn(viewModelScope, SharingStarted.Eagerly, "User")
    val assistantName   = prefs.assistantName.stateIn(viewModelScope, SharingStarted.Eagerly, "Assistant")
    val showHidden      = prefs.showHidden.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val showNames       = prefs.showNames.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val layoutMode      = prefs.layoutMode.stateIn(viewModelScope, SharingStarted.Eagerly, "grid")
    val avatarPath      = prefs.avatarPath.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // u2500u2500 avatarVersion u2014 increment setiap ganti foto, dipakai sebagai cache-bust key di Dock u2500u2500
    private val _avatarVersion = MutableStateFlow(0)
    val avatarVersion: StateFlow<Int> = _avatarVersion.asStateFlow()

    // ── Icon sizes ─────────────────────────────────────────────────────────
    val iconSize     = prefs.iconSize.stateIn(viewModelScope, SharingStarted.Eagerly, 54)
    val dockIconSize = prefs.dockIconSize.stateIn(viewModelScope, SharingStarted.Eagerly, 56)
    val hiddenPackages  = prefs.hiddenPackages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dockPackages    = prefs.dockPackages.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val todos           = prefs.todos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val countdowns      = prefs.countdowns.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val weatherLocations = prefs.weatherLocations.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Derived state — pengganti useMemo di RN ────────────────────────────
    val filteredApps = combine(allApps, hiddenPackages, dockPackages, showHidden) {
        apps, hidden, dock, showH ->
        apps.filter { a ->
            !dock.contains(a.packageName) &&
            (showH || !hidden.contains(a.packageName))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dockApps = combine(allApps, dockPackages) { apps, dock ->
        dock.mapNotNull { pkg -> apps.find { it.packageName == pkg } }.take(4)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init { refreshApps() }

    // ── App management ─────────────────────────────────────────────────────
    fun refreshApps() = viewModelScope.launch {
        _allApps.value = repo.getInstalledApps()
    }

    fun launchApp(pkg: String) = repo.launchApp(pkg)

    fun uninstallApp(pkg: String) {
        // Optimistic remove — refresh setelah dialog uninstall ditutup
        _allApps.value = _allApps.value.filter { it.packageName != pkg }
        repo.uninstallApp(pkg)
    }

    // ── Hide / dock ────────────────────────────────────────────────────────
    fun hideApp(pkg: String) = viewModelScope.launch {
        val newHidden = hiddenPackages.value.toMutableList()
        if (!newHidden.contains(pkg)) newHidden.add(pkg)
        prefs.setHiddenPackages(newHidden)
        // Auto-remove dari dock
        if (dockPackages.value.contains(pkg))
            prefs.setDockPackages(dockPackages.value.filter { it != pkg })
    }

    fun unhideApp(pkg: String) = viewModelScope.launch {
        prefs.setHiddenPackages(hiddenPackages.value.filter { it != pkg })
    }

    fun toggleDock(pkg: String) = viewModelScope.launch {
        val dock = dockPackages.value.toMutableList()
        if (dock.contains(pkg)) {
            dock.remove(pkg)
        } else {
            if (dock.size >= 4) return@launch
            dock.add(pkg)
            // Auto-unhide jika tersembunyi
            if (hiddenPackages.value.contains(pkg))
                prefs.setHiddenPackages(hiddenPackages.value.filter { it != pkg })
        }
        prefs.setDockPackages(dock)
    }

    // ── Settings ───────────────────────────────────────────────────────────
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
        _avatarVersion.value++  // trigger Dock untuk reload avatar
    }

    // ── Todo ───────────────────────────────────────────────────────────────
    fun addTodo(text: String) = viewModelScope.launch {
        prefs.setTodos(todos.value + TodoItem(makeId(), text, false))
    }
    fun toggleTodo(id: String) = viewModelScope.launch {
        prefs.setTodos(todos.value.map { if (it.id == id) it.copy(done = !it.done) else it })
    }
    fun removeTodo(id: String) = viewModelScope.launch {
        prefs.setTodos(todos.value.filter { it.id != id })
    }

    // ── Countdown ──────────────────────────────────────────────────────────
    fun addCountdown(name: String, isoDate: String) = viewModelScope.launch {
        prefs.setCountdowns(countdowns.value + CountdownItem(makeId(), name, isoDate))
    }
    fun removeCountdown(id: String) = viewModelScope.launch {
        prefs.setCountdowns(countdowns.value.filter { it.id != id })
    }

    // ── Weather locations ──────────────────────────────────────────────────
    fun addWeatherLocation(loc: String) = viewModelScope.launch {
        if (weatherLocations.value.contains(loc)) return@launch
        if (weatherLocations.value.size >= 8) return@launch
        prefs.setWeatherLocations(weatherLocations.value + loc)
    }
    fun removeWeatherLocation(loc: String) = viewModelScope.launch {
        prefs.setWeatherLocations(weatherLocations.value.filter { it != loc })
    }

    private fun makeId() = "${System.currentTimeMillis()}-${(Math.random() * 100000).toInt()}"
}