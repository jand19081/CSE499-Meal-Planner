@file:Suppress("unused")

package io.github.and19081.mealplanner.data.db.entity

import androidx.room.*
import io.github.and19081.mealplanner.core.theme.AppTheme
import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.core.util.UnitType
import io.github.and19081.mealplanner.feature.settings.Mode
import kotlin.uuid.Uuid

// ─────────────────────────────────────────────────────────────────────────────
// Embedded value objects
// ─────────────────────────────────────────────────────────────────────────────

data class DashboardConfig(
    @ColumnInfo(name = "show_weekly_cost") val showWeeklyCost: Boolean = false,
    @ColumnInfo(name = "show_shopping_list_summary") val showShoppingListSummary: Boolean = false,
    @ColumnInfo(name = "show_meal_plan") val showMealPlan: Boolean = false,
)

data class ItemMeasurement(
    @ColumnInfo(name = "food_item_id") val foodItemId: Uuid?,
    @ColumnInfo(name = "unit_id") val unitId: Uuid?,
    @ColumnInfo(name = "quantity") val quantity: Double,
)

// ─────────────────────────────────────────────────────────────────────────────
// LOOKUP / REFERENCE NODES
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "units", indices = [Index(value = ["name"])])
data class UnitEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "abbreviation") val abbreviation: String? = null,
    @ColumnInfo(name = "display_name") val displayName: String? = null,
    @ColumnInfo(name = "is_system_unit") val isSystemUnit: Boolean = false,
    @ColumnInfo(name = "factor_to_base") val factorToBase: Double? = null,
    @ColumnInfo(name = "unit_type") val unitType: UnitType,
)

@Entity(tableName = "categories", indices = [Index(value = ["name"])])
data class CategoryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name") val name: String,
)

@Entity(tableName = "stores", indices = [Index(value = ["name"])])
data class StoreEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name") val name: String,
)

@Entity(tableName = "restaurants", indices = [Index(value = ["name"])])
data class RestaurantEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name") val name: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// ECS CORE: FOOD ITEMS
// ─────────────────────────────────────────────────────────────────────────────

/** The root Entity for all food-related data (Ingredients, Recipes, Meals, Leftovers). */
@Entity(
    tableName = "food_items",
    indices = [Index(value = ["name"]), Index(value = ["preferred_unit_id"])],
    foreignKeys =
        [
            ForeignKey(
                entity = UnitEntity::class,
                parentColumns = ["id"],
                childColumns = ["preferred_unit_id"],
                onDelete = ForeignKey.SET_NULL,
            )
        ],
)
data class FoodItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "preferred_unit_id") val preferredUnitId: Uuid? = null,
    @ColumnInfo(name = "is_purchasable") val isPurchasable: Boolean = false,
    @ColumnInfo(name = "is_recipe") val isRecipe: Boolean = false,
    @ColumnInfo(name = "is_leftover") val isLeftover: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long? = null,
)

/** Component for items that can be purchased from a store. */
@Entity(
    tableName = "purchasable_components",
    foreignKeys =
        [
            ForeignKey(
                entity = FoodItemEntity::class,
                parentColumns = ["id"],
                childColumns = ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = CategoryEntity::class,
                parentColumns = ["id"],
                childColumns = ["category_id"],
                onDelete = ForeignKey.SET_NULL,
            ),
        ],
    indices = [Index(value = ["food_item_id"]), Index(value = ["category_id"])],
)
data class PurchasableComponentEntity(
    @PrimaryKey @ColumnInfo(name = "food_item_id") val foodItemId: Uuid,
    @ColumnInfo(name = "expected_price_cents") val expectedPriceCents: Int? = null,
    @ColumnInfo(name = "category_id") val categoryId: Uuid? = null,
)

/** Component for items that have a recipe or assembly instructions. */
@Entity(
    tableName = "recipe_components",
    foreignKeys =
        [
            ForeignKey(
                entity = FoodItemEntity::class,
                parentColumns = ["id"],
                childColumns = ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["food_item_id"])],
)
data class RecipeComponentEntity(
    @PrimaryKey @ColumnInfo(name = "food_item_id") val foodItemId: Uuid,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "servings") val servings: Double = 1.0,
    @ColumnInfo(name = "meal_type") val mealType: RecipeMealType = RecipeMealType.Other,
    @ColumnInfo(name = "is_meal") val isMeal: Boolean = false,
    @ColumnInfo(name = "prep_time_minutes") val prepTimeMinutes: Int? = null,
    @ColumnInfo(name = "cook_time_minutes") val cookTimeMinutes: Int? = null,
)

/** Component for items currently in the pantry as leftovers/prepared food. */
@Entity(
    tableName = "leftover_components",
    foreignKeys =
        [
            ForeignKey(
                entity = FoodItemEntity::class,
                parentColumns = ["id"],
                childColumns = ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["food_item_id"])],
)
data class LeftoverComponentEntity(
    @PrimaryKey @ColumnInfo(name = "food_item_id") val foodItemId: Uuid,
    @ColumnInfo(name = "remaining_servings") val remainingServings: Double,
    @ColumnInfo(name = "date_added") val dateAdded: String,
    @ColumnInfo(name = "expiration_date") val expirationDate: String? = null,
)

@Entity(
    tableName = "recipe_instructions",
    foreignKeys =
        [
            ForeignKey(
                entity = FoodItemEntity::class,
                parentColumns = ["id"],
                childColumns = ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["food_item_id"])],
)
data class RecipeInstructionEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "food_item_id") val foodItemId: Uuid,
    @ColumnInfo(name = "step_order") val stepOrder: Int,
    @ColumnInfo(name = "instruction") val instruction: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// MEAL PLANNING & CALENDAR
// ─────────────────────────────────────────────────────────────────────────────

@Entity(
    tableName = "scheduled_meals",
    foreignKeys =
        [
            ForeignKey(
                entity = FoodItemEntity::class,
                parentColumns = ["id"],
                childColumns = ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = RestaurantEntity::class,
                parentColumns = ["id"],
                childColumns = ["restaurant_id"],
                onDelete = ForeignKey.RESTRICT,
            ),
        ],
    indices =
        [
            Index(value = ["date"]),
            Index(value = ["food_item_id"]),
            Index(value = ["restaurant_id"]),
        ],
)
data class ScheduledMealEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "food_item_id") val foodItemId: Uuid? = null,
    @ColumnInfo(name = "restaurant_id") val restaurantId: Uuid? = null,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "time") val time: String,
    @ColumnInfo(name = "meal_type") val mealType: RecipeMealType = RecipeMealType.Other,
    @ColumnInfo(name = "people_count") val peopleCount: Int = 1,
    @ColumnInfo(name = "is_consumed") val isConsumed: Boolean = false,
    @ColumnInfo(name = "meal_source") val mealSource: String,
    @ColumnInfo(name = "anticipated_cost_cents") val anticipatedCostCents: Int? = null,
    @ColumnInfo(name = "scheduled_quantity") val scheduledQuantity: Double? = null,
    @ColumnInfo(name = "scheduled_quantity_unit_id") val scheduledQuantityUnitId: Uuid? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// APP SETTINGS
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = SINGLETON_ID,
    @ColumnInfo(name = "is_first_launch") val isFirstLaunch: Boolean = true,
    @ColumnInfo(name = "notification_delay_minutes") val notificationDelayMinutes: Int? = null,
    @ColumnInfo(name = "default_tax_rate_percentage") val defaultTaxRatePercentage: Double? = null,
    @ColumnInfo(name = "app_mode") val appMode: Mode = Mode.AUTO,
    @ColumnInfo(name = "theme_preference") val themePreference: AppTheme = AppTheme.SYSTEM,
    @ColumnInfo(name = "corner_style") val cornerStyle: String = "ROUNDED",
    @ColumnInfo(name = "accent_color") val accentColor: String = "GREEN",
    @Embedded val dashboard: DashboardConfig = DashboardConfig(),
) {
  companion object {
    const val SINGLETON_ID = "app_settings_singleton"
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// RECEIPTS & LEDGER
// ─────────────────────────────────────────────────────────────────────────────

@Entity(
    tableName = "store_receipts",
    foreignKeys =
        [
            ForeignKey(
                entity = StoreEntity::class,
                ["id"],
                ["store_id"],
                onDelete = ForeignKey.RESTRICT,
            ),
            ForeignKey(
                entity = RestaurantEntity::class,
                ["id"],
                ["restaurant_id"],
                onDelete = ForeignKey.RESTRICT,
            ),
            ForeignKey(
                entity = ScheduledMealEntity::class,
                ["id"],
                ["scheduled_meal_id"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices =
        [
            Index(value = ["store_id"]),
            Index(value = ["restaurant_id"]),
            Index(value = ["scheduled_meal_id"]),
        ],
)
data class StoreReceiptEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "time") val time: String,
    @ColumnInfo(name = "store_id") val storeId: Uuid? = null,
    @ColumnInfo(name = "restaurant_id") val restaurantId: Uuid? = null,
    @ColumnInfo(name = "scheduled_meal_id") val scheduledMealId: Uuid? = null,
    @ColumnInfo(name = "projected_total_cents") val projectedTotalCents: Int? = null,
    @ColumnInfo(name = "actual_total_cents") val actualTotalCents: Int? = null,
    @ColumnInfo(name = "tax_paid_cents") val taxPaidCents: Int? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// EDGES & COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Entity(
    tableName = "unit_conversion_bridges",
    foreignKeys =
        [
            ForeignKey(
                entity = FoodItemEntity::class,
                ["id"],
                ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = UnitEntity::class,
                ["id"],
                ["from_unit_id"],
                onDelete = ForeignKey.RESTRICT,
            ),
            ForeignKey(
                entity = UnitEntity::class,
                ["id"],
                ["to_unit_id"],
                onDelete = ForeignKey.RESTRICT,
            ),
        ],
    indices =
        [
            Index(value = ["food_item_id"]),
            Index(value = ["from_unit_id"]),
            Index(value = ["to_unit_id"]),
        ],
)
data class UnitConversionBridgeEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "food_item_id") val foodItemId: Uuid,
    @ColumnInfo(name = "from_unit_id") val fromUnitId: Uuid,
    @ColumnInfo(name = "to_unit_id") val toUnitId: Uuid,
    @ColumnInfo(name = "from_quantity") val fromQuantity: Double,
    @ColumnInfo(name = "to_quantity") val toQuantity: Double,
)

@Entity(
    tableName = "purchase_options",
    foreignKeys =
        [
            ForeignKey(StoreEntity::class, ["id"], ["store_id"], onDelete = ForeignKey.CASCADE),
            ForeignKey(
                FoodItemEntity::class,
                ["id"],
                ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(UnitEntity::class, ["id"], ["unit_id"], onDelete = ForeignKey.RESTRICT),
        ],
    indices =
        [Index(value = ["store_id"]), Index(value = ["food_item_id"]), Index(value = ["unit_id"])],
)
data class PurchaseOptionEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "store_id") val storeId: Uuid,
    @ColumnInfo(name = "food_item_id") val foodItemId: Uuid,
    @ColumnInfo(name = "unit_id") val unitId: Uuid,
    @ColumnInfo(name = "price_cents") val priceCents: Int? = null,
    @ColumnInfo(name = "quantity") val quantity: Double? = null,
)

@Entity(
    tableName = "recipe_requirement_groups",
    foreignKeys =
        [
            ForeignKey(
                entity = FoodItemEntity::class,
                ["id"],
                ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["food_item_id"])],
)
data class RecipeRequirementGroupEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "food_item_id") val foodItemId: Uuid,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
)

@Entity(
    tableName = "recipe_requirements",
    foreignKeys =
        [
            ForeignKey(
                entity = RecipeRequirementGroupEntity::class,
                ["id"],
                ["group_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = FoodItemEntity::class,
                ["id"],
                ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = UnitEntity::class,
                ["id"],
                ["unit_id"],
                onDelete = ForeignKey.RESTRICT,
            ),
        ],
    indices =
        [Index(value = ["group_id"]), Index(value = ["food_item_id"]), Index(value = ["unit_id"])],
)
data class RecipeRequirementEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "group_id") val groupId: Uuid,
    @Embedded val measurement: ItemMeasurement,
    @ColumnInfo(name = "is_primary") val isPrimary: Boolean = true,
)

@Entity(
    tableName = "pantry_inventory",
    foreignKeys =
        [
            ForeignKey(
                entity = FoodItemEntity::class,
                ["id"],
                ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = UnitEntity::class,
                ["id"],
                ["unit_id"],
                onDelete = ForeignKey.RESTRICT,
            ),
        ],
    indices = [Index(value = ["food_item_id"]), Index(value = ["unit_id"])],
)
data class PantryInventoryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @Embedded val measurement: ItemMeasurement,
)

@Entity(
    tableName = "shopping_cart_items",
    foreignKeys =
        [
            ForeignKey(
                entity = FoodItemEntity::class,
                ["id"],
                ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(StoreEntity::class, ["id"], ["store_id"], onDelete = ForeignKey.SET_NULL),
            ForeignKey(UnitEntity::class, ["id"], ["unit_id"], onDelete = ForeignKey.RESTRICT),
            ForeignKey(
                PurchaseOptionEntity::class,
                ["id"],
                ["purchase_option_id"],
                onDelete = ForeignKey.SET_NULL,
            ),
        ],
    indices =
        [
            Index(value = ["food_item_id"]),
            Index(value = ["store_id"]),
            Index(value = ["unit_id"]),
            Index(value = ["purchase_option_id"]),
        ],
)
data class ShoppingCartItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "store_id") val storeId: Uuid? = null,
    @ColumnInfo(name = "purchase_option_id") val purchaseOptionId: Uuid? = null,
    @ColumnInfo(name = "custom_name") val customName: String? = null,
    @Embedded val measurement: ItemMeasurement,
    @ColumnInfo(name = "is_purchased") val isPurchased: Boolean = false,
    @ColumnInfo(name = "is_pantry_item") val isPantryItem: Boolean = true,
)

@Entity(
    tableName = "receipt_line_items",
    foreignKeys =
        [
            ForeignKey(
                StoreReceiptEntity::class,
                ["id"],
                ["receipt_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                FoodItemEntity::class,
                ["id"],
                ["food_item_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(UnitEntity::class, ["id"], ["unit_id"], onDelete = ForeignKey.RESTRICT),
        ],
    indices =
        [
            Index(value = ["receipt_id"]),
            Index(value = ["food_item_id"]),
            Index(value = ["unit_id"]),
        ],
)
data class ReceiptLineItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Uuid = Uuid.random(),
    @ColumnInfo(name = "receipt_id") val receiptId: Uuid,
    @ColumnInfo(name = "custom_name") val customName: String? = null,
    @Embedded val measurement: ItemMeasurement,
    @ColumnInfo(name = "price_paid_cents") val pricePaidCents: Int,
)
