package io.github.and19081.mealplanner.data.db.relation

import androidx.room.ColumnInfo
import kotlin.uuid.Uuid

data class RecipeCost(
    @ColumnInfo(name = "recipe_id") val recipeId: Uuid,
    @ColumnInfo(name = "total_cost_cents") val totalCostCents: Long
)

data class MealCost(
    @ColumnInfo(name = "meal_id") val mealId: Uuid,
    @ColumnInfo(name = "total_cost_cents") val totalCostCents: Long
)
