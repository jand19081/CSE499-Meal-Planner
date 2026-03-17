package io.github.and19081.mealplanner.data.db

import androidx.room.*
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.and19081.mealplanner.data.db.converters.MealPlannerTypeConverters
import io.github.and19081.mealplanner.data.db.dao.*
import io.github.and19081.mealplanner.data.db.entity.*

/** Room database for the Meal Planner application with ECS architecture. */
@Database(
    version = 3,
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
    fun getDatabase(builder: RoomDatabase.Builder<MealPlannerDatabase>): MealPlannerDatabase {
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
