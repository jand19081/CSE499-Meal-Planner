package io.github.and19081.mealplanner.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.core.util.DataQualityValidator
import io.github.and19081.mealplanner.core.util.DataWarning
import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.domain.logic.CommitConsumptionUseCase
import io.github.and19081.mealplanner.domain.logic.CookingTimeCalculator
import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.PurchaseOption
import io.github.and19081.mealplanner.domain.model.isMeal
import io.github.and19081.mealplanner.domain.model.isRecipe
import io.github.and19081.mealplanner.domain.model.recipeInfo
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.repository.MealPlanRepository
import io.github.and19081.mealplanner.domain.repository.PantryRepository
import io.github.and19081.mealplanner.domain.repository.RestaurantRepository
import io.github.and19081.mealplanner.domain.repository.SettingsRepository
import io.github.and19081.mealplanner.feature.kitchen.ConsumptionResult
import io.github.and19081.mealplanner.notification.MealNotificationScheduler
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class CalendarUiState(
    val currentMonth: LocalDate,
    val dates: List<DateUiModel>,
    val weekDates: List<DateUiModel>,
    val focusedDate: LocalDate = currentMonth,
    val availableMeals: List<FoodItem> = emptyList(),
    val allItems: List<FoodItem> = emptyList(),
    val allUnits: List<UnitModel> = emptyList(),
    val allRestaurants: List<io.github.and19081.mealplanner.feature.meals.Restaurant> = emptyList(),
    val allEntries: List<io.github.and19081.mealplanner.feature.meals.ScheduledMeal> = emptyList(),
    val errorMessage: String? = null,
    val warnings: Map<Uuid, List<DataWarning>> = emptyMap(),
) {
  data class DateUiModel(
      val date: LocalDate,
      val isCurrentMonth: Boolean,
      val isToday: Boolean,
      val isSelected: Boolean,
      val events: List<CalendarEvent> = emptyList(),
  )
}

data class CalendarEvent(
    val entryId: Uuid,
    val mealType: RecipeMealType,
    val title: String,
    val time: kotlinx.datetime.LocalTime,
    val peopleCount: Int,
    val isConsumed: Boolean,
    val warnings: List<DataWarning> = emptyList(),
)

object CalendarDataSource {
  fun getDates(referenceDate: LocalDate): List<LocalDate> {
    val firstDayOfMonth = LocalDate(referenceDate.year, referenceDate.month, 1)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek
    val daysToSubtract = startDayOfWeek.ordinal
    val startOfGrid = firstDayOfMonth.minus(daysToSubtract, DateTimeUnit.DAY)

    val dates = mutableListOf<LocalDate>()
    for (i in 0 until 42) {
      dates.add(startOfGrid.plus(i, DateTimeUnit.DAY))
    }
    return dates
  }
}

class CalendarViewModel(
    currentMonthFlow: StateFlow<LocalDate>,
    private val mealPlanRepository: MealPlanRepository,
    private val foodItemRepository: FoodItemRepository,
    private val pantryRepository: PantryRepository,
    private val unitRepository: UnitRepository,
    private val restaurantRepository: RestaurantRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationScheduler: MealNotificationScheduler,
    private val commitConsumptionUseCase: CommitConsumptionUseCase,
) : ViewModel() {
  private val _selectedDate = MutableStateFlow<LocalDate?>(null)
  private val _errorMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<CalendarUiState> =
      combine(
              currentMonthFlow,
              _selectedDate,
              mealPlanRepository.entries,
              foodItemRepository.foodItems,
              foodItemRepository.purchaseOptions,
              foodItemRepository.conversions,
              unitRepository.units,
              restaurantRepository.restaurants,
              _errorMessage,
          ) { args: Array<Any?> ->
            val currentMonth = args[0] as LocalDate
            val selectedDate = args[1] as LocalDate?
            val entries =
                args[2] as List<io.github.and19081.mealplanner.feature.meals.ScheduledMeal>
            val allItems = args[3] as List<FoodItem>
            val purchaseOptions = args[4] as List<PurchaseOption>
            val bridges = args[5] as List<BridgeConversion>
            val allUnits = args[6] as List<UnitModel>
            val restaurants =
                args[7] as List<io.github.and19081.mealplanner.feature.meals.Restaurant>
            val error = args[8] as String?

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val gridReference = selectedDate ?: currentMonth
            val dateList = CalendarDataSource.getDates(gridReference)

            val entriesByDate = entries.groupBy { it.date }
            val itemsById = allItems.associateBy { it.id }
            val restaurantsById = restaurants.associateBy { it.id }

            val availableMeals = allItems.filter { it.isRecipe() || it.isMeal() }

            val mealWarnings = availableMeals.associate { meal ->
              meal.id to
                  DataQualityValidator.validateFoodItem(
                      meal,
                      itemsById,
                      purchaseOptions,
                      bridges,
                      allUnits,
                  )
            }

            val dateUiModels = dateList.map { date ->
              val daysEntries = entriesByDate[date] ?: emptyList()

              val resolvedEvents = daysEntries.map { entry ->
                val mealName =
                    entry.prePlannedMealId?.let { itemsById[it]?.name }
                        ?: entry.restaurantId?.let { restaurantsById[it]?.name }
                        ?: "Unknown Meal"
                val warnings = entry.prePlannedMealId?.let { mealWarnings[it] } ?: emptyList()

                CalendarEvent(
                    entryId = entry.id,
                    mealType = entry.mealType,
                    title = mealName,
                    time = entry.time,
                    peopleCount = entry.peopleCount,
                    isConsumed = entry.isConsumed,
                    warnings = warnings,
                )
              }

              CalendarUiState.DateUiModel(
                  date = date,
                  isCurrentMonth = date.month == currentMonth.month,
                  isToday = date == today,
                  isSelected = date == selectedDate,
                  events = resolvedEvents,
              )
            }

            val anchor = selectedDate ?: currentMonth
            val anchorDayOfWeek = anchor.dayOfWeek.ordinal
            val weekDates =
                dateUiModels.filter {
                  it.date >= anchor.minus(anchorDayOfWeek, DateTimeUnit.DAY) &&
                      it.date <= anchor.plus(6 - anchorDayOfWeek, DateTimeUnit.DAY)
                }

            CalendarUiState(
                currentMonth = currentMonth,
                dates = dateUiModels,
                weekDates = weekDates,
                focusedDate = anchor,
                availableMeals = availableMeals,
                allItems = allItems,
                allUnits = allUnits,
                allRestaurants = restaurants,
                allEntries = entries,
                errorMessage = error,
                warnings = mealWarnings,
            )
          }
          .flowOn(Dispatchers.Default)
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue =
                  CalendarUiState(
                      currentMonth = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                      dates = emptyList(),
                      weekDates = emptyList(),
                  ),
          )

  fun saveRestaurant(name: String) {
    viewModelScope.launch {
      restaurantRepository.addRestaurant(
          io.github.and19081.mealplanner.feature.meals.Restaurant(id = Uuid.random(), name = name)
      )
    }
  }

  fun selectDate(date: LocalDate) {
    _selectedDate.value = date
    _errorMessage.value = null
  }

  fun clearError() {
    _errorMessage.value = null
  }

  fun addPlan(
      date: LocalDate,
      time: LocalTime,
      meal: FoodItem? = null,
      restaurant: io.github.and19081.mealplanner.feature.meals.Restaurant? = null,
      mealType: RecipeMealType,
      peopleCount: Int,
      anticipatedCostCents: Int? = null,
  ) {
    _errorMessage.value = null
    val source =
        when {
          restaurant != null ->
              io.github.and19081.mealplanner.domain.model.MealSource.Restaurant(
                  restaurant.id,
                  anticipatedCostCents ?: 0,
              )
          meal?.isMeal() == true ->
              io.github.and19081.mealplanner.domain.model.MealSource.PrePlannedMeal(meal.id)
          meal?.isRecipe() == true ->
              io.github.and19081.mealplanner.domain.model.MealSource.StandaloneRecipe(meal.id)
          meal != null ->
              io.github.and19081.mealplanner.domain.model.MealSource.StandaloneIngredient(
                  meal.id,
                  0.0,
                  meal.preferredUnitId ?: Uuid.NIL,
              )
          else -> null
        }
    val newEntry =
        io.github.and19081.mealplanner.feature.meals.ScheduledMeal(
            id = Uuid.random(),
            date = date,
            time = time,
            mealType = mealType,
            source = source,
            peopleCount = peopleCount,
            anticipatedCostCents = anticipatedCostCents,
        )

    viewModelScope.launch {
      mealPlanRepository.addPlan(newEntry)
      val title = meal?.name ?: restaurant?.name ?: "Unknown Meal"
      val delay = settingsRepository.appSettings.value.mealConsumedNotificationDelayMinutes.toLong()
      notificationScheduler.scheduleVerificationNotification(newEntry.id, title, delay)

      // Schedule Start Cooking Reminder if it's a recipe
      if (meal != null && (meal.isRecipe() || meal.isMeal())) {
        val recipeInfo = meal.recipeInfo
        if (recipeInfo != null) {
          val startMillis =
              CookingTimeCalculator.calculateStartCookingTimestampMillis(
                  scheduledDate = date,
                  scheduledTime = time,
                  prepTimeMinutes = recipeInfo.prepTimeMinutes ?: 0,
                  cookTimeMinutes = recipeInfo.cookTimeMinutes ?: 0,
              )
          notificationScheduler.scheduleStartCookingNotification(newEntry.id, title, startMillis)
        }
      }
    }
  }

  fun commitConsumptionResult(result: ConsumptionResult) {
    viewModelScope.launch {
      try {
        commitConsumptionUseCase(result)
      } catch (e: Exception) {
        _errorMessage.value = e.message ?: "Failed to record consumption"
      }
    }
  }

  fun updatePlan(entry: io.github.and19081.mealplanner.feature.meals.ScheduledMeal) {
    _errorMessage.value = null
    viewModelScope.launch { mealPlanRepository.addPlan(entry) }
  }

  fun removePlan(entryId: Uuid) {
    viewModelScope.launch {
      mealPlanRepository.removePlan(entryId)
      notificationScheduler.cancelNotification(entryId)
    }
  }

  fun toggleMealConsumption(entryId: Uuid) {
    viewModelScope.launch {
      val entry = mealPlanRepository.entries.value.find { it.id == entryId } ?: return@launch
      val newStatus = !entry.isConsumed

      if (entry.restaurantId != null && newStatus) {
        return@launch
      }

      if (newStatus) {
        mealPlanRepository.setConsumedStatus(entryId, true)
      } else {
        mealPlanRepository.setConsumedStatus(entryId, false)
      }
    }
  }
}
