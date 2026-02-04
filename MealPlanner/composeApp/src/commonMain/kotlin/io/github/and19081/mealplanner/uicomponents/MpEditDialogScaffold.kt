package io.github.and19081.mealplanner.uicomponents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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