package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.PrePlannedMeal
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.MealIndependentIngredientEntity
import io.github.and19081.mealplanner.data.db.entity.MealRecipeEntity
import io.github.and19081.mealplanner.data.db.entity.PrePlannedMealEntity
import io.github.and19081.mealplanner.data.toModel
import io.github.and19081.mealplanner.meals.MealRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.uuid.Uuid

class RoomMealRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope
) : MealRepository {

    private val mealDao = db.prePlannedMealDao()

    override val meals: StateFlow<List<PrePlannedMeal>> = mealDao.observeAllWithDetails()
        .map { list -> list.map { it.toModel() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override suspend fun addMeal(meal: PrePlannedMeal) {
        upsertMeal(meal)
    }

    override suspend fun updateMeal(meal: PrePlannedMeal) {
        upsertMeal(meal)
    }

    override suspend fun upsertMeal(meal: PrePlannedMeal) {
        mealDao.upsert(PrePlannedMealEntity(id = meal.id, name = meal.name))
        
        // Sync recipes
        meal.recipes.forEach { recipeId ->
            mealDao.addRecipe(MealRecipeEntity(meal.id, recipeId))
        }

        // Sync independent ingredients
        meal.independentIngredients.forEach { mi ->
            mealDao.upsertIndependentIngredient(MealIndependentIngredientEntity(
                prePlannedMealId = meal.id,
                ingredientId = mi.ingredientId,
                unitId = mi.unitId,
                quantity = mi.quantity
            ))
        }
    }

    override suspend fun removeMeal(id: Uuid) {
        val entity = mealDao.getById(id.toString())
        if (entity != null) mealDao.delete(entity)
    }

    override suspend fun setMeals(newMeals: List<PrePlannedMeal>) {
        newMeals.forEach { addMeal(it) }
    }
}
