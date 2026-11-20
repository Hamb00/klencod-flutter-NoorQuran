package com.alquran.noor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alquran.noor.R
import com.alquran.noor.player.PlaybackUiState
import com.alquran.noor.player.PlayerController
import com.alquran.noor.util.ArabicText

private val Speeds = listOf(0.75f, 1f, 1.25f, 1.5f)
private val SleepOptions = listOf(15, 30, 60)

@Composable
fun MiniPlayer(
    state: PlaybackUiState,
    controller: PlayerController,
    padNavigationBar: Boolean,
    onOpen: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = if (padNavigationBar) {
                Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            } else {
                Modifier
            },
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${state.surahName} • ${ArabicText.number(state.ayah)}",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sleep = state.sleepMinutesLeft
                    val subtitle = if (sleep != null) {
                        "${state.reciterName} • " + stringResource(R.string.sleep_left, ArabicText.number(sleep))
                    } else {
                        state.reciterName
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(onClick = controller::previous) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.previous))
                }
                IconButton(onClick = controller::togglePlay) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                    )
                }
                IconButton(onClick = controller::next) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.next))
                }

                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    Speeds.forEach { speed ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.speed_value, speed.toString())) },
                            trailingIcon = {
                                if (state.speed == speed) Icon(Icons.Rounded.Check, contentDescription = null)
                            },
                            onClick = {
                                controller.setSpeed(speed)
                                menuOpen = false
                            },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.repeat_ayah)) },
                        leadingIcon = { Icon(Icons.Rounded.Repeat, contentDescription = null) },
                        trailingIcon = {
                            if (state.repeatAyah) Icon(Icons.Rounded.Check, contentDescription = null)
                        },
                        onClick = {
                            controller.toggleRepeatAyah()
                            menuOpen = false
                        },
                    )
                    HorizontalDivider()
                    SleepOptions.forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sleep_in, ArabicText.number(minutes))) },
                            leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null) },
                            onClick = {
                                controller.setSleepTimer(minutes)
                                menuOpen = false
                            },
                        )
                    }
                    if (state.sleepMinutesLeft != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sleep_cancel)) },
                            onClick = {
                                controller.setSleepTimer(null)
                                menuOpen = false
                            },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.stop)) },
                        leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null) },
                        onClick = {
                            controller.stop()
                            menuOpen = false
                        },
                    )
                }
            }
        }
    }
}
