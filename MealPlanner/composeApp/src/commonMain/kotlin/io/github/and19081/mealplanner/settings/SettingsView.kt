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

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.lazy.items
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.ingredients.*
import io.github.and19081.mealplanner.uicomponents.SearchableDropdown

@Composable
fun SettingsView(
    viewModel: SettingsViewModel
) {
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

            Text("App Mode", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.appMode == mode,
                        onClick = { viewModel.setAppMode(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
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

        // Data Management Section
        item {
            HorizontalDivider()
            SectionHeader("Administrative")
            
            var showManager by remember { mutableStateOf<String?>(null) }

            Button(
                onClick = { showManager = "Data" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage System Data (Stores, Categories, etc.)")
            }

            if (showManager == "Data") {
                DataManagementDialog(
                    onDismiss = { showManager = null },
                    viewModel = viewModel
                )
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Stores", "Categories", "Restaurants", "Units")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage System Data") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp)) {
                SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> GenericDataManager(
                            items = uiState.allStores.map { it.id to it.name },
                            onSave = { id, name -> viewModel.saveStore(io.github.and19081.mealplanner.ingredients.Store(id ?: kotlin.uuid.Uuid.random(), name)) },
                            onDelete = { viewModel.deleteStore(it) },
                            label = "Store"
                        )
                        1 -> GenericDataManager(
                            items = uiState.allCategories.map { it.id to it.name },
                            onSave = { id, name -> viewModel.saveCategory(io.github.and19081.mealplanner.ingredients.Category(id ?: kotlin.uuid.Uuid.random(), name)) },
                            onDelete = { viewModel.deleteCategory(it) },
                            label = "Category"
                        )
                        2 -> GenericDataManager(
                            items = uiState.allRestaurants.map { it.id to it.name },
                            onSave = { id, name -> viewModel.saveRestaurant(io.github.and19081.mealplanner.Restaurant(id ?: kotlin.uuid.Uuid.random(), name)) },
                            onDelete = { viewModel.deleteRestaurant(it) },
                            label = "Restaurant"
                        )
                        3 -> UnitDataManager(
                            units = uiState.allUnits,
                            onSave = { viewModel.saveUnit(it) },
                            onDelete = { viewModel.deleteUnit(it) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun GenericDataManager(
    items: List<Pair<kotlin.uuid.Uuid, String>>,
    onSave: (kotlin.uuid.Uuid?, String) -> Unit,
    onDelete: (kotlin.uuid.Uuid) -> Unit,
    label: String
) {
    var editingId by remember { mutableStateOf<kotlin.uuid.Uuid?>(null) }
    var textValue by remember { mutableStateOf("") }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(if (editingId == null) "Add New $label" else "Rename $label") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { 
                    onSave(editingId, textValue)
                    textValue = ""
                    editingId = null
                },
                enabled = textValue.isNotBlank()
            ) {
                Text(if (editingId == null) "Add" else "Save")
            }
            if (editingId != null) {
                IconButton(onClick = { editingId = null; textValue = "" }) {
                    Icon(Icons.Default.Close, null)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items.sortedBy { it.second }) { (id, name) ->
                ListItem(
                    headlineContent = { Text(name) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { editingId = id; textValue = name }) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDelete(id) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun UnitDataManager(
    units: List<UnitModel>,
    onSave: (UnitModel) -> Unit,
    onDelete: (kotlin.uuid.Uuid) -> Unit
) {
    var editingUnit by remember { mutableStateOf<UnitModel?>(null) }
    
    var name by remember { mutableStateOf("") }
    var abbr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(UnitType.Weight) }
    var factor by remember { mutableStateOf("1.0") }

    fun reset() {
        editingUnit = null
        name = ""
        abbr = ""
        type = UnitType.Weight
        factor = "1.0"
    }

    Column {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (editingUnit == null) "Add Custom Unit" else "Edit Unit", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = abbr, onValueChange = { abbr = it }, label = { Text("Abbr") }, modifier = Modifier.weight(0.5f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        SearchableDropdown(
                            label = "Type",
                            options = UnitType.entries.map { it.name },
                            selectedOption = type.name,
                            onOptionSelected = { type = UnitType.valueOf(it) },
                            onAddOption = {}, onDeleteOption = {}, deleteWarningMessage = ""
                        )
                    }
                    OutlinedTextField(value = factor, onValueChange = { factor = it }, label = { Text("Factor to Base") }, modifier = Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        val f = factor.toDoubleOrNull() ?: 1.0
                        onSave(UnitModel(editingUnit?.id ?: kotlin.uuid.Uuid.random(), type, abbr, name, false, f))
                        reset()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && abbr.isNotBlank()
                ) {
                    Text(if (editingUnit == null) "Add Unit" else "Save Changes")
                }
                if (editingUnit != null) {
                    TextButton(onClick = { reset() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel Edit") }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(units.filter { !it.isSystemUnit }) { unit ->
                ListItem(
                    headlineContent = { Text(unit.displayName) },
                    supportingContent = { Text("${unit.abbreviation} (${unit.type}) • Factor: ${unit.factorToBase}") },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { 
                                editingUnit = unit
                                name = unit.displayName
                                abbr = unit.abbreviation
                                type = unit.type
                                factor = unit.factorToBase.toString()
                            }) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDelete(unit.id) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
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