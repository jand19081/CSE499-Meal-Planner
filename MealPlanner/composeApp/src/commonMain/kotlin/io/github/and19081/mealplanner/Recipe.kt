@file:OptIn(ExperimentalUuidApi::class)

package io.github.and19081.mealplanner

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.uuid.ExperimentalUuidApi
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

object RecipeRepository {
    val recipes = MutableStateFlow(
        listOf(
            Recipe(id = Uuid.parse("550e8400-e29b-41d4-a716-446655440000"), name = "Lasagna", baseServings = 4.0, instructions = emptyList()),
            Recipe(id = Uuid.parse("550e8400-e29b-41d4-a716-446655440001"), name = "Tacos", baseServings = 2.0, instructions = emptyList()),
            Recipe(id = Uuid.parse("550e8400-e29b-41d4-a716-446655440002"), name = "Omelette", baseServings = 1.0, instructions = emptyList())
        )
    )
}