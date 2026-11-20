package com.alquran.noor.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.alquran.noor.AppContainer
import com.alquran.noor.container
import com.alquran.noor.data.Reciters
import com.alquran.noor.data.local.AyahEntity
import com.alquran.noor.data.local.SurahEntity
import com.alquran.noor.data.settings.AppSettings
import com.alquran.noor.player.PlaybackUiState
import com.alquran.noor.ui.navigation.ReaderRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** صفحة من المصحف (صفحة المصحف المطبوع) محصورة في نطاق السورة الحالية. */
data class PageGroup(val page: Int, val juz: Int, val ayahs: List<AyahEntity>)

sealed interface TafsirState {
    data object Idle : TafsirState
    data object Loading : TafsirState
    data class Ready(val text: String) : TafsirState
    data object Error : TafsirState
}

class ReaderViewModel(
    private val c: AppContainer,
    val surahNumber: Int,
    val initialAyah: Int,
) : ViewModel() {

    private val repo = c.repository

    private val _surah = MutableStateFlow<SurahEntity?>(null)
    val surah: StateFlow<SurahEntity?> = _surah.asStateFlow()

    val pages: StateFlow<List<PageGroup>?> = repo.ayahs(surahNumber)
        .map { list ->
            list.groupBy { it.page }.map { (page, ayahs) -> PageGroup(page, ayahs.first().juz, ayahs) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val bookmarked: StateFlow<Set<Int>> = repo.bookmarkedAyahs(surahNumber)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val settings: StateFlow<AppSettings> =
        c.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val playback: StateFlow<PlaybackUiState> = c.player.state

    private val _tafsir = MutableStateFlow<TafsirState>(TafsirState.Idle)
    val tafsir: StateFlow<TafsirState> = _tafsir.asStateFlow()

    init {
        viewModelScope.launch { _surah.value = repo.surah(surahNumber) }
    }

    fun loadTafsir(ayah: Int) {
        _tafsir.value = TafsirState.Loading
        viewModelScope.launch {
            repo.tafsir(surahNumber, ayah)
                .onSuccess { _tafsir.value = TafsirState.Ready(it) }
                .onFailure { _tafsir.value = TafsirState.Error }
        }
    }

    fun clearTafsir() {
        _tafsir.value = TafsirState.Idle
    }

    fun toggleBookmark(ayah: Int) {
        viewModelScope.launch { repo.toggleBookmark(surahNumber, ayah) }
    }

    fun saveLastRead(ayah: Int) {
        viewModelScope.launch { c.settings.setLastRead(surahNumber, ayah) }
    }

    fun adjustFont(delta: Float) {
        viewModelScope.launch {
            val current = c.settings.settings.first().fontSize
            c.settings.setFontSize((current + delta).coerceIn(MIN_FONT, MAX_FONT))
        }
    }

    fun play(startAyah: Int) {
        val s = _surah.value ?: return
        viewModelScope.launch {
            val reciter = Reciters.byId(c.settings.settings.first().reciterId)
            c.player.playSurah(s.number, s.ayahCount, startAyah, s.name, reciter)
        }
    }

    fun togglePlayback() = c.player.togglePlay()

    companion object {
        const val MIN_FONT = 20f
        const val MAX_FONT = 52f

        val Factory = viewModelFactory {
            initializer {
                val route = createSavedStateHandle().toRoute<ReaderRoute>()
                ReaderViewModel(container(), route.surah, route.ayah)
            }
        }
    }
}
