package io.github.and19081.mealplanner.shoppinglist

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalUuidApi::class)
data class ShoppingListOverride(
    val foodItemId: Uuid,
    val forceStoreId: Uuid? = null, // If set, force this store.
    val inPantry: Boolean = false,
)

@OptIn(ExperimentalUuidApi::class)
class ShoppingListRepository {
  // Key: Ingredient ID
  private val _overrides = MutableStateFlow<Map<Uuid, ShoppingListOverride>>(emptyMap())
  val overrides = _overrides

  fun setStoreOverride(foodItemId: Uuid, storeId: Uuid) {
    val current = _overrides.value.toMutableMap()
    current[foodItemId] =
        ShoppingListOverride(foodItemId, forceStoreId = storeId, inPantry = false)
    _overrides.value = current
  }

  fun markInPantry(foodItemId: Uuid) {
    val current = _overrides.value.toMutableMap()
    current[foodItemId] = ShoppingListOverride(foodItemId, inPantry = true)
    _overrides.value = current
  }

  fun clearOverride(foodItemId: Uuid) {
    val current = _overrides.value.toMutableMap()
    current.remove(foodItemId)
    _overrides.value = current
  }
}
