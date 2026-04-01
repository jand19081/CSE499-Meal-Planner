package io.github.and19081.mealplanner.domain.logic

import io.github.and19081.mealplanner.core.util.UnitConverter
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.domain.repository.PantryRepository
import kotlin.math.max
import kotlin.uuid.Uuid

class ConsumePantryItemUseCase(
    private val pantryRepository: PantryRepository,
    private val unitRepository: UnitRepository,
) {
  suspend operator fun invoke(
      pantryItemId: Uuid,
      foodItemId: Uuid,
      amountConsumed: Double,
      unitId: Uuid,
  ) {
    val current = pantryRepository.getPantryItemById(pantryItemId) ?: return
    val allUnits = unitRepository.units.value.associateBy { it.id }
    val storedUnitId = current.measurement.unitId ?: unitId
    val convertedAmount =
        UnitConverter.convert(
            amountConsumed,
            unitId,
            storedUnitId,
            allUnits,
        ) ?: amountConsumed
    val newQty = max(0.0, current.measurement.quantity - convertedAmount)
    if (newQty <= 0.0) {
      pantryRepository.removeBatch(pantryItemId)
    } else {
      pantryRepository.updateQuantity(foodItemId, newQty, storedUnitId)
    }
  }
}
