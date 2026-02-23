package io.github.and19081.mealplanner.data.db.relation

import androidx.room.*
import io.github.and19081.mealplanner.data.db.entity.*

/**
 * Room relation data classes.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Ingredient relations
// ─────────────────────────────────────────────────────────────────────────────

data class IngredientWithCategories(
    @Embedded val ingredient: IngredientEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = IngredientCategoryEntity::class,
            parentColumn = "ingredient_id",
            entityColumn = "category_id"
        ),
    )
    val categories: List<CategoryEntity>,
)

data class IngredientWithConversions(
    @Embedded val ingredient: IngredientEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "ingredient_id",
    )
    val conversions: List<UnitConversionBridgeEntity>,
)

// ─────────────────────────────────────────────────────────────────────────────
// Recipe relations
// ─────────────────────────────────────────────────────────────────────────────

data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "recipe_id",
    )
    val instructions: List<RecipeInstructionEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "recipe_id",
    )
    val requirements: List<RecipeRequirementEntity>,
)

// ─────────────────────────────────────────────────────────────────────────────
// Pre-Planned Meal relations
// ─────────────────────────────────────────────────────────────────────────────

data class PrePlannedMealWithRecipes(
    @Embedded val meal: PrePlannedMealEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MealRecipeEntity::class,
            parentColumn = "pre_planned_meal_id",
            entityColumn = "recipe_id",
        ),
    )
    val recipes: List<RecipeEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "pre_planned_meal_id",
    )
    val independentIngredients: List<MealIndependentIngredientEntity>,
)

// ─────────────────────────────────────────────────────────────────────────────
// Scheduled Meal relations
// ─────────────────────────────────────────────────────────────────────────────

data class ScheduledMealWithSource(
    @Embedded val scheduledMeal: ScheduledMealEntity,

    @Relation(
        parentColumn = "pre_planned_meal_id",
        entityColumn = "id",
    )
    val prePlannedMeal: List<PrePlannedMealEntity>,

    @Relation(
        parentColumn = "restaurant_id",
        entityColumn = "id",
    )
    val restaurant: List<RestaurantEntity>,
)

// ─────────────────────────────────────────────────────────────────────────────
// Shopping List relations
// ─────────────────────────────────────────────────────────────────────────────

data class ShoppingCartItemWithDetails(
    @Embedded val cartItem: ShoppingCartItemEntity,

    @Relation(parentColumn = "ingredient_id",           entityColumn = "id")
    val ingredient: List<IngredientEntity>,

    @Relation(parentColumn = "custom_shopping_item_id", entityColumn = "id")
    val customItem: List<CustomShoppingItemEntity>,

    @Relation(parentColumn = "store_id",                entityColumn = "id")
    val store: List<StoreEntity>,

    @Relation(parentColumn = "unit_id",                 entityColumn = "id")
    val unit: List<UnitEntity>,

    @Relation(parentColumn = "package_option_id",       entityColumn = "id")
    val packageOption: List<PackageOptionEntity>,
)

// ─────────────────────────────────────────────────────────────────────────────
// Pantry relations
// ─────────────────────────────────────────────────────────────────────────────

data class PantryInventoryWithDetails(
    @Embedded val pantryItem: PantryInventoryEntity,

    @Relation(parentColumn = "ingredient_id", entityColumn = "id")
    val ingredient: List<IngredientEntity>,

    @Relation(parentColumn = "unit_id", entityColumn = "id")
    val unit: List<UnitEntity>,
)

// ─────────────────────────────────────────────────────────────────────────────
// Receipt relations
// ─────────────────────────────────────────────────────────────────────────────

data class ReceiptLineItemWithDetails(
    @Embedded val lineItem: ReceiptLineItemEntity,

    @Relation(parentColumn = "ingredient_id", entityColumn = "id")
    val ingredient: List<IngredientEntity>,

    @Relation(parentColumn = "unit_id", entityColumn = "id")
    val unit: List<UnitEntity>,
)

data class StoreReceiptWithLineItems(
    @Embedded val receipt: StoreReceiptEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "receipt_id",
    )
    val lineItems: List<ReceiptLineItemEntity>,
)

data class StoreWithReceipts(
    @Embedded val store: StoreEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "store_id",
    )
    val receipts: List<StoreReceiptEntity>,
)

// ─────────────────────────────────────────────────────────────────────────────
// Package Option relations
// ─────────────────────────────────────────────────────────────────────────────

data class PackageOptionWithDetails(
    @Embedded val packageOption: PackageOptionEntity,

    @Relation(parentColumn = "store_id",      entityColumn = "id") val store: List<StoreEntity>,
    @Relation(parentColumn = "ingredient_id", entityColumn = "id") val ingredient: List<IngredientEntity>,
    @Relation(parentColumn = "unit_id",       entityColumn = "id") val unit: List<UnitEntity>,
)
