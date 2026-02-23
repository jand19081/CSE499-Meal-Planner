package io.github.and19081.mealplanner.data.db

import androidx.room.*
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.and19081.mealplanner.data.db.dao.*
import io.github.and19081.mealplanner.data.db.entity.*
import io.github.and19081.mealplanner.data.db.converters.MealPlannerTypeConverters

/**
 * Room database for the Meal Planner application.
 */
@Database(
    version = 2,
    exportSchema = true,
    entities = [
        // Lookup / Reference
        UnitEntity::class,
        CategoryEntity::class,
        StoreEntity::class,
        RestaurantEntity::class,

        // Shopping-item hierarchy
        IngredientEntity::class,
        CustomShoppingItemEntity::class,

        // Recipe domain
        RecipeEntity::class,
        RecipeInstructionEntity::class,

        // Meal planning domain
        PrePlannedMealEntity::class,
        ScheduledMealEntity::class,

        // Settings
        AppSettingsEntity::class,

        // Ledger
        StoreReceiptEntity::class,

        // Edges / junction tables
        UnitConversionBridgeEntity::class,
        IngredientCategoryEntity::class,
        PackageOptionEntity::class,
        RecipeRequirementEntity::class,
        MealRecipeEntity::class,
        MealIndependentIngredientEntity::class,
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
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeDao(): RecipeDao
    abstract fun prePlannedMealDao(): PrePlannedMealDao
    abstract fun scheduledMealDao(): ScheduledMealDao
    abstract fun packageOptionDao(): PackageOptionDao
    abstract fun pantryDao(): PantryDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        fun getDatabase(
            builder: RoomDatabase.Builder<MealPlannerDatabase>
        ): MealPlannerDatabase {
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
