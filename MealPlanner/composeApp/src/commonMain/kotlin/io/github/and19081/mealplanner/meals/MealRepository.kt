package io.github.and19081.mealplanner.meals

import io.github.and19081.mealplanner.PrePlannedMeal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

object MealRepository {
    private val _meals = MutableStateFlow<List<PrePlannedMeal>>(emptyList())
    val meals = _meals.asStateFlow()

    fun addMeal(meal: PrePlannedMeal) {
        _meals.update { it + meal }
    }

    fun updateMeal(meal: PrePlannedMeal) {
        _meals.update { list -> list.map { if (it.id == meal.id) meal else it } }
    }

    fun upsertMeal(meal: PrePlannedMeal) {
        _meals.update { current ->
            val list = current.toMutableList()
            list.removeAll { it.id == meal.id }
            list.add(meal)
            list
        }
    }

    fun removeMeal(id: Uuid) {
        _meals.update { list -> list.filter { it.id != id } }
    }

    fun setMeals(newMeals: List<PrePlannedMeal>) {
        _meals.value = newMeals
    }
}