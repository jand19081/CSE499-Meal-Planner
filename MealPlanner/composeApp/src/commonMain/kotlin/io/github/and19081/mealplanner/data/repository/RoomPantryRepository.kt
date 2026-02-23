package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.PantryItem
import io.github.and19081.mealplanner.PantryRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.PantryInventoryEntity
import io.github.and19081.mealplanner.data.db.relation.PantryInventoryWithDetails
import io.github.and19081.mealplanner.data.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.uuid.Uuid

class RoomPantryRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope
) : PantryRepository {

    private val pantryDao = db.pantryDao()

    private val _pantryItemsState = pantryDao.observeAllWithDetails()
        .map { list: List<PantryInventoryWithDetails> -> list.map { it.toModel() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val pantryItems: StateFlow<List<PantryItem>> = _pantryItemsState

    override suspend fun updateQuantity(ingredientId: Uuid, quantity: Double, unitId: Uuid) {
        if (quantity <= 0) {
            pantryDao.remove(ingredientId.toString(), unitId.toString())
        } else {
            pantryDao.upsert(PantryInventoryEntity(ingredientId, unitId, quantity))
        }
    }

    override suspend fun setPantryItems(items: List<PantryItem>) {
        items.forEach { updateQuantity(it.ingredientId, it.quantity, it.unitId) }
    }
}
