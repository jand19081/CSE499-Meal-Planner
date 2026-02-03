package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.shoppinglist.ShoppingListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

object PantryRepository {
    private val _pantryItems = MutableStateFlow<List<PantryItem>>(emptyList())
    val pantryItems = _pantryItems.asStateFlow()

    fun updateQuantity(ingredientId: Uuid, quantity: Double, unitId: Uuid) {
         _pantryItems.update { current ->
             val list = current.toMutableList()
             list.removeAll { it.ingredientId == ingredientId }
             if (quantity > 0) {
                 list.add(PantryItem(ingredientId = ingredientId, quantity = quantity, unitId = unitId))
             }
             list
         }
    }
    
    fun setPantryItems(items: List<PantryItem>) {
        _pantryItems.value = items
    }
}

object ShoppingListItemRepository {
    private val _items = MutableStateFlow<List<ShoppingListItem>>(emptyList())
    val items = _items.asStateFlow()

    fun addItem(item: ShoppingListItem) {
        _items.update { it + item }
    }
    
    fun removeItem(id: Uuid) {
         _items.update { list -> list.filter { it.id != id } }
    }
    
    fun toggleItem(id: Uuid) {
        _items.update { list -> list.map { if(it.id == id) it.copy(isPurchased = !it.isPurchased) else it } }
    }
}

object ShoppingSessionRepository {
    // Set of IDs (Shopping List Item IDs or Ingredient IDs if legacy logic persists, but likely ShoppingListItem IDs now)
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

object ReceiptHistoryRepository {
    private val _trips = MutableStateFlow<List<ReceiptHistory>>(emptyList())
    val trips = _trips.asStateFlow()

    fun addTrip(trip: ReceiptHistory) {
        _trips.update { it + trip }
    }
    
    fun setTrips(history: List<ReceiptHistory>) {
        _trips.value = history
    }
}