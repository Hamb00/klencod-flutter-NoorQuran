package com.alquran.noor.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alquran.noor.R
import com.alquran.noor.data.local.JuzStart
import com.alquran.noor.data.local.SurahEntity
import com.alquran.noor.util.ArabicText
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenReader: (surah: Int, ayah: Int) -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val juz by vm.juz.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(stringResource(R.string.tab_surahs)) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(stringResource(R.string.tab_juz)) },
                )
            }

            if (tab == 0) {
                SurahGrid(
                    surahs = surahs,
                    lastSurah = if (settings.hasLastRead) settings.lastSurah else 0,
                    lastAyah = settings.lastAyah,
                    playingSurah = if (playback.hasMedia) playback.surah else 0,
                    onOpen = onOpenReader,
                    onPlay = vm::playSurah,
                )
            } else {
                JuzGrid(juz = juz, onOpen = onOpenReader)
            }
        }
    }
}

@Composable
private fun SurahGrid(
    surahs: List<SurahEntity>,
    lastSurah: Int,
    lastAyah: Int,
    playingSurah: Int,
    onOpen: (Int, Int) -> Unit,
    onPlay: (SurahEntity) -> Unit,
) {
    val last = surahs.firstOrNull { it.number == lastSurah }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 360.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (last != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ContinueCard(surah = last, ayah = lastAyah, onClick = { onOpen(last.number, lastAyah) })
            }
        }
        items(surahs, key = { it.number }) { surah ->
            SurahRow(
                surah = surah,
                playing = surah.number == playingSurah,
                onClick = { onOpen(surah.number, 1) },
                onPlay = { onPlay(surah) },
            )
        }
    }
}

@Composable
private fun ContinueCard(surah: SurahEntity, ayah: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AutoStories, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.continue_reading), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = stringResource(
                        R.string.continue_reading_desc,
                        surah.name,
                        ArabicText.number(ayah),
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun SurahRow(
    surah: SurahEntity,
    playing: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
) {
    val revelation = stringResource(if (surah.isMeccan) R.string.meccan else R.string.medinan)

    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (playing) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberBadge(surah.number)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(surah.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(
                        R.string.surah_meta,
                        surah.englishName,
                        revelation,
                        ArabicText.number(surah.ayahCount),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play))
            }
        }
    }
}

/** شارة رقم السورة على شكل نجمة ثمانية (مربعان متراكبان). */
@Composable
private fun NumberBadge(number: Int) {
    val color = MaterialTheme.colorScheme.primaryContainer
    Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(32.dp)
                .rotate(45f)
                .background(color, RoundedCornerShape(8.dp)),
        )
        Box(
            Modifier
                .size(32.dp)
                .background(color, RoundedCornerShape(8.dp)),
        )
        Text(
            text = ArabicText.arabicIndic(number),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun JuzGrid(juz: List<JuzStart>, onOpen: (Int, Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(juz, key = { it.juz }) { item ->
            ElevatedCard(
                onClick = { onOpen(item.surah, item.ayah) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NumberBadge(item.juz)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.juz_title, ArabicText.number(item.juz)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(
                                R.string.juz_start,
                                item.surahName,
                                ArabicText.number(item.ayah),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
