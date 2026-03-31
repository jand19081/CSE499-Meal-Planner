package io.github.and19081.mealplanner.domain.model

import kotlin.uuid.Uuid

enum class RecipeExecutionView {
    INSTRUCTIONS, INGREDIENTS
}

data class RecipeExecutionState(
    val recipe: Recipe,
    val targetServings: Double,
    val currentStepIndex: Int, // -1 for overview
    val view: RecipeExecutionView,
    val totalSteps: Int
) {
    val scaleFactor: Double get() = targetServings / recipe.recipeInfo.servings
}
