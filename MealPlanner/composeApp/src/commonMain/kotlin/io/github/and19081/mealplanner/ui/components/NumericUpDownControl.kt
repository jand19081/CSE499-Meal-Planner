package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun VerticalNumericUpDownControl(
    value: Double,
    onValueChange: (Double) -> Unit,
    min: Double = 0.0,
    max: Double = Double.MAX_VALUE,
    step: Double = 1.0,
    modifier: Modifier = Modifier
) {
    // 1. Keep a local string state so typing doesn't get interrupted or reformatted mid-keystroke
    var textValue by remember { mutableStateOf(formatForDisplay(value)) }

    // 2. Sync local text ONLY when the external value changes via the +/- buttons
    // (This prevents erasing decimal points while the user is actively typing)
    LaunchedEffect(value) {
        val parsedLocal = textValue.toDoubleOrNull()
        if (parsedLocal != value) {
            textValue = formatForDisplay(value)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        // Increase Button
        Button(
            onClick = {
                val newValue = (value + step).coerceAtMost(max)
                onValueChange(newValue)
            },
            enabled = value < max
        ) {
            Text("+")
        }

        // Value Display/Input
        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText // Always let the user type

                // Only push valid, bounded numbers up to the state
                val parsedDouble = newText.toDoubleOrNull()
                if (parsedDouble != null) {
                    onValueChange(parsedDouble.coerceIn(min, max))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.width(100.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        // Decrease Button
        Button(
            onClick = {
                val newValue = (value - step).coerceAtLeast(min)
                onValueChange(newValue)
            },
            enabled = value > min
        ) {
            Text("-")
        }


    }
}

@Composable
fun HorizontalNumericUpDownControl(
    value: Double,
    onValueChange: (Double) -> Unit,
    min: Double = 0.0,
    max: Double = Double.MAX_VALUE,
    step: Double = 1.0,
    modifier: Modifier = Modifier
) {
    // 1. Keep a local string state so typing doesn't get interrupted or reformatted mid-keystroke
    var textValue by remember { mutableStateOf(formatForDisplay(value)) }

    // 2. Sync local text ONLY when the external value changes via the +/- buttons
    // (This prevents erasing decimal points while the user is actively typing)
    LaunchedEffect(value) {
        val parsedLocal = textValue.toDoubleOrNull()
        if (parsedLocal != value) {
            textValue = formatForDisplay(value)
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decrease Button
        Button(
            onClick = {
                val newValue = (value - step).coerceAtLeast(min)
                onValueChange(newValue)
            },
            enabled = value > min
        ) {
            Text("-")
        }

        // Value Display/Input
        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText // Always let the user type

                // Only push valid, bounded numbers up to the state
                val parsedDouble = newText.toDoubleOrNull()
                if (parsedDouble != null) {
                    onValueChange(parsedDouble.coerceIn(min, max))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.width(100.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        // Increase Button
        Button(
            onClick = {
                val newValue = (value + step).coerceAtMost(max)
                onValueChange(newValue)
            },
            enabled = value < max
        ) {
            Text("+")
        }
    }
}

// Helper to clean up numbers like "8.0" to just "8" for cleaner UI
private fun formatForDisplay(value: Double): String {
    val stringValue = value.toString()
    return if (stringValue.endsWith(".0")) stringValue.removeSuffix(".0") else stringValue
}