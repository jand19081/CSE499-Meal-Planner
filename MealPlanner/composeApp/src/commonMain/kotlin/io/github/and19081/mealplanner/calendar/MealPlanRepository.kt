package io.github.and19081.mealplanner.calendar

import io.github.and19081.mealplanner.ScheduledMeal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

object MealPlanRepository {
    private val _entries = MutableStateFlow<List<ScheduledMeal>>(emptyList())

    val entries: StateFlow<List<ScheduledMeal>> = _entries.asStateFlow()

    fun addPlan(entry: ScheduledMeal) {
        _entries.update { it + entry }
    }

    fun removePlan(entryId: Uuid) {
        _entries.update { list -> list.filter { it.id != entryId } }
    }

    fun markConsumed(entryId: Uuid) {
         _entries.update { list -> list.map { if (it.id == entryId) it.copy(isConsumed = true) else it } }
    }

    fun clearAll() {
        _entries.value = emptyList()
    }
}