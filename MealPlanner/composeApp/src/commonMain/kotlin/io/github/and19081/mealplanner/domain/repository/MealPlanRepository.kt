package io.github.and19081.mealplanner.domain.repository

import io.github.and19081.mealplanner.feature.meals.ScheduledMeal
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.StateFlow

interface MealPlanRepository {
  val entries: StateFlow<List<ScheduledMeal>>

  suspend fun getMealById(id: Uuid): ScheduledMeal?

  suspend fun addPlan(entry: ScheduledMeal)

  suspend fun removePlan(entryId: Uuid)

  suspend fun setConsumedStatus(entryId: Uuid, consumed: Boolean)

  suspend fun addReceipt(
      mealId: Uuid,
      actualTotalCents: Int,
      taxCents: Int,
      lineItems: List<Triple<String, Double, Int>>,
  )

  suspend fun clearAll()
}
