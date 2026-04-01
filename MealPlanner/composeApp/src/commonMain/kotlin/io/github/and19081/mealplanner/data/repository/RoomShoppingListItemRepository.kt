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
import kotlinx.coroutines.flow.*

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
            packageOptionId = item.packageId,
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
    val existing = items.value.find { it.id == id }
    if (existing != null) {
      dao.setPurchased(id, !existing.isPurchased)
    }
  }

  override suspend fun removeItem(id: Uuid) {
    val existing = dao.observeAllWithDetails().first().find { it.cartItem.id == id }
    if (existing != null) {
      dao.delete(existing.cartItem)
    }
  }

  private fun ShoppingCartItemWithDetails.toModel(): ShoppingListItem =
      ShoppingListItem(
          id = cartItem.id,
          customName = cartItem.customName,
          storeId = cartItem.storeId ?: Uuid.parse("00000000-0000-0000-0000-000000000000"),
          measurement = cartItem.measurement.toDomain(),
          packageId = cartItem.packageOptionId,
          isPurchased = cartItem.isPurchased,
          isPantryItem = cartItem.isPantryItem,
      )
}
