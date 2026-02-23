package io.github.and19081.mealplanner.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.DataQualityValidator
import io.github.and19081.mealplanner.domain.DataWarning
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.meals.MealRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.uuid.Uuid
import io.github.and19081.mealplanner.calendar.CalendarEvent
import io.github.and19081.mealplanner.settings.DashboardConfig
import io.github.and19081.mealplanner.settings.SettingsRepository

class DashboardViewModel(
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository,
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val pantryRepository: PantryRepository,
    private val unitRepository: UnitRepository,
    private val settingsRepository: SettingsRepository,
    private val receiptHistoryRepository: ReceiptHistoryRepository,
    private val shoppingListItemRepository: ShoppingListItemRepository,
    private val restaurantRepository: io.github.and19081.mealplanner.ingredients.RestaurantRepository
) : ViewModel() {

    data class DashboardUiState(
        val todaysMeals: List<CalendarEvent>,
        val lowStockCount: Int,
        val shoppingListCount: Int,
        val nextMeal: CalendarEvent?,
        val dashboardConfig: DashboardConfig,
        val currentWeekCost: Long = 0L,
        val warnings: List<DataWarning> = emptyList()
    )

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    val uiState = combine(
        mealPlanRepository.entries,
        mealRepository.meals,
        recipeRepository.recipes,
        ingredientRepository.ingredients,
        ingredientRepository.packages,
        ingredientRepository.bridges,
        unitRepository.units,
        pantryRepository.pantryItems,
        settingsRepository.dashboardConfig,
        receiptHistoryRepository.trips,
        shoppingListItemRepository.items,
        restaurantRepository.restaurants
    ) { args: Array<Any> ->
        val entries = args[0] as List<ScheduledMeal>
        val meals = args[1] as List<PrePlannedMeal>
        val recipes = args[2] as List<Recipe>
        val ingredients = args[3] as List<Ingredient>
        val packages = args[4] as List<io.github.and19081.mealplanner.ingredients.Package>
        val bridges = args[5] as List<io.github.and19081.mealplanner.ingredients.BridgeConversion>
        val allUnits = args[6] as List<UnitModel>
        val pantry = args[7] as List<PantryItem>
        val config = args[8] as DashboardConfig
        val trips = args[9] as List<io.github.and19081.mealplanner.shoppinglist.ReceiptHistory>
        val shoppingItems = args[10] as List<io.github.and19081.mealplanner.shoppinglist.ShoppingListItem>
        val restaurants = args[11] as List<Restaurant>
        
        val mealsMap = meals.associateBy { it.id }
        val recipesMap = recipes.associateBy { it.id }
        val ingredientsMap = ingredients.associateBy { it.id }
        val restaurantsMap = restaurants.associateBy { it.id }

        // 1. Today's Meals
        val todaysEntries = entries.filter { it.date == today }
            .sortedBy { it.mealType.ordinal }
            
        val todaysEvents = todaysEntries.map { entry ->
            val meal = mealsMap[entry.prePlannedMealId]
            val restaurant = restaurantsMap[entry.restaurantId]
            
            val title = meal?.name ?: restaurant?.name ?: "Unknown Meal"
            
            val warnings = meal?.let { DataQualityValidator.validateMeal(it, recipesMap, ingredientsMap, packages, bridges, allUnits) } ?: emptyList()

            CalendarEvent(
                entryId = entry.id,
                title = title,
                mealType = entry.mealType,
                peopleCount = entry.peopleCount,
                isConsumed = entry.isConsumed,
                warnings = warnings
            )
        }

        val nextMeal = todaysEvents.firstOrNull { !it.isConsumed }
        val allWarnings = todaysEvents.flatMap { it.warnings }.distinctBy { it.message }

        // 2. Low Stock
        val stockCount = pantry.size

        // 3. Shopping List Count
        val shoppingCount = shoppingItems.count { !it.isPurchased }
        
        // 4. Weekly Cost (Sum of actual totals in cents)
        val cost = trips.sumOf { it.actualTotalCents.toLong() }

        DashboardUiState(
            todaysMeals = todaysEvents,
            lowStockCount = stockCount,
            shoppingListCount = shoppingCount,
            nextMeal = nextMeal,
            dashboardConfig = config,
            currentWeekCost = cost,
            warnings = allWarnings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState(emptyList(), 0, 0, null, DashboardConfig()))
    
    fun consumeMeal(entryId: Uuid) {
        viewModelScope.launch {
            mealPlanRepository.markConsumed(entryId)
        }
    }
}
