package io.github.and19081.mealplanner.uicomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

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
    var searchText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxWidth()) {
        MpOutlinedTextField(
            value = if (expanded) searchText else selectedOption,
            onValueChange = {
                searchText = it
                if (!expanded) expanded = true
            },
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = {
                    expanded = !expanded
                    if (expanded) searchText = ""
                }) {
                    Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 200.dp)
        ) {
            val filtered = options.filter { it.contains(searchText, ignoreCase = true) }

            filtered.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(option)
                            IconButton(onClick = { showDeleteConfirm = option }) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                        searchText = ""
                    }
                )
            }

            if (filtered.isEmpty() && searchText.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("Add \"$searchText\"") },
                    onClick = {
                        onAddOption(searchText)
                        onOptionSelected(searchText)
                        expanded = false
                        searchText = ""
                    },
                    leadingIcon = { Icon(Icons.Default.Add, null) }
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
                MpButton(
                    onClick = {
                        onDeleteOption(showDeleteConfirm!!)
                        showDeleteConfirm = null
                        if (selectedOption == showDeleteConfirm) {
                            onOptionSelected("")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                MpTextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }
}