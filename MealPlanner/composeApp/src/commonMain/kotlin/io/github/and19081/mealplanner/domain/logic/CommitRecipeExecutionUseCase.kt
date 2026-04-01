package io.github.and19081.mealplanner.domain.logic

import io.github.and19081.mealplanner.core.util.SystemUnits
import io.github.and19081.mealplanner.core.util.UnitConverter
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.domain.model.Ingredient
import io.github.and19081.mealplanner.domain.model.Leftover
import io.github.and19081.mealplanner.domain.model.LeftoverInfo
import io.github.and19081.mealplanner.domain.model.Recipe
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.repository.PantryRepository
import kotlin.math.max
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class CommitRecipeExecutionUseCase(
    private val foodItemRepository: FoodItemRepository,
    private val pantryRepository: PantryRepository,
    private val unitRepository: UnitRepository,
) {
  suspend operator fun invoke(
      recipe: Recipe,
      scaleFactor: Double,
      yieldServings: Double,
  ) {
    deductIngredients(recipe, scaleFactor)
    addToLeftovers(recipe, yieldServings)
  }

  private suspend fun deductIngredients(recipe: Recipe, scaleFactor: Double) {
    val allUnits = unitRepository.units.value.associateBy { it.id }

    recipe.recipeInfo.requirementGroups.forEach { group ->
      val req =
          group.requirements.find { it.isPrimary }
              ?: group.requirements.firstOrNull()
              ?: return@forEach
      val subItem =
          foodItemRepository.getFoodItem(req.measurement.foodItemId ?: return@forEach)
              ?: return@forEach
      if (subItem !is Ingredient) return@forEach

      val targetUnitId = subItem.preferredUnitId ?: SystemUnits.Each.id
      val bridges = foodItemRepository.getConversionsForFoodItem(subItem.id)
      val deductAmt =
          UnitConverter.convert(
              req.measurement.quantity * scaleFactor,
              req.measurement.unitId ?: Uuid.NIL,
              targetUnitId,
              allUnits,
              bridges,
          ) ?: 0.0

      val current = pantryRepository.getPantryItemByFoodItemId(subItem.id)
      val currentQty =
          if (current != null) {
            UnitConverter.convert(
                current.measurement.quantity,
                current.measurement.unitId ?: Uuid.NIL,
                targetUnitId,
                allUnits,
                bridges,
            ) ?: 0.0
          } else 0.0

      pantryRepository.updateQuantity(subItem.id, max(0.0, currentQty - deductAmt), targetUnitId)
    }
  }

  private suspend fun addToLeftovers(recipe: Recipe, yieldServings: Double) {
    val leftover =
        Leftover(
            name = "${recipe.name} (Leftover)",
            leftoverInfo =
                LeftoverInfo(
                    remainingServings = yieldServings,
                    dateAdded = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString(),
                ),
        )
    foodItemRepository.saveFoodItem(leftover)
  }
}
