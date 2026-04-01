package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.core.util.toDomain
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.ItemMeasurement as EntityMeasurement
import io.github.and19081.mealplanner.data.db.entity.ShoppingCartItemEntity
import io.github.and19081.mealplanner.data.db.relation.ShoppingCartItemWithDetails
import io.github.and19081.mealplanner.domain.repository.ShoppingListItemRepository
import io.github.and19081.mealplanner.feature.shoppinglist.ShoppingListItem
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RoomShoppingListItemRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : ShoppingListItemRepository {
  private val dao = db.shoppingListDao()

  override val items: StateFlow<List<ShoppingListItem>> =
      dao.observeAllWithDetails()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun addItem(item: ShoppingListItem) {
    dao.upsert(
        ShoppingCartItemEntity(
            id = item.id,
            storeId = if (item.storeId == Uuid.NIL) null else item.storeId,
            purchaseOptionId = item.purchaseOptionId,
            customName = item.customName,
            measurement =
                EntityMeasurement(
                    foodItemId = item.measurement.foodItemId,
                    unitId = item.measurement.unitId,
                    quantity = item.measurement.quantity,
                ),
            isPurchased = item.isPurchased,
            isPantryItem = item.isPantryItem,
        )
    )
  }

  override suspend fun toggleItem(id: Uuid) {
    val existing = dao.getById(id)
    if (existing != null) {
      dao.setPurchased(id, !existing.isPurchased)
    }
  }

  override suspend fun removeItem(id: Uuid) {
    dao.deleteById(id)
  }

  private fun ShoppingCartItemWithDetails.toModel(): ShoppingListItem =
      ShoppingListItem(
          id = cartItem.id,
          customName = cartItem.customName,
          storeId = cartItem.storeId ?: Uuid.NIL,
          measurement = cartItem.measurement.toDomain(),
          purchaseOptionId = cartItem.purchaseOptionId,
          isPurchased = cartItem.isPurchased,
          isPantryItem = cartItem.isPantryItem,
      )
}
