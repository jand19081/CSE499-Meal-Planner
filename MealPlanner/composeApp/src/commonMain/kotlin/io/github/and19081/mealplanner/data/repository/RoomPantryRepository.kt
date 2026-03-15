package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.PantryItem
import io.github.and19081.mealplanner.PantryRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.PantryInventoryEntity
import io.github.and19081.mealplanner.data.db.relation.PantryInventoryWithDetails
import io.github.and19081.mealplanner.data.toModel
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomPantryRepository(private val db: MealPlannerDatabase, private val scope: CoroutineScope) :
    PantryRepository {

  private val pantryDao = db.pantryDao()

  private val _pantryItemsState =
      pantryDao
          .observeAllWithDetails()
          .map { list: List<PantryInventoryWithDetails> -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override val pantryItems: StateFlow<List<PantryItem>> = _pantryItemsState

  override suspend fun updateQuantity(
      ingredientId: Uuid,
      quantity: Double,
      unitId: Uuid,
  ) {
    if (quantity <= 0) {
      pantryDao.remove(ingredientId, unitId)
    } else {
      val existing = pantryDao.getForIngredient(ingredientId).find { it.unitId == unitId }
      pantryDao.upsert(
          PantryInventoryEntity(
              id = existing?.id ?: Uuid.random(),
              ingredientId = ingredientId,
              unitId = unitId,
              quantity = quantity,
          )
      )
    }
  }

  override suspend fun setPantryItems(items: List<PantryItem>) {
    items.forEach { updateQuantity(it.ingredientId, it.quantity, it.unitId) }
  }

  override suspend fun remove(ingredientId: Uuid, unitId: Uuid) {
    pantryDao.remove(ingredientId, unitId)
  }

  override suspend fun removeBatch(batchId: Uuid) {
    val batch = pantryDao.observeAll().first().find { it.id == batchId }
    if (batch != null) pantryDao.delete(batch)
  }
}
