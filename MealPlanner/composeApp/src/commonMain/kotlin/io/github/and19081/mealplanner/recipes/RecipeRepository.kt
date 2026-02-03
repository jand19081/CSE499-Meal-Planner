package io.github.and19081.mealplanner.recipes

import io.github.and19081.mealplanner.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

object RecipeRepository {
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes = _recipes.asStateFlow()

    fun addRecipe(recipe: Recipe) {
        _recipes.update { it + recipe }
    }

    fun updateRecipe(recipe: Recipe) {
        _recipes.update { list -> list.map { if (it.id == recipe.id) recipe else it } }
    }

    fun upsertRecipe(recipe: Recipe) {
        _recipes.update { current ->
            val list = current.toMutableList()
            list.removeAll { it.id == recipe.id }
            list.add(recipe)
            list
        }
    }

    fun removeRecipe(id: Uuid) {
        _recipes.update { list -> list.filter { it.id != id } }
    }
    
    fun setRecipes(newRecipes: List<Recipe>) {
        _recipes.value = newRecipes
    }
}