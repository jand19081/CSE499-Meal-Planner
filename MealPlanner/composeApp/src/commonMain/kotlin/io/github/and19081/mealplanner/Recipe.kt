package io.github.and19081.mealplanner

import kotlin.uuid.Uuid

enum class RecipeMealType {
    Breakfast, Lunch, Dinner, Snack, Other
}

data class RecipeIngredient(
    val ingredientId: Uuid,
    val quantity: Double,
    val unitId: Uuid
)

data class Recipe(
    val id: Uuid = Uuid.random(),
    val name: String,
    val description: String? = null,
    val instructions: List<String> = emptyList(),
    val servings: Double,
    val mealType: RecipeMealType,
    val prepTimeMinutes: Int = 0,
    val cookTimeMinutes: Int = 0,
    val ingredients: List<RecipeIngredient> = emptyList()
)