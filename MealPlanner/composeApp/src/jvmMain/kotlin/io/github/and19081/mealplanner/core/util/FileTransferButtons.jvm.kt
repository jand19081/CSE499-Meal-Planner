package io.github.and19081.mealplanner.core.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.coroutines.launch

@Composable
actual fun FileTransferButtons(
    onExport: suspend () -> String,
    onImport: suspend (String) -> Unit,
    modifier: Modifier
) {
    val scope = rememberCoroutineScope()

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                val dialog = FileDialog(null as Frame?, "Export Backup", FileDialog.SAVE)
                dialog.file = "meal_planner_backup.json"
                dialog.isVisible = true
                val file = dialog.file
                val dir = dialog.directory
                if (file != null && dir != null) {
                    scope.launch {
                        val data = onExport()
                        File(dir, file).writeText(data)
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("Export Backup (JSON)")
        }
        
        Button(
            onClick = {
                val dialog = FileDialog(null as Frame?, "Import Backup", FileDialog.LOAD)
                dialog.isVisible = true
                val file = dialog.file
                val dir = dialog.directory
                if (file != null && dir != null) {
                    scope.launch {
                        val data = File(dir, file).readText()
                        onImport(data)
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("Import Backup (JSON)")
        }
    }
}
