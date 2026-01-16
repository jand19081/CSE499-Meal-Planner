@file:OptIn(ExperimentalUuidApi::class)

package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.PurchaseOption
import io.github.and19081.mealplanner.ingredients.Store
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object MealPlannerRepository {
    val stores = mutableListOf<Store>()
    val ingredients = mutableListOf<Ingredient>()
    val allPurchaseOptions = mutableListOf<PurchaseOption>()

    // Added to support RecipeIngredient lookups
    val recipeIngredients = mutableListOf<RecipeIngredient>()

    fun deleteStore(storeId: Uuid) {
        stores.removeAll { it.id == storeId }
        allPurchaseOptions.removeAll { it.storeId == storeId }
    }
}