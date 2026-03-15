package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.Recipe
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.*
import io.github.and19081.mealplanner.data.toModel
import io.github.and19081.mealplanner.recipes.RecipeRepository
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomRecipeRepository(private val db: MealPlannerDatabase, private val scope: CoroutineScope) :
    RecipeRepository {

  private val recipeDao = db.recipeDao()

  override val recipes: StateFlow<List<Recipe>> =
      recipeDao
          .observeAllWithDetails()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun addRecipe(recipe: Recipe) {
    upsertRecipe(recipe)
  }

  override suspend fun updateRecipe(recipe: Recipe) {
    upsertRecipe(recipe)
  }

  override suspend fun upsertRecipe(recipe: Recipe) {
    val recipeEntity =
        RecipeEntity(
            id = recipe.id,
            name = recipe.name,
            description = recipe.description,
            servings = recipe.servings,
            mealType = recipe.mealType,
            prepTimeMinutes = recipe.prepTimeMinutes,
            cookTimeMinutes = recipe.cookTimeMinutes,
            producesIngredientId = recipe.producesIngredientId,
            amountPerServing = recipe.amountPerServing,
        )

    val instructions =
        recipe.instructions.mapIndexed { index, instr ->
          RecipeInstructionEntity(recipeId = recipe.id, stepOrder = index, instruction = instr)
        }

    val requirementGroups =
        recipe.requirementGroups.mapIndexed { index, group ->
          RecipeRequirementGroupEntity(id = group.id, recipeId = recipe.id, sortOrder = index)
        }

    val requirements =
        recipe.requirementGroups.flatMap { group ->
          group.requirements.map { req ->
            RecipeRequirementEntity(
                id = req.id,
                groupId = group.id,
                ingredientId = req.ingredientId,
                subRecipeId = req.subRecipeId,
                unitId = req.unitId,
                quantity = req.quantity,
                isPrimary = req.isPrimary,
            )
          }
        }

    recipeDao.upsertRecipeWithDetails(recipeEntity, instructions, requirementGroups, requirements)
  }

  override suspend fun removeRecipe(id: Uuid) {
    val entity = recipeDao.getById(id)
    if (entity != null) recipeDao.deleteRecipe(entity)
  }

  override suspend fun setRecipes(newRecipes: List<Recipe>) {
    newRecipes.forEach { upsertRecipe(it) }
  }
}
