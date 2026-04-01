@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package io.github.and19081.mealplanner.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.Package
import io.github.and19081.mealplanner.feature.settings.Mode
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Shows all meals for a selected day in time order. Acts as a dialog on mobile and a side panel on
 * desktop.
 *
 * @param date The date to display
 * @param dayEvents All meal events for the selected day
 * @param onEventClick Callback when an event is clicked
 * @param onAddEvent Callback to add a new event
 * @param onConsumeEvent Callback to consume an event
 * @param onDismiss Request to dismiss the panel
 * @param isExpanded Whether the panel is in desktop expanded mode
 * @param allMeals List of available meals for reference
 * @param allRestaurants List of available restaurants
 * @param mode The UI mode (mobile, desktop, auto)
 * @param modifier Modifier for the root layout
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DayDetailPanel(
    date: LocalDate,
    dayEvents: List<DayEventUi>,
    onEventClick: (Uuid) -> Unit,
    onAddEvent: () -> Unit,
    onConsumeEvent: (Uuid) -> Unit,
    onDismiss: () -> Unit,
    allMeals: List<FoodItem> = emptyList(),
    allRestaurants: List<Package> = emptyList(),
    mode: Mode = Mode.AUTO,
    modifier: Modifier = Modifier,
) {
  val isDesktopExpanded =
      when (mode) {
        Mode.AUTO -> false
        Mode.DESKTOP -> true
        Mode.MOBILE -> false
      }

  if (isDesktopExpanded) {
    Surface(
        modifier = modifier.fillMaxHeight().wrapContentWidth(),
        tonalElevation = 4.dp,
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                  "${date.dayOfWeek.name}, ${date.dayOfMonth} ${date.month.name}",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
              )
              Text(
                  "$dayEvents events for the day",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close panel") }
          }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (dayEvents.isEmpty()) {
            item {
              EmptyListMessage(
                  message = "No meals planned for ${date.month.name} $date.dayOfMonth",
                  modifier = Modifier.fillMaxWidth(),
              )
            }
          } else {
            items(dayEvents) { event ->
              DayEventRow(
                  event = event,
                  onEventClick = { onEventClick(event.entryId) },
                  onConsume = { onConsumeEvent(event.entryId) },
                  allMeals = allMeals,
                  allRestaurants = allRestaurants,
              )
            }
          }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
          ExtendedFloatingActionButton(onClick = onAddEvent) {
            Icon(Icons.Default.Add, "Add meal")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Meal")
          }
        }
      }
    }
  } else {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
          Column {
            Text(
                "${date.dayOfWeek.name}, ${date.dayOfMonth} ${date.month.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${dayEvents.size} events for the day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        text = {
          if (dayEvents.isEmpty()) {
            EmptyListMessage(
                message = "No meals planned for ${date.month.name} $date.dayOfMonth",
                modifier = Modifier.fillMaxWidth(),
            )
          } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(dayEvents) { event ->
                DayEventRow(
                    event = event,
                    onEventClick = { onEventClick(event.entryId) },
                    onConsume = { onConsumeEvent(event.entryId) },
                    allMeals = allMeals,
                    allRestaurants = allRestaurants,
                )
              }
            }
          }
        },
        confirmButton = {
          Button(onClick = onAddEvent) {
            Icon(Icons.Default.Add, "Add", modifier = Modifier.padding(end = 8.dp))
            Text("Add Meal")
          }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
  }
}

data class DayEventUi(
    val entryId: Uuid,
    val title: String,
    val mealType: RecipeMealType,
    val time: LocalTime,
    val isConsumed: Boolean,
    val peopleCount: Int,
)

@Composable
private fun DayEventRow(
    event: DayEventUi,
    onEventClick: () -> Unit,
    onConsume: () -> Unit,
    allMeals: List<FoodItem>,
    allRestaurants: List<Package>,
) {
  val color =
      when (event.mealType) {
        RecipeMealType.Breakfast -> MaterialTheme.colorScheme.primary
        RecipeMealType.Lunch -> MaterialTheme.colorScheme.secondary
        RecipeMealType.Dinner -> MaterialTheme.colorScheme.tertiary
        RecipeMealType.Snack -> MaterialTheme.colorScheme.outline
        RecipeMealType.Side -> MaterialTheme.colorScheme.surfaceTint
        RecipeMealType.Other -> MaterialTheme.colorScheme.surfaceTint
      }

  Card(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onEventClick),
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (event.isConsumed) MaterialTheme.colorScheme.surfaceVariant
                  else MaterialTheme.colorScheme.surface
          ),
  ) {
    Column(
        modifier = Modifier.padding(16.dp),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
              event.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
              "${event.time} • ${event.peopleCount} ppl",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Surface(
            color = color,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.size(12.dp),
        ) {}
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            event.mealType.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!event.isConsumed) {
          FilledTonalButton(
              onClick = onConsume,
              colors = ButtonDefaults.filledTonalButtonColors(containerColor = color),
          ) {
            Icon(Icons.Default.CheckCircle, "Consume")
            Spacer(modifier = Modifier.width(4.dp))
            Text("Consume")
          }
        } else {
          Icon(
              Icons.Default.CheckCircle,
              contentDescription = "Consumed",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}
