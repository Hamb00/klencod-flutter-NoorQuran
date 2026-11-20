package com.alquran.noor.ui.reader

import android.content.ClipData
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alquran.noor.R
import com.alquran.noor.data.local.AyahEntity
import com.alquran.noor.ui.theme.rememberQuranFont
import com.alquran.noor.util.ArabicText
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    vm: ReaderViewModel = viewModel(factory = ReaderViewModel.Factory),
) {
    val surah by vm.surah.collectAsStateWithLifecycle()
    val pages by vm.pages.collectAsStateWithLifecycle()
    val bookmarked by vm.bookmarked.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val tafsirState by vm.tafsir.collectAsStateWithLifecycle()

    val quranFont = rememberQuranFont()
    val listState = rememberLazyListState()
    var selected by remember { mutableStateOf<AyahEntity?>(null) }

    val playingAyah = if (playback.hasMedia && playback.surah == vm.surahNumber) playback.ayah else 0
    val isPlayingThisSurah = playback.isPlaying && playback.surah == vm.surahNumber

    // إبقاء الشاشة مضاءة أثناء القراءة (اختياري من الإعدادات)
    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // الانتقال إلى الآية المطلوبة عند الفتح (الفهرس 0 هو ترويسة السورة)
    var initialScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(pages) {
        val list = pages ?: return@LaunchedEffect
        if (initialScrollDone || list.isEmpty()) return@LaunchedEffect
        val index = list.indexOfFirst { g -> g.ayahs.any { it.numberInSurah == vm.initialAyah } }
        listState.scrollToItem(if (index >= 0) index + 1 else 0)
        initialScrollDone = true
    }

    // متابعة الآية الجارية في التلاوة
    LaunchedEffect(playingAyah, pages) {
        val list = pages ?: return@LaunchedEffect
        if (playingAyah == 0 || !initialScrollDone) return@LaunchedEffect
        val index = list.indexOfFirst { g -> g.ayahs.any { it.numberInSurah == playingAyah } }
        if (index >= 0 && index + 1 !in listState.layoutInfo.visibleItemsInfo.map { it.index }) {
            listState.animateScrollToItem(index + 1)
        }
    }

    // حفظ موضع القراءة
    LaunchedEffect(pages, initialScrollDone) {
        val list = pages ?: return@LaunchedEffect
        if (!initialScrollDone) return@LaunchedEffect
        snapshotFlowIndex(listState)
            .debounce(800)
            .distinctUntilChanged()
            .collect { index ->
                val group = list.getOrNull((index - 1).coerceAtLeast(0)) ?: return@collect
                vm.saveLastRead(group.ayahs.first().numberInSurah)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(surah?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                        surah?.let {
                            Text(
                                text = stringResource(
                                    R.string.reader_subtitle,
                                    stringResource(if (it.isMeccan) R.string.meccan else R.string.medinan),
                                    ArabicText.number(it.ayahCount),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { vm.adjustFont(-2f) }) {
                        Icon(Icons.Rounded.TextDecrease, contentDescription = stringResource(R.string.font_smaller))
                    }
                    IconButton(onClick = { vm.adjustFont(2f) }) {
                        Icon(Icons.Rounded.TextIncrease, contentDescription = stringResource(R.string.font_larger))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (playback.hasMedia && playback.surah == vm.surahNumber) {
                        vm.togglePlayback()
                    } else {
                        vm.play(vm.initialAyah.coerceAtLeast(1))
                    }
                },
            ) {
                Icon(
                    imageVector = if (isPlayingThisSurah) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (isPlayingThisSurah) R.string.pause else R.string.play),
                )
            }
        },
    ) { padding ->
        val groups = pages
        if (groups == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    SurahHeader(
                        name = surah?.name.orEmpty(),
                        showBismillah = vm.surahNumber != 1 && vm.surahNumber != 9,
                        fontFamily = quranFont,
                        fontSize = settings.fontSize,
                    )
                }
                items(groups.size, key = { groups[it].page }) { i ->
                    PageCard(
                        group = groups[i],
                        fontFamily = quranFont,
                        fontSize = settings.fontSize,
                        playingAyah = playingAyah,
                        bookmarked = bookmarked,
                        onAyahClick = { selected = it },
                    )
                }
                item(key = "footer") {
                    val next = vm.surahNumber + 1
                    if (next <= 114) {
                        TextButton(
                            onClick = { onOpenSurah(next) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.next_surah)) }
                    }
                }
            }
        }
    }

    selected?.let { ayah ->
        AyahSheet(
            ayah = ayah,
            surahName = surah?.name.orEmpty(),
            isBookmarked = ayah.numberInSurah in bookmarked,
            showTafsir = settings.showTafsir,
            tafsirState = tafsirState,
            fontFamily = quranFont,
            onPlay = { vm.play(ayah.numberInSurah) },
            onToggleBookmark = { vm.toggleBookmark(ayah.numberInSurah) },
            onLoadTafsir = { vm.loadTafsir(ayah.numberInSurah) },
            onDismiss = {
                selected = null
                vm.clearTafsir()
            },
        )
    }
}

@Composable
private fun SurahHeader(name: String, showBismillah: Boolean, fontFamily: FontFamily, fontSize: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (showBismillah) {
                Text(
                    text = ArabicText.BISMILLAH,
                    style = TextStyle(
                        fontFamily = fontFamily,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.9f).sp,
                        textAlign = TextAlign.Center,
                        textDirection = TextDirection.Rtl,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** صفحة المصحف: نص متصل تتخلله علامات نهاية الآية، وكل آية قابلة للنقر. */
@Composable
private fun PageCard(
    group: PageGroup,
    fontFamily: FontFamily,
    fontSize: Float,
    playingAyah: Int,
    bookmarked: Set<Int>,
    onAyahClick: (AyahEntity) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val highlight = colors.secondaryContainer
    val markColor = colors.primary
    val textColor = colors.onSurface

    val text: AnnotatedString = remember(group, playingAyah, bookmarked, markColor, highlight, textColor) {
        buildAnnotatedString {
            group.ayahs.forEach { ayah ->
                val playing = ayah.numberInSurah == playingAyah
                val style = SpanStyle(
                    color = textColor,
                    background = if (playing) highlight else Color.Unspecified,
                )
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "ayah-${ayah.numberInSurah}",
                        styles = TextLinkStyles(style = style),
                        linkInteractionListener = { onAyahClick(ayah) },
                    ),
                ) {
                    append(ayah.text)
                    append(' ')
                }
                withStyle(SpanStyle(color = markColor, fontWeight = FontWeight.Bold)) {
                    append("۝")
                    append(ArabicText.arabicIndic(ayah.numberInSurah))
                    if (ayah.numberInSurah in bookmarked) append("★")
                    if (ayah.sajda) append("۩")
                }
                append("  ")
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.juz_title, ArabicText.number(group.juz)),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.page_title, ArabicText.number(group.page)),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.9f).sp,
                    textAlign = TextAlign.Justify,
                    textDirection = TextDirection.Rtl,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyahSheet(
    ayah: AyahEntity,
    surahName: String,
    isBookmarked: Boolean,
    showTafsir: Boolean,
    tafsirState: TafsirState,
    fontFamily: FontFamily,
    onPlay: () -> Unit,
    onToggleBookmark: () -> Unit,
    onLoadTafsir: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val reference = "$surahName • ${ArabicText.number(ayah.numberInSurah)}"
    val shareText = "﴿${ayah.text}﴾\n[$reference]"

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(reference, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                text = ayah.text,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = 24.sp,
                    lineHeight = 46.sp,
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.Rtl,
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.play_from_here)) },
                leadingContent = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                modifier = Modifier.clickableItem {
                    onPlay()
                    onDismiss()
                },
            )
            ListItem(
                headlineContent = {
                    Text(stringResource(if (isBookmarked) R.string.bookmark_remove else R.string.bookmark_add))
                },
                leadingContent = {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickableItem(onToggleBookmark),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.copy)) },
                leadingContent = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                modifier = Modifier.clickableItem {
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(reference, shareText)))
                    }
                    onDismiss()
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.share)) },
                leadingContent = { Icon(Icons.Rounded.Share, contentDescription = null) },
                modifier = Modifier.clickableItem {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                },
            )

            if (showTafsir) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.tafsir_title)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null) },
                    modifier = Modifier.clickableItem(onLoadTafsir),
                )
                AnimatedVisibility(visible = tafsirState != TafsirState.Idle) {
                    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        when (tafsirState) {
                            TafsirState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                            TafsirState.Error -> Text(
                                text = stringResource(R.string.tafsir_error),
                                color = MaterialTheme.colorScheme.error,
                            )
                            is TafsirState.Ready -> Text(
                                text = tafsirState.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 30.sp,
                                    textDirection = TextDirection.Rtl,
                                ),
                            )
                            TafsirState.Idle -> Unit
                        }
                    }
                }
            }
        }
    }
}
