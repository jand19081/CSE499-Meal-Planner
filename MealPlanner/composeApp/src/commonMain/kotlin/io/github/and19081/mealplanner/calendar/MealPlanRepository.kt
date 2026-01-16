@file:OptIn(ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.calendar


import io.github.and19081.mealplanner.MealPlanEntry
import io.github.and19081.mealplanner.ScheduledItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object MealPlanRepository {
    private val _entries = MutableStateFlow<List<MealPlanEntry>>(emptyList())
    private val _scheduledItems = MutableStateFlow<List<ScheduledItem>>(emptyList())

    val entries: StateFlow<List<MealPlanEntry>> = _entries.asStateFlow()
    val scheduledItems: StateFlow<List<ScheduledItem>> = _scheduledItems.asStateFlow()

    /**
     * Creates a Meal Plan Entry (or Slot) and populates it with items (Food).
     */
    fun addPlan(entry: MealPlanEntry, items: List<ScheduledItem>) {
        _entries.update { it + entry }
        _scheduledItems.update { it + items }
    }

    fun removePlan(entryId: Uuid) {
        _entries.update { list -> list.filter { it.id != entryId } }
        // Cascade delete items
        _scheduledItems.update { list -> list.filter { it.mealPlanEntryId != entryId } }
    }

    // Helper to clear data for prototyping
    fun clearAll() {
        _entries.value = emptyList()
        _scheduledItems.value = emptyList()
    }
}