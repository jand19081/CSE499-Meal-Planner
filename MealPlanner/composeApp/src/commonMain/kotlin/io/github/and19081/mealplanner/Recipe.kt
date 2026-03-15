package io.github.and19081.mealplanner

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
enum class RecipeMealType {

  Breakfast,
  Lunch,
  Dinner,
    Side,
  Snack,
  Other,
}

@Serializable
data class RecipeRequirement(
    val id: Uuid = Uuid.random(),
    val ingredientId: Uuid? = null,
    val subRecipeId: Uuid? = null,
    val quantity: Double,
    val unitId: Uuid? = null,
    val isPrimary: Boolean = true,
)

@Serializable
data class RecipeRequirementGroup(val id: Uuid, val requirements: List<RecipeRequirement>)

@Serializable
data class Recipe(
    val id: Uuid,
    val name: String,
    val description: String? = null,
    val instructions: List<String> = emptyList(),
    val servings: Double,
    val mealType: RecipeMealType,
    val prepTimeMinutes: Int = 0,
    val cookTimeMinutes: Int = 0,
    val producesIngredientId: Uuid? = null,
    val amountPerServing: Double? = null,
    val requirementGroups: List<RecipeRequirementGroup> = emptyList(),
) {
  // Helper to get primary ingredients for legacy/simple calculations
  val ingredients: List<RecipeRequirement>
    get() =
        requirementGroups.mapNotNull { group ->
          group.requirements.find { it.isPrimary } ?: group.requirements.firstOrNull()
        }
}
