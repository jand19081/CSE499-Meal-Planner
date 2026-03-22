package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.feature.meals.PantryItem
import io.github.and19081.mealplanner.domain.repository.PantryRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.PantryInventoryEntity
import io.github.and19081.mealplanner.data.db.entity.ItemMeasurement as EntityMeasurement
import io.github.and19081.mealplanner.data.db.relation.PantryInventoryWithDetails
import io.github.and19081.mealplanner.core.util.toDomain
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

import io.github.and19081.mealplanner.domain.repository.PantryUpdate

class RoomPantryRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : PantryRepository {
  private val dao = db.pantryDao()

  override val pantryItems: StateFlow<List<PantryItem>> =
      dao.observeAllWithDetails()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun updateQuantity(foodItemId: Uuid, quantity: Double, unitId: Uuid) {
    updateQuantities(listOf(PantryUpdate(foodItemId, quantity, unitId)))
  }

  override suspend fun updateQuantities(updates: List<PantryUpdate>) {
    dao.updateQuantities(updates)
  }

  override suspend fun remove(foodItemId: Uuid, unitId: Uuid) {
      val existing = dao.observeAllWithDetails().first().find { 
          it.pantryItem.measurement.foodItemId == foodItemId && 
          it.pantryItem.measurement.unitId == unitId 
      }
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
    updateQuantities(newItems.map { PantryUpdate(it.measurement.foodItemId ?: Uuid.NIL, it.measurement.quantity, it.measurement.unitId ?: Uuid.NIL) })
  }

  private fun PantryInventoryWithDetails.toModel(): PantryItem =
      PantryItem(
          id = pantryItem.id,
          measurement = pantryItem.measurement.toDomain(),
      )
}
