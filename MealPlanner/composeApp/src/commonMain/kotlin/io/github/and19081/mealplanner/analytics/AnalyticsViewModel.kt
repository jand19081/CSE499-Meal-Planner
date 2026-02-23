package io.github.and19081.mealplanner.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.domain.DataQualityValidator
import io.github.and19081.mealplanner.domain.DataWarning
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlin.uuid.Uuid

enum class AnalyticsFilter {
    ALL, STORES, RESTAURANTS
}

enum class AnalyticsDateRange {
    DAY, WEEK, MONTH, YEAR, CUSTOM
}

class AnalyticsViewModel(
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository,
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val receiptHistoryRepository: ReceiptHistoryRepository,
    private val storeRepository: StoreRepository,
    private val unitRepository: UnitRepository,
    private val restaurantRepository: io.github.and19081.mealplanner.ingredients.RestaurantRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(AnalyticsFilter.ALL)
    private val _dateRange = MutableStateFlow(AnalyticsDateRange.WEEK)
    private val _customStartDate = MutableStateFlow<LocalDate?>(null)
    private val _customEndDate = MutableStateFlow<LocalDate?>(null)

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
        val restaurants: List<Restaurant>,
        val allUnits: List<UnitModel>,
        val filter: AnalyticsFilter,
        val dateRange: AnalyticsDateRange,
        val customStart: LocalDate?,
        val customEnd: LocalDate?
    )

    val uiState = combine(
        mealPlanRepository.entries,
        mealRepository.meals,
        recipeRepository.recipes,
        ingredientRepository.ingredients,
        ingredientRepository.packages,
        ingredientRepository.bridges,
        receiptHistoryRepository.trips,
        storeRepository.stores,
        restaurantRepository.restaurants,
        unitRepository.units,
        _filter,
        _dateRange,
        _customStartDate,
        _customEndDate
    ) { args: Array<Any?> ->
        val data = InputData(
            entries = args[0] as List<ScheduledMeal>,
            meals = args[1] as List<PrePlannedMeal>,
            recipes = args[2] as List<Recipe>,
            ingredients = args[3] as List<Ingredient>,
            packages = args[4] as List<Package>,
            bridges = args[5] as List<BridgeConversion>,
            receiptHistory = args[6] as List<ReceiptHistory>,
            stores = args[7] as List<Store>,
            restaurants = args[8] as List<Restaurant>,
            allUnits = args[9] as List<UnitModel>,
            filter = args[10] as AnalyticsFilter,
            dateRange = args[11] as AnalyticsDateRange,
            customStart = args[12] as LocalDate?,
            customEnd = args[13] as LocalDate?
        )
        
        // Pre-calculate Maps for O(1) Lookups
        val mealsMap = data.meals.associateBy { it.id }
        val recipesMap = data.recipes.associateBy { it.id }
        val ingredientsMap = data.ingredients.associateBy { it.id }
        val storeMap = data.stores.associateBy { it.id }
        val restaurantMap = data.restaurants.associateBy { it.id }

        // Data Quality Validation
        val globalWarnings = data.meals.flatMap { meal ->
            DataQualityValidator.validateMeal(meal, recipesMap, ingredientsMap, data.packages, data.bridges, data.allUnits)
        }.distinctBy { it.message }

        // Determine active date range
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val (startDate, endDate) = when (data.dateRange) {
            AnalyticsDateRange.DAY -> today to today
            AnalyticsDateRange.WEEK -> today.minus(DatePeriod(days = 7)) to today
            AnalyticsDateRange.MONTH -> today.minus(DatePeriod(months = 1)) to today
            AnalyticsDateRange.YEAR -> today.minus(DatePeriod(years = 1)) to today
            AnalyticsDateRange.CUSTOM -> (data.customStart ?: today) to (data.customEnd ?: today)
        }

        // Filter Receipts
        val filteredReceipts = data.receiptHistory.filter { receipt ->
            val matchesFilter = when (data.filter) {
                AnalyticsFilter.ALL -> true
                AnalyticsFilter.STORES -> receipt.storeId != null
                AnalyticsFilter.RESTAURANTS -> receipt.restaurantId != null
            }
            val matchesDate = receipt.date >= startDate && receipt.date <= endDate
            matchesFilter && matchesDate
        }

        // Filter Scheduled Meals for projected costs
        val futureStartDate = today
        val futureEndDate = when (data.dateRange) {
            AnalyticsDateRange.DAY -> today
            AnalyticsDateRange.WEEK -> today.plus(DatePeriod(days = 7))
            AnalyticsDateRange.MONTH -> today.plus(DatePeriod(months = 1))
            AnalyticsDateRange.YEAR -> today.plus(DatePeriod(years = 1))
            AnalyticsDateRange.CUSTOM -> data.customEnd ?: today
        }

        val projectedEntries = data.entries.filter { it.date >= futureStartDate && it.date <= futureEndDate }

        // Cost Calculations
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
        
        val projectedTotal = sumCost(projectedEntries)
        val actualTotal = filteredReceipts.sumOf { it.actualTotalCents.toLong() }

        // Average Cost Calculation (Actuals)
        // We want to include both store receipts (divided by some heuristic or just per meal) 
        // and restaurant receipts (actual per meal).
        val restaurantActuals = data.receiptHistory.filter { it.restaurantId != null }
        val groceryActuals = data.receiptHistory.filter { it.storeId != null }
        
        // Count consumed home meals for averaging grocery costs
        val consumedHomeMealsCount = data.entries.count { it.isConsumed && it.prePlannedMealId != null }
        val consumedRestaurantMealsCount = data.entries.count { it.isConsumed && it.restaurantId != null }
        val totalConsumed = consumedHomeMealsCount + consumedRestaurantMealsCount

        val totalSpentActual = restaurantActuals.sumOf { it.actualTotalCents.toLong() } + groceryActuals.sumOf { it.actualTotalCents.toLong() }
        
        val avgMealCost = if (totalConsumed > 0) totalSpentActual.toDouble() / totalConsumed else 0.0
        
        // Spending by Location
        val spendingByLocation = filteredReceipts.groupBy { it.storeId ?: it.restaurantId ?: Uuid.NIL }
            .mapValues { (_, trips) -> trips.sumOf { it.actualTotalCents.toLong() } }
            .mapKeys { (locId, _) -> 
                storeMap[locId]?.name ?: restaurantMap[locId]?.name ?: "Other/Unknown"
            }
            .toList()
            .sortedByDescending { it.second }
            .toMap()

        // Most Expensive Meals (Planned or Consumed)
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

        AnalyticsUiState(
            avgMealCostCents = avgMealCost.toLong(),
            avgCostPerPersonCents = (avgMealCost / 4.0).toLong(),
            projectedTotalCents = projectedTotal,
            actualTotalCents = actualTotal,
            mostExpensiveMeals = mealCosts.take(5),
            spendingByLocation = spendingByLocation,
            recentShoppingTrips = filteredReceipts.filter { it.storeId != null }.sortedByDescending { it.date },
            recentRestaurantMeals = filteredReceipts.filter { it.restaurantId != null }.sortedByDescending { it.date },
            allStores = data.stores,
            allRestaurants = data.restaurants,
            allIngredients = data.ingredients,
            allUnits = data.allUnits,
            warnings = globalWarnings,
            currentFilter = data.filter,
            currentDateRange = data.dateRange,
            startDate = startDate,
            endDate = endDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    fun setFilter(filter: AnalyticsFilter) { _filter.value = filter }
    fun setDateRange(range: AnalyticsDateRange) { _dateRange.value = range }
    fun setCustomRange(start: LocalDate, end: LocalDate) {
        _customStartDate.value = start
        _customEndDate.value = end
        _dateRange.value = AnalyticsDateRange.CUSTOM
    }

    suspend fun getTripDetails(id: Uuid): ReceiptHistory? {
        return receiptHistoryRepository.getTripWithLineItems(id)
    }

    fun updateTrip(trip: ReceiptHistory) {
        viewModelScope.launch {
            receiptHistoryRepository.updateTrip(trip)
        }
    }

    fun deleteTrip(id: Uuid) {
        viewModelScope.launch {
            receiptHistoryRepository.removeTrip(id)
        }
    }
}

data class AnalyticsUiState(
    val avgMealCostCents: Long = 0,
    val avgCostPerPersonCents: Long = 0,
    val projectedTotalCents: Long = 0,
    val actualTotalCents: Long = 0,
    val mostExpensiveMeals: List<Pair<String, Long>> = emptyList(),
    val spendingByLocation: Map<String, Long> = emptyMap(),
    val recentShoppingTrips: List<ReceiptHistory> = emptyList(),
    val recentRestaurantMeals: List<ReceiptHistory> = emptyList(),
    val allStores: List<Store> = emptyList(),
    val allRestaurants: List<Restaurant> = emptyList(),
    val allIngredients: List<Ingredient> = emptyList(),
    val allUnits: List<UnitModel> = emptyList(),
    val warnings: List<DataWarning> = emptyList(),
    val currentFilter: AnalyticsFilter = AnalyticsFilter.ALL,
    val currentDateRange: AnalyticsDateRange = AnalyticsDateRange.WEEK,
    val startDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val endDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
)