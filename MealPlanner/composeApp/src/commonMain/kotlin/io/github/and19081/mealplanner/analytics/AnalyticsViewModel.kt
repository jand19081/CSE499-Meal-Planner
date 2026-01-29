package io.github.and19081.mealplanner.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.domain.PriceCalculator
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.meals.MealRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import io.github.and19081.mealplanner.shoppinglist.ShoppingTrip
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
        val entries: List<MealPlanEntry>,
        val meals: List<Meal>,
        val components: List<MealComponent>,
        val recipes: List<Recipe>,
        val ingredients: List<Ingredient>,
        val recipeIngredients: List<RecipeIngredient>,
        val shoppingHistory: List<ShoppingTrip>
    )

    val uiState = combine(
        MealPlanRepository.entries,
        MealRepository.meals,
        MealRepository.mealComponents,
        RecipeRepository.recipes,
        IngredientRepository.ingredients,
        RecipeRepository.recipeIngredients,
        ShoppingHistoryRepository.trips
    ) { args: Array<Any> ->
        val data = InputData(
            entries = args[0] as List<MealPlanEntry>,
            meals = args[1] as List<Meal>,
            components = args[2] as List<MealComponent>,
            recipes = args[3] as List<Recipe>,
            ingredients = args[4] as List<Ingredient>,
            recipeIngredients = args[5] as List<RecipeIngredient>,
            shoppingHistory = args[6] as List<ShoppingTrip>
        )
        
        // Pre-calculate Maps for O(1) Lookups
        val mealsMap = data.meals.associateBy { it.id }
        val recipesMap = data.recipes.associateBy { it.id }
        val ingredientsMap = data.ingredients.associateBy { it.id }
        val componentsMap = data.components.groupBy { it.mealId }
        val recipeIngredientsMap = data.recipeIngredients.groupBy { it.recipeId }

        // Cost Per Meal (Average)
        val meaningfulMeals = data.meals.filter { m -> componentsMap.containsKey(m.id) }
        val mealCosts = meaningfulMeals.map { meal ->
            val cost = PriceCalculator.calculateMealCost(meal, componentsMap, recipesMap, ingredientsMap, recipeIngredientsMap)
            meal.name to cost
        }.sortedByDescending { it.second }

        val avgMealCost = if (mealCosts.isNotEmpty()) mealCosts.map { it.second }.average() else 0.0

        // Avg Cost Per Person
        val mealCostsPerPerson = meaningfulMeals.map { meal ->
            val cost = PriceCalculator.calculateMealCost(meal, componentsMap, recipesMap, ingredientsMap, recipeIngredientsMap)
            val servings = 1.0 // Placeholder
            cost / 4.0
        }
        val avgCostPerPerson = if (mealCostsPerPerson.isNotEmpty()) mealCostsPerPerson.average() else 0.0


        // Projected Costs (Future)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        val next7DaysEntries = data.entries.filter { it.date >= today && it.date < today.plus(DatePeriod(days=7)) }
        val next30DaysEntries = data.entries.filter { it.date >= today && it.date < today.plus(DatePeriod(days=30)) }
        
        fun sumCost(list: List<MealPlanEntry>): Long {
            return list.sumOf { entry ->
                PriceCalculator.calculateEstimatedCost(entry, mealsMap, componentsMap, recipesMap, ingredientsMap, recipeIngredientsMap)
            }
        }
        
        val projectedWeek = sumCost(next7DaysEntries)
        val projectedMonth = sumCost(next30DaysEntries)

        // Actual Costs (Past)
        // Last 7 days
        val last7Days = data.shoppingHistory.filter { it.date >= today.minus(DatePeriod(days=7)) && it.date <= today }
        val actualWeek = last7Days.sumOf { it.totalPaidCents }
        
        // Last 30 days
        val last30Days = data.shoppingHistory.filter { it.date >= today.minus(DatePeriod(days=30)) && it.date <= today }
        val actualMonth = last30Days.sumOf { it.totalPaidCents }

        // Spending by Store
        val spendingByStore = data.shoppingHistory.groupBy { it.storeName }
            .mapValues { (_, trips) -> trips.sumOf { it.totalPaidCents } }
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
            recentTrips = data.shoppingHistory.sortedByDescending { it.date }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState(0, 0, 0, 0, 0, 0, emptyList(), emptyMap(), emptyList()))
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
    val recentTrips: List<ShoppingTrip>
)