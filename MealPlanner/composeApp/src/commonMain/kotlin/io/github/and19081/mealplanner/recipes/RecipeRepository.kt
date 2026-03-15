package io.github.and19081.mealplanner.recipes

import io.github.and19081.mealplanner.Recipe
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.StateFlow

interface RecipeRepository {
  val recipes: StateFlow<List<Recipe>>

  suspend fun addRecipe(recipe: Recipe)

  suspend fun updateRecipe(recipe: Recipe)

  suspend fun upsertRecipe(recipe: Recipe)

  suspend fun removeRecipe(id: Uuid)

  suspend fun setRecipes(newRecipes: List<Recipe>)
}
