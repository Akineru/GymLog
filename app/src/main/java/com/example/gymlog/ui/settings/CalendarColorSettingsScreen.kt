package com.example.gymlog.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.CalendarColorCategory
import com.example.gymlog.data.CalendarColorMode
import com.example.gymlog.data.CalendarColorStore
import com.example.gymlog.ui.calendar.composeColor
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarColorSettingsScreen(
    store: CalendarColorStore,
    onBack: () -> Unit,
    onOpenCustom: () -> Unit,
) {
    val settings by store.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    val palette = settings.palette()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar_colors_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                GymLogCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            CalendarColorMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = settings.mode == mode,
                                    onClick = {
                                        store.setMode(mode)
                                        if (mode == CalendarColorMode.CUSTOM) onOpenCustom()
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = CalendarColorMode.entries.size,
                                    ),
                                    icon = {},
                                    label = { Text(calendarColorModeLabel(mode)) },
                                )
                            }
                        }
                        Text(
                            text = when (settings.mode) {
                                CalendarColorMode.VIVID -> stringResource(R.string.calendar_colors_vivid_hint)
                                CalendarColorMode.SOFT -> stringResource(R.string.calendar_colors_soft_hint)
                                CalendarColorMode.CUSTOM -> stringResource(R.string.calendar_colors_custom_hint)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                GymLogCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_colors_preview),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            CalendarColorCategory.entries.forEach { category ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(palette.composeColor(category)),
                                    )
                                    Text(
                                        text = calendarColorCategoryLabel(category),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 5.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (settings.mode == CalendarColorMode.CUSTOM) {
                item {
                    SettingsLinkRow(
                        title = stringResource(R.string.calendar_custom_colors_title),
                        subtitle = stringResource(R.string.calendar_custom_colors_hint),
                        icon = Icons.Filled.Palette,
                        onClick = onOpenCustom,
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.calendar_colors_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun calendarColorCategoryLabel(category: CalendarColorCategory): String = stringResource(
    when (category) {
        CalendarColorCategory.CHEST -> R.string.calendar_color_chest
        CalendarColorCategory.BACK -> R.string.calendar_color_back
        CalendarColorCategory.SHOULDERS -> R.string.calendar_color_shoulders
        CalendarColorCategory.LEGS -> R.string.calendar_color_legs
        CalendarColorCategory.ARMS -> R.string.calendar_color_arms
        CalendarColorCategory.OTHER -> R.string.calendar_color_other
    },
)
