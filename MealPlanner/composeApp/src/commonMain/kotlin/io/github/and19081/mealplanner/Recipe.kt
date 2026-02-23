package io.github.and19081.mealplanner

import kotlinx.serialization.Serializable

import kotlin.uuid.Uuid



@Serializable

enum class RecipeMealType {

    Breakfast, Lunch, Dinner, Snack, Other

}



@Serializable
data class RecipeIngredient(
    val ingredientId: Uuid? = null,
    val subRecipeId: Uuid? = null,
    val quantity: Double,
    val unitId: Uuid
)



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

    val ingredients: List<RecipeIngredient> = emptyList()

)
