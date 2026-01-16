@file:OptIn(ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.calendar

import io.github.and19081.mealplanner.MealPlanEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object MealPlanRepository {
    private val _entries = MutableStateFlow<List<MealPlanEntry>>(emptyList())

    val entries: StateFlow<List<MealPlanEntry>> = _entries.asStateFlow()

    fun addPlan(entry: MealPlanEntry) {
        _entries.update { it + entry }
    }

    fun removePlan(entryId: Uuid) {
        _entries.update { list -> list.filter { it.id != entryId } }
    }

    fun clearAll() {
        _entries.value = emptyList()
    }
}
