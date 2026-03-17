package io.github.and19081.mealplanner.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.calendar.CalendarEvent
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.domain.*
import io.github.and19081.mealplanner.settings.DashboardConfig
import io.github.and19081.mealplanner.settings.SettingsRepository
import io.github.and19081.mealplanner.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.shoppinglist.ShoppingListItem
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

class DashboardViewModel(
    private val mealPlanRepository: MealPlanRepository,
    private val foodItemRepository: FoodItemRepository,
    private val pantryRepository: PantryRepository,
    private val unitRepository: UnitRepository,
    private val settingsRepository: SettingsRepository,
    private val receiptHistoryRepository: ReceiptHistoryRepository,
    private val shoppingListItemRepository: ShoppingListItemRepository,
    private val restaurantRepository: RestaurantRepository,
) : ViewModel() {

  data class DashboardUiState(
      val todaysMeals: List<CalendarEvent>,
      val pantryItemCount: Int,
      val shoppingListCount: Int,
      val nextMeal: CalendarEvent?,
      val dashboardConfig: DashboardConfig,
      val currentWeekCost: Long = 0L,
      val warnings: List<DataWarning> = emptyList(),
  )

  private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

  val uiState =
      combine(
              mealPlanRepository.entries,
              foodItemRepository.foodItems,
              foodItemRepository.packages,
              foodItemRepository.conversions,
              unitRepository.units,
              pantryRepository.pantryItems,
              settingsRepository.dashboardConfig,
              receiptHistoryRepository.trips,
              shoppingListItemRepository.items,
              restaurantRepository.restaurants,
          ) { args: Array<Any?> ->
            val entries = args[0] as List<ScheduledMeal>
            val allItems = args[1] as List<FoodItem>
            val packages = args[2] as List<Package>
            val bridges = args[3] as List<BridgeConversion>
            val allUnits = args[4] as List<UnitModel>
            val pantry = args[5] as List<PantryItem>
            val config = args[6] as DashboardConfig
            val trips = args[7] as List<ReceiptHistory>
            val shoppingItems = args[8] as List<ShoppingListItem>
            val restaurants = args[9] as List<Restaurant>

            val itemsMap = allItems.associateBy { it.id }
            val restaurantsMap = restaurants.associateBy { it.id }

            val todaysEntries = entries.filter { it.date == today }.sortedBy { it.time }

            val todaysEvents =
                todaysEntries.map { entry ->
                  val item = itemsMap[entry.prePlannedMealId]
                  val restaurant = restaurantsMap[entry.restaurantId]

                  val title = item?.name ?: restaurant?.name ?: "Unknown Meal"

                  val warnings =
                      item?.let { foodItem ->
                        DataQualityValidator.validateFoodItem(
                            foodItem,
                            itemsMap,
                            packages,
                            bridges,
                            allUnits,
                        )
                      } ?: emptyList()

                  CalendarEvent(
                      entryId = entry.id,
                      title = title,
                      mealType = entry.mealType,
                      peopleCount = entry.peopleCount,
                      isConsumed = entry.isConsumed,
                      warnings = warnings,
                  )
                }

            val nextMeal = todaysEvents.firstOrNull { !it.isConsumed }
            val allWarnings = todaysEvents.flatMap { event -> event.warnings }.distinctBy { it.message }

            val pantryCount = pantry.size
            val shoppingCount = shoppingItems.count { !it.isPurchased }

            val sevenDaysAgo = today.minus(DatePeriod(days = 7))
            val cost =
                trips.filter { it.date >= sevenDaysAgo }.sumOf { it.actualTotalCents.toLong() }

            DashboardUiState(
                todaysMeals = todaysEvents,
                pantryItemCount = pantryCount,
                shoppingListCount = shoppingCount,
                nextMeal = nextMeal,
                dashboardConfig = config,
                currentWeekCost = cost,
                warnings = allWarnings,
            )
          }
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5000),
              DashboardUiState(emptyList(), 0, 0, null, DashboardConfig()),
          )

  fun toggleMealConsumption(entryId: Uuid, currentStatus: Boolean) {
    viewModelScope.launch {
        if (!currentStatus) {
            val consumeMealUseCase = ConsumeMealUseCase(
                mealPlanRepository,
                foodItemRepository,
                pantryRepository,
                unitRepository
            )
            consumeMealUseCase(entryId)
        } else {
            mealPlanRepository.setConsumedStatus(entryId, false)
        }
    }
  }
}
