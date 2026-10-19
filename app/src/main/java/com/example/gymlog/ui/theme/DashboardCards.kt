package com.example.gymlog.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun dashboardCardShape(): Shape = MaterialTheme.shapes.medium

@Composable
fun dashboardCardColors() = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
)

@Composable
fun dashboardCardElevation(): CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

@Composable
fun dashboardCardBorder(): BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
