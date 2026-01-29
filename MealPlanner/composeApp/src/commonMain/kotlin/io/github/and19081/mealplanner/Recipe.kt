package io.github.and19081.mealplanner

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.uuid.Uuid

data class Recipe(
    val id: Uuid = Uuid.random(),
    val name: String,
    val description: String? = null,
    val baseServings: Double, // The recipe makes food for 4 people
    val instructions: List<String>
)

// The Join Table: Recipe <-> Ingredient
// "This recipe needs 2 cups of Flour"
data class RecipeIngredient(
    val id: Uuid = Uuid.random(),
    val recipeId: Uuid,
    val ingredientId: Uuid,
    val quantity: Measure // e.g. 2.0 CUPS
)

