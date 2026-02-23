package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.ShoppingListItemRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.ShoppingCartItemEntity
import io.github.and19081.mealplanner.data.db.relation.ShoppingCartItemWithDetails
import io.github.and19081.mealplanner.data.toModel
import io.github.and19081.mealplanner.shoppinglist.ShoppingListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.uuid.Uuid

class RoomShoppingListItemRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope
) : ShoppingListItemRepository {

    private val shoppingDao = db.shoppingListDao()

    override val items: StateFlow<List<ShoppingListItem>> = shoppingDao.observeAllWithDetails()
        .map { list: List<ShoppingCartItemWithDetails> -> list.map { it.toModel() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override suspend fun addItem(item: ShoppingListItem) {
        shoppingDao.upsert(ShoppingCartItemEntity(
            id = item.id,
            ingredientId = item.ingredientId,
            customShoppingItemId = null,
            storeId = item.storeId,
            unitId = item.unitId ?: Uuid.NIL,
            packageOptionId = item.packageId,
            customName = item.customName,
            neededQuantity = item.neededQuantity ?: 1.0,
            isPurchased = item.isPurchased
        ))
    }

    override suspend fun removeItem(id: Uuid) {
        shoppingDao.deleteById(id.toString())
    }

    override suspend fun toggleItem(id: Uuid) {
        val item = items.value.find { it.id == id }
        if (item != null) {
            shoppingDao.setPurchased(id.toString(), !item.isPurchased)
        }
    }
}
