package io.github.and19081.mealplanner.data.db.dao

import androidx.room.*
import io.github.and19081.mealplanner.data.db.entity.AppSettingsEntity
import io.github.and19081.mealplanner.data.db.entity.CategoryEntity
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
import io.github.and19081.mealplanner.data.db.entity.RestaurantEntity
import io.github.and19081.mealplanner.data.db.entity.ScheduledMealEntity
import io.github.and19081.mealplanner.data.db.entity.ShoppingCartItemEntity
import io.github.and19081.mealplanner.data.db.entity.StoreEntity
import io.github.and19081.mealplanner.data.db.entity.StoreReceiptEntity
import io.github.and19081.mealplanner.data.db.entity.UnitConversionBridgeEntity
import io.github.and19081.mealplanner.data.db.entity.UnitEntity
import io.github.and19081.mealplanner.data.db.relation.ComposedFoodItemRelation
import io.github.and19081.mealplanner.data.db.relation.MonthlyExpenditure
import io.github.and19081.mealplanner.data.db.relation.PantryInventoryWithDetails
import io.github.and19081.mealplanner.data.db.relation.RecipeBOMItem
import io.github.and19081.mealplanner.data.db.relation.ScheduledMealWithSource
import io.github.and19081.mealplanner.data.db.relation.ShoppingCartItemWithDetails
import io.github.and19081.mealplanner.data.db.relation.StoreReceiptWithLineItems
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// UnitDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface UnitDao {
    @Query("SELECT * FROM units ORDER BY name ASC")
    fun observeAll(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units ORDER BY name ASC")
    suspend fun getAll(): List<UnitEntity>

    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun getById(id: Uuid): UnitEntity?

    @Query("SELECT * FROM units WHERE unit_type = :type ORDER BY name ASC")
    suspend fun getByType(type: String): List<UnitEntity>

    @Upsert
    suspend fun upsert(unit: UnitEntity)

    @Upsert
    suspend fun upsertAll(units: List<UnitEntity>)

    @Delete
    suspend fun delete(unit: UnitEntity)

    @Query("DELETE FROM units")
    suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────────────────────
// StoreDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores ORDER BY name ASC")
    fun observeAll(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores ORDER BY name ASC")
    suspend fun getAll(): List<StoreEntity>

    @Query("SELECT * FROM stores WHERE id = :id")
    suspend fun getById(id: Uuid): StoreEntity?

    @Upsert
    suspend fun upsert(store: StoreEntity)

    @Upsert
    suspend fun upsertAll(stores: List<StoreEntity>)

    @Delete
    suspend fun delete(store: StoreEntity)

    @Query("DELETE FROM stores")
    suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────────────────────
// RestaurantDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface RestaurantDao {
    @Query("SELECT * FROM restaurants ORDER BY name ASC")
    fun observeAll(): Flow<List<RestaurantEntity>>

    @Query("SELECT * FROM restaurants ORDER BY name ASC")
    suspend fun getAll(): List<RestaurantEntity>

    @Upsert
    suspend fun upsert(restaurant: RestaurantEntity)

    @Upsert
    suspend fun upsertAll(restaurants: List<RestaurantEntity>)

    @Delete
    suspend fun delete(restaurant: RestaurantEntity)

    @Query("DELETE FROM restaurants")
    suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────────────────────
// ECS: FoodItemDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun observeAll(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items ORDER BY name ASC")
    suspend fun getAll(): List<FoodItemEntity>

    @Transaction
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun observeAllComposed(): Flow<List<ComposedFoodItemRelation>>

    @Transaction
    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getComposedById(id: Uuid): ComposedFoodItemRelation?

    @Query("""
        SELECT f.* FROM food_items f
        INNER JOIN recipe_components r ON f.id = r.food_item_id
        WHERE NOT EXISTS (
            SELECT 1 FROM recipe_requirements req
            INNER JOIN recipe_requirement_groups grp ON req.group_id = grp.id
            LEFT JOIN pantry_inventory p ON req.food_item_id = p.food_item_id
            WHERE grp.food_item_id = f.id
            AND (p.food_item_id IS NULL OR p.quantity < req.quantity)
        )
    """)
    suspend fun getMakeableRecipes(): List<FoodItemEntity>

    @Query("""
        WITH RECURSIVE RecipeBOM(food_item_id, required_qty, unit_id) AS (
            SELECT req.food_item_id, req.quantity, req.unit_id
            FROM recipe_requirements req
            JOIN recipe_requirement_groups grp ON req.group_id = grp.id
            WHERE grp.food_item_id = :recipeId

            UNION ALL

            SELECT req.food_item_id, (req.quantity * bom.required_qty), req.unit_id
            FROM recipe_requirements req
            JOIN recipe_requirement_groups grp ON req.group_id = grp.id
            JOIN RecipeBOM bom ON grp.food_item_id = bom.food_item_id
        )
        SELECT * FROM RecipeBOM
    """)
    suspend fun getRecursiveIngredients(recipeId: Uuid): List<RecipeBOMItem>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun search(query: String): List<FoodItemEntity>

    @Upsert
    suspend fun upsertEntity(item: FoodItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<FoodItemEntity>)

    @Upsert
    suspend fun upsertPurchasable(comp: PurchasableComponentEntity)

    @Upsert
    suspend fun upsertAllPurchasable(items: List<PurchasableComponentEntity>)

    @Query("SELECT * FROM purchasable_components")
    suspend fun getAllPurchasable(): List<PurchasableComponentEntity>

    @Query("DELETE FROM purchasable_components")
    suspend fun clearAllPurchasable()

    @Upsert
    suspend fun upsertRecipe(comp: RecipeComponentEntity)

    @Upsert
    suspend fun upsertAllRecipeComponents(items: List<RecipeComponentEntity>)

    @Query("SELECT * FROM recipe_components")
    suspend fun getAllRecipeComponents(): List<RecipeComponentEntity>

    @Query("DELETE FROM recipe_components")
    suspend fun clearAllRecipeComponents()

    @Upsert
    suspend fun upsertLeftover(comp: LeftoverComponentEntity)

    @Upsert
    suspend fun upsertAllLeftoverComponents(items: List<LeftoverComponentEntity>)

    @Query("SELECT * FROM leftover_components")
    suspend fun getAllLeftoverComponents(): List<LeftoverComponentEntity>

    @Query("DELETE FROM leftover_components")
    suspend fun clearAllLeftoverComponents()

    @Transaction
    suspend fun upsertFoodItem(
        item: FoodItemEntity,
        purchasable: PurchasableComponentEntity? = null,
        recipe: RecipeComponentEntity? = null,
        leftover: LeftoverComponentEntity? = null,
        instructions: List<RecipeInstructionEntity> = emptyList(),
        requirementGroups: List<RecipeRequirementGroupEntity> = emptyList(),
        requirements: List<RecipeRequirementEntity> = emptyList(),
    ) {
        val updatedItem = item.copy(
            isPurchasable = purchasable != null,
            isRecipe = recipe != null,
            isLeftover = leftover != null
        )
        upsertEntity(updatedItem)

        // Components
        if (purchasable != null) upsertPurchasable(purchasable) else deletePurchasable(item.id)
        if (recipe != null) upsertRecipe(recipe) else deleteRecipeComponent(item.id)
        if (leftover != null) upsertLeftover(leftover) else deleteLeftoverComponent(item.id)

        // Nested Data
        deleteInstructions(item.id)
        if (instructions.isNotEmpty()) upsertAllInstructions(instructions)

        deleteRequirementGroups(item.id)
        if (requirementGroups.isNotEmpty()) {
            upsertAllRequirementGroups(requirementGroups)
            upsertAllRequirements(requirements)
        }
    }

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFoodItem(id: Uuid)

    @Query("DELETE FROM food_items")
    suspend fun clearAll()

    @Query("DELETE FROM purchasable_components WHERE food_item_id = :id")
    suspend fun deletePurchasable(id: Uuid)

    @Query("DELETE FROM recipe_components WHERE food_item_id = :id")
    suspend fun deleteRecipeComponent(id: Uuid)

    @Query("DELETE FROM leftover_components WHERE food_item_id = :id")
    suspend fun deleteLeftoverComponent(id: Uuid)

    @Upsert
    suspend fun upsertAllInstructions(instructions: List<RecipeInstructionEntity>)

    @Query("SELECT * FROM recipe_instructions")
    suspend fun getAllInstructions(): List<RecipeInstructionEntity>

    @Query("DELETE FROM recipe_instructions")
    suspend fun clearAllInstructions()

    @Query("DELETE FROM recipe_instructions WHERE food_item_id = :foodItemId")
    suspend fun deleteInstructions(foodItemId: Uuid)

    @Upsert
    suspend fun upsertAllRequirementGroups(groups: List<RecipeRequirementGroupEntity>)

    @Query("SELECT * FROM recipe_requirement_groups")
    suspend fun getAllRequirementGroups(): List<RecipeRequirementGroupEntity>

    @Query("DELETE FROM recipe_requirement_groups")
    suspend fun clearAllRequirementGroups()

    @Query("DELETE FROM recipe_requirement_groups WHERE food_item_id = :foodItemId")
    suspend fun deleteRequirementGroups(foodItemId: Uuid)

    @Upsert
    suspend fun upsertAllRequirements(requirements: List<RecipeRequirementEntity>)

    @Query("SELECT * FROM recipe_requirements")
    suspend fun getAllRequirements(): List<RecipeRequirementEntity>

    @Query("DELETE FROM recipe_requirements")
    suspend fun clearAllRequirements()

    @Query("SELECT * FROM unit_conversion_bridges")
    fun observeAllConversions(): Flow<List<UnitConversionBridgeEntity>>

    @Query("SELECT * FROM unit_conversion_bridges")
    suspend fun getAllConversions(): List<UnitConversionBridgeEntity>

    @Query("SELECT * FROM unit_conversion_bridges WHERE food_item_id = :foodItemId")
    suspend fun getConversionsByFoodItemId(foodItemId: Uuid): List<UnitConversionBridgeEntity>

    @Upsert
    suspend fun upsertConversion(bridge: UnitConversionBridgeEntity)

    @Upsert
    suspend fun upsertAllConversions(items: List<UnitConversionBridgeEntity>)

    @Query("DELETE FROM unit_conversion_bridges WHERE id = :id")
    suspend fun deleteConversion(id: Uuid)

    @Query("DELETE FROM unit_conversion_bridges")
    suspend fun clearAllConversions()
}

// ─────────────────────────────────────────────────────────────────────────────
// ScheduledMealDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface ScheduledMealDao {
    @Query(
        "SELECT * FROM scheduled_meals WHERE date BETWEEN :from AND :to ORDER BY date ASC, time ASC"
    )
    fun observeInRange(from: String, to: String): Flow<List<ScheduledMealEntity>>

    @Query("SELECT * FROM scheduled_meals ORDER BY date ASC, time ASC")
    suspend fun getAll(): List<ScheduledMealEntity>

    @Query("SELECT * FROM scheduled_meals ORDER BY date ASC, time ASC")
    fun observeAll(): Flow<List<ScheduledMealEntity>>

    @Transaction
    @Query("SELECT * FROM scheduled_meals WHERE date BETWEEN :from AND :to ORDER BY date ASC, time ASC")
    fun observeInRangeWithSource(from: String, to: String): Flow<List<ScheduledMealWithSource>>

    @Upsert
    suspend fun upsert(meal: ScheduledMealEntity)

    @Upsert
    suspend fun upsertAll(meals: List<ScheduledMealEntity>)

    @Delete
    suspend fun delete(meal: ScheduledMealEntity)

    @Query("UPDATE scheduled_meals SET is_consumed = :consumed WHERE id = :id")
    suspend fun setConsumed(id: Uuid, consumed: Boolean)

    @Transaction
    @Query("SELECT * FROM scheduled_meals WHERE id = :id")
    suspend fun getWithSource(id: Uuid): ScheduledMealWithSource?

    @Query("DELETE FROM scheduled_meals")
    suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────────────────────
// PurchaseOptionDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface PurchaseOptionDao {
    @Query("SELECT * FROM purchase_options ORDER BY food_item_id ASC")
    fun observeAll(): Flow<List<PurchaseOptionEntity>>

    @Query("SELECT * FROM purchase_options")
    suspend fun getAll(): List<PurchaseOptionEntity>

    @Query("SELECT * FROM purchase_options WHERE food_item_id = :foodItemId ORDER BY price_cents ASC")
    fun observeForFoodItem(foodItemId: Uuid): Flow<List<PurchaseOptionEntity>>

    @Query("""
        SELECT * FROM purchase_options 
        WHERE food_item_id = :foodItemId 
        AND quantity > 0
        ORDER BY (price_cents / quantity) ASC LIMIT 1
    """)
    suspend fun getCheapestOption(foodItemId: Uuid): PurchaseOptionEntity?

    @Query("SELECT * FROM purchase_options WHERE id = :id")
    suspend fun getById(id: Uuid): PurchaseOptionEntity?

    @Upsert
    suspend fun upsert(option: PurchaseOptionEntity)

    @Upsert
    suspend fun upsertAll(options: List<PurchaseOptionEntity>)

    @Delete
    suspend fun delete(option: PurchaseOptionEntity)

    @Query("DELETE FROM purchase_options WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Query("DELETE FROM purchase_options")
    suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────────────────────
// PantryDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface PantryDao {
    @Transaction
    @Query("SELECT * FROM pantry_inventory")
    fun observeAllWithDetails(): Flow<List<PantryInventoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM pantry_inventory")
    suspend fun getAllWithDetails(): List<PantryInventoryWithDetails>

    @Query("SELECT * FROM pantry_inventory")
    suspend fun getAll(): List<PantryInventoryEntity>

    @Query("SELECT * FROM pantry_inventory WHERE id = :id")
    suspend fun getById(id: Uuid): PantryInventoryEntity?

    @Query("SELECT * FROM pantry_inventory WHERE food_item_id = :foodItemId LIMIT 1")
    suspend fun getByFoodItemId(foodItemId: Uuid): PantryInventoryEntity?

    @Query("DELETE FROM pantry_inventory WHERE food_item_id = :foodItemId AND unit_id = :unitId")
    suspend fun deleteByFoodItemAndUnit(foodItemId: Uuid, unitId: Uuid)

    @Query("DELETE FROM pantry_inventory WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Upsert
    suspend fun upsert(item: PantryInventoryEntity)

    @Upsert
    suspend fun upsertAll(items: List<PantryInventoryEntity>)

    @Delete
    suspend fun delete(item: PantryInventoryEntity)

    @Query("DELETE FROM pantry_inventory")
    suspend fun clearAll()

    @Transaction
    suspend fun updateQuantities(
        updates: List<io.github.and19081.mealplanner.domain.repository.PantryUpdate>
    ) {
        updates.forEach { update ->
            val existing = getByFoodItemId(update.foodItemId)
            if (existing != null) {
                if (update.newQuantity <= 0) {
                    delete(existing)
                } else {
                    upsert(
                        existing.copy(
                            measurement =
                                existing.measurement.copy(
                                    quantity = update.newQuantity,
                                    unitId = update.unitId,
                                )
                        )
                    )
                }
            } else if (update.newQuantity > 0) {
                upsert(
                    PantryInventoryEntity(
                        measurement =
                            io.github.and19081.mealplanner.data.db.entity.ItemMeasurement(
                                foodItemId = update.foodItemId,
                                quantity = update.newQuantity,
                                unitId = update.unitId,
                            )
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ShoppingListDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface ShoppingListDao {
    @Transaction
    @Query("SELECT * FROM shopping_cart_items ORDER BY is_purchased ASC, store_id ASC")
    fun observeAllWithDetails(): Flow<List<ShoppingCartItemWithDetails>>

    @Query("SELECT * FROM shopping_cart_items")
    suspend fun getAll(): List<ShoppingCartItemEntity>

    @Query("SELECT * FROM shopping_cart_items WHERE id = :id")
    suspend fun getById(id: Uuid): ShoppingCartItemEntity?

    @Query("DELETE FROM shopping_cart_items WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Upsert
    suspend fun upsert(item: ShoppingCartItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<ShoppingCartItemEntity>)

    @Delete
    suspend fun delete(item: ShoppingCartItemEntity)

    @Query("UPDATE shopping_cart_items SET is_purchased = :purchased WHERE id = :id")
    suspend fun setPurchased(id: Uuid, purchased: Boolean)

    @Query("DELETE FROM shopping_cart_items WHERE is_purchased = 1")
    suspend fun clearPurchased()

    @Query("DELETE FROM shopping_cart_items")
    suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────────────────────
// ReceiptDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface ReceiptDao {
    @Transaction
    @Query("SELECT * FROM store_receipts ORDER BY date DESC")
    fun observeAllWithLineItems(): Flow<List<StoreReceiptWithLineItems>>

    @Query("SELECT * FROM store_receipts")
    suspend fun getAllReceipts(): List<StoreReceiptEntity>

    @Query("""
        SELECT strftime('%Y-%m', date) as month, SUM(actual_total_cents) as totalSpent 
        FROM store_receipts 
        GROUP BY month
        ORDER BY month DESC
    """)
    fun observeMonthlyExpenditures(): Flow<List<MonthlyExpenditure>>

    @Query("SELECT * FROM receipt_line_items")
    suspend fun getAllLineItems(): List<ReceiptLineItemEntity>

    @Upsert
    suspend fun upsertReceipt(receipt: StoreReceiptEntity)

    @Upsert
    suspend fun upsertAllReceipts(receipts: List<StoreReceiptEntity>)

    @Upsert
    suspend fun upsertAllLineItems(items: List<ReceiptLineItemEntity>)

    @Transaction
    suspend fun upsertReceiptWithDetails(
        receipt: StoreReceiptEntity,
        lineItems: List<ReceiptLineItemEntity>,
    ) {
        upsertReceipt(receipt)
        clearLineItemsForReceipt(receipt.id)
        upsertAllLineItems(lineItems)
    }

    @Query("DELETE FROM receipt_line_items WHERE receipt_id = :receiptId")
    suspend fun clearLineItemsForReceipt(receiptId: Uuid)

    @Delete
    suspend fun deleteReceipt(receipt: StoreReceiptEntity)

    @Query("DELETE FROM store_receipts")
    suspend fun clearAllReceipts()

    @Query("DELETE FROM receipt_line_items")
    suspend fun clearAllLineItems()
}

// ─────────────────────────────────────────────────────────────────────────────
// AppSettingsDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface AppSettingsDao {
    @Query(
        "SELECT * FROM app_settings WHERE id = '${AppSettingsEntity.Companion.SINGLETON_ID}' LIMIT 1"
    )
    fun observe(): Flow<AppSettingsEntity?>

    @Query(
        "SELECT * FROM app_settings WHERE id = '${AppSettingsEntity.Companion.SINGLETON_ID}' LIMIT 1"
    )
    suspend fun get(): AppSettingsEntity?

    @Upsert
    suspend fun upsert(settings: AppSettingsEntity)

    @Query("DELETE FROM app_settings")
    suspend fun clearAll()
}
