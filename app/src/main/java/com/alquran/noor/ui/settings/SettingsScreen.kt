package com.alquran.noor.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alquran.noor.AppContainer
import com.alquran.noor.BuildConfig
import com.alquran.noor.R
import com.alquran.noor.containerFactory
import com.alquran.noor.data.Reciters
import com.alquran.noor.data.displayName
import com.alquran.noor.data.settings.AppSettings
import com.alquran.noor.ui.theme.rememberQuranFont
import com.alquran.noor.util.ArabicText
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(c: AppContainer) : ViewModel() {
    private val store = c.settings

    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(v: Int) = viewModelScope.launch { store.setThemeMode(v) }
    fun setDynamicColor(v: Boolean) = viewModelScope.launch { store.setDynamicColor(v) }
    fun setFontSize(v: Float) = viewModelScope.launch { store.setFontSize(v) }
    fun setReciter(id: String) = viewModelScope.launch { store.setReciter(id) }
    fun setShowTafsir(v: Boolean) = viewModelScope.launch { store.setShowTafsir(v) }
    fun setKeepScreenOn(v: Boolean) = viewModelScope.launch { store.setKeepScreenOn(v) }

    companion object {
        val Factory = containerFactory { SettingsViewModel(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val s by vm.settings.collectAsStateWithLifecycle()
    var showReciters by remember { mutableStateOf(false) }
    val quranFont = rememberQuranFont()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ---- المظهر ----
            item { SectionTitle(R.string.settings_appearance) }
            item {
                val labels = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    labels.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = s.themeMode == index,
                            onClick = { vm.setThemeMode(index) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
            }
            item {
                SwitchItem(
                    title = R.string.dynamic_color,
                    subtitle = R.string.dynamic_color_desc,
                    checked = s.dynamicColor,
                    onChange = vm::setDynamicColor,
                )
            }

            // ---- القراءة ----
            item { SectionTitle(R.string.settings_reading) }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.font_size), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(ArabicText.number(s.fontSize.toInt()), style = MaterialTheme.typography.labelLarge)
                    }
                    Slider(
                        value = s.fontSize,
                        onValueChange = { vm.setFontSize(it) },
                        valueRange = 20f..52f,
                    )
                    Text(
                        text = ArabicText.BISMILLAH,
                        style = TextStyle(
                            fontFamily = quranFont,
                            fontSize = s.fontSize.sp,
                            lineHeight = (s.fontSize * 1.9f).sp,
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
            }
            item {
                SwitchItem(
                    title = R.string.show_tafsir,
                    subtitle = R.string.show_tafsir_desc,
                    checked = s.showTafsir,
                    onChange = vm::setShowTafsir,
                )
            }
            item {
                SwitchItem(
                    title = R.string.keep_screen_on,
                    subtitle = null,
                    checked = s.keepScreenOn,
                    onChange = vm::setKeepScreenOn,
                )
            }

            // ---- الصوت ----
            item { SectionTitle(R.string.settings_audio) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.reciter)) },
                    supportingContent = { Text(Reciters.byId(s.reciterId).displayName()) },
                    modifier = Modifier.clickable { showReciters = true },
                )
            }

            // ---- حول ----
            item { SectionTitle(R.string.settings_about) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.version)) },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.credits)) },
                    supportingContent = { Text(stringResource(R.string.credits_desc)) },
                )
            }
        }
    }

    if (showReciters) {
        AlertDialog(
            onDismissRequest = { showReciters = false },
            title = { Text(stringResource(R.string.reciter)) },
            text = {
                LazyColumn {
                    items(Reciters.all, key = { it.id }) { reciter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.setReciter(reciter.id)
                                    showReciters = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = reciter.id == s.reciterId, onClick = null)
                            Text(reciter.displayName(), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReciters = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(title: Int) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SwitchItem(title: Int, subtitle: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = if (subtitle != null) {
            { Text(stringResource(subtitle)) }
        } else {
            null
        },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}
