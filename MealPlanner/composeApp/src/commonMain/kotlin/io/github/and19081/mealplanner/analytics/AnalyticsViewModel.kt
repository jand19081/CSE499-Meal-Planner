package io.github.and19081.mealplanner.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.domain.PriceCalculator
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import io.github.and19081.mealplanner.ingredients.Store
import io.github.and19081.mealplanner.ingredients.StoreRepository
import io.github.and19081.mealplanner.meals.MealRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import io.github.and19081.mealplanner.shoppinglist.ReceiptHistory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import kotlinx.datetime.minus

class AnalyticsViewModel : ViewModel() {

    // Helper for typed combination
    data class InputData(
        val entries: List<ScheduledMeal>,
        val meals: List<PrePlannedMeal>,
        val recipes: List<Recipe>,
        val ingredients: List<Ingredient>,
        val packages: List<Package>,
        val bridges: List<BridgeConversion>,
        val receiptHistory: List<ReceiptHistory>,
        val stores: List<Store>,
        val allUnits: List<UnitModel>
    )

    val uiState = combine(
        MealPlanRepository.entries,
        MealRepository.meals,
        RecipeRepository.recipes,
        IngredientRepository.ingredients,
        IngredientRepository.packages,
        IngredientRepository.bridges,
        ReceiptHistoryRepository.trips,
        StoreRepository.stores,
        UnitRepository.units
    ) { args: Array<Any> ->
        val data = InputData(
            entries = args[0] as List<ScheduledMeal>,
            meals = args[1] as List<PrePlannedMeal>,
            recipes = args[2] as List<Recipe>,
            ingredients = args[3] as List<Ingredient>,
            packages = args[4] as List<Package>,
            bridges = args[5] as List<BridgeConversion>,
            receiptHistory = args[6] as List<ReceiptHistory>,
            stores = args[7] as List<Store>,
            allUnits = args[8] as List<UnitModel>
        )
        
        // Pre-calculate Maps for O(1) Lookups
        val mealsMap = data.meals.associateBy { it.id }
        val recipesMap = data.recipes.associateBy { it.id }
        val ingredientsMap = data.ingredients.associateBy { it.id }
        val storeMap = data.stores.associateBy { it.id }

        // Cost Per Meal (Average)
        val mealCosts = data.meals.map { meal ->
            val cost = PriceCalculator.calculateMealCost(
                meal = meal, 
                recipesMap = recipesMap, 
                ingredientsMap = ingredientsMap, 
                allPackages = data.packages, 
                allBridges = data.bridges,
                allUnits = data.allUnits
            )
            meal.name to cost
        }.sortedByDescending { it.second }

        val avgMealCost = if (mealCosts.isNotEmpty()) mealCosts.map { it.second }.average() else 0.0

        // Avg Cost Per Person
        val avgCostPerPerson = avgMealCost / 4.0


        // Projected Costs (Future)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        val next7DaysEntries = data.entries.filter { it.date >= today && it.date < today.plus(DatePeriod(days=7)) }
        val next30DaysEntries = data.entries.filter { it.date >= today && it.date < today.plus(DatePeriod(days=30)) }
        
        fun sumCost(list: List<ScheduledMeal>): Long {
            return list.sumOf { entry ->
                PriceCalculator.calculateEstimatedCost(
                    entry = entry, 
                    mealsMap = mealsMap, 
                    recipesMap = recipesMap, 
                    ingredientsMap = ingredientsMap, 
                    allPackages = data.packages, 
                    allBridges = data.bridges,
                    allUnits = data.allUnits
                )
            }
        }
        
        val projectedWeek = sumCost(next7DaysEntries)
        val projectedMonth = sumCost(next30DaysEntries)

        // Actual Costs (Past)
        // Last 7 days
        val last7Days = data.receiptHistory.filter { it.date >= today.minus(DatePeriod(days=7)) && it.date <= today }
        val actualWeek = last7Days.sumOf { it.actualTotalCents.toLong() }
        
        // Last 30 days
        val last30Days = data.receiptHistory.filter { it.date >= today.minus(DatePeriod(days=30)) && it.date <= today }
        val actualMonth = last30Days.sumOf { it.actualTotalCents.toLong() }

        // Spending by Store
        val spendingByStore = data.receiptHistory.groupBy { it.storeId }
            .mapValues { (_, trips) -> trips.sumOf { it.actualTotalCents.toLong() } }
            .mapKeys { (storeId, _) -> storeMap[storeId]?.name ?: "Unknown Store" }
            .toList()
            .sortedByDescending { it.second }
            .toMap()

        AnalyticsUiState(
            avgMealCostCents = avgMealCost.toLong(),
            avgCostPerPersonCents = avgCostPerPerson.toLong(),
            projectedWeekCents = projectedWeek,
            projectedMonthCents = projectedMonth,
            actualWeekCents = actualWeek,
            actualMonthCents = actualMonth,
            mostExpensiveMeals = mealCosts.take(5),
            spendingByStore = spendingByStore,
            recentTrips = data.receiptHistory.sortedByDescending { it.date },
            allStores = data.stores
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState(0, 0, 0, 0, 0, 0, emptyList(), emptyMap(), emptyList(), emptyList()))
}

data class AnalyticsUiState(
    val avgMealCostCents: Long,
    val avgCostPerPersonCents: Long,
    val projectedWeekCents: Long,
    val projectedMonthCents: Long,
    val actualWeekCents: Long,
    val actualMonthCents: Long,
    val mostExpensiveMeals: List<Pair<String, Long>>,
    val spendingByStore: Map<String, Long>,
    val recentTrips: List<ReceiptHistory>,
    val allStores: List<Store>
)