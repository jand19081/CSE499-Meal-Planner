package io.github.and19081.mealplanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A standard toolbar for List Views (Ingredients, Recipes, Meals).
 * Includes: Search Bar, Sort Toggle, Add Button.
 */
@Composable
fun ListControlToolbar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    isSortByPrimary: Boolean,
    onToggleSort: () -> Unit,
    onAddClick: () -> Unit,
    primarySortIcon: ImageVector = Icons.Default.Category,
    secondarySortIcon: ImageVector = Icons.Default.SortByAlpha
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(searchPlaceholder) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )

        // Sort Toggle
        IconButton(onClick = onToggleSort) {
            Icon(
                if (isSortByPrimary) primarySortIcon else secondarySortIcon,
                contentDescription = "Toggle Sort"
            )
        }

        // Add Button
        FloatingActionButton(
            onClick = onAddClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.Add, "Add Item")
        }
    }
}

/**
 * A generic expandable list item row.
 */
@Composable
fun ExpandableListItem(
    title: String,
    subtitle: String? = null,
    onEditClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            // Leading Arrow
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Expand",
                modifier = Modifier.rotate(rotation)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (!expanded && subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Edit Button
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Expanded Content
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 32.dp, top = 8.dp)) {
                content()
            }
        }
    }
}

/**
 * A generic Searchable Dropdown that supports Adding and Deleting items.
 */
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
        OutlinedTextField(
            value = if (expanded) searchText else selectedOption, // Show search text when typing
            onValueChange = {
                searchText = it
                if (!expanded) expanded = true
            },
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = {
                    expanded = !expanded
                    if (expanded) searchText = "" // Clear search on open
                }) {
                    Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
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
                Button(
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
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }
}
