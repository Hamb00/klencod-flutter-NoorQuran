package com.alquran.noor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alquran.noor.AppContainer
import com.alquran.noor.containerFactory
import com.alquran.noor.data.Reciters
import com.alquran.noor.data.local.JuzStart
import com.alquran.noor.data.local.SurahEntity
import com.alquran.noor.data.settings.AppSettings
import com.alquran.noor.player.PlaybackUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val c: AppContainer) : ViewModel() {

    val surahs: StateFlow<List<SurahEntity>> =
        c.repository.surahs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val juz: StateFlow<List<JuzStart>> =
        c.repository.juzStarts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> =
        c.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val playback: StateFlow<PlaybackUiState> = c.player.state

    fun playSurah(surah: SurahEntity) {
        viewModelScope.launch {
            val reciter = Reciters.byId(c.settings.settings.first().reciterId)
            c.player.playSurah(surah.number, surah.ayahCount, 1, surah.name, reciter)
        }
    }

    companion object {
        val Factory = containerFactory { HomeViewModel(it) }
    }
}
