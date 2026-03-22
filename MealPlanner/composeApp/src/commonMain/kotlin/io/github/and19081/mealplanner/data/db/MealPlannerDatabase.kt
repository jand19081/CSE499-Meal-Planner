package io.github.and19081.mealplanner.data.db

import androidx.room.*
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.and19081.mealplanner.data.db.dao.AppSettingsDao
import io.github.and19081.mealplanner.data.db.entity.AppSettingsEntity
import io.github.and19081.mealplanner.data.db.dao.CategoryDao
import io.github.and19081.mealplanner.data.db.entity.CategoryEntity
import io.github.and19081.mealplanner.data.db.dao.FoodItemDao
import io.github.and19081.mealplanner.data.db.entity.FoodItemEntity
import io.github.and19081.mealplanner.data.db.entity.LeftoverComponentEntity
import io.github.and19081.mealplanner.data.db.converters.MealPlannerTypeConverters
import io.github.and19081.mealplanner.data.db.dao.PackageOptionDao
import io.github.and19081.mealplanner.data.db.entity.PackageOptionEntity
import io.github.and19081.mealplanner.data.db.dao.PantryDao
import io.github.and19081.mealplanner.data.db.entity.PantryInventoryEntity
import io.github.and19081.mealplanner.data.db.entity.PurchasableComponentEntity
import io.github.and19081.mealplanner.data.db.dao.ReceiptDao
import io.github.and19081.mealplanner.data.db.entity.ReceiptLineItemEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeComponentEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeInstructionEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeRequirementEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeRequirementGroupEntity
import io.github.and19081.mealplanner.data.db.dao.RestaurantDao
import io.github.and19081.mealplanner.data.db.entity.RestaurantEntity
import io.github.and19081.mealplanner.data.db.dao.ScheduledMealDao
import io.github.and19081.mealplanner.data.db.entity.ScheduledMealEntity
import io.github.and19081.mealplanner.data.db.entity.ShoppingCartItemEntity
import io.github.and19081.mealplanner.data.db.dao.ShoppingListDao
import io.github.and19081.mealplanner.data.db.dao.StoreDao
import io.github.and19081.mealplanner.data.db.entity.StoreEntity
import io.github.and19081.mealplanner.data.db.entity.StoreReceiptEntity
import io.github.and19081.mealplanner.data.db.entity.UnitConversionBridgeEntity
import io.github.and19081.mealplanner.data.db.dao.UnitDao
import io.github.and19081.mealplanner.data.db.entity.UnitEntity

/** Room database for the Meal Planner application with ECS architecture. */
@Database(
    version = 4,
    exportSchema = true,
    entities =
        [
            // Lookup / Reference
            UnitEntity::class,
            CategoryEntity::class,
            StoreEntity::class,
            RestaurantEntity::class,

            // ECS Food Items & Components
            FoodItemEntity::class,
            PurchasableComponentEntity::class,
            RecipeComponentEntity::class,
            LeftoverComponentEntity::class,
            RecipeInstructionEntity::class,

            // Meal planning domain
            ScheduledMealEntity::class,

            // Settings
            AppSettingsEntity::class,

            // Ledger
            StoreReceiptEntity::class,

            // Edges / junction tables
            UnitConversionBridgeEntity::class,
            PackageOptionEntity::class,
            RecipeRequirementGroupEntity::class,
            RecipeRequirementEntity::class,
            PantryInventoryEntity::class,
            ShoppingCartItemEntity::class,
            ReceiptLineItemEntity::class,
        ],
)
@TypeConverters(MealPlannerTypeConverters::class)
@ConstructedBy(MealPlannerDatabaseConstructor::class)
abstract class MealPlannerDatabase : RoomDatabase() {

  abstract fun unitDao(): UnitDao

  abstract fun categoryDao(): CategoryDao

  abstract fun storeDao(): StoreDao

  abstract fun restaurantDao(): RestaurantDao

  abstract fun foodItemDao(): FoodItemDao

  abstract fun scheduledMealDao(): ScheduledMealDao

  abstract fun packageOptionDao(): PackageOptionDao

  abstract fun pantryDao(): PantryDao

  abstract fun shoppingListDao(): ShoppingListDao

  abstract fun receiptDao(): ReceiptDao

  abstract fun appSettingsDao(): AppSettingsDao

  companion object {
    fun getDatabase(builder: Builder<MealPlannerDatabase>): MealPlannerDatabase {
      return builder
          .setDriver(BundledSQLiteDriver())
          .addCallback(MealPlannerCallback())
          .fallbackToDestructiveMigration(true)
          .build()
    }

    class MealPlannerCallback : Callback() {
      override fun onOpen(connection: SQLiteConnection) {
        super.onOpen(connection)
        val stmt = connection.prepare("PRAGMA foreign_keys = ON;")
        try {
          stmt.step()
        } finally {
          stmt.close()
        }
      }
    }
  }
}

// The expect object that Room will use to construct the database implementation
@Suppress("KotlinNoActualForExpect")
expect object MealPlannerDatabaseConstructor : RoomDatabaseConstructor<MealPlannerDatabase> {
  override fun initialize(): MealPlannerDatabase
}
