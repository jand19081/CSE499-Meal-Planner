package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.PurchaseOption
import io.github.and19081.mealplanner.ingredients.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

object MealPlannerRepository {
    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredients: StateFlow<List<Ingredient>> = _ingredients.asStateFlow()

    private val _allPurchaseOptions = MutableStateFlow<List<PurchaseOption>>(emptyList())
    val allPurchaseOptions: StateFlow<List<PurchaseOption>> = _allPurchaseOptions.asStateFlow()

    // Join Tables
    private val _recipeIngredients = MutableStateFlow<List<RecipeIngredient>>(emptyList())
    val recipeIngredients: StateFlow<List<RecipeIngredient>> = _recipeIngredients.asStateFlow()
    
    // New Meal Definitions
    val meals = MutableStateFlow<List<Meal>>(emptyList())
    val mealComponents = MutableStateFlow<List<MealComponent>>(emptyList())

    // Mutations
    fun addStore(store: Store) {
        _stores.update { it + store }
    }

    fun deleteStore(storeId: Uuid) {
        _stores.update { list -> list.filter { it.id != storeId } }
        _allPurchaseOptions.update { list -> list.filter { it.storeId != storeId } }
        // Cascade to ingredients
        _ingredients.update { list ->
            list.map { ing ->
                ing.copy(purchaseOptions = ing.purchaseOptions.filter { it.storeId != storeId })
            }
        }
    }

    fun addIngredient(ingredient: Ingredient) {
        _ingredients.update { it + ingredient }
    }

    fun updateIngredient(ingredient: Ingredient) {
        _ingredients.update { list -> list.map { if (it.id == ingredient.id) ingredient else it } }
    }

    fun removeIngredient(id: Uuid) {
        _ingredients.update { list -> list.filter { it.id != id } }
    }
    
    fun addPurchaseOption(option: PurchaseOption) {
        _allPurchaseOptions.update { it + option }
    }

    fun addRecipeIngredient(ri: RecipeIngredient) {
        _recipeIngredients.update { it + ri }
    }

    fun removeRecipeIngredients(recipeId: Uuid) {
        _recipeIngredients.update { list -> list.filter { it.recipeId != recipeId } }
    }

    // Bulk Setters (for Mock Data)
    fun setStores(newStores: List<Store>) {
        _stores.value = newStores
    }

    fun setIngredients(newIngredients: List<Ingredient>) {
        _ingredients.value = newIngredients
    }

    fun setRecipeIngredients(newRi: List<RecipeIngredient>) {
        _recipeIngredients.value = newRi
    }
}
