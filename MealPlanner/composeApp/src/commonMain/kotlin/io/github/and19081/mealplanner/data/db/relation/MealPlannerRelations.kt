package io.github.and19081.mealplanner.data.db.relation

import androidx.room.*
import io.github.and19081.mealplanner.data.db.entity.FoodItemEntity
import io.github.and19081.mealplanner.data.db.entity.LeftoverComponentEntity
import io.github.and19081.mealplanner.data.db.entity.PurchaseOptionEntity
import io.github.and19081.mealplanner.data.db.entity.PantryInventoryEntity
import io.github.and19081.mealplanner.data.db.entity.PurchasableComponentEntity
import io.github.and19081.mealplanner.data.db.entity.ReceiptLineItemEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeComponentEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeInstructionEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeRequirementEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeRequirementGroupEntity
import io.github.and19081.mealplanner.data.db.entity.ScheduledMealEntity
import io.github.and19081.mealplanner.data.db.entity.ShoppingCartItemEntity
import io.github.and19081.mealplanner.data.db.entity.StoreEntity
import io.github.and19081.mealplanner.data.db.entity.StoreReceiptEntity
import io.github.and19081.mealplanner.data.db.entity.UnitConversionBridgeEntity
import io.github.and19081.mealplanner.data.db.entity.UnitEntity

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
        entityColumn = "food_item_id",
    )
    val requirementGroups: List<RecipeRequirementGroupWithRequirements>,
    @Relation(parentColumn = "id", entityColumn = "food_item_id")
    val purchaseOptions: List<PurchaseOptionEntity>,
    @Relation(parentColumn = "id", entityColumn = "food_item_id")
    val conversions: List<UnitConversionBridgeEntity>,
)

data class RecipeRequirementGroupWithRequirements(
    @Embedded val group: RecipeRequirementGroupEntity,
    @Relation(parentColumn = "id", entityColumn = "group_id")
    val requirements: List<RecipeRequirementEntity>,
)

// ─────────────────────────────────────────────────────────────────────────────
// Scheduled Meal relations
// ─────────────────────────────────────────────────────────────────────────────

data class ScheduledMealWithSource(
    @Embedded val scheduledMeal: ScheduledMealEntity,
)

// ─────────────────────────────────────────────────────────────────────────────
// Shopping List relations
// ─────────────────────────────────────────────────────────────────────────────

data class ShoppingCartItemWithDetails(
    @Embedded val cartItem: ShoppingCartItemEntity,
    @Relation(parentColumn = "food_item_id", entityColumn = "id") val foodItem: FoodItemEntity?,
    @Relation(parentColumn = "store_id", entityColumn = "id") val store: StoreEntity?,
    @Relation(parentColumn = "unit_id", entityColumn = "id") val unit: UnitEntity?,
    @Relation(parentColumn = "purchase_option_id", entityColumn = "id")
    val purchaseOption: PurchaseOptionEntity?,
)

// ─────────────────────────────────────────────────────────────────────────────
// Pantry relations
// ─────────────────────────────────────────────────────────────────────────────

data class PantryInventoryWithDetails(
    @Embedded val pantryItem: PantryInventoryEntity,
    @Relation(parentColumn = "food_item_id", entityColumn = "id") val foodItem: FoodItemEntity?,
    @Relation(parentColumn = "unit_id", entityColumn = "id") val unit: UnitEntity?,
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
    @Relation(parentColumn = "food_item_id", entityColumn = "id") val foodItem: FoodItemEntity?,
    @Relation(parentColumn = "unit_id", entityColumn = "id") val unit: UnitEntity?,
)
