package io.github.and19081.mealplanner.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.DataQualityValidator
import io.github.and19081.mealplanner.domain.DataWarning
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.kitchen.*
import io.github.and19081.mealplanner.meals.MealRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

// The UI State
data class CalendarUiState(
    val currentMonth: LocalDate,
    val dates: List<DateUiModel>,
    val weekDates: List<DateUiModel>,
    val availableMeals: List<PrePlannedMeal> = emptyList(),
    val allRecipes: List<Recipe> = emptyList(),
    val allIngredients: List<Ingredient> = emptyList(),
    val allUnits: List<UnitModel> = emptyList(),
    val allRestaurants: List<Restaurant> = emptyList(), // Added
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

// The Logic to generate the grid
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

// The ViewModel
class CalendarViewModel(
    currentMonthFlow: StateFlow<LocalDate>,
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository,
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val pantryRepository: PantryRepository,
    private val leftoverRepository: LeftoverRepository,
    private val unitRepository: UnitRepository,
    private val restaurantRepository:
        io.github.and19081.mealplanner.ingredients.RestaurantRepository,
    private val notificationScheduler: io.github.and19081.mealplanner.notifications.MealNotificationScheduler,
) : ViewModel() {
  private val _selectedDate = MutableStateFlow<LocalDate?>(null)
  private val _errorMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<CalendarUiState> =
      combine(
              listOf(
                  currentMonthFlow,
                  _selectedDate,
                  mealPlanRepository.entries,
                  mealRepository.meals,
                  recipeRepository.recipes,
                  ingredientRepository.ingredients,
                  ingredientRepository.packages,
                  ingredientRepository.bridges,
                  unitRepository.units,
                  restaurantRepository.restaurants,
                  _errorMessage,
              )
          ) { array ->
            val currentMonth = array[0] as LocalDate
            val selectedDate = array[1] as LocalDate?
            val entries = array[2] as List<ScheduledMeal>
            val meals = array[3] as List<PrePlannedMeal>
            val recipes = array[4] as List<Recipe>
            val ingredients = array[5] as List<Ingredient>
            val packages = array[6] as List<Package>
            val bridges = array[7] as List<BridgeConversion>
            val allUnits = array[8] as List<UnitModel>
            val restaurants = array[9] as List<Restaurant>
            val error = array[10] as String?

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // Use selectedDate as reference for the month grid if it's available
            // This ensures that when we switch views, the selected date is in the generated models.
            val gridReference = selectedDate ?: currentMonth
            val dateList = CalendarDataSource.getDates(gridReference)

            val entriesByDate = entries.groupBy { it.date }
            val mealsById = meals.associateBy { it.id }
            val recipesById = recipes.associateBy { it.id }
            val ingredientsById = ingredients.associateBy { it.id }
            val restaurantsById = restaurants.associateBy { it.id }

            // Pre-calculate warnings for all meals
            val mealWarnings =
                meals.associate { meal ->
                  meal.id to
                      DataQualityValidator.validateMeal(
                          meal,
                          recipesById,
                          ingredientsById,
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
                            entry.prePlannedMealId?.let { mealsById[it]?.name }
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
                availableMeals = meals,
                allRecipes = recipes,
                allIngredients = ingredients,
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
                      Clock.System.todayIn(TimeZone.currentSystemDefault()),
                      emptyList(),
                      emptyList(),
                      emptyList(),
                      emptyList(),
                      emptyList(),
                      emptyList(),
                      emptyList(),
                      emptyList(),
                      null,
                      emptyMap(),
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
      meal: PrePlannedMeal? = null,
      restaurant: Restaurant? = null,
      mealType: RecipeMealType,
      peopleCount: Int,
      anticipatedCostCents: Int? = null,
  ) {
    _errorMessage.value = null
    val newEntry =
        ScheduledMeal(
            date = date,
            time = time,
            mealType = mealType,
            peopleCount = peopleCount,
            prePlannedMealId = meal?.id,
            restaurantId = restaurant?.id,
            anticipatedCostCents = anticipatedCostCents,
            id = Uuid.random(),
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

  fun consumeMeal(
      entryId: Uuid,
      totalCents: Int? = null,
      taxCents: Int? = null,
      lineItems: List<Triple<String, Double, Int>> = emptyList(),
      leftovers: List<Pair<Uuid, Double>> = emptyList(),
  ) {
    viewModelScope.launch {
      if (totalCents != null && taxCents != null) {
        mealPlanRepository.addReceipt(entryId, totalCents, taxCents, lineItems)
      }

      val todayStr = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
      leftovers.forEach { (recipeId, servings) ->
        if (servings > 0) {
          leftoverRepository.addLeftover(recipeId, servings, todayStr)
        }
      }

      mealPlanRepository.setConsumedStatus(entryId, true)
    }
  }

  fun createConsumptionTransaction(entry: ScheduledMeal): KitchenTransaction? {
    val meal = mealRepository.meals.value.find { it.id == entry.prePlannedMealId } ?: return null
    val allRecipes = recipeRepository.recipes.value
    val allIngredients = ingredientRepository.ingredients.value
    val allUnits = unitRepository.units.value
    val ingredientsMap = allIngredients.associateBy { it.id }

    val changes = mutableListOf<InventoryChange>()

    // Helper to add changes
    fun addChange(ingId: Uuid, qty: Double, unitId: Uuid?) {
      val name = ingredientsMap[ingId]?.name ?: "Unknown"
      val unit = allUnits.find { it.id == unitId }
      changes.add(
          InventoryChange(
              ingredientId = ingId,
              ingredientName = name,
              quantity = qty,
              unitId = unitId,
              unitAbbreviation = unit?.abbreviation ?: "?",
              direction = TransactionDirection.OUT
          )
      )
    }

    // Recipes
    meal.recipes.forEach { rId ->
      val recipe = allRecipes.find { it.id == rId }
      if (recipe != null) {
        val servingsPerBatch = if (recipe.servings > 0) recipe.servings else 1.0
        val batchesCooked = max(1.0, ceil(entry.peopleCount / servingsPerBatch))
        recipe.ingredients.forEach { ri ->
          if (ri.ingredientId != null) {
            addChange(ri.ingredientId, ri.quantity * batchesCooked, ri.unitId)
          }
        }
      }
    }

    // Independent Ingredients
    meal.independentIngredients.forEach { item ->
      addChange(item.ingredientId, item.quantity * entry.peopleCount, item.unitId)
    }

    return KitchenTransaction(
        type = TransactionType.Consumption,
        title = "Consuming: ${meal.name}",
        changes = changes
    )
  }

  fun commitTransaction(entryId: Uuid, transaction: KitchenTransaction, leftovers: List<Pair<Uuid, Double>> = emptyList()) {
    viewModelScope.launch {
      val allUnits = unitRepository.units.value
      val allIngredients = ingredientRepository.ingredients.value
      val allBridges = ingredientRepository.bridges.value
      val pantryItems = pantryRepository.pantryItems.value

      // 1. Process Changes
      transaction.changes.forEach { change ->
        val ingredient = allIngredients.find { it.id == change.ingredientId }
        val unit = allUnits.find { it.id == change.unitId }

        if (ingredient != null && unit != null) {
          val preferredUnitId = ingredient.preferredUnitId
          val targetUnitId = preferredUnitId ?: when (unit.type) {
              UnitType.Weight -> SystemUnits.Gram.id
              UnitType.Volume -> SystemUnits.Ml.id
              UnitType.Count -> SystemUnits.Each.id
              else -> unit.id
          }

          val targetUnit = allUnits.find { it.id == targetUnitId }

          if (targetUnit != null) {
            val bridges = allBridges.filter { it.ingredientId == change.ingredientId }
            val changeInTargetUnit = UnitConverter.convert(
                amount = change.quantity,
                fromUnitId = change.unitId,
                toUnitId = targetUnitId,
                allUnits = allUnits.associateBy { it.id },
                bridges = bridges
            ) ?: 0.0

            val currentPantryItem = pantryItems.find { it.ingredientId == change.ingredientId }
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
            pantryRepository.updateQuantity(change.ingredientId, newQty, targetUnitId)
          }
        }
      }

      // 2. Process Leftovers
      val todayStr = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
      leftovers.forEach { (recipeId, servings) ->
        if (servings > 0) {
          leftoverRepository.addLeftover(recipeId, servings, todayStr)
        }
      }

      // 3. Mark as Consumed
      mealPlanRepository.setConsumedStatus(entryId, true)
    }
  }

  fun toggleMealConsumption(entryId: Uuid) {
    val entry = mealPlanRepository.entries.value.find { it.id == entryId } ?: return
    val newStatus = !entry.isConsumed

    if (entry.restaurantId != null && newStatus) {
      // For restaurants we should show a dialog, handled by UI.
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
}
