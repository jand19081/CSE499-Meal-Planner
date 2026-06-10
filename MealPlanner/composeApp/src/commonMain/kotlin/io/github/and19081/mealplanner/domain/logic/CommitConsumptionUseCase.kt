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
        val allUnits = unitRepository.units.value
        val allUnitsMap = allUnits.associateBy { it.id }
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
                allUnitsMap,
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

  private suspend fun calculateDeductions(item: FoodItem, servingsNeeded: Double): List<PantryUpdate> {
    val recipeInfo =
        when (item) {
          is Recipe -> item.recipeInfo
          is Meal -> item.recipeInfo
          else -> return emptyList()
        }
    val allUnitsMap = unitRepository.units.value.associateBy { it.id }
    
    val batchMultiplier =
        servingsNeeded / (if (recipeInfo.servings > 0) recipeInfo.servings else 1.0)

    // Use CTE to get flat Bill of Materials
    val bom = foodItemRepository.getRecursiveIngredients(item.id)

    // Group deductions by item and convert to target units
    val totalDeductions = mutableMapOf<Uuid, Double>()
    val itemTargetUnits = mutableMapOf<Uuid, Uuid>()

    bom.forEach { measurement ->
      val subItemId = measurement.foodItemId ?: return@forEach
      val subItem = foodItemRepository.getFoodItem(subItemId) ?: return@forEach
      
      // The CTE returns base ingredients (those that are not recipes)
      // Wait, the CTE as written returns ALL recursive items. 
      // I should check if the subItem is an ingredient.
      
      if (subItem is Ingredient) {
        val targetUnitId =
            subItem.preferredUnitId
                ?: when (allUnitsMap[measurement.unitId]?.type) {
                  UnitType.Mass -> SystemUnits.Gram.id
                  UnitType.Volume -> SystemUnits.Ml.id
                  else -> SystemUnits.Each.id
                }
        
        val bridges = foodItemRepository.getConversionsForFoodItem(subItem.id)
        val deductAmt =
            UnitConverter.convert(
                measurement.quantity * batchMultiplier,
                measurement.unitId ?: Uuid.NIL,
                targetUnitId,
                allUnitsMap,
                bridges,
            ) ?: 0.0
        
        totalDeductions[subItemId] = (totalDeductions[subItemId] ?: 0.0) + deductAmt
        itemTargetUnits[subItemId] = targetUnitId
      }
    }

    // Now convert total deductions into actual PantryUpdate objects by checking current inventory
    return totalDeductions.map { (itemId, deductAmt) ->
        val targetUnitId = itemTargetUnits[itemId] ?: SystemUnits.Each.id
        val current = pantryRepository.getPantryItemByFoodItemId(itemId)
        val bridges = foodItemRepository.getConversionsForFoodItem(itemId)
        
        val currentQtyInTargetUnit = if (current != null) {
            UnitConverter.convert(
                current.measurement.quantity,
                current.measurement.unitId ?: Uuid.NIL,
                targetUnitId,
                allUnitsMap,
                bridges,
            ) ?: 0.0
        } else 0.0
        
        val newQty = max(0.0, currentQtyInTargetUnit - deductAmt)
        PantryUpdate(itemId, newQty, targetUnitId)
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
