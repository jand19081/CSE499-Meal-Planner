package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.ShoppingListItemRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.ShoppingCartItemEntity
import io.github.and19081.mealplanner.data.db.relation.ShoppingCartItemWithDetails
import io.github.and19081.mealplanner.data.toModel
import io.github.and19081.mealplanner.shoppinglist.ShoppingListItem
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomShoppingListItemRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : ShoppingListItemRepository {

  private val shoppingDao = db.shoppingListDao()

  override val items: StateFlow<List<ShoppingListItem>> =
      shoppingDao
          .observeAllWithDetails()
          .map { list: List<ShoppingCartItemWithDetails> -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun addItem(item: ShoppingListItem) {
    val anyStoreId = Uuid.parse("00000000-0000-0000-0000-000000000000")
    shoppingDao.upsert(
        ShoppingCartItemEntity(
            id = item.id,
            ingredientId = item.ingredientId,
            customShoppingItemId = null,
            storeId = if (item.storeId == anyStoreId) null else item.storeId,
            unitId = item.unitId ?: Uuid.NIL,
            packageOptionId = item.packageId,
            customName = item.customName,
            neededQuantity = item.neededQuantity ?: 1.0,
            isPurchased = item.isPurchased,
            isPantryItem = item.isPantryItem,
        )
    )
  }

  override suspend fun removeItem(id: Uuid) {
    shoppingDao.deleteById(id)
  }

  override suspend fun toggleItem(id: Uuid) {
    val item = items.value.find { it.id == id }
    if (item != null) {
      shoppingDao.setPurchased(id, !item.isPurchased)
    }
  }
}
