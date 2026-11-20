package com.alquran.noor.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alquran.noor.AppContainer
import com.alquran.noor.containerFactory
import com.alquran.noor.data.local.SearchHit
import com.alquran.noor.data.local.SurahEntity
import com.alquran.noor.util.ArabicText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class SearchResult(
    val surahs: List<SurahEntity> = emptyList(),
    val ayahs: List<SearchHit> = emptyList(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(c: AppContainer) : ViewModel() {

    private val repo = c.repository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val result: StateFlow<SearchResult> = _query
        .debounce(250)
        .distinctUntilChanged()
        .mapLatest { search(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResult())

    fun setQuery(value: String) {
        _query.value = value
    }

    private suspend fun search(q: String): SearchResult {
        if (q.isEmpty()) return SearchResult()
        val normalized = ArabicText.normalize(q)

        val surahs = repo.surahsOnce().filter {
            it.number.toString() == q ||
                ArabicText.normalize(it.name).contains(normalized) ||
                it.englishName.contains(q, ignoreCase = true)
        }
        // البحث في نص الآيات يبدأ من حرفين فصاعدًا
        val ayahs = if (normalized.length >= 2) repo.search(normalized) else emptyList()
        return SearchResult(surahs, ayahs)
    }

    companion object {
        val Factory = containerFactory { SearchViewModel(it) }
    }
}
