package io.github.and19081.mealplanner.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.*
import io.github.and19081.mealplanner.kitchen.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class CalendarUiState(
    val currentMonth: LocalDate,
    val dates: List<DateUiModel>,
    val weekDates: List<DateUiModel>,
    val availableMeals: List<FoodItem> = emptyList(),
    val allItems: List<FoodItem> = emptyList(),
    val allUnits: List<UnitModel> = emptyList(),
    val allRestaurants: List<Restaurant> = emptyList(),
    val allEntries: List<ScheduledMeal> = emptyList(),
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
    private val notificationScheduler: io.github.and19081.mealplanner.notifications.MealNotificationScheduler,
) : ViewModel() {
  private val _selectedDate = MutableStateFlow<LocalDate?>(null)
  private val _errorMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<CalendarUiState> =
      combine(
              currentMonthFlow,
              _selectedDate,
              mealPlanRepository.entries,
              foodItemRepository.foodItems,
              foodItemRepository.packages,
              foodItemRepository.conversions,
              unitRepository.units,
              restaurantRepository.restaurants,
              _errorMessage,
          ) { args: Array<Any?> ->
            val currentMonth = args[0] as LocalDate
            val selectedDate = args[1] as LocalDate?
            val entries = args[2] as List<ScheduledMeal>
            val allItems = args[3] as List<FoodItem>
            val packages = args[4] as List<Package>
            val bridges = args[5] as List<BridgeConversion>
            val allUnits = args[6] as List<UnitModel>
            val restaurants = args[7] as List<Restaurant>
            val error = args[8] as String?

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val gridReference = selectedDate ?: currentMonth
            val dateList = CalendarDataSource.getDates(gridReference)

            val entriesByDate = entries.groupBy { it.date }
            val itemsById = allItems.associateBy { it.id }
            val restaurantsById = restaurants.associateBy { it.id }

            val availableMeals = allItems.filter { it.isRecipe }

            val mealWarnings =
                availableMeals.associate { meal ->
                  meal.id to
                      DataQualityValidator.validateFoodItem(
                          meal,
                          itemsById,
                          packages,
                          bridges,
                          allUnits,
                      )
                }

            val dateUiModels =
                dateList.map { date ->
                  val daysEntries = entriesByDate[date] ?: emptyList()

                  val resolvedEvents =
                      daysEntries.map { entry ->
                        val mealName =
                            entry.prePlannedMealId?.let { itemsById[it]?.name }
                                ?: entry.restaurantId?.let { restaurantsById[it]?.name }
                                ?: "Unknown Meal"
                        val warnings =
                            entry.prePlannedMealId?.let { mealWarnings[it] } ?: emptyList()

                        CalendarEvent(
                            entryId = entry.id,
                            mealType = entry.mealType,
                            title = mealName,
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

            val weekDates =
                if (selectedDate != null) {
                  val dayOfWeek = selectedDate.dayOfWeek.ordinal
                  dateUiModels.filter {
                    it.date >= selectedDate.minus(dayOfWeek, DateTimeUnit.DAY) &&
                        it.date <= selectedDate.plus(6 - dayOfWeek, DateTimeUnit.DAY)
                  }
                } else {
                  val todayDayOfWeek = today.dayOfWeek.ordinal
                  dateUiModels.filter {
                    it.date >= today.minus(todayDayOfWeek, DateTimeUnit.DAY) &&
                        it.date <= today.plus(6 - todayDayOfWeek, DateTimeUnit.DAY)
                  }
                }

            CalendarUiState(
                currentMonth = currentMonth,
                dates = dateUiModels,
                weekDates = weekDates,
                availableMeals = availableMeals,
                allItems = allItems,
                allUnits = allUnits,
                allRestaurants = restaurants,
                allEntries = entries,
                errorMessage = error,
                warnings = mealWarnings,
            )
          }
          .flowOn(kotlinx.coroutines.Dispatchers.Default)
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
      restaurantRepository.addRestaurant(Restaurant(id = Uuid.random(), name = name))
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
      restaurant: Restaurant? = null,
      mealType: RecipeMealType,
      peopleCount: Int,
      anticipatedCostCents: Int? = null,
  ) {
    _errorMessage.value = null
    val newEntry =
        ScheduledMeal(
            id = Uuid.random(),
            date = date,
            time = time,
            mealType = mealType,
            prePlannedMealId = meal?.id,
            restaurantId = restaurant?.id,
            peopleCount = peopleCount,
            anticipatedCostCents = anticipatedCostCents,
        )

    viewModelScope.launch { 
        mealPlanRepository.addPlan(newEntry) 
        val title = meal?.name ?: restaurant?.name ?: "Unknown Meal"
        notificationScheduler.scheduleMealNotification(newEntry, title)
    }
  }

  fun updatePlan(entry: ScheduledMeal) {
    _errorMessage.value = null
    viewModelScope.launch { mealPlanRepository.addPlan(entry) }
  }

  fun removePlan(entryId: Uuid) {
    viewModelScope.launch { 
        mealPlanRepository.removePlan(entryId) 
        notificationScheduler.cancelMealNotification(entryId)
    }
  }

  fun toggleMealConsumption(entryId: Uuid) {
    val entry = mealPlanRepository.entries.value.find { it.id == entryId } ?: return
    val newStatus = !entry.isConsumed

    if (entry.restaurantId != null && newStatus) {
      return
    }

    if (newStatus) {
      val transaction = createConsumptionTransaction(entry)
      if (transaction != null) {
          commitTransaction(entryId, transaction)
      } else {
          viewModelScope.launch { mealPlanRepository.setConsumedStatus(entryId, true) }
      }
    } else {
      viewModelScope.launch { mealPlanRepository.setConsumedStatus(entryId, false) }
    }
  }

  fun createConsumptionTransaction(entry: ScheduledMeal): KitchenTransaction? {
    val allItems = foodItemRepository.foodItems.value
    val itemsMap = allItems.associateBy { it.id }
    val meal = itemsMap[entry.prePlannedMealId] ?: return null
    val recipeInfo = meal.recipeInfo ?: return null
    
    val allUnits = unitRepository.units.value
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

  fun commitTransaction(entryId: Uuid, transaction: KitchenTransaction, leftovers: List<Pair<Uuid, Double>> = emptyList()) {
    viewModelScope.launch {
      val allUnits = unitRepository.units.value
      val allItems = foodItemRepository.foodItems.value
      val allBridges = foodItemRepository.conversions.value
      val pantryItems = pantryRepository.pantryItems.value

      transaction.changes.forEach { change ->
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

          val newQty = if (change.direction == TransactionDirection.IN) {
              currentQtyInTargetUnit + changeInTargetUnit
          } else {
              max(0.0, currentQtyInTargetUnit - changeInTargetUnit)
          }
          pantryRepository.updateQuantity(change.foodItemId, newQty, targetUnitId)
        }
      }

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

      mealPlanRepository.setConsumedStatus(entryId, true)
    }
  }
}
