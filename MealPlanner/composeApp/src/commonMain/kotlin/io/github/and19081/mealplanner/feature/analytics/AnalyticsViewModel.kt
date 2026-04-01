package io.github.and19081.mealplanner.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.feature.meals.Restaurant
import io.github.and19081.mealplanner.feature.meals.ScheduledMeal
import io.github.and19081.mealplanner.core.util.DataQualityValidator
import io.github.and19081.mealplanner.core.util.DataWarning
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.domain.logic.PriceCalculator
import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.isRecipe
import io.github.and19081.mealplanner.domain.model.isMeal
import io.github.and19081.mealplanner.domain.model.isIngredient
import io.github.and19081.mealplanner.domain.model.Package
import io.github.and19081.mealplanner.domain.model.Store
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.repository.MealPlanRepository
import io.github.and19081.mealplanner.domain.repository.ReceiptHistoryRepository
import io.github.and19081.mealplanner.domain.repository.RestaurantRepository
import io.github.and19081.mealplanner.domain.repository.StoreRepository
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

enum class AnalyticsFilter {
    ALL,
    STORES,
    RESTAURANTS,
}

enum class AnalyticsDateRange {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    CUSTOM,
}

class AnalyticsViewModel(
    private val mealPlanRepository: MealPlanRepository,
    private val foodItemRepository: FoodItemRepository,
    private val receiptHistoryRepository: ReceiptHistoryRepository,
    private val storeRepository: StoreRepository,
    private val unitRepository: UnitRepository,
    private val restaurantRepository: RestaurantRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(AnalyticsFilter.ALL)
    private val _dateRange = MutableStateFlow(AnalyticsDateRange.WEEK)
    private val _customStartDate = MutableStateFlow<LocalDate?>(null)
    private val _customEndDate = MutableStateFlow<LocalDate?>(null)
    private val _filterState = MutableStateFlow<AnalyticsFilterState>(AnalyticsFilterState.ALL)

    data class InputData(
        val entries: List<ScheduledMeal>,
        val allItems: List<FoodItem>,
        val packages: List<Package>,
        val bridges: List<BridgeConversion>,
        val receiptHistory: List<ReceiptHistory>,
        val stores: List<Store>,
        val restaurants: List<Restaurant>,
        val allUnits: List<UnitModel>,
        val filter: AnalyticsFilter,
        val dateRange: AnalyticsDateRange,
        val customStart: LocalDate?,
        val customEnd: LocalDate?,
    )

    val uiState =
        combine(
            mealPlanRepository.entries,
            foodItemRepository.foodItems,
            foodItemRepository.packages,
            foodItemRepository.conversions,
            receiptHistoryRepository.trips,
            storeRepository.stores,
            restaurantRepository.restaurants,
            unitRepository.units,
            _filter,
            _dateRange,
            _customStartDate,
            _customEndDate,
        ) { args: Array<Any?> ->
            val entries = args[0] as List<ScheduledMeal>
            val allItems = args[1] as List<FoodItem>
            val packages = args[2] as List<Package>
            val bridges = args[3] as List<BridgeConversion>
            val trips = args[4] as List<ReceiptHistory>
            val stores = args[5] as List<Store>
            val restaurants = args[6] as List<Restaurant>
            val units = args[7] as List<UnitModel>
            val filter = args[8] as AnalyticsFilter
            val range = args[9] as AnalyticsDateRange
            val customStart = args[10] as LocalDate?
            val customEnd = args[11] as LocalDate?

            val data =
                InputData(
                    entries = entries,
                    allItems = allItems,
                    packages = packages,
                    bridges = bridges,
                    receiptHistory = trips,
                    stores = stores,
                    restaurants = restaurants,
                    allUnits = units,
                    filter = filter,
                    dateRange = range,
                    customStart = customStart,
                    customEnd = customEnd,
                )

            val itemsMap = data.allItems.associateBy { it.id }
            val storeMap = data.stores.associateBy { it.id }
            val restaurantMap = data.restaurants.associateBy { it.id }

            val globalWarnings =
                data.allItems.filter { it.isRecipe() || it.isMeal() }
                    .flatMap { meal ->
                        DataQualityValidator.validateFoodItem(
                            meal,
                            itemsMap,
                            data.packages,
                            data.bridges,
                            data.allUnits,
                        )
                    }
                    .distinctBy { it.message }

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val (startDate, endDate) =
                when (data.dateRange) {
                    AnalyticsDateRange.DAY -> today to today
                    AnalyticsDateRange.WEEK -> today.minus(DatePeriod(days = 7)) to today
                    AnalyticsDateRange.MONTH -> today.minus(DatePeriod(months = 1)) to today
                    AnalyticsDateRange.YEAR -> today.minus(DatePeriod(years = 1)) to today
                    AnalyticsDateRange.CUSTOM ->
                        (data.customStart ?: today) to (data.customEnd ?: today)
                }

            val filteredReceipts =
                data.receiptHistory.filter { receipt ->
                    val matchesFilter =
                        when (data.filter) {
                            AnalyticsFilter.ALL -> true
                            AnalyticsFilter.STORES -> receipt.storeId != null
                            AnalyticsFilter.RESTAURANTS -> receipt.restaurantId != null
                        }
                    val matchesDate = receipt.date >= startDate && receipt.date <= endDate
                    matchesFilter && matchesDate
                }

            val futureStartDate = today
            val futureEndDate =
                when (data.dateRange) {
                    AnalyticsDateRange.DAY -> today
                    AnalyticsDateRange.WEEK -> today.plus(DatePeriod(days = 7))
                    AnalyticsDateRange.MONTH -> today.plus(DatePeriod(months = 1))
                    AnalyticsDateRange.YEAR -> today.plus(DatePeriod(years = 1))
                    AnalyticsDateRange.CUSTOM -> data.customEnd ?: today
                }

            val projectedEntries =
                data.entries.filter { it.date >= futureStartDate && it.date <= futureEndDate }

            fun sumCost(list: List<ScheduledMeal>): Long {
                return list.sumOf { entry ->
                    PriceCalculator.calculateEstimatedCost(
                        entry = entry,
                        allItemsMap = itemsMap,
                        packagesByIngredient = data.packages.groupBy { it.foodItemId },
                        bridgesByIngredient = data.bridges.groupBy { it.foodItemId },
                        allUnits = data.allUnits.associateBy { it.id },
                    )
                }
            }

            val projectedTotal = sumCost(projectedEntries)
            val actualTotal = filteredReceipts.sumOf { it.actualTotalCents.toLong() }

            val restaurantActuals = data.receiptHistory.filter { it.restaurantId != null }
            val groceryActuals = data.receiptHistory.filter { it.storeId != null }

            val consumedHomeMealsCount =
                data.entries.count { it.isConsumed && it.prePlannedMealId != null }
            val consumedRestaurantMealsCount =
                data.entries.count { it.isConsumed && it.restaurantId != null }
            val totalConsumed = consumedHomeMealsCount + consumedRestaurantMealsCount

            val totalSpentActual =
                restaurantActuals.sumOf { it.actualTotalCents.toLong() } +
                        groceryActuals.sumOf { it.actualTotalCents.toLong() }

            val avgMealCost =
                if (totalConsumed > 0) totalSpentActual.toDouble() / totalConsumed else 0.0

            val spendingByLocation =
                filteredReceipts
                    .groupBy { it.storeId ?: it.restaurantId ?: Uuid.NIL }
                    .mapValues { (_, trips) -> trips.sumOf { it.actualTotalCents.toLong() } }
                    .mapKeys { (locId, _) ->
                        storeMap[locId]?.name ?: restaurantMap[locId]?.name ?: "Other/Unknown"
                    }
                    .toList()
                    .sortedByDescending { it.second }
                    .toMap()

            val mealCosts =
                data.allItems.filter { it.isRecipe() || it.isMeal() }
                    .map { meal ->
                        val cost =
                            PriceCalculator.calculateFoodItemCost(
                                item = meal,
                                allItemsMap = itemsMap,
                                packagesByIngredient = data.packages.groupBy { it.foodItemId },
                                bridgesByIngredient = data.bridges.groupBy { it.foodItemId },
                                allUnits = data.allUnits.associateBy { it.id },
                            )
                        meal.name to cost
                    }
                    .sortedByDescending { it.second ?: 0L }

            // Calculate actual people count from consumed entries
            val actualPeopleCount = data.entries
                .filter { it.isConsumed }
                .sumOf { it.peopleCount.toLong() }

            val avgCostPerPersonCents =
                if (actualPeopleCount > 0) {
                    totalSpentActual / actualPeopleCount
                } else 0L

            // Projected vs actual cost comparisons
            val weeklyProjected = data.entries
                .filter {
                    val weekStart = today.minus(DatePeriod(days = 7))
                    it.date >= weekStart && it.date <= today && !it.isConsumed
                }
                .sumOf { entry ->
                    PriceCalculator.calculateEstimatedCost(
                        entry = entry,
                        allItemsMap = itemsMap,
                        packagesByIngredient = data.packages.groupBy { it.foodItemId },
                        bridgesByIngredient = data.bridges.groupBy { it.foodItemId },
                        allUnits = data.allUnits.associateBy { it.id },
                    )
                }

            val weeklyActual = filteredReceipts
                .filter {
                    val weekStart = today.minus(DatePeriod(days = 7))
                    it.date >= weekStart && it.date <= today
                }
                .sumOf { it.actualTotalCents.toLong() }

            val weeklyProjectedVsActual =
                if (weeklyProjected > 0) {
                    val variance = weeklyActual.toDouble() - weeklyProjected
                    val percentDiff = (variance / weeklyProjected.toDouble()) * 100
                    CostComparison(
                        period = "Weekly",
                        projected = weeklyProjected,
                        actual = weeklyActual,
                        percentDifference = percentDiff
                    )
                } else null

            val monthlyProjected = data.entries
                .filter {
                    val monthStart = today.minus(DatePeriod(months = 1))
                    it.date >= monthStart && it.date <= today && !it.isConsumed
                }
                .sumOf { entry ->
                    PriceCalculator.calculateEstimatedCost(
                        entry = entry,
                        allItemsMap = itemsMap,
                        packagesByIngredient = data.packages.groupBy { it.foodItemId },
                        bridgesByIngredient = data.bridges.groupBy { it.foodItemId },
                        allUnits = data.allUnits.associateBy { it.id },
                    )
                }

            val monthlyActual = filteredReceipts
                .filter {
                    val monthStart = today.minus(DatePeriod(months = 1))
                    it.date >= monthStart && it.date <= today
                }
                .sumOf { it.actualTotalCents.toLong() }

            val monthlyProjectedVsActual =
                if (monthlyProjected > 0) {
                    val variance = monthlyActual.toDouble() - monthlyProjected
                    val percentDiff = (variance / monthlyProjected.toDouble()) * 100
                    CostComparison(
                        period = "Monthly",
                        projected = monthlyProjected,
                        actual = monthlyActual,
                        percentDifference = percentDiff
                    )
                } else null

            val annualProjected = data.entries
                .filter {
                    val yearStart = today.minus(DatePeriod(years = 1))
                    it.date >= yearStart && it.date <= today && !it.isConsumed
                }
                .sumOf { entry ->
                    PriceCalculator.calculateEstimatedCost(
                        entry = entry,
                        allItemsMap = itemsMap,
                        packagesByIngredient = data.packages.groupBy { it.foodItemId },
                        bridgesByIngredient = data.bridges.groupBy { it.foodItemId },
                        allUnits = data.allUnits.associateBy { it.id },
                    )
                }

            val annualActual = filteredReceipts
                .filter {
                    val yearStart = today.minus(DatePeriod(years = 1))
                    it.date >= yearStart && it.date <= today
                }
                .sumOf { it.actualTotalCents.toLong() }

            val annualProjectedVsActual =
                if (annualProjected > 0) {
                    val variance = annualActual.toDouble() - annualProjected
                    val percentDiff = (variance / annualProjected.toDouble()) * 100
                    CostComparison(
                        period = "Annual",
                        projected = annualProjected,
                        actual = annualActual,
                        percentDifference = percentDiff
                    )
                } else null

            AnalyticsUiState(
                avgMealCostCents = avgMealCost.toLong(),
                avgCostPerPersonCents = avgCostPerPersonCents,
                projectedTotalCents = projectedTotal,
                actualTotalCents = actualTotal,
                mostExpensiveMeals = mealCosts.take(5),
                spendingByLocation = spendingByLocation,
                recentShoppingTrips =
                    filteredReceipts.filter { it.storeId != null }.sortedByDescending { it.date },
                recentRestaurantMeals =
                    filteredReceipts
                        .filter { it.restaurantId != null }
                        .sortedByDescending { it.date },
                allStores = data.stores,
                allRestaurants = data.restaurants,
                allIngredients = data.allItems.filter { it.isIngredient() },
                allUnits = data.allUnits,
                warnings = globalWarnings,
                currentFilter = data.filter,
                currentDateRange = data.dateRange,
                startDate = startDate,
                endDate = endDate,
                projectedVsActualWeekly = weeklyProjectedVsActual,
                projectedVsActualMonthly = monthlyProjectedVsActual,
                projectedVsActualAnnual = annualProjectedVsActual,
                totalPeopleCount = actualPeopleCount,
            )
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    fun setFilter(filter: AnalyticsFilter) {
        _filter.value = filter
    }

    fun setDateRange(range: AnalyticsDateRange) {
        _dateRange.value = range
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        _customStartDate.value = start
        _customEndDate.value = end
        _dateRange.value = AnalyticsDateRange.CUSTOM
    }

    suspend fun getTripDetails(id: Uuid): ReceiptHistory? {
        return receiptHistoryRepository.getTripWithLineItems(id)
    }

    fun updateTrip(trip: ReceiptHistory) {
        viewModelScope.launch { receiptHistoryRepository.updateTrip(trip) }
    }

    fun deleteTrip(id: Uuid) {
        viewModelScope.launch { receiptHistoryRepository.removeTrip(id) }
    }
}

data class AnalyticsUiState(
    val avgMealCostCents: Long = 0,
    val avgCostPerPersonCents: Long = 0,
    val projectedTotalCents: Long = 0,
    val actualTotalCents: Long = 0,
    val mostExpensiveMeals: List<Pair<String, Long?>> = emptyList(),
    val spendingByLocation: Map<String, Long> = emptyMap(),
    val recentShoppingTrips: List<ReceiptHistory> = emptyList(),
    val recentRestaurantMeals: List<ReceiptHistory> = emptyList(),
    val allStores: List<Store> = emptyList(),
    val allRestaurants: List<Restaurant> = emptyList(),
    val allIngredients: List<FoodItem> = emptyList(),
    val allUnits: List<UnitModel> = emptyList(),
    val warnings: List<DataWarning> = emptyList(),
    val currentFilter: AnalyticsFilter = AnalyticsFilter.ALL,
    val currentDateRange: AnalyticsDateRange = AnalyticsDateRange.WEEK,
    val startDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val endDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val projectedVsActualWeekly: CostComparison? = null,
    val projectedVsActualMonthly: CostComparison? = null,
    val projectedVsActualAnnual: CostComparison? = null,
    val totalPeopleCount: Long = 0,
)

data class CostComparison(
    val period: String,
    val projected: Long,
    val actual: Long,
    val percentDifference: Double,
)
