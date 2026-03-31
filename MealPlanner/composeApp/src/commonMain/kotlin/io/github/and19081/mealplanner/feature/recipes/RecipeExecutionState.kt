package io.github.and19081.mealplanner.feature.recipes

import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.FoodItemRequirement
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

enum class RecipeExecutionView { INSTRUCTIONS, INGREDIENTS }

data class ScaledRequirement(
    val req: FoodItemRequirement,
    val scaledQuantity: Double,
    val ingredientName: String,
    val unitAbbreviation: String,
)

data class RecipeExecutionState(
    val recipe: FoodItem.Recipe,
    val targetServings: Double,
    val currentStepIndex: Int,      // -1 = overview screen
    val view: RecipeExecutionView,
) {
    val scaleFactor: Double
        get() = if (recipe.recipeInfo.servings > 0)
            targetServings / recipe.recipeInfo.servings else 1.0

    val totalSteps: Int get() = recipe.recipeInfo.instructions.size
    val isOnOverview: Boolean get() = currentStepIndex == -1
    val isOnFinalStep: Boolean get() = currentStepIndex == totalSteps - 1
    val currentInstruction: String?
        get() = recipe.recipeInfo.instructions.getOrNull(currentStepIndex)

    fun scaledRequirements(
        allItemNames: Map<Uuid, String>,
        allUnitAbbr: Map<Uuid, String>,
    ): List<ScaledRequirement> =
        recipe.recipeInfo.requirementGroups
            .flatMap { it.requirements.filter { r -> r.isPrimary } }
            .map { req ->
                ScaledRequirement(
                    req = req,
                    scaledQuantity = req.measurement.quantity * scaleFactor,
                    ingredientName = req.measurement.foodItemId
                        ?.let { allItemNames[it] } ?: "Unknown",
                    unitAbbreviation = req.measurement.unitId
                        ?.let { allUnitAbbr[it] } ?: "",
                )
            }
}
