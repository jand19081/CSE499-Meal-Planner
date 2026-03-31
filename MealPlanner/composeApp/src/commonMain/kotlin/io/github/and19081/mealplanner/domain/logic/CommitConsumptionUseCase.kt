package io.github.and19081.mealplanner.domain.logic

import io.github.and19081.mealplanner.core.util.SystemUnits
import io.github.and19081.mealplanner.core.util.UnitConverter
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.core.util.UnitType
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.ItemMeasurement
import io.github.and19081.mealplanner.domain.model.LeftoverInfo
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.repository.MealPlanRepository
import io.github.and19081.mealplanner.domain.repository.PantryRepository
import io.github.and19081.mealplanner.domain.repository.ReceiptHistoryRepository
import io.github.and19081.mealplanner.feature.kitchen.ConsumptionResult
import io.github.and19081.mealplanner.feature.meals.MealSource
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptLineItem
import kotlin.math.max
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.coroutines.flow.first
import kotlinx.datetime.toLocalDateTime

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
                val meal = mealPlanRepository.entries.first()
                    .find { it.id == result.scheduledMealId } ?: return
                val recipeId = (meal.source as? MealSource.PrePlannedMeal)?.mealId ?: return
                val foodItem = foodItemRepository.getFoodItem(recipeId) ?: return
                deductRecipeIngredients(foodItem, meal.peopleCount.toDouble())
                if (result.leftoverServings > 0)
                    addLeftover(foodItem, result.leftoverServings)
                mealPlanRepository.setConsumedStatus(result.scheduledMealId, true)
            }

            is ConsumptionResult.HomeRecipeConsumed -> {
                val meal = mealPlanRepository.entries.first()
                    .find { it.id == result.scheduledMealId } ?: return
                val recipeId = (meal.source as? MealSource.StandaloneRecipe)?.recipeId ?: return
                val foodItem = foodItemRepository.getFoodItem(recipeId) ?: return
                deductRecipeIngredients(foodItem, meal.peopleCount.toDouble())
                if (result.leftoverServings > 0)
                    addLeftover(foodItem, result.leftoverServings)
                mealPlanRepository.setConsumedStatus(result.scheduledMealId, true)
            }

            is ConsumptionResult.HomeIngredientConsumed -> {
                val meal = mealPlanRepository.entries.first()
                    .find { it.id == result.scheduledMealId } ?: return
                val src = meal.source as? MealSource.StandaloneIngredient ?: return
                val allUnits = unitRepository.units.value
                val unit = allUnits.find { it.id == src.unitId }
                    ?: allUnits.find { it.type == UnitType.Count && it.factorToBase == 1.0 }
                    ?: return
                val pantryItems = pantryRepository.pantryItems.first()
                val current = pantryItems.find { it.measurement.foodItemId == src.ingredientId }
                val currentQty = current?.measurement?.quantity ?: 0.0
                val currentUnitId = current?.measurement?.unitId ?: unit.id
                val convertedDeduct = UnitConverter.convert(
                    src.quantity, unit.id, currentUnitId,
                    allUnits.associateBy { it.id },
                ) ?: 0.0
                val newQty = max(0.0, currentQty - convertedDeduct)
                pantryRepository.updateQuantity(src.ingredientId, newQty, currentUnitId)
                mealPlanRepository.setConsumedStatus(result.scheduledMealId, true)
            }

            is ConsumptionResult.RestaurantMealConsumed -> {
                saveRestaurantReceipt(result)
                if (result.leftoverServings > 0) {
                    val name = result.leftoverDescription ?: "Restaurant Meal"
                    val leftover = FoodItem.Leftover(
                        name = "$name (Leftover)",
                        leftoverInfo = LeftoverInfo(
                            remainingServings = result.leftoverServings,
                            dateAdded = Clock.System.todayIn(
                                TimeZone.currentSystemDefault()
                            ).toString(),
                        ),
                    )
                    foodItemRepository.saveFoodItem(leftover)
                }
                mealPlanRepository.setConsumedStatus(result.scheduledMealId, true)
            }
        }
    }

    private suspend fun deductRecipeIngredients(item: FoodItem, servingsNeeded: Double) {
        val recipeInfo = when (item) {
            is FoodItem.Recipe -> item.recipeInfo
            is FoodItem.Meal -> item.recipeInfo
            else -> return
        }
        val allItems = foodItemRepository.foodItems.first().associateBy { it.id }
        val allUnits = unitRepository.units.value
        val allBridges = foodItemRepository.conversions.first()
        val pantryItems = pantryRepository.pantryItems.first()

        fun deductRecursive(fi: FoodItem, multiplier: Double) {
            val info = when (fi) {
                is FoodItem.Recipe -> fi.recipeInfo
                is FoodItem.Meal -> fi.recipeInfo
                else -> return
            }
            val servingsPerBatch = if (info.servings > 0) info.servings else 1.0
            info.requirementGroups.forEach { group ->
                val primary = group.requirements.find { it.isPrimary }
                    ?: group.requirements.firstOrNull() ?: return@forEach
                val subItem = allItems[primary.measurement.foodItemId] ?: return@forEach
                when (subItem) {
                    is FoodItem.Recipe, is FoodItem.Meal -> {
                        val subInfo = when (subItem) {
                            is FoodItem.Recipe -> subItem.recipeInfo
                            is FoodItem.Meal -> subItem.recipeInfo
                            else -> return@forEach
                        }
                        val scale = if (subInfo.servings > 0)
                            primary.measurement.quantity / subInfo.servings else 1.0
                        deductRecursive(subItem, multiplier * scale)
                    }

                    is FoodItem.Ingredient -> {
                        val targetUnitId = subItem.preferredUnitId ?: when (
                            allUnits.find { it.id == primary.measurement.unitId }?.type
                        ) {
                            UnitType.Mass -> SystemUnits.Gram.id
                            UnitType.Volume -> SystemUnits.Ml.id
                            else -> SystemUnits.Each.id
                        }
                        val bridges = allBridges.filter { it.foodItemId == subItem.id }
                        val deductAmt = UnitConverter.convert(
                            primary.measurement.quantity * multiplier,
                            primary.measurement.unitId ?: Uuid.NIL,
                            targetUnitId,
                            allUnits.associateBy { it.id },
                            bridges,
                        ) ?: 0.0
                        val current = pantryItems.find {
                            it.measurement.foodItemId == subItem.id
                        }
                        val currentQty = if (current != null) {
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

        val batchMultiplier = servingsNeeded / (if (recipeInfo.servings > 0) recipeInfo.servings else 1.0)
        deductRecursive(item, batchMultiplier)
    }

    private suspend fun addLeftover(source: FoodItem, servings: Double) {
        val leftover = FoodItem.Leftover(
            name = "${source.name} (Leftover)",
            leftoverInfo = LeftoverInfo(
                remainingServings = servings,
                dateAdded = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString(),
            ),
        )
        foodItemRepository.saveFoodItem(leftover)
    }

    private suspend fun saveRestaurantReceipt(result: ConsumptionResult.RestaurantMealConsumed) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).time
        val receiptId = Uuid.random()
        receiptHistoryRepository.addTrip(
            ReceiptHistory(
                id = receiptId,
                date = today,
                time = now,
                restaurantId = run {
                    val meal = mealPlanRepository.entries.first()
                        .find { it.id == result.scheduledMealId }
                    (meal?.source as? MealSource.Restaurant)?.restaurantId
                },
                projectedTotalCents = 0,
                actualTotalCents = result.actualCostCents,
                taxPaidCents = 0,
            )
        )
    }
}
