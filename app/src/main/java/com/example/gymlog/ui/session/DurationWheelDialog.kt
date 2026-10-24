package com.example.gymlog.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymlog.R
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun DurationWheelDialog(
    title: String,
    initialMillis: Long,
    showHours: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val totalSeconds = (initialMillis.coerceAtLeast(0L) / 1000L)
    var hours by remember {
        mutableIntStateOf(if (showHours) (totalSeconds / 3600L).toInt().coerceIn(0, 23) else 0)
    }
    var minutes by remember { mutableIntStateOf(((totalSeconds % 3600L) / 60L).toInt().coerceIn(0, 59)) }
    var seconds by remember { mutableIntStateOf((totalSeconds % 60L).toInt().coerceIn(0, 59)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showHours) {
                        WheelUnit(
                            values = (0..23).toList(),
                            selected = hours,
                            onSelected = { hours = it },
                            label = "时",
                        )
                        HorizontalDivider(
                            modifier = Modifier.width(1.dp).height(72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    WheelUnit(
                        values = (0..59).toList(),
                        selected = minutes,
                        onSelected = { minutes = it },
                        label = "分",
                    )
                    HorizontalDivider(
                        modifier = Modifier.width(1.dp).height(72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    WheelUnit(
                        values = (0..59).toList(),
                        selected = seconds,
                        onSelected = { seconds = it },
                        label = "秒",
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        (hours * 3600L + minutes * 60L + seconds) * 1000L,
                    )
                },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun WheelUnit(
    values: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    label: String,
) {
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = selected.coerceIn(0, values.lastIndex),
    )
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index -> onSelected(values.getOrElse(index) { values.last() }) }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.width(58.dp).height(120.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
            )
            LazyColumn(
                state = state,
                flingBehavior = fling,
                contentPadding = PaddingValues(vertical = 40.dp),
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(values.size) { index ->
                    val value = values[index]
                    Box(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = value.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFeatureSettings = "tnum",
                                fontWeight = if (value == selected) {
                                    FontWeight.Normal
                                } else {
                                    FontWeight.Normal
                                },
                            ),
                            color = if (value == selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
