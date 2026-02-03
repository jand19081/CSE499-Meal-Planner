package io.github.and19081.mealplanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

// --- Wrapped Components ---

@Composable
fun MpSurface(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        content = content
    )
}

@Composable
fun MpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium, // Default to theme's medium shape
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun MpTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun MpOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun MpOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.medium,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}

@Composable
fun MpCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium, // Card defaults to medium, but explicit ensures adherence
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    }
}

@Composable
fun MpFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}

// --- Helper Components ---

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
        MpOutlinedTextField(
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
        MpFloatingActionButton(
            onClick = onAddClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(48.dp) // Standard small FAB size or just custom?
        ) {
            Icon(Icons.Default.Add, "Add Item")
        }
    }
}

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

            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 32.dp, top = 8.dp)) {
                content()
            }
        }
    }
}

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

// --- Refactored Components ---


@Composable
fun CreateNewItemRow(
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Create '$searchQuery'",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Add this new item to the database",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ListSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    MpSurface(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyListMessage(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).padding(bottom = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DialogActionButtons(
    onCancel: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    saveLabel: String = "Save",
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (onDelete != null) Arrangement.SpaceBetween else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onDelete != null) {
            MpTextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MpTextButton(onClick = onCancel) { Text("Cancel") }
            MpButton(
                onClick = onSave,
                enabled = saveEnabled
            ) { Text(saveLabel) }
        }
    }
}

@Composable
fun MpEditDialogScaffold(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    onDelete: (() -> Unit)? = null,
    tabs: List<String> = emptyList(),
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
                if (tabs.isNotEmpty()) {
                    SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, t ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { onTabSelected(index) },
                                text = { Text(t) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        },
        confirmButton = {
            DialogActionButtons(onCancel = onDismiss, onSave = onSave, saveEnabled = saveEnabled, onDelete = onDelete)
        }
    )
}