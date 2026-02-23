package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.Recipe
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.*
import io.github.and19081.mealplanner.data.toModel
import io.github.and19081.mealplanner.recipes.RecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.uuid.Uuid

class RoomRecipeRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope
) : RecipeRepository {

    private val recipeDao = db.recipeDao()

    override val recipes: StateFlow<List<Recipe>> = recipeDao.observeAllWithDetails()
        .map { list -> list.map { it.toModel() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override suspend fun addRecipe(recipe: Recipe) {
        upsertRecipe(recipe)
    }

    override suspend fun updateRecipe(recipe: Recipe) {
        upsertRecipe(recipe)
    }

    override suspend fun upsertRecipe(recipe: Recipe) {
        val recipeEntity = RecipeEntity(
            id = recipe.id,
            name = recipe.name,
            description = recipe.description,
            servings = recipe.servings,
            mealType = recipe.mealType,
            prepTimeMinutes = recipe.prepTimeMinutes,
            cookTimeMinutes = recipe.cookTimeMinutes
        )
        recipeDao.upsertRecipe(recipeEntity)

        // Sync instructions
        recipeDao.replaceInstructions(
            recipe.id.toString(),
            recipe.instructions.mapIndexed { index, instr ->
                RecipeInstructionEntity(recipeId = recipe.id, stepOrder = index, instruction = instr)
            }
        )

        // Sync requirements
        recipeDao.clearRequirements(recipe.id.toString())
        recipe.ingredients.forEachIndexed { index, ri ->
            recipeDao.upsertRequirement(RecipeRequirementEntity(
                recipeId = recipe.id,
                ingredientId = ri.ingredientId,
                subRecipeId = ri.subRecipeId,
                unitId = ri.unitId,
                quantity = ri.quantity,
                sortOrder = index
            ))
        }
    }

    override suspend fun removeRecipe(id: Uuid) {
        val entity = recipeDao.getById(id.toString())
        if (entity != null) recipeDao.deleteRecipe(entity)
    }

    override suspend fun setRecipes(newRecipes: List<Recipe>) {
        newRecipes.forEach { upsertRecipe(it) }
    }
}
