package io.github.and19081.mealplanner.meals

import io.github.and19081.mealplanner.PrePlannedMeal
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.Uuid

interface MealRepository {
    val meals: StateFlow<List<PrePlannedMeal>>
    suspend fun addMeal(meal: PrePlannedMeal)
    suspend fun updateMeal(meal: PrePlannedMeal)
    suspend fun upsertMeal(meal: PrePlannedMeal)
    suspend fun removeMeal(id: Uuid)
    suspend fun setMeals(newMeals: List<PrePlannedMeal>)
}
