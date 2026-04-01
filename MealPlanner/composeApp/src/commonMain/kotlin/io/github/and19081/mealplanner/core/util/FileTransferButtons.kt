package io.github.and19081.mealplanner.core.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun FileTransferButtons(
    onExport: suspend () -> String,
    onImport: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
)
