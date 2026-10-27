package com.example.gymlog.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.CalendarColorCategory
import com.example.gymlog.data.CalendarColorStore
import com.example.gymlog.data.formatHexColor
import com.example.gymlog.data.parseHexColor
import com.example.gymlog.ui.theme.GymLogActionButton
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarCustomColorsScreen(
    store: CalendarColorStore,
    onBack: () -> Unit,
) {
    val settings by store.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(CalendarColorCategory.CHEST) }
    val selectedColor = Color(settings.customColors[selected] ?: selected.argb)
    var draftColor by remember(selected, settings.customColors) { mutableStateOf(selectedColor) }
    var hexText by remember(selected, settings.customColors) {
        mutableStateOf(formatHexColor(selectedColor.toArgb()))
    }
    var hexError by remember(selected) { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar_custom_colors_title)) },
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
                Text(
                    text = stringResource(R.string.calendar_custom_colors_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item {
                GymLogCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        CalendarColorCategory.entries.forEach { category ->
                            val isSelected = category == selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selected = category }
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        } else {
                                            Color.Transparent
                                        },
                                    )
                                    .padding(horizontal = GymLogSpacing.md, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = calendarColorCategoryLabel(category),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = String.format(
                                        "#%06X",
                                        (settings.customColors[category] ?: category.argb) and 0xFFFFFF,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 10.dp),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(settings.customColors[category] ?: category.argb)),
                                )
                            }
                        }
                    }
                }
            }
            item {
                GymLogCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(GymLogSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.calendar_custom_selected,
                                calendarColorCategoryLabel(selected),
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        key(selected) {
                            HsvColorPicker(
                                initialColor = selectedColor,
                                onColorChange = {
                                    draftColor = it
                                    hexText = formatHexColor(it.toArgb())
                                    hexError = false
                                },
                            )
                        }
                        OutlinedTextField(
                            value = hexText,
                            onValueChange = { input ->
                                hexText = input
                                val parsed = parseHexColor(input)
                                hexError = parsed == null
                                parsed?.let { draftColor = Color(it) }
                            },
                            label = { Text(stringResource(R.string.calendar_color_hex_label)) },
                            placeholder = { Text("#E57373") },
                            singleLine = true,
                            isError = hexError,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                            ),
                            supportingText = {
                                if (hexError) {
                                    Text(stringResource(R.string.calendar_color_hex_invalid))
                                } else {
                                    Text(stringResource(R.string.calendar_color_hex_hint))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        GymLogActionButton(
                            text = stringResource(R.string.calendar_color_apply),
                            onClick = {
                                store.setCustomColor(selected, draftColor.toArgb())
                            },
                            enabled = parseHexColor(hexText) != null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(
                            onClick = { confirmReset = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.calendar_color_reset))
                        }
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.calendar_color_reset),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = { Text(stringResource(R.string.calendar_custom_reset_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        store.resetCustomColors()
                        draftColor = Color(selected.argb)
                        hexText = formatHexColor(selected.argb)
                        hexError = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
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
