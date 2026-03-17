package io.github.and19081.mealplanner.core.di

import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.notification.MealNotificationScheduler
import io.github.and19081.mealplanner.domain.repository.MealPlanRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.core.util.MockData
import io.github.and19081.mealplanner.domain.repository.PantryRepository
import io.github.and19081.mealplanner.domain.repository.ReceiptHistoryRepository
import io.github.and19081.mealplanner.domain.repository.RestaurantRepository
import io.github.and19081.mealplanner.data.repository.RoomFoodItemRepository
import io.github.and19081.mealplanner.data.repository.RoomMealPlanRepository
import io.github.and19081.mealplanner.data.repository.RoomPantryRepository
import io.github.and19081.mealplanner.data.repository.RoomReceiptHistoryRepository
import io.github.and19081.mealplanner.data.repository.RoomRestaurantRepository
import io.github.and19081.mealplanner.data.repository.RoomSettingsRepository
import io.github.and19081.mealplanner.data.repository.RoomShoppingListItemRepository
import io.github.and19081.mealplanner.data.repository.RoomStoreRepository
import io.github.and19081.mealplanner.data.repository.RoomUnitRepository
import io.github.and19081.mealplanner.domain.repository.SettingsRepository
import io.github.and19081.mealplanner.domain.repository.ShoppingListItemRepository
import io.github.and19081.mealplanner.domain.repository.ShoppingListRepository
import io.github.and19081.mealplanner.domain.repository.StoreRepository
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.notification.createNotificationScheduler
import kotlinx.coroutines.CoroutineScope

class DependencyInjectionContainer(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
    private val platformContext: Any? = null,
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
  val notificationScheduler: MealNotificationScheduler =
      createNotificationScheduler(platformContext)

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
