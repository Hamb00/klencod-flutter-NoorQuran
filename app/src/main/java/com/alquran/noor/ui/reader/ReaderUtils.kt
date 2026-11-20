package com.alquran.noor.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow

/** فهرس أول عنصر ظاهر في القائمة كتدفق. */
internal fun snapshotFlowIndex(state: LazyListState): Flow<Int> =
    snapshotFlow { state.firstVisibleItemIndex }

internal fun Modifier.clickableItem(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
