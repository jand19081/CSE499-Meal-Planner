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
import io.github.and19081.mealplanner.domain.repository.PantryUpdate
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
    mealPlanRepository.withTransaction {
      executeConsumption(result)
    }
  }

  private suspend fun executeConsumption(result: ConsumptionResult) {
    when (result) {
      is ConsumptionResult.HomeMealConsumed -> {
        val meal = mealPlanRepository.getMealById(result.scheduledMealId) ?: return
        val foodItemId = meal.prePlannedMealId ?: return
        val foodItem = foodItemRepository.getFoodItem(foodItemId) ?: return
        val deductions = calculateDeductions(foodItem, meal.peopleCount.toDouble())
        pantryRepository.updateQuantities(deductions)
        if (result.leftoverServings > 0) addLeftover(foodItem, result.leftoverServings)
        mealPlanRepository.setConsumedStatus(result.scheduledMealId, true)
      }

      is ConsumptionResult.HomeRecipeConsumed -> {
        val meal = mealPlanRepository.getMealById(result.scheduledMealId) ?: return
        val foodItemId = meal.prePlannedMealId ?: return
        val foodItem = foodItemRepository.getFoodItem(foodItemId) ?: return
        val deductions = calculateDeductions(foodItem, meal.peopleCount.toDouble())
        pantryRepository.updateQuantities(deductions)
        if (result.leftoverServings > 0) addLeftover(foodItem, result.leftoverServings)
        mealPlanRepository.setConsumedStatus(result.scheduledMealId, true)
      }

      is ConsumptionResult.HomeIngredientConsumed -> {
        val meal = mealPlanRepository.getMealById(result.scheduledMealId) ?: return
        val src = meal.source as? MealSource.StandaloneIngredient ?: return
        
        val current = pantryRepository.getPantryItemByFoodItemId(src.id)
        val currentQty = current?.measurement?.quantity ?: 0.0
        val currentUnitId = current?.measurement?.unitId ?: src.unitId
        
        // Both quantities are now expected to be in base units.
        val newQty = max(0.0, currentQty - src.quantity)
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

  private suspend fun calculateDeductions(item: FoodItem, servingsNeeded: Double): List<PantryUpdate> {
    val recipeInfo =
        when (item) {
          is Recipe -> item.recipeInfo
          is Meal -> item.recipeInfo
          else -> return emptyList()
        }
    
    val batchMultiplier =
        servingsNeeded / (if (recipeInfo.servings > 0) recipeInfo.servings else 1.0)

    // Use CTE to get flat Bill of Materials (quantities are already in base units)
    val bom = foodItemRepository.getRecursiveIngredients(item.id)

    // Group deductions by item
    val totalDeductions = mutableMapOf<Uuid, Double>()

    bom.forEach { measurement ->
      val subItemId = measurement.foodItemId ?: return@forEach
      // We assume the CTE returns all base ingredients expanded.
      // Since quantities are normalized to base units, we just multiply by servings scale.
      val deductAmt = measurement.quantity * batchMultiplier
      totalDeductions[subItemId] = (totalDeductions[subItemId] ?: 0.0) + deductAmt
    }

    // Now convert total deductions into actual PantryUpdate objects
    return totalDeductions.map { (itemId, deductAmt) ->
        val current = pantryRepository.getPantryItemByFoodItemId(itemId)
        val currentQty = current?.measurement?.quantity ?: 0.0
        val unitId = current?.measurement?.unitId ?: Uuid.NIL // Default to NIL if not in pantry
        
        val newQty = max(0.0, currentQty - deductAmt)
        PantryUpdate(itemId, newQty, unitId)
    }
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
