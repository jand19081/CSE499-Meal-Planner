package io.github.and19081.mealplanner.ingredients

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

object IngredientRepository {
    private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredients = _ingredients.asStateFlow()

    fun addIngredient(ingredient: Ingredient) {
        _ingredients.update { it + ingredient }
    }

    fun updateIngredient(ingredient: Ingredient) {
        _ingredients.update { list -> list.map { if (it.id == ingredient.id) ingredient else it } }
    }

    fun removeIngredient(id: Uuid) {
        _ingredients.update { list -> list.filter { it.id != id } }
    }

    fun setIngredients(newIngredients: List<Ingredient>) {
        _ingredients.value = newIngredients
    }

    fun removePurchaseOptionsForStore(storeId: Uuid) {
        _ingredients.update { list ->
            list.map { ing ->
                ing.copy(purchaseOptions = ing.purchaseOptions.filter { it.storeId != storeId })
            }
        }
    }
}
