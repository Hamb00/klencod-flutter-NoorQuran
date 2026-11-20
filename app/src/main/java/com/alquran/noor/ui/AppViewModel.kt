package com.alquran.noor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alquran.noor.AppContainer
import com.alquran.noor.containerFactory
import com.alquran.noor.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SyncState { Idle, Loading, Error }

class AppViewModel(private val c: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings?> =
        c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _ready = MutableStateFlow<Boolean?>(null)
    /** null = جارٍ الفحص، false = يحتاج التنزيل الأول، true = جاهز */
    val ready: StateFlow<Boolean?> = _ready.asStateFlow()

    private val _sync = MutableStateFlow(SyncState.Idle)
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    init {
        viewModelScope.launch {
            val downloaded = c.repository.isDownloaded()
            _ready.value = downloaded
            if (!downloaded) startSync()
        }
    }

    fun startSync() {
        if (_sync.value == SyncState.Loading) return
        _sync.value = SyncState.Loading
        viewModelScope.launch {
            runCatching { c.repository.download() }
                .onSuccess {
                    _sync.value = SyncState.Idle
                    _ready.value = true
                }
                .onFailure { _sync.value = SyncState.Error }
        }
    }

    companion object {
        val Factory = containerFactory { AppViewModel(it) }
    }
}
