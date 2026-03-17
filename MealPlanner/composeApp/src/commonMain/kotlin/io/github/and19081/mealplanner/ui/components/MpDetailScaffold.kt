@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MpDetailScaffold(
    title: String,
    onClose: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    onDelete: (() -> Unit)? = null,
    tabs: List<String> = emptyList(),
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    isScrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Header
      Row(
          modifier = Modifier.fillMaxWidth().padding(8.dp), // Reduced padding for header
          verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onClose) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
      }

      if (tabs.isNotEmpty()) {
        SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
          tabs.forEachIndexed { index, t ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = { Text(t) },
            )
          }
        }
      }

      // Content
      val scrollModifier = if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
      Column(
          modifier = Modifier.weight(1f).then(scrollModifier).padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        content()
      }

      // Footer / Actions
      Surface(tonalElevation = 2.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          if (onDelete != null) {
            TextButton(
                onClick = onDelete,
                colors =
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
              Text("Delete")
            }
          } else {
            Spacer(modifier = Modifier.width(1.dp))
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onClose) { Text("Cancel") }
            Button(onClick = onSave, enabled = saveEnabled) { Text("Save") }
          }
        }
      }
    }
  }
}
