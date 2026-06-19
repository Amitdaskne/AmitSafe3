package com.amitdas.amitsafe.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amitdas.amitsafe.database.VaultDatabase
import com.amitdas.amitsafe.database.VaultItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VaultDatabase.getInstance(application)
    private val dao = db.dao()
    private val prefs = application.getSharedPreferences("amitsafe_prefs", Context.MODE_PRIVATE)

    // Lock Status & PIN Flow
    private val _isRegistered = MutableStateFlow(prefs.getString("vault_pin", null) != null)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _lockoutTimeRemaining = MutableStateFlow(0) // in seconds
    val lockoutTimeRemaining: StateFlow<Int> = _lockoutTimeRemaining.asStateFlow()

    private val _wrongPinTrigger = MutableSharedFlow<Unit>()
    val wrongPinTrigger: SharedFlow<Unit> = _wrongPinTrigger.asSharedFlow()

    // Security Options
    private val _screenshotProtection = MutableStateFlow(prefs.getBoolean("screenshot_protection", false))
    val screenshotProtection: StateFlow<Boolean> = _screenshotProtection.asStateFlow()

    private val _autoLockEnabled = MutableStateFlow(prefs.getBoolean("auto_lock", true))
    val autoLockEnabled: StateFlow<Boolean> = _autoLockEnabled.asStateFlow()

    private val _amoledTheme = MutableStateFlow(prefs.getBoolean("amoled_theme", true))
    val amoledTheme: StateFlow<Boolean> = _amoledTheme.asStateFlow()

    // Import Manager States
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Boolean> = _isImporting.asStateFlow() // mapped below, let's export float version
    val importProgressFloat: StateFlow<Float> = _importProgress.asStateFlow()

    private val _showImportSuccess = MutableStateFlow(false)
    val showImportSuccess: StateFlow<Boolean> = _showImportSuccess.asStateFlow()

    // Gallery Filters, Sort and Search
    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow("All") // "All", "Images", "Videos"
    val activeSort = MutableStateFlow("Newest") // "Newest", "Oldest", "Larger", "Name"

    // Raw Sources
    private val _allActiveItems = dao.getAllActive()
    val favoriteItems: StateFlow<List<VaultItem>> = dao.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recycleBinItems: StateFlow<List<VaultItem>> = dao.getRecycleBin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined & Filtered active gallery source
    val vaultItems: StateFlow<List<VaultItem>> = combine(
        _allActiveItems,
        searchQuery,
        activeFilter,
        activeSort
    ) { items, query, filter, sort ->
        var list = items

        // Search Filter
        if (query.isNotEmpty()) {
            list = list.filter { it.fileName.contains(query, ignoreCase = true) }
        }

        // Drop-down Filter
        list = when (filter) {
            "Images" -> list.filter { it.fileType == "image" }
            "Videos" -> list.filter { it.fileType == "video" }
            else -> list
        }

        // Sort rules
        list = when (sort) {
            "Oldest" -> list.sortedBy { it.dateAdded }
            "Name" -> list.sortedBy { it.fileName.lowercase() }
            "Larger" -> list.sortedByDescending { it.fileSize }
            else -> list.sortedByDescending { it.dateAdded }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Storage analysis
    val totalFilesCount = _allActiveItems.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalImagesCount = _allActiveItems.map { list -> list.count { it.fileType == "image" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalVideosCount = _allActiveItems.map { list -> list.count { it.fileType == "video" } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalStorageBytes = _allActiveItems.map { list -> list.sumOf { it.fileSize } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        // Handle Lockout countdown recovery on launch
        val lockedUntil = prefs.getLong("lockout_until", 0L)
        val now = System.currentTimeMillis()
        if (lockedUntil > now) {
            val remainSeconds = ((lockedUntil - now) / 1000).toInt()
            startLockoutCountdown(remainSeconds)
        }
    }

    // Lock Actions
    fun setPIN(pin: String) {
        prefs.edit().putString("vault_pin", pin).apply()
        _isRegistered.value = true
        _isLocked.value = false
        _failedAttempts.value = 0
    }

    fun verifyAndUnlock(pin: String): Boolean {
        if (_lockoutTimeRemaining.value > 0) return false

        val savedPin = prefs.getString("vault_pin", "") ?: ""
        if (pin == savedPin) {
            _isLocked.value = false
            _failedAttempts.value = 0
            return true
        } else {
            val attempts = _failedAttempts.value + 1
            _failedAttempts.value = attempts
            viewModelScope.launch {
                _wrongPinTrigger.emit(Unit)
            }
            if (attempts >= 3) {
                // Lockout for 30s
                prefs.edit().putLong("lockout_until", System.currentTimeMillis() + 30000L).apply()
                startLockoutCountdown(30)
            }
            return false
        }
    }

    fun lock() {
        _isLocked.value = true
    }

    fun unlockExternal() {
        _isLocked.value = false
    }

    private fun startLockoutCountdown(seconds: Int) {
        _lockoutTimeRemaining.value = seconds
        viewModelScope.launch {
            var time = seconds
            while (time > 0) {
                delay(1000)
                time--
                _lockoutTimeRemaining.value = time
            }
            prefs.edit().remove("lockout_until").apply()
            _failedAttempts.value = 0
        }
    }

    // Toggle Settings
    fun toggleScreenshotProtection(enabled: Boolean) {
        prefs.edit().putBoolean("screenshot_protection", enabled).apply()
        _screenshotProtection.value = enabled
    }

    fun toggleAutoLock(enabled: Boolean) {
        prefs.edit().putBoolean("auto_lock", enabled).apply()
        _autoLockEnabled.value = enabled
    }

    fun toggleAmoledTheme(enabled: Boolean) {
        prefs.edit().putBoolean("amoled_theme", enabled).apply()
        _amoledTheme.value = enabled
    }

    // File Action Processors
    fun importFiles(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            _isImporting.value = true
            _importProgress.value = 0f
            _showImportSuccess.value = false
            val total = uris.size
            val resolver = context.contentResolver
            val vaultDir = context.filesDir.resolve("amitsafe_secured_vault")
            if (!vaultDir.exists()) {
                vaultDir.mkdirs()
            }

            for ((index, uri) in uris.withIndex()) {
                try {
                    val originalName = getFileName(resolver, uri) ?: "file_${System.currentTimeMillis()}"
                    val mime = resolver.getType(uri) ?: ""
                    val isVideo = mime.startsWith("video")
                    val type = if (isVideo) "video" else "image"
                    val size = getFileSize(resolver, uri)

                    val suffix = if (originalName.contains(".")) originalName.substringAfterLast(".") else if (isVideo) "mp4" else "jpg"
                    val uniqueName = "sc_${System.currentTimeMillis()}_$index.$suffix"
                    val destFile = vaultDir.resolve(uniqueName)

                    resolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    val element = VaultItem(
                        originalPath = uri.toString(),
                        vaultPath = destFile.absolutePath,
                        fileName = originalName,
                        fileType = type,
                        fileSize = size,
                        dateAdded = System.currentTimeMillis()
                    )
                    dao.insert(element)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                _importProgress.value = (index + 1).toFloat() / total
            }
            delay(300) // Brief pause to see progress completeness
            _isImporting.value = false
            _showImportSuccess.value = true
        }
    }

    fun dismissImportSuccess() {
        _showImportSuccess.value = false
    }

    fun toggleFavorite(item: VaultItem) {
        viewModelScope.launch {
            dao.updateFavorite(item.id, !item.isFavorite)
        }
    }

    fun moveToBin(item: VaultItem) {
        viewModelScope.launch {
            dao.moveToRecycleBin(item.id, System.currentTimeMillis())
        }
    }

    fun restoreFromBin(item: VaultItem) {
        viewModelScope.launch {
            dao.restoreFromRecycleBin(item.id)
        }
    }

    fun deletePermanently(item: VaultItem) {
        viewModelScope.launch {
            try {
                val file = File(item.vaultPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            dao.deletePermanently(item.id)
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            val list = recycleBinItems.value
            for (item in list) {
                try {
                    val file = File(item.vaultPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            dao.clearRecycleBin()
        }
    }

    // Share secure file
    fun shareSecureItem(context: Context, item: VaultItem) {
        val file = File(item.vaultPath)
        if (!file.exists()) return
        try {
            val providerAuthority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                providerAuthority,
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = if (item.fileType == "video") "video/*" else "image/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share safe item"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Helper queries
    private fun getFileName(resolver: ContentResolver, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = resolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun getFileSize(resolver: ContentResolver, uri: Uri): Long {
        var size = 0L
        if (uri.scheme == "content") {
            val cursor = resolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0) {
                        size = it.getLong(index)
                    }
                }
            }
        }
        return if (size > 0L) size else 1024L // Fallback if query returns 0
    }
}
