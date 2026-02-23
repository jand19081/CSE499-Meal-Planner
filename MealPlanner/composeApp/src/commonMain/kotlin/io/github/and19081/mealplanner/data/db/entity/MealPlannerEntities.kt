@file:Suppress("unused")

package io.github.and19081.mealplanner.data.db.entity

import androidx.room.*
import io.github.and19081.mealplanner.RecipeMealType
import io.github.and19081.mealplanner.UnitType
import io.github.and19081.mealplanner.settings.AppTheme
import io.github.and19081.mealplanner.settings.Mode
import kotlin.uuid.Uuid

// ─────────────────────────────────────────────────────────────────────────────
// Embedded value objects
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Embedded struct that lives inside [AppSettingsEntity].
 * Maps to three flat columns in the app_settings table.
 */
data class DashboardConfig(
    @ColumnInfo(name = "show_weekly_cost")        val showWeeklyCost: Boolean = false,
    @ColumnInfo(name = "show_shopping_list_summary") val showShoppingListSummary: Boolean = false,
    @ColumnInfo(name = "show_meal_plan")           val showMealPlan: Boolean = false,
)

// ─────────────────────────────────────────────────────────────────────────────
// LOOKUP / REFERENCE NODES
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Represents a physical or logical unit of measurement (g, ml, cups, etc.).
 * [factorToBase] stores the multiplier needed to convert this unit to the
 * canonical base unit within its [unitType] group, enabling universal conversions.
 */
@Entity(
    tableName = "units",
    indices = [Index(value = ["name"])]
)
data class UnitEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")              val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name")            val name: String,
    @ColumnInfo(name = "abbreviation")    val abbreviation: String? = null,
    @ColumnInfo(name = "display_name")    val displayName: String? = null,
    @ColumnInfo(name = "is_system_unit")  val isSystemUnit: Boolean = false,
    @ColumnInfo(name = "factor_to_base")  val factorToBase: Double? = null,
    @ColumnInfo(name = "unit_type")       val unitType: UnitType,
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"])]
)
data class CategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")   val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name") val name: String,
)

@Entity(
    tableName = "stores",
    indices = [Index(value = ["name"])]
)
data class StoreEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")   val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name") val name: String,
)

@Entity(
    tableName = "restaurants",
    indices = [Index(value = ["name"])]
)
data class RestaurantEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")   val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name") val name: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// SHOPPING-ITEM HIERARCHY
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A known food ingredient that can appear in recipes, the pantry, and the
 * shopping list.
 */
@Entity(
    tableName = "ingredients",
    indices = [Index(value = ["name"])]
)
data class IngredientEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")         val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name")       val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long? = null,
)

/**
 * A free-form shopping item not tied to any recipe ingredient (e.g. "Paper Towels").
 * Shares the polymorphic [ShoppingCartItemEntity.ingredientId] /
 * [ShoppingCartItemEntity.customShoppingItemId] pattern.
 */
@Entity(
    tableName = "custom_shopping_items",
    indices = [Index(value = ["name"])]
)
data class CustomShoppingItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")   val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name") val name: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// RECIPE DOMAIN
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A culinary recipe with metadata.
 * Instructions are stored in a child [RecipeInstructionEntity] table to keep
 * this entity in 3NF (a List<String> column would violate 1NF).
 */
@Entity(
    tableName = "recipes",
    indices = [
        Index(value = ["name"]),
        Index(value = ["description"]),
    ]
)
data class RecipeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")                val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name")              val name: String,
    @ColumnInfo(name = "description")       val description: String? = null,
    @ColumnInfo(name = "servings")          val servings: Double? = null,
    @ColumnInfo(name = "meal_type")         val mealType: RecipeMealType? = null,
    @ColumnInfo(name = "prep_time_minutes") val prepTimeMinutes: Int? = null,
    @ColumnInfo(name = "cook_time_minutes") val cookTimeMinutes: Int? = null,
    @ColumnInfo(name = "created_at")        val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")        val updatedAt: Long? = null,
)

/**
 * One ordered step from a recipe's instruction list.
 * [stepOrder] is a zero-based index; queries should ORDER BY step_order ASC.
 */
@Entity(
    tableName = "recipe_instructions",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["recipe_id"])]
)
data class RecipeInstructionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")          val id: Long = 0,
    @ColumnInfo(name = "recipe_id")   val recipeId: Uuid,
    @ColumnInfo(name = "step_order")  val stepOrder: Int,
    @ColumnInfo(name = "instruction") val instruction: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// MEAL PLANNING DOMAIN
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A reusable, named meal template composed of recipes and/or standalone
 * ingredients.  Think "Sunday Roast" or "Quick Weekday Lunch".
 */
@Entity(tableName = "pre_planned_meals")
data class PrePlannedMealEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")   val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name") val name: String,
)

/**
 * A specific occurrence of a meal placed on the calendar.
 *
 * Exactly one of [prePlannedMealId] or [restaurantId] must be non-null
 * (enforced by a CHECK constraint in the schema and validated by the DAO).
 */
@Entity(
    tableName = "scheduled_meals",
    foreignKeys = [
        ForeignKey(
            entity = PrePlannedMealEntity::class,
            parentColumns = ["id"],
            childColumns = ["pre_planned_meal_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RestaurantEntity::class,
            parentColumns = ["id"],
            childColumns = ["restaurant_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["pre_planned_meal_id"]),
        Index(value = ["restaurant_id"]),
    ]
)
data class ScheduledMealEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")                      val id: Uuid = Uuid.random(),
    /** ISO-8601 date string, e.g. "2025-03-15" */
    @ColumnInfo(name = "date")                    val date: String,
    /** ISO-8601 time string, e.g. "18:30:00" */
    @ColumnInfo(name = "time")                    val time: String,
    @ColumnInfo(name = "meal_type")               val mealType: RecipeMealType? = null,
    @ColumnInfo(name = "people_count")            val peopleCount: Int? = null,
    @ColumnInfo(name = "is_consumed")             val isConsumed: Boolean = false,
    @ColumnInfo(name = "anticipated_cost_cents")  val anticipatedCostCents: Int? = null,
    @ColumnInfo(name = "pre_planned_meal_id")     val prePlannedMealId: Uuid? = null,
    @ColumnInfo(name = "restaurant_id")           val restaurantId: Uuid? = null,
) {
    init {
        require(
            (prePlannedMealId != null) xor (restaurantId != null)
        ) { "A ScheduledMeal must reference exactly one of prePlannedMealId or restaurantId." }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APP SETTINGS (singleton)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Application-wide settings stored as a single row.
 * Singleton enforcement strategy: the DAO's upsert uses a fixed sentinel ID.
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = SINGLETON_ID,

    @ColumnInfo(name = "notification_delay_minutes")
    val notificationDelayMinutes: Int? = null,

    @ColumnInfo(name = "default_tax_rate_percentage")
    val defaultTaxRatePercentage: Double? = null,

    @ColumnInfo(name = "app_mode")
    val appMode: Mode = Mode.AUTO,

    @ColumnInfo(name = "theme_preference")
    val themePreference: AppTheme = AppTheme.SYSTEM,

    @ColumnInfo(name = "corner_style")
    val cornerStyle: String = "ROUNDED",

    @ColumnInfo(name = "accent_color")
    val accentColor: String = "GREEN",

    @Embedded
    val dashboard: DashboardConfig = DashboardConfig(),
) {
    companion object {
        const val SINGLETON_ID = "app_settings_singleton"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RECEIPT / LEDGER DOMAIN
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A historical purchase receipt from a store trip.
 */
@Entity(
    tableName = "store_receipts",
    foreignKeys = [
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["store_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = RestaurantEntity::class,
            parentColumns = ["id"],
            childColumns = ["restaurant_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ScheduledMealEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduled_meal_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["store_id"]),
        Index(value = ["restaurant_id"]),
        Index(value = ["scheduled_meal_id"]),
    ]
)
data class StoreReceiptEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")                    val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name")                  val name: String,
    /** ISO-8601 date string */
    @ColumnInfo(name = "date")                  val date: String,
    /** ISO-8601 time string */
    @ColumnInfo(name = "time")                  val time: String,
    @ColumnInfo(name = "store_id")              val storeId: Uuid? = null,
    @ColumnInfo(name = "restaurant_id")         val restaurantId: Uuid? = null,
    @ColumnInfo(name = "scheduled_meal_id")     val scheduledMealId: Uuid? = null,
    @ColumnInfo(name = "projected_total_cents") val projectedTotalCents: Int? = null,
    @ColumnInfo(name = "actual_total_cents")    val actualTotalCents: Int? = null,
    @ColumnInfo(name = "tax_paid_cents")        val taxPaidCents: Int? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// EDGE / JUNCTION ENTITIES
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Unit conversion factor specific to a single ingredient.
 * e.g. "For 'Butter': 1 cup → 227 g".
 */
@Entity(
    tableName = "unit_conversion_bridges",
    foreignKeys = [
        ForeignKey(IngredientEntity::class, ["id"], ["ingredient_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(UnitEntity::class,       ["id"], ["from_unit_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(UnitEntity::class,       ["id"], ["to_unit_id"],   onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["ingredient_id"]),
        Index(value = ["from_unit_id"]),
        Index(value = ["to_unit_id"]),
        Index(value = ["ingredient_id", "from_unit_id", "to_unit_id"], unique = true),
    ]
)
data class UnitConversionBridgeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")            val id: Long = 0,
    @ColumnInfo(name = "ingredient_id") val ingredientId: Uuid,
    @ColumnInfo(name = "from_unit_id")  val fromUnitId: Uuid,
    @ColumnInfo(name = "to_unit_id")    val toUnitId: Uuid,
    @ColumnInfo(name = "from_quantity") val fromQuantity: Double,
    @ColumnInfo(name = "to_quantity")   val toQuantity: Double,
)

/**
 * M:N association between [IngredientEntity] and [CategoryEntity].
 */
@Entity(
    tableName = "ingredient_categories",
    primaryKeys = ["ingredient_id", "category_id"],
    foreignKeys = [
        ForeignKey(IngredientEntity::class, ["id"], ["ingredient_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class,   ["id"], ["category_id"],  onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["category_id"])]
)
data class IngredientCategoryEntity(
    @ColumnInfo(name = "ingredient_id") val ingredientId: Uuid,
    @ColumnInfo(name = "category_id")   val categoryId: Uuid,
)

/**
 * A store's specific product listing for an ingredient (SKU-level detail).
 */
@Entity(
    tableName = "package_options",
    foreignKeys = [
        ForeignKey(StoreEntity::class,      ["id"], ["store_id"],      onDelete = ForeignKey.CASCADE),
        ForeignKey(IngredientEntity::class, ["id"], ["ingredient_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(UnitEntity::class,       ["id"], ["unit_id"],       onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["store_id"]),
        Index(value = ["ingredient_id"]),
        Index(value = ["unit_id"]),
    ]
)
data class PackageOptionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")            val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "store_id")      val storeId: Uuid,
    @ColumnInfo(name = "ingredient_id") val ingredientId: Uuid,
    @ColumnInfo(name = "unit_id")       val unitId: Uuid,
    @ColumnInfo(name = "price_cents")   val priceCents: Int? = null,
    @ColumnInfo(name = "quantity")      val quantity: Double? = null,
)

/**
 * Links an ingredient to a recipe, specifying the amount and unit required.
 */
@Entity(
    tableName = "recipe_requirements",
    foreignKeys = [
        ForeignKey(RecipeEntity::class,     ["id"], ["recipe_id"],     onDelete = ForeignKey.CASCADE),
        ForeignKey(IngredientEntity::class, ["id"], ["ingredient_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(UnitEntity::class,       ["id"], ["unit_id"],       onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["recipe_id"]),
        Index(value = ["ingredient_id"]),
        Index(value = ["unit_id"]),
    ]
)
data class RecipeRequirementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")              val id: Long = 0,
    @ColumnInfo(name = "recipe_id")     val recipeId: Uuid,
    @ColumnInfo(name = "ingredient_id") val ingredientId: Uuid? = null,
    @ColumnInfo(name = "sub_recipe_id") val subRecipeId: Uuid? = null,
    @ColumnInfo(name = "unit_id")       val unitId: Uuid,
    @ColumnInfo(name = "quantity")      val quantity: Double,
    @ColumnInfo(name = "sort_order")    val sortOrder: Int = 0,
)

/**
 * M:N join between [PrePlannedMealEntity] and [RecipeEntity].
 */
@Entity(
    tableName = "meal_recipes",
    primaryKeys = ["pre_planned_meal_id", "recipe_id"],
    foreignKeys = [
        ForeignKey(
            entity = PrePlannedMealEntity::class,
            parentColumns = ["id"],
            childColumns = ["pre_planned_meal_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["recipe_id"])]
)
data class MealRecipeEntity(
    @ColumnInfo(name = "pre_planned_meal_id") val prePlannedMealId: Uuid,
    @ColumnInfo(name = "recipe_id")           val recipeId: Uuid,
)

/**
 * Standalone ingredients added directly to a pre-planned meal.
 */
@Entity(
    tableName = "meal_independent_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = PrePlannedMealEntity::class,
            parentColumns = ["id"],
            childColumns = ["pre_planned_meal_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(IngredientEntity::class,     ["id"], ["ingredient_id"],       onDelete = ForeignKey.RESTRICT),
        ForeignKey(UnitEntity::class,           ["id"], ["unit_id"],             onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["pre_planned_meal_id"]),
        Index(value = ["ingredient_id"]),
        Index(value = ["unit_id"]),
    ]
)
data class MealIndependentIngredientEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")                  val id: Long = 0,
    @ColumnInfo(name = "pre_planned_meal_id") val prePlannedMealId: Uuid,
    @ColumnInfo(name = "ingredient_id")       val ingredientId: Uuid,
    @ColumnInfo(name = "unit_id")             val unitId: Uuid,
    @ColumnInfo(name = "quantity")            val quantity: Double,
)

/**
 * Current pantry stock for a given ingredient.
 */
@Entity(
    tableName = "pantry_inventory",
    primaryKeys = ["ingredient_id", "unit_id"],
    foreignKeys = [
        ForeignKey(IngredientEntity::class, ["id"], ["ingredient_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(UnitEntity::class,       ["id"], ["unit_id"],       onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["unit_id"])]
)
data class PantryInventoryEntity(
    @ColumnInfo(name = "ingredient_id") val ingredientId: Uuid,
    @ColumnInfo(name = "unit_id")       val unitId: Uuid,
    @ColumnInfo(name = "quantity")      val quantity: Double = 0.0,
)

/**
 * An item on the active shopping list.
 */
@Entity(
    tableName = "shopping_cart_items",
    foreignKeys = [
        ForeignKey(IngredientEntity::class,       ["id"], ["ingredient_id"],           onDelete = ForeignKey.CASCADE),
        ForeignKey(CustomShoppingItemEntity::class,["id"], ["custom_shopping_item_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(StoreEntity::class,            ["id"], ["store_id"],                onDelete = ForeignKey.RESTRICT),
        ForeignKey(UnitEntity::class,             ["id"], ["unit_id"],                 onDelete = ForeignKey.RESTRICT),
        ForeignKey(PackageOptionEntity::class,    ["id"], ["package_option_id"],       onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index(value = ["ingredient_id"]),
        Index(value = ["custom_shopping_item_id"]),
        Index(value = ["store_id"]),
        Index(value = ["unit_id"]),
        Index(value = ["package_option_id"]),
    ]
)
data class ShoppingCartItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")                      val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "ingredient_id")           val ingredientId: Uuid? = null,
    @ColumnInfo(name = "custom_shopping_item_id") val customShoppingItemId: Uuid? = null,
    @ColumnInfo(name = "store_id")                val storeId: Uuid,
    @ColumnInfo(name = "unit_id")                 val unitId: Uuid,
    @ColumnInfo(name = "package_option_id")       val packageOptionId: Uuid? = null,
    @ColumnInfo(name = "custom_name")             val customName: String? = null,
    @ColumnInfo(name = "needed_quantity")         val neededQuantity: Double,
    @ColumnInfo(name = "is_purchased")            val isPurchased: Boolean = false,
) {
    init {
        require(
            (ingredientId != null) xor (customShoppingItemId != null)
        ) { "ShoppingCartItem must reference exactly one of ingredientId or customShoppingItemId." }
    }
}

/**
 * One line on a [StoreReceiptEntity] — what was bought, how much, and at what price.
 */
@Entity(
    tableName = "receipt_line_items",
    foreignKeys = [
        ForeignKey(StoreReceiptEntity::class, ["id"], ["receipt_id"],    onDelete = ForeignKey.CASCADE),
        ForeignKey(IngredientEntity::class,   ["id"], ["ingredient_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(UnitEntity::class,         ["id"], ["unit_id"],       onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["receipt_id"]),
        Index(value = ["ingredient_id"]),
        Index(value = ["unit_id"]),
    ]
)
data class ReceiptLineItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")              val id: Long = 0,
    @ColumnInfo(name = "receipt_id")      val receiptId: Uuid,
    @ColumnInfo(name = "ingredient_id")   val ingredientId: Uuid? = null,
    @ColumnInfo(name = "unit_id")         val unitId: Uuid? = null,
    @ColumnInfo(name = "custom_name")     val customName: String? = null,
    @ColumnInfo(name = "quantity_bought") val quantityBought: Double,
    @ColumnInfo(name = "price_paid_cents") val pricePaidCents: Int,
)
