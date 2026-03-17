package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.PantryItem
import io.github.and19081.mealplanner.PantryRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.PantryInventoryEntity
import io.github.and19081.mealplanner.data.db.relation.PantryInventoryWithDetails
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomPantryRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : PantryRepository {
  private val dao = db.pantryDao()

  override val pantryItems: StateFlow<List<PantryItem>> =
      dao.observeAllWithDetails()
          .map { list -> list.map { it.toDomain() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun updateQuantity(foodItemId: Uuid, quantity: Double, unitId: Uuid) {
    val existing = dao.observeAllWithDetails().first().find { it.pantryItem.foodItemId == foodItemId }
    if (existing != null) {
      if (quantity <= 0) {
        dao.delete(existing.pantryItem)
      } else {
        dao.upsert(existing.pantryItem.copy(quantity = quantity, unitId = unitId))
      }
    } else if (quantity > 0) {
      dao.upsert(
          PantryInventoryEntity(
              foodItemId = foodItemId,
              quantity = quantity,
              unitId = unitId,
          )
      )
    }
  }

  override suspend fun remove(foodItemId: Uuid, unitId: Uuid) {
      val existing = dao.observeAllWithDetails().first().find { it.pantryItem.foodItemId == foodItemId && it.pantryItem.unitId == unitId }
      if (existing != null) {
          dao.delete(existing.pantryItem)
      }
  }

  override suspend fun removeBatch(batchId: Uuid) {
    val existing = dao.observeAllWithDetails().first().find { it.pantryItem.id == batchId }
    if (existing != null) {
      dao.delete(existing.pantryItem)
    }
  }

  override suspend fun setPantryItems(newItems: List<PantryItem>) {
    newItems.forEach { updateQuantity(it.foodItemId, it.quantity, it.unitId) }
  }

  private fun PantryInventoryWithDetails.toDomain(): PantryItem =
      PantryItem(
          id = pantryItem.id,
          foodItemId = pantryItem.foodItemId,
          quantity = pantryItem.quantity,
          unitId = pantryItem.unitId,
      )
}
