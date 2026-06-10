package io.github.and19081.mealplanner.domain.repository

import io.github.and19081.mealplanner.feature.meals.LeftoverItem
import io.github.and19081.mealplanner.feature.meals.PantryItem
import io.github.and19081.mealplanner.feature.meals.Restaurant
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.feature.shoppinglist.ShoppingListItem
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PantryRepository {
  val pantryItems: StateFlow<List<PantryItem>>

  suspend fun getPantryItemById(id: Uuid): PantryItem?

  suspend fun getPantryItemByFoodItemId(foodItemId: Uuid): PantryItem?

  suspend fun updateQuantity(
      foodItemId: Uuid,
      quantity: Double,
      unitId: Uuid,
  )

  suspend fun updateQuantities(updates: List<PantryUpdate>)

  suspend fun setPantryItems(items: List<PantryItem>)

  suspend fun remove(foodItemId: Uuid, unitId: Uuid)

  suspend fun removeBatch(batchId: Uuid)
}

data class PantryUpdate(val foodItemId: Uuid, val newQuantity: Double, val unitId: Uuid)

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

  fun observeMonthlyExpenditures(): Flow<List<io.github.and19081.mealplanner.data.db.relation.MonthlyExpenditure>>

  suspend fun getTripWithLineItems(id: Uuid): ReceiptHistory?

  suspend fun addTrip(trip: ReceiptHistory)

  suspend fun updateTrip(trip: ReceiptHistory)

  suspend fun removeTrip(id: Uuid)

  suspend fun setTrips(history: List<ReceiptHistory>)
}

interface RestaurantRepository {
  val restaurants: StateFlow<List<Restaurant>>

  suspend fun addRestaurant(restaurant: Restaurant)

  suspend fun updateRestaurant(restaurant: Restaurant)

  suspend fun deleteRestaurant(id: Uuid)

  suspend fun setRestaurants(restaurants: List<Restaurant>)
}
