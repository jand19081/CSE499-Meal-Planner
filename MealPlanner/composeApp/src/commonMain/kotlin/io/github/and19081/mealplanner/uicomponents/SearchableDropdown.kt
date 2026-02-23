package io.github.and19081.mealplanner.uicomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onAddOption: (String) -> Unit,
    onDeleteOption: (String) -> Unit,
    deleteWarningMessage: String
) {
    var expanded by remember { mutableStateOf(false) }
    // Maintain internal text state to prevent focus-change resets on Android
    var searchText by remember(selectedOption) { mutableStateOf(selectedOption) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        MpOutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                expanded = true
            },
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { 
                expanded = false
                searchText = selectedOption // Revert to actual selection on dismiss
            },
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            val filtered = options.filter { it.contains(searchText, ignoreCase = true) }

            filtered.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            horizontalArrangement = Arrangement.SpaceBetween, 
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option)
                            IconButton(onClick = { showDeleteConfirm = option }) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    onClick = {
                        onOptionSelected(option)
                        searchText = option
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }

            if (filtered.isEmpty() && searchText.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("Add \"$searchText\"") },
                    onClick = {
                        onAddOption(searchText)
                        onOptionSelected(searchText)
                        expanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.Add, null) },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete '${showDeleteConfirm}'? $deleteWarningMessage") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteOption(showDeleteConfirm!!)
                        if (selectedOption == showDeleteConfirm) {
                            onOptionSelected("")
                            searchText = ""
                        }
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }
}
