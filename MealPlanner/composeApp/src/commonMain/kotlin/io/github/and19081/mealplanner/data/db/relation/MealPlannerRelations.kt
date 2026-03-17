package io.github.and19081.mealplanner.data.db.relation

import androidx.room.*
import io.github.and19081.mealplanner.data.db.entity.*

/** Room relation data classes for ECS Food Items. */

data class ComposedFoodItemRelation(
    @Embedded val item: FoodItemEntity,
    @Relation(parentColumn = "id", entityColumn = "food_item_id")
    val purchasable: PurchasableComponentEntity?,
    @Relation(parentColumn = "id", entityColumn = "food_item_id")
    val recipe: RecipeComponentEntity?,
    @Relation(parentColumn = "id", entityColumn = "food_item_id")
    val leftover: LeftoverComponentEntity?,
    @Relation(parentColumn = "id", entityColumn = "food_item_id")
    val instructions: List<RecipeInstructionEntity>,
    @Relation(
        entity = RecipeRequirementGroupEntity::class,
        parentColumn = "id",
        entityColumn = "food_item_id"
    )
    val requirementGroups: List<RecipeRequirementGroupWithRequirements>,
    @Relation(parentColumn = "id", entityColumn = "food_item_id")
    val packageOptions: List<PackageOptionEntity>,
    @Relation(parentColumn = "id", entityColumn = "food_item_id")
    val conversions: List<UnitConversionBridgeEntity>,
)

data class RecipeRequirementGroupWithRequirements(
    @Embedded val group: RecipeRequirementGroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "group_id"
    )
    val requirements: List<RecipeRequirementEntity>
)

// ─────────────────────────────────────────────────────────────────────────────
// Scheduled Meal relations
// ─────────────────────────────────────────────────────────────────────────────

data class ScheduledMealWithSource(
    @Embedded val scheduledMeal: ScheduledMealEntity,
    @Relation(parentColumn = "food_item_id", entityColumn = "id")
    val foodItem: FoodItemEntity?,
    @Relation(parentColumn = "restaurant_id", entityColumn = "id")
    val restaurant: RestaurantEntity?,
)

// ─────────────────────────────────────────────────────────────────────────────
// Shopping List relations
// ─────────────────────────────────────────────────────────────────────────────

data class ShoppingCartItemWithDetails(
    @Embedded val cartItem: ShoppingCartItemEntity,
    @Relation(parentColumn = "food_item_id", entityColumn = "id")
    val foodItem: FoodItemEntity,
    @Relation(parentColumn = "store_id", entityColumn = "id") val store: StoreEntity?,
    @Relation(parentColumn = "unit_id", entityColumn = "id") val unit: UnitEntity,
    @Relation(parentColumn = "package_option_id", entityColumn = "id")
    val packageOption: PackageOptionEntity?,
)

// ─────────────────────────────────────────────────────────────────────────────
// Pantry relations
// ─────────────────────────────────────────────────────────────────────────────

data class PantryInventoryWithDetails(
    @Embedded val pantryItem: PantryInventoryEntity,
    @Relation(parentColumn = "food_item_id", entityColumn = "id")
    val foodItem: FoodItemEntity,
    @Relation(parentColumn = "unit_id", entityColumn = "id") val unit: UnitEntity,
)

// ─────────────────────────────────────────────────────────────────────────────
// Receipt relations
// ─────────────────────────────────────────────────────────────────────────────

data class StoreReceiptWithLineItems(
    @Embedded val receipt: StoreReceiptEntity,
    @Relation(parentColumn = "id", entityColumn = "receipt_id")
    val lineItems: List<ReceiptLineItemEntity>,
)

data class ReceiptLineItemWithDetails(
    @Embedded val lineItem: ReceiptLineItemEntity,
    @Relation(parentColumn = "food_item_id", entityColumn = "id")
    val foodItem: FoodItemEntity?,
    @Relation(parentColumn = "unit_id", entityColumn = "id") val unit: UnitEntity?,
)
