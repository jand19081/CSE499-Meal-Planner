package io.github.and19081.mealplanner.uicomponents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.domain.DataWarning

@Composable
fun MpValidationWarning(
    warnings: List<DataWarning>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = warnings.isNotEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Data Quality Warnings",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                warnings.take(3).forEach { warning ->
                    Text(
                        "• ${warning.message}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (warnings.size > 3) {
                    Text(
                        "• ...and ${warnings.size - 3} more",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
