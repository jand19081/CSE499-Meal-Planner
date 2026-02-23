package io.github.and19081.mealplanner.dependencyinjection

import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.repository.*
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.StoreRepository
import io.github.and19081.mealplanner.meals.MealRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import io.github.and19081.mealplanner.settings.SettingsRepository
import io.github.and19081.mealplanner.shoppinglist.ShoppingListRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DependencyInjectionContainer(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope
) {
    val settingsRepository: SettingsRepository = RoomSettingsRepository(db, scope)
    val unitRepository: UnitRepository = RoomUnitRepository(db, scope)
    val storeRepository: StoreRepository = RoomStoreRepository(db, scope)
    val ingredientRepository: IngredientRepository = RoomIngredientRepository(db, scope)
    val recipeRepository: RecipeRepository = RoomRecipeRepository(db, scope)
    val mealRepository: MealRepository = RoomMealRepository(db, scope)
    val mealPlanRepository: MealPlanRepository = RoomMealPlanRepository(db, scope)
    val pantryRepository: PantryRepository = RoomPantryRepository(db, scope)
    val shoppingListRepository = ShoppingListRepository() 
    val shoppingListItemRepository: ShoppingListItemRepository = RoomShoppingListItemRepository(db, scope)
    val receiptHistoryRepository: ReceiptHistoryRepository = RoomReceiptHistoryRepository(db, scope)
    val restaurantRepository: io.github.and19081.mealplanner.ingredients.RestaurantRepository = RoomRestaurantRepository(db, scope)

    init {
        // Initialize Mock Data asynchronously but sequentially within the launch block
        scope.launch {
            MockData.initialize(
                unitRepository = unitRepository,
                storeRepository = storeRepository,
                ingredientRepository = ingredientRepository,
                recipeRepository = recipeRepository,
                mealRepository = mealRepository,
                mealPlanRepository = mealPlanRepository,
                restaurantRepository = restaurantRepository
            )
        }
    }
}
