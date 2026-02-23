package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.shoppinglist.ShoppingListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

interface PantryRepository {
    val pantryItems: StateFlow<List<PantryItem>>
    suspend fun updateQuantity(ingredientId: Uuid, quantity: Double, unitId: Uuid)
    suspend fun setPantryItems(items: List<PantryItem>)
}

interface ShoppingListItemRepository {
    val items: StateFlow<List<ShoppingListItem>>
    suspend fun addItem(item: ShoppingListItem)
    suspend fun removeItem(id: Uuid)
    suspend fun toggleItem(id: Uuid)
}

interface ReceiptHistoryRepository {
    val trips: StateFlow<List<ReceiptHistory>>
    suspend fun getTripWithLineItems(id: Uuid): ReceiptHistory?
    suspend fun addTrip(trip: ReceiptHistory)
    suspend fun updateTrip(trip: ReceiptHistory)
    suspend fun removeTrip(id: Uuid)
    suspend fun setTrips(history: List<ReceiptHistory>)
}
