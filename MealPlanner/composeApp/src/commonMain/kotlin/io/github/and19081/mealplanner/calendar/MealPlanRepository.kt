package io.github.and19081.mealplanner.calendar

import io.github.and19081.mealplanner.ScheduledMeal
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.Uuid

interface MealPlanRepository {
    val entries: StateFlow<List<ScheduledMeal>>
    suspend fun addPlan(entry: ScheduledMeal)
    suspend fun removePlan(entryId: Uuid)
    suspend fun setConsumedStatus(entryId: Uuid, consumed: Boolean)
    suspend fun addReceipt(
        mealId: Uuid,
        actualTotalCents: Int,
        taxCents: Int,
        lineItems: List<Triple<String, Double, Int>>
    )
    suspend fun clearAll()
}
