package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.shoppinglist.ShoppingListItem
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.StateFlow

interface PantryRepository {
  val pantryItems: StateFlow<List<PantryItem>>

  suspend fun updateQuantity(
      ingredientId: Uuid,
      quantity: Double,
      unitId: Uuid,
  )

  suspend fun setPantryItems(items: List<PantryItem>)

  suspend fun remove(ingredientId: Uuid, unitId: Uuid)

  suspend fun removeBatch(batchId: Uuid)
}

interface LeftoverRepository {
  val leftovers: StateFlow<List<LeftoverItem>>

  suspend fun addLeftover(
      recipeId: Uuid,
      servings: Double,
      dateAdded: String,
      expirationDate: String? = null,
  )

  suspend fun removeLeftover(id: Uuid)

  suspend fun consumeLeftover(id: Uuid, servings: Double)
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
