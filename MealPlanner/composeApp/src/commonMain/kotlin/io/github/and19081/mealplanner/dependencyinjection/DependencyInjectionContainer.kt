package io.github.and19081.mealplanner.dependencyinjection

import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.repository.*
import io.github.and19081.mealplanner.domain.FoodItemRepository
import io.github.and19081.mealplanner.ingredients.StoreRepository
import io.github.and19081.mealplanner.settings.SettingsRepository
import io.github.and19081.mealplanner.shoppinglist.ShoppingListRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DependencyInjectionContainer(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) {
  val settingsRepository: SettingsRepository = RoomSettingsRepository(db, scope)
  val unitRepository: UnitRepository = RoomUnitRepository(db, scope)
  val storeRepository: StoreRepository = RoomStoreRepository(db, scope)
  val restaurantRepository: RestaurantRepository = RoomRestaurantRepository(db, scope)
  
  // ECS Unified Repository
  val foodItemRepository: FoodItemRepository = RoomFoodItemRepository(db, scope)
  
  val mealPlanRepository: MealPlanRepository = RoomMealPlanRepository(db, scope)
  val pantryRepository: PantryRepository = RoomPantryRepository(db, scope)
  val shoppingListRepository = ShoppingListRepository()
  val shoppingListItemRepository: ShoppingListItemRepository =
      RoomShoppingListItemRepository(db, scope)
  val receiptHistoryRepository: ReceiptHistoryRepository = RoomReceiptHistoryRepository(db, scope)
  val notificationScheduler: io.github.and19081.mealplanner.notifications.MealNotificationScheduler =
      io.github.and19081.mealplanner.notifications.createNotificationScheduler()

  val viewModelFactory = ViewModelFactory(this)

  suspend fun initializeMockData() {
    MockData.initialize(
        unitRepository,
        storeRepository,
        foodItemRepository,
        mealPlanRepository,
        restaurantRepository
    )
  }
}
