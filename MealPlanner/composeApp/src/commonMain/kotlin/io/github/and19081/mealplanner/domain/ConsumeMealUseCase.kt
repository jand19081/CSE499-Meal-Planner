package io.github.and19081.mealplanner.domain

import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.kitchen.*
import kotlin.uuid.Uuid
import kotlin.math.max
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class ConsumeMealUseCase(
    private val mealPlanRepository: MealPlanRepository,
    private val foodItemRepository: FoodItemRepository,
    private val pantryRepository: PantryRepository,
    private val unitRepository: UnitRepository,
) {
    suspend operator fun invoke(
        entryId: Uuid, 
        manualTransaction: KitchenTransaction? = null,
        leftovers: List<Pair<Uuid, Double>> = emptyList()
    ) {
        val entry = mealPlanRepository.entries.value.find { it.id == entryId } ?: return
        if (entry.isConsumed) return

        val allItems = foodItemRepository.foodItems.value
        val allUnits = unitRepository.units.value
        val allBridges = foodItemRepository.conversions.value
        val pantryItems = pantryRepository.pantryItems.value

        val transaction = manualTransaction ?: createDefaultTransaction(entry, allItems, allUnits)

        // Apply changes to pantry
        transaction?.changes?.forEach { change ->
            val item = allItems.find { it.id == change.foodItemId }
            val unit = allUnits.find { it.id == change.unitId }

            if (item != null && unit != null) {
                val targetUnitId = item.preferredUnitId ?: when (unit.type) {
                    UnitType.Mass -> SystemUnits.Gram.id
                    UnitType.Volume -> SystemUnits.Ml.id
                    UnitType.Count -> SystemUnits.Each.id
                    else -> unit.id
                }

                val bridges = allBridges.filter { it.foodItemId == change.foodItemId }
                val changeInTargetUnit = UnitConverter.convert(
                    amount = change.quantity ?: 0.0,
                    fromUnitId = change.unitId ?: Uuid.NIL,
                    toUnitId = targetUnitId,
                    allUnits = allUnits.associateBy { it.id },
                    bridges = bridges
                ) ?: 0.0

                val currentPantryItem = pantryItems.find { it.foodItemId == change.foodItemId }
                val currentQtyInTargetUnit = if (currentPantryItem != null) {
                    UnitConverter.convert(
                        amount = currentPantryItem.quantity,
                        fromUnitId = currentPantryItem.unitId,
                        toUnitId = targetUnitId,
                        allUnits = allUnits.associateBy { it.id },
                        bridges = bridges
                    ) ?: 0.0
                } else 0.0

                val newQty = max(0.0, currentQtyInTargetUnit - changeInTargetUnit)
                pantryRepository.updateQuantity(change.foodItemId, newQty, targetUnitId)
            }
        }

        // Handle leftovers
        val todayStr = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        leftovers.forEach { (recipeId, servings) ->
            if (servings > 0) {
                val originalRecipe = allItems.find { it.id == recipeId }
                val leftoverItem = FoodItem(
                    name = "${originalRecipe?.name ?: "Meal"} (Leftover)",
                    leftoverInfo = LeftoverInfo(
                        remainingServings = servings,
                        dateAdded = todayStr
                    )
                )
                foodItemRepository.saveFoodItem(leftoverItem)
            }
        }

        // Mark as consumed
        mealPlanRepository.setConsumedStatus(entryId, true)
    }

    private fun createDefaultTransaction(
        entry: ScheduledMeal,
        allItems: List<FoodItem>,
        allUnits: List<UnitModel>
    ): KitchenTransaction? {
        val itemsMap = allItems.associateBy { it.id }
        val meal = itemsMap[entry.prePlannedMealId] ?: return null
        val recipeInfo = meal.recipeInfo ?: return null
        
        val changes = mutableListOf<InventoryChange>()

        fun addRequirementsRecursive(item: FoodItem, multiplier: Double) {
            val info = item.recipeInfo ?: return
            info.requirements.forEach { req ->
                val subItem = itemsMap[req.foodItemId] ?: return@forEach
                if (subItem.isRecipe) {
                    val subRecipeInfo = subItem.recipeInfo!!
                    val scale = if (subRecipeInfo.servings > 0) req.quantity / subRecipeInfo.servings else 1.0
                    addRequirementsRecursive(subItem, multiplier * scale)
                } else {
                    val unit = allUnits.find { it.id == req.unitId }
                    changes.add(
                        InventoryChange(
                            foodItemId = req.foodItemId,
                            ingredientName = subItem.name,
                            quantity = req.quantity * multiplier,
                            unitId = req.unitId ?: subItem.preferredUnitId,
                            unitAbbreviation = unit?.abbreviation ?: "?",
                            direction = TransactionDirection.OUT
                        )
                    )
                }
            }
        }

        val servingsPerBatch = if (recipeInfo.servings > 0) recipeInfo.servings else 1.0
        val batchesCooked = max(1.0, entry.peopleCount / servingsPerBatch)
        
        addRequirementsRecursive(meal, batchesCooked)

        return KitchenTransaction(
            type = TransactionType.Consumption,
            title = "Consuming: ${meal.name}",
            changes = changes
        )
    }
}
