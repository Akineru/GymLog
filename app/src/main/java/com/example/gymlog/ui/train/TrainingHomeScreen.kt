package com.example.gymlog.ui.train

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymlog.ui.theme.dashboardCardBorder
import com.example.gymlog.ui.theme.dashboardCardColors
import com.example.gymlog.ui.theme.dashboardCardElevation
import com.example.gymlog.ui.theme.dashboardCardShape
import com.example.gymlog.R
import com.example.gymlog.ui.settings.SettingsTopBarAction

enum class TrainingPlaceholderKind {
    PLAN,
    TOOLS,
}

private data class TrainDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String? = null,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingHomeScreen(
    onOpenLibrary: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val comingSoon = stringResource(R.string.train_coming_soon_badge)
    val entries = listOf(
        TrainDestination(
            title = stringResource(R.string.exercise_library),
            subtitle = stringResource(R.string.train_library_hint),
            icon = Icons.Filled.FitnessCenter,
            onClick = onOpenLibrary,
        ),
        TrainDestination(
            title = stringResource(R.string.train_plan_title),
            subtitle = stringResource(R.string.train_plan_hint),
            icon = Icons.Filled.DateRange,
            badge = comingSoon,
            onClick = onOpenPlan,
        ),
        TrainDestination(
            title = stringResource(R.string.history_title),
            subtitle = stringResource(R.string.train_history_hint),
            icon = Icons.AutoMirrored.Filled.List,
            onClick = onOpenHistory,
        ),
        TrainDestination(
            title = stringResource(R.string.train_tools_title),
            subtitle = stringResource(R.string.train_tools_hint),
            icon = Icons.Filled.Build,
            badge = comingSoon,
            onClick = onOpenTools,
        ),
    )
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_train)) },
                actions = { SettingsTopBarAction(onOpenSettings) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entries, key = { it.title }) { entry ->
                TrainEntryCard(entry)
            }
        }
    }
}

@Composable
private fun TrainEntryCard(entry: TrainDestination) {
    Card(
        onClick = entry.onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = dashboardCardShape(),
        colors = dashboardCardColors(),
        elevation = dashboardCardElevation(),
        border = dashboardCardBorder(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (entry.badge != null) {
                Text(
                    text = entry.badge,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlaceholderScreen(
    kind: TrainingPlaceholderKind,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val title = stringResource(
        when (kind) {
            TrainingPlaceholderKind.PLAN -> R.string.train_plan_title
            TrainingPlaceholderKind.TOOLS -> R.string.train_tools_title
        },
    )
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
        Text(
            text = stringResource(R.string.train_coming_soon_message),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
        )
    }
}
