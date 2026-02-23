package io.github.and19081.mealplanner.shoppinglist

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class ShoppingListOverride(
    val ingredientId: Uuid,
    val forceStoreId: Uuid? = null, // If set, force this store.
    val inPantry: Boolean = false
)

@OptIn(ExperimentalUuidApi::class)
class ShoppingListRepository {
    // Key: Ingredient ID
    private val _overrides = MutableStateFlow<Map<Uuid, ShoppingListOverride>>(emptyMap())
    val overrides = _overrides

    fun setStoreOverride(ingredientId: Uuid, storeId: Uuid) {
        val current = _overrides.value.toMutableMap()
        current[ingredientId] = ShoppingListOverride(ingredientId, forceStoreId = storeId, inPantry = false)
        _overrides.value = current
    }

    fun markInPantry(ingredientId: Uuid) {
        val current = _overrides.value.toMutableMap()
        current[ingredientId] = ShoppingListOverride(ingredientId, inPantry = true)
        _overrides.value = current
    }

    fun clearOverride(ingredientId: Uuid) {
        val current = _overrides.value.toMutableMap()
        current.remove(ingredientId)
        _overrides.value = current
    }
}