@file:OptIn(ExperimentalUuidApi::class)

package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.PurchaseOption
import io.github.and19081.mealplanner.ingredients.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object MealPlannerRepository {
    val stores = mutableListOf<Store>()
    val ingredients = mutableListOf<Ingredient>()
    val allPurchaseOptions = mutableListOf<PurchaseOption>()

    // Join Tables
    val recipeIngredients = mutableListOf<RecipeIngredient>()
    
    // New Meal Definitions
    val meals = MutableStateFlow<List<Meal>>(emptyList())
    val mealComponents = MutableStateFlow<List<MealComponent>>(emptyList())

    fun deleteStore(storeId: Uuid) {
        stores.removeAll { it.id == storeId }
        allPurchaseOptions.removeAll { it.storeId == storeId }
    }
}
