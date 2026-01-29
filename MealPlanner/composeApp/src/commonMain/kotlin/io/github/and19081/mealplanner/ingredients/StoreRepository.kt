package io.github.and19081.mealplanner.ingredients

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

object StoreRepository {
    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores = _stores.asStateFlow()

    fun addStore(store: Store) {
        _stores.update { it + store }
    }

    fun deleteStore(storeId: Uuid) {
        _stores.update { list -> list.filter { it.id != storeId } }
        // Note: Logic to cascade delete purchase options should be handled by the caller or 
        // IngredientRepository should listen to store changes. 
        // For now, we will handle the cascade in IngredientRepository or a centralized cleanup.
    }

    fun setStores(newStores: List<Store>) {
        _stores.value = newStores
    }
}
