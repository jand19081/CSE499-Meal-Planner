package io.github.and19081.mealplanner.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.DataQualityValidator
import io.github.and19081.mealplanner.domain.DataWarning
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import io.github.and19081.mealplanner.meals.MealRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.uuid.Uuid
import kotlin.math.max

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
    val warnings: Map<Uuid, List<DataWarning>> = emptyMap()
) {
    data class DateUiModel(
        val date: LocalDate,
        val isCurrentMonth: Boolean,
        val isToday: Boolean,
        val isSelected: Boolean,
        val events: List<CalendarEvent> = emptyList()
    )
}

data class CalendarEvent(
    val entryId: Uuid,
    val mealType: RecipeMealType,
    val title: String,
    val peopleCount: Int,
    val isConsumed: Boolean,
    val warnings: List<DataWarning> = emptyList()
)

// The Logic to generate the grid
object CalendarDataSource {
    fun getDates(currentMonth: LocalDate, selectedDate: LocalDate?): List<LocalDate> {
        val firstDayOfMonth = LocalDate(currentMonth.year, currentMonth.month, 1)
        val startDayOfWeek = firstDayOfMonth.dayOfWeek
        val daysToSubtract = startDayOfWeek.ordinal
        val startOfGrid = firstDayOfMonth.minus(DatePeriod(days = daysToSubtract))

        val dates = mutableListOf<LocalDate>()
        for (i in 0 until 42) {
            dates.add(startOfGrid.plus(DatePeriod(days = i)))
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
    private val unitRepository: UnitRepository,
    private val restaurantRepository: io.github.and19081.mealplanner.ingredients.RestaurantRepository
) : ViewModel() {
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CalendarUiState> = combine(
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
        _errorMessage
    ) { args: Array<Any?> ->
        val currentMonth = args[0] as LocalDate
        val selectedDate = args[1] as LocalDate?
        val entries = args[2] as List<ScheduledMeal>
        val meals = args[3] as List<PrePlannedMeal>
        val recipes = args[4] as List<Recipe>
        val ingredients = args[5] as List<Ingredient>
        val packages = args[6] as List<Package>
        val bridges = args[7] as List<BridgeConversion>
        val allUnits = args[8] as List<UnitModel>
        val restaurants = args[9] as List<Restaurant>
        val error = args[10] as String?

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val dateList = CalendarDataSource.getDates(currentMonth, selectedDate)

        val entriesByDate = entries.groupBy { it.date }
        val mealsById = meals.associateBy { it.id }
        val recipesById = recipes.associateBy { it.id }
        val ingredientsById = ingredients.associateBy { it.id }
        val restaurantsById = restaurants.associateBy { it.id }

        // Pre-calculate warnings for all meals
        val mealWarnings = meals.associate { meal ->
            meal.id to DataQualityValidator.validateMeal(meal, recipesById, ingredientsById, packages, bridges, allUnits)
        }

        val dateUiModels = dateList.map { date ->
            val daysEntries = entriesByDate[date] ?: emptyList()

            val resolvedEvents = daysEntries.map { entry ->
                val mealName = entry.prePlannedMealId?.let { mealsById[it]?.name }
                    ?: entry.restaurantId?.let { restaurantsById[it]?.name }
                    ?: "Unknown Meal"
                val warnings = entry.prePlannedMealId?.let { mealWarnings[it] } ?: emptyList()

                CalendarEvent(
                    entryId = entry.id,
                    mealType = entry.mealType,
                    title = mealName,
                    peopleCount = entry.peopleCount,
                    isConsumed = entry.isConsumed,
                    warnings = warnings
                )
            }

            CalendarUiState.DateUiModel(
                date = date,
                isCurrentMonth = date.month == currentMonth.month,
                isToday = date == today,
                isSelected = date == selectedDate,
                events = resolvedEvents
            )
        }

        val weekDates = if (selectedDate != null) {
            val dayOfWeek = selectedDate.dayOfWeek.ordinal
            dateUiModels.filter {
                it.date >= selectedDate.minus(DatePeriod(days = dayOfWeek)) &&
                        it.date <= selectedDate.plus(DatePeriod(days = 6 - dayOfWeek))
            }
        } else {
            val todayDayOfWeek = today.dayOfWeek.ordinal
            dateUiModels.filter {
                it.date >= today.minus(DatePeriod(days = todayDayOfWeek)) &&
                        it.date <= today.plus(DatePeriod(days = 6 - todayDayOfWeek))
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
            warnings = mealWarnings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(Clock.System.todayIn(TimeZone.currentSystemDefault()), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), null, emptyMap())
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
        anticipatedCostCents: Int? = null
    ) {
        _errorMessage.value = null
        val newEntry = ScheduledMeal(
            date = date,
            time = time,
            mealType = mealType,
            peopleCount = peopleCount,
            prePlannedMealId = meal?.id,
            restaurantId = restaurant?.id,
            anticipatedCostCents = anticipatedCostCents,
            id = Uuid.random()
        )

        viewModelScope.launch {
            mealPlanRepository.addPlan(newEntry)
        }
    }

    fun updatePlan(entry: ScheduledMeal) {
        _errorMessage.value = null
        viewModelScope.launch {
            mealPlanRepository.addPlan(entry)
        }
    }

    fun removePlan(entryId: Uuid) {
        viewModelScope.launch {
            mealPlanRepository.removePlan(entryId)
        }
    }

    fun consumeRestaurantMeal(
        entryId: Uuid,
        totalCents: Int,
        taxCents: Int,
        lineItems: List<Triple<String, Double, Int>>
    ) {
        viewModelScope.launch {
            mealPlanRepository.addReceipt(entryId, totalCents, taxCents, lineItems)
        }
    }

    fun consumeMeal(entryId: Uuid) {
        val entry = mealPlanRepository.entries.value.find { it.id == entryId } ?: return
        if (entry.isConsumed) return

        if (entry.restaurantId != null) {
            // For restaurants we should show a dialog, but this method is called directly.
            // I'll leave this to be handled by the UI check.
            return
        }

        viewModelScope.launch {
            // Mark as consumed
            mealPlanRepository.markConsumed(entryId)

            // Decrement Pantry Logic
            val meal = mealRepository.meals.value.find { it.id == entry.prePlannedMealId } ?: return@launch
            
            val allRecipes = recipeRepository.recipes.value
            val allIngredients = ingredientRepository.ingredients.value
            val allBridges = ingredientRepository.bridges.value
            val allUnits = unitRepository.units.value
            
            // Helper to decrement
            suspend fun decrement(ingId: Uuid, qtyUsed: Double, unitId: Uuid) {
                // Get Current Pantry
                val currentPantryItem = pantryRepository.pantryItems.value.find { it.ingredientId == ingId }
                if (currentPantryItem == null) return

                val ingredient = allIngredients.find { it.id == ingId }
                val bridges = allBridges.filter { it.ingredientId == ingId }

                // Convert used qty to pantry unit
                val usedInPantryUnit = UnitConverter.convert(
                    amount = qtyUsed,
                    fromUnitId = unitId,
                    toUnitId = currentPantryItem.unitId,
                    allUnits = allUnits,
                    bridges = bridges
                )
                
                val newQty = max(0.0, currentPantryItem.quantity - usedInPantryUnit)
                
                pantryRepository.updateQuantity(ingId, newQty, currentPantryItem.unitId)
            }

            // Recursive Recipe Decrementer
            suspend fun decrementRecipe(recipe: Recipe, scale: Double) {
                recipe.ingredients.forEach { ri ->
                    if (ri.subRecipeId != null) {
                        val subRecipe = allRecipes.find { it.id == ri.subRecipeId }
                        if (subRecipe != null) {
                            decrementRecipe(subRecipe, scale * ri.quantity)
                        }
                    } else if (ri.ingredientId != null) {
                        decrement(ri.ingredientId, ri.quantity * scale, ri.unitId)
                    }
                }
            }

            // Recipes
            meal.recipes.forEach { rId ->
                val recipe = allRecipes.find { it.id == rId }
                if (recipe != null) {
                    val scale = if (recipe.servings > 0) entry.peopleCount / recipe.servings else 1.0
                    decrementRecipe(recipe, scale)
                }
            }
            
            // Independent Ingredients
            meal.independentIngredients.forEach { item ->
                val totalQty = item.quantity * entry.peopleCount
                decrement(item.ingredientId, totalQty, item.unitId)
            }
        }
    }
}
