package io.github.and19081.mealplanner.data.db.relation

import androidx.room.ColumnInfo
import kotlin.uuid.Uuid

data class RecipeCost(
    @ColumnInfo(name = "recipe_id") val recipeId: Uuid,
    @ColumnInfo(name = "total_cost_cents") val totalCostCents: Long,
)

data class MealCost(
    @ColumnInfo(name = "meal_id") val mealId: Uuid,
    @ColumnInfo(name = "total_cost_cents") val totalCostCents: Long,
)

data class MonthlyExpenditure(
    @ColumnInfo(name = "month") val month: String,
    @ColumnInfo(name = "totalSpent") val totalSpentCents: Long,
)

data class RecipeBOMItem(
    @ColumnInfo(name = "food_item_id") val foodItemId: Uuid,
    @ColumnInfo(name = "required_qty") val requiredQty: Double,
)
