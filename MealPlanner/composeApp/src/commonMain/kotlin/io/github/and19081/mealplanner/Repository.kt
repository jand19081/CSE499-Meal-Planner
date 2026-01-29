package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.shoppinglist.ShoppingTrip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

object PantryRepository {
    private val _pantryItems = MutableStateFlow<List<PantryItem>>(emptyList())
    val pantryItems = _pantryItems.asStateFlow()

    fun updateQuantity(ingredientId: Uuid, quantity: Measure) {
         _pantryItems.update { current ->
             val list = current.toMutableList()
             list.removeAll { it.ingredientId == ingredientId }
             if (quantity.amount > 0) {
                 list.add(PantryItem(ingredientId, quantity))
             }
             list
         }
    }
}

object CustomItemRepository {
    private val _items = MutableStateFlow<List<CustomShoppingItem>>(emptyList())
    val items = _items.asStateFlow()

    fun addItem(item: CustomShoppingItem) {
        _items.update { it + item }
    }
    
    fun removeItem(id: Uuid) {
         _items.update { list -> list.filter { it.id != id } }
    }
    
    fun toggleItem(id: Uuid) {
        _items.update { list -> list.map { if(it.id == id) it.copy(isChecked = !it.isChecked) else it } }
    }
}

object ShoppingSessionRepository {
    // Set of IDs (Ingredient ID or Custom Item ID) that are currently in the cart
    private val _inCartItems = MutableStateFlow<Set<Uuid>>(emptySet())
    val inCartItems = _inCartItems.asStateFlow()

    fun toggleCartStatus(id: Uuid) {
        _inCartItems.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun removeItemsFromCart(ids: Collection<Uuid>) {
        _inCartItems.update { current -> current - ids.toSet() }
    }

    fun clearCart() {
        _inCartItems.value = emptySet()
    }
}

object ShoppingHistoryRepository {
    private val _trips = MutableStateFlow<List<ShoppingTrip>>(emptyList())
    val trips = _trips.asStateFlow()

    fun addTrip(trip: ShoppingTrip) {
        _trips.update { it + trip }
    }
}