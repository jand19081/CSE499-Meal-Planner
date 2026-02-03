package io.github.and19081.mealplanner.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsView() {
    val viewModel = viewModel { SettingsViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Appearance Section
        item {
            SectionHeader("Appearance")
            
            Text("Theme", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = uiState.appTheme == theme,
                        onClick = { viewModel.setTheme(theme) },
                        label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Corner Style", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CornerStyle.entries.forEach { style ->
                    FilterChip(
                        selected = uiState.cornerStyle == style,
                        onClick = { viewModel.setCornerStyle(style) },
                        label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Accent Color", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AccentColor.entries.forEach { accent ->
                    ColorOption(
                        color = accent.color,
                        isSelected = uiState.accentColor == accent,
                        onClick = { viewModel.setAccentColor(accent) }
                    )
                }
            }
        }

        // Dashboard Section
        item {
            HorizontalDivider()
            SectionHeader("Dashboard")
            
            SettingsSwitchRow(
                label = "Show Weekly Cost Widget",
                checked = uiState.dashboardConfig.showWeeklyCost,
                onCheckedChange = { viewModel.toggleShowWeeklyCost(it) }
            )
            
            SettingsSwitchRow(
                label = "Show Meal Plan (Today/Next)",
                checked = uiState.dashboardConfig.showMealPlan,
                onCheckedChange = { viewModel.toggleShowMealPlan(it) }
            )
            
            SettingsSwitchRow(
                label = "Show Shopping List Summary",
                checked = uiState.dashboardConfig.showShoppingListSummary,
                onCheckedChange = { viewModel.toggleShowShoppingList(it) }
            )
        }

        // Notifications Section
        item {
            HorizontalDivider()
            SectionHeader("Notifications")
            
            Text("Meal Consumed Check Delay: ${uiState.notificationDelayMinutes} min", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = uiState.notificationDelayMinutes.toFloat(),
                onValueChange = { viewModel.setNotificationDelay(it.toInt()) },
                valueRange = 5f..120f,
                steps = 22 // (120-5)/5 - 1 roughly
            )
        }

        // Financials Section
        item {
            HorizontalDivider()
            SectionHeader("Financials")
            
            OutlinedTextField(
                value = (uiState.taxRate * 100).toString(),
                onValueChange = {
                    val doubleVal = it.toDoubleOrNull()
                    if (doubleVal != null) {
                        viewModel.updateTaxRate(doubleVal / 100.0)
                    }
                },
                label = { Text("Sales Tax Rate (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Enter tax as percentage (e.g., 8.0 for 8%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ColorOption(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(color)
            .clickable(onClick = onClick)
            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraLarge) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}