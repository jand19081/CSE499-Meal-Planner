package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.core.util.UnitConverter
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.core.util.toDomain
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.relation.PantryInventoryWithDetails
import io.github.and19081.mealplanner.domain.repository.PantryRepository
import io.github.and19081.mealplanner.domain.repository.PantryUpdate
import io.github.and19081.mealplanner.feature.meals.PantryItem
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomPantryRepository(
    private val db: MealPlannerDatabase,
    private val unitRepository: UnitRepository,
    private val scope: CoroutineScope,
) : PantryRepository {
  private val dao = db.pantryDao()

  override val pantryItems: StateFlow<List<PantryItem>> =
      dao.observeAllWithDetails()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun getPantryItemById(id: Uuid): PantryItem? {
    return dao.getById(id)?.let { PantryItem(id = it.id, measurement = it.measurement.toDomain()) }
  }

  override suspend fun getPantryItemByFoodItemId(foodItemId: Uuid): PantryItem? {
    return dao.getByFoodItemId(foodItemId)?.let {
      PantryItem(id = it.id, measurement = it.measurement.toDomain())
    }
  }

  override suspend fun updateQuantity(foodItemId: Uuid, quantity: Double, unitId: Uuid) {
    updateQuantities(listOf(PantryUpdate(foodItemId, quantity, unitId)))
  }

  override suspend fun updateQuantities(updates: List<PantryUpdate>) {
    val allUnits = unitRepository.units.value.associateBy { it.id }
    val normalizedUpdates = updates.map { update ->
        val fromUnit = allUnits[update.unitId]
        if (fromUnit != null) {
            val (baseQty, baseUnit) = UnitConverter.toStandard(update.newQuantity, fromUnit, allUnits)
            PantryUpdate(update.foodItemId, baseQty, baseUnit?.id ?: update.unitId)
        } else {
            update
        }
    }
    dao.updateQuantities(normalizedUpdates)
  }

  override suspend fun remove(foodItemId: Uuid, unitId: Uuid) {
    dao.deleteByFoodItemAndUnit(foodItemId, unitId)
  }

  override suspend fun removeBatch(batchId: Uuid) {
    dao.deleteById(batchId)
  }

  override suspend fun setPantryItems(newItems: List<PantryItem>) {
    updateQuantities(
        newItems.map {
          PantryUpdate(
              it.measurement.foodItemId ?: Uuid.NIL,
              it.measurement.quantity,
              it.measurement.unitId ?: Uuid.NIL,
          )
        }
    )
  }

  private fun PantryInventoryWithDetails.toModel(): PantryItem =
      PantryItem(
          id = pantryItem.id,
          measurement = pantryItem.measurement.toDomain(),
      )
}
