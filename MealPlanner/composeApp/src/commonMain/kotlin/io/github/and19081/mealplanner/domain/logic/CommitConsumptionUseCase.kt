package io.github.and19081.mealplanner.domain.logic

import io.github.and19081.mealplanner.core.util.SystemUnits
import io.github.and19081.mealplanner.core.util.UnitConverter
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.core.util.UnitType
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.Ingredient
import io.github.and19081.mealplanner.domain.model.Leftover
import io.github.and19081.mealplanner.domain.model.LeftoverInfo
import io.github.and19081.mealplanner.domain.model.Meal
import io.github.and19081.mealplanner.domain.model.MealSource
import io.github.and19081.mealplanner.domain.model.Recipe
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.repository.MealPlanRepository
import io.github.and19081.mealplanner.domain.repository.PantryRepository
import io.github.and19081.mealplanner.domain.repository.ReceiptHistoryRepository
import io.github.and19081.mealplanner.feature.kitchen.ConsumptionResult
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptHistory
import kotlin.math.max
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

class CommitConsumptionUseCase(
    private val mealPlanRepository: MealPlanRepository,
    private val foodItemRepository: FoodItemRepository,
    private val pantryRepository: PantryRepository,
    private val receiptHistoryRepository: ReceiptHistoryRepository,
    private val unitRepository: UnitRepository,
) {
  suspend operator fun invoke(result: ConsumptionResult) {
    when (result) {
      is ConsumptionResult.HomeMealConsumed -> {
        val meal = mealPlanRepository.getMealById(result.scheduledMealId) ?: return
        val foodItemId = meal.prePlannedMealId ?: return
        val foodItem = foodItemRepository.getFoodItem(foodItemId) ?: return
        deductRecipeIngredients(foodItem, meal.peopleCount.toDouble())
        if (result.leftoverServings > 0) addLeftover(foodItem, result.leftoverServings)
        mealPlanRepository.setConsumedStatus(result.scheduledMealId, true)
      }

      is ConsumptionResult.HomeRecipeConsumed -> {
        val meal = mealPlanRepository.getMealById(result.scheduledMealId) ?: return
        val foodItemId = meal.prePlannedMealId ?: return
        val foodItem = foodItemRepository.getFoodItem(foodItemId) ?: return
        deductRecipeIngredients(foodItem, meal.peopleCount.toDouble())
        if (result.leftoverServings > 0) addLeftover(foodItem, result.leftoverServings)
        mealPlanRepository.setConsumedStatus(result.scheduledMealId, true)
      }

      is ConsumptionResult.HomeIngredientConsumed -> {
        val meal = mealPlanRepository.getMealById(result.scheduledMealId) ?: return
        val src = meal.source as? MealSource.StandaloneIngredient ?: return
        val allUnits = unitRepository.units.value
        val unit =
            allUnits.find { it.id == src.unitId }
                ?: allUnits.find { it.type == UnitType.Count && it.factorToBase == 1.0 }
                ?: return
        val current = pantryRepository.getPantryItemByFoodItemId(src.id)
        val currentQty = current?.measurement?.quantity ?: 0.0
        val currentUnitId = current?.measurement?.unitId ?: unit.id
        val convertedDeduct =
            UnitConverter.convert(
                src.quantity,
                unit.id,
                currentUnitId,
                allUnits.associateBy { it.id },
            ) ?: 0.0
        val newQty = max(0.0, currentQty - convertedDeduct)
        pantryRepository.updateQuantity(src.id, newQty, currentUnitId)
        mealPlanRepository.setConsumedStatus(result.scheduledMealId, true)
      }

      is ConsumptionResult.RestaurantMealConsumed -> {
        saveRestaurantReceipt(result)
        if (result.leftoverServings > 0) {
          val name = result.leftoverDescription ?: "Restaurant Meal"
          val leftover =
              Leftover(
                  name = "$name (Leftover)",
                  leftoverInfo =
                      LeftoverInfo(
                          remainingServings = result.leftoverServings,
                          dateAdded =
                              Clock.System.todayIn(TimeZone.currentSystemDefault()).toString(),
                      ),
              )
          foodItemRepository.saveFoodItem(leftover)
        }
        mealPlanRepository.setConsumedStatus(result.scheduledMealId, true)
      }
    }
  }

  private suspend fun deductRecipeIngredients(item: FoodItem, servingsNeeded: Double) {
    val recipeInfo =
        when (item) {
          is Recipe -> item.recipeInfo
          is Meal -> item.recipeInfo
          else -> return
        }
    val allUnits = unitRepository.units.value

    suspend fun deductRecursive(fi: FoodItem, multiplier: Double) {
      val info =
          when (fi) {
            is Recipe -> fi.recipeInfo
            is Meal -> fi.recipeInfo
            else -> return
          }
      info.requirementGroups.forEach { group ->
        val primary =
            group.requirements.find { it.isPrimary }
                ?: group.requirements.firstOrNull()
                ?: return@forEach
        val subItem =
            foodItemRepository.getFoodItem(primary.measurement.foodItemId ?: return@forEach)
                ?: return@forEach
        when (subItem) {
          is Recipe,
          is Meal -> {
            val subInfo =
                when (subItem) {
                  is Recipe -> subItem.recipeInfo
                  is Meal -> subItem.recipeInfo
                  else -> return@forEach
                }
            val scale =
                if (subInfo.servings > 0) primary.measurement.quantity / subInfo.servings else 1.0
            deductRecursive(subItem, multiplier * scale)
          }

          is Ingredient -> {
            val targetUnitId =
                subItem.preferredUnitId
                    ?: when (allUnits.find { it.id == primary.measurement.unitId }?.type) {
                      UnitType.Mass -> SystemUnits.Gram.id
                      UnitType.Volume -> SystemUnits.Ml.id
                      else -> SystemUnits.Each.id
                    }
            val bridges = foodItemRepository.getConversionsForFoodItem(subItem.id)
            val deductAmt =
                UnitConverter.convert(
                    primary.measurement.quantity * multiplier,
                    primary.measurement.unitId ?: Uuid.NIL,
                    targetUnitId,
                    allUnits.associateBy { it.id },
                    bridges,
                ) ?: 0.0
            val current = pantryRepository.getPantryItemByFoodItemId(subItem.id)
            val currentQty =
                if (current != null) {
                  UnitConverter.convert(
                      current.measurement.quantity,
                      current.measurement.unitId ?: Uuid.NIL,
                      targetUnitId,
                      allUnits.associateBy { it.id },
                      bridges,
                  ) ?: 0.0
                } else 0.0
            val newQty = max(0.0, currentQty - deductAmt)
            pantryRepository.updateQuantity(subItem.id, newQty, targetUnitId)
          }

          else -> Unit
        }
      }
    }

    val batchMultiplier =
        servingsNeeded / (if (recipeInfo.servings > 0) recipeInfo.servings else 1.0)
    deductRecursive(item, batchMultiplier)
  }

  private suspend fun addLeftover(source: FoodItem, servings: Double) {
    val leftover =
        Leftover(
            name = "${source.name} (Leftover)",
            leftoverInfo =
                LeftoverInfo(
                    remainingServings = servings,
                    dateAdded = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString(),
                ),
        )
    foodItemRepository.saveFoodItem(leftover)
  }

  private suspend fun saveRestaurantReceipt(result: ConsumptionResult.RestaurantMealConsumed) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    val receiptId = Uuid.random()
    receiptHistoryRepository.addTrip(
        ReceiptHistory(
            id = receiptId,
            date = today,
            time = now,
            restaurantId =
                (mealPlanRepository.getMealById(result.scheduledMealId)?.source
                        as? MealSource.Restaurant)
                    ?.restaurantId,
            projectedTotalCents = 0,
            actualTotalCents = result.actualCostCents,
            taxPaidCents = 0,
        )
    )
  }
}
