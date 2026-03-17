package io.github.and19081.mealplanner.data.db.dao

import androidx.room.*
import io.github.and19081.mealplanner.data.db.entity.*
import io.github.and19081.mealplanner.data.db.relation.*
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

// ─────────────────────────────────────────────────────────────────────────────
// UnitDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface UnitDao {
  @Query("SELECT * FROM units ORDER BY name ASC") fun observeAll(): Flow<List<UnitEntity>>
  @Query("SELECT * FROM units WHERE id = :id") suspend fun getById(id: Uuid): UnitEntity?
  @Query("SELECT * FROM units WHERE unit_type = :type ORDER BY name ASC") suspend fun getByType(type: String): List<UnitEntity>
  @Upsert suspend fun upsert(unit: UnitEntity)
  @Delete suspend fun delete(unit: UnitEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface CategoryDao {
  @Query("SELECT * FROM categories ORDER BY name ASC") fun observeAll(): Flow<List<CategoryEntity>>
  @Upsert suspend fun upsert(category: CategoryEntity)
  @Delete suspend fun delete(category: CategoryEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// StoreDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface StoreDao {
  @Query("SELECT * FROM stores ORDER BY name ASC") fun observeAll(): Flow<List<StoreEntity>>
  @Query("SELECT * FROM stores WHERE id = :id") suspend fun getById(id: Uuid): StoreEntity?
  @Upsert suspend fun upsert(store: StoreEntity)
  @Delete suspend fun delete(store: StoreEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// RestaurantDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface RestaurantDao {
  @Query("SELECT * FROM restaurants ORDER BY name ASC") fun observeAll(): Flow<List<RestaurantEntity>>
  @Upsert suspend fun upsert(restaurant: RestaurantEntity)
  @Delete suspend fun delete(restaurant: RestaurantEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// ECS: FoodItemDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun observeAll(): Flow<List<FoodItemEntity>>

    @Transaction
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun observeAllComposed(): Flow<List<ComposedFoodItemRelation>>

    @Transaction
    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getComposedById(id: Uuid): ComposedFoodItemRelation?

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun search(query: String): List<FoodItemEntity>

    @Upsert suspend fun upsertEntity(item: FoodItemEntity)
    @Upsert suspend fun upsertPurchasable(comp: PurchasableComponentEntity)
    @Upsert suspend fun upsertRecipe(comp: RecipeComponentEntity)
    @Upsert suspend fun upsertLeftover(comp: LeftoverComponentEntity)
    
    @Transaction
    suspend fun upsertFoodItem(
        item: FoodItemEntity,
        purchasable: PurchasableComponentEntity? = null,
        recipe: RecipeComponentEntity? = null,
        leftover: LeftoverComponentEntity? = null,
        instructions: List<RecipeInstructionEntity> = emptyList(),
        requirementGroups: List<RecipeRequirementGroupEntity> = emptyList(),
        requirements: List<RecipeRequirementEntity> = emptyList()
    ) {
        upsertEntity(item)
        
        // Components
        if (purchasable != null) upsertPurchasable(purchasable) else deletePurchasable(item.id)
        if (recipe != null) upsertRecipe(recipe) else deleteRecipeComponent(item.id)
        if (leftover != null) upsertLeftover(leftover) else deleteLeftoverComponent(item.id)
        
        // Nested Data
        deleteInstructions(item.id)
        if (instructions.isNotEmpty()) insertInstructions(instructions)
        
        deleteRequirementGroups(item.id)
        if (requirementGroups.isNotEmpty()) {
            insertRequirementGroups(requirementGroups)
            insertRequirements(requirements)
        }
    }

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFoodItem(id: Uuid)

    @Query("DELETE FROM purchasable_components WHERE food_item_id = :id")
    suspend fun deletePurchasable(id: Uuid)

    @Query("DELETE FROM recipe_components WHERE food_item_id = :id")
    suspend fun deleteRecipeComponent(id: Uuid)

    @Query("DELETE FROM leftover_components WHERE food_item_id = :id")
    suspend fun deleteLeftoverComponent(id: Uuid)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstructions(instructions: List<RecipeInstructionEntity>)

    @Query("DELETE FROM recipe_instructions WHERE food_item_id = :foodItemId")
    suspend fun deleteInstructions(foodItemId: Uuid)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequirementGroups(groups: List<RecipeRequirementGroupEntity>)

    @Query("DELETE FROM recipe_requirement_groups WHERE food_item_id = :foodItemId")
    suspend fun deleteRequirementGroups(foodItemId: Uuid)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequirements(requirements: List<RecipeRequirementEntity>)

    @Query("SELECT * FROM unit_conversion_bridges")
    fun observeAllConversions(): Flow<List<UnitConversionBridgeEntity>>

    @Upsert suspend fun upsertConversion(bridge: UnitConversionBridgeEntity)
    @Query("DELETE FROM unit_conversion_bridges WHERE id = :id")
    suspend fun deleteConversion(id: Uuid)
}

// ─────────────────────────────────────────────────────────────────────────────
// ScheduledMealDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface ScheduledMealDao {
  @Query("SELECT * FROM scheduled_meals WHERE date BETWEEN :from AND :to ORDER BY date ASC, time ASC")
  fun observeInRange(from: String, to: String): Flow<List<ScheduledMealEntity>>

  @Transaction
  @Query("SELECT * FROM scheduled_meals WHERE date BETWEEN :from AND :to ORDER BY date ASC, time ASC")
  fun observeInRangeWithSource(from: String, to: String): Flow<List<ScheduledMealWithSource>>

  @Upsert suspend fun upsert(meal: ScheduledMealEntity)
  @Delete suspend fun delete(meal: ScheduledMealEntity)
  @Query("UPDATE scheduled_meals SET is_consumed = :consumed WHERE id = :id")
  suspend fun setConsumed(id: Uuid, consumed: Boolean)

  @Transaction
  @Query("SELECT * FROM scheduled_meals WHERE id = :id")
  suspend fun getWithSource(id: Uuid): ScheduledMealWithSource?

  @Query("DELETE FROM scheduled_meals")
  suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────────────────────
// PackageOptionDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface PackageOptionDao {
  @Query("SELECT * FROM package_options ORDER BY food_item_id ASC")
  fun observeAll(): Flow<List<PackageOptionEntity>>
  @Query("SELECT * FROM package_options WHERE food_item_id = :foodItemId ORDER BY price_cents ASC")
  fun observeForFoodItem(foodItemId: Uuid): Flow<List<PackageOptionEntity>>
  @Query("SELECT * FROM package_options WHERE id = :id")
  suspend fun getById(id: Uuid): PackageOptionEntity?
  @Upsert suspend fun upsert(option: PackageOptionEntity)
  @Delete suspend fun delete(option: PackageOptionEntity)
  @Query("DELETE FROM package_options WHERE id = :id")
  suspend fun deleteById(id: Uuid)
}

// ─────────────────────────────────────────────────────────────────────────────
// PantryDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface PantryDao {
  @Transaction
  @Query("SELECT * FROM pantry_inventory")
  fun observeAllWithDetails(): Flow<List<PantryInventoryWithDetails>>
  @Upsert suspend fun upsert(item: PantryInventoryEntity)
  @Delete suspend fun delete(item: PantryInventoryEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// ShoppingListDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface ShoppingListDao {
  @Transaction
  @Query("SELECT * FROM shopping_cart_items ORDER BY is_purchased ASC, store_id ASC")
  fun observeAllWithDetails(): Flow<List<ShoppingCartItemWithDetails>>
  @Upsert suspend fun upsert(item: ShoppingCartItemEntity)
  @Delete suspend fun delete(item: ShoppingCartItemEntity)
  @Query("UPDATE shopping_cart_items SET is_purchased = :purchased WHERE id = :id")
  suspend fun setPurchased(id: Uuid, purchased: Boolean)
  @Query("DELETE FROM shopping_cart_items WHERE is_purchased = 1") suspend fun clearPurchased()
  @Query("DELETE FROM shopping_cart_items") suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────────────────────
// ReceiptDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface ReceiptDao {
  @Transaction
  @Query("SELECT * FROM store_receipts ORDER BY date DESC")
  fun observeAllWithLineItems(): Flow<List<StoreReceiptWithLineItems>>
  @Upsert suspend fun upsertReceipt(receipt: StoreReceiptEntity)
  @Upsert suspend fun upsertLineItems(items: List<ReceiptLineItemEntity>)
  @Transaction
  suspend fun upsertReceiptWithDetails(receipt: StoreReceiptEntity, lineItems: List<ReceiptLineItemEntity>) {
    upsertReceipt(receipt)
    clearLineItems(receipt.id)
    upsertLineItems(lineItems)
  }
  @Query("DELETE FROM receipt_line_items WHERE receipt_id = :receiptId")
  suspend fun clearLineItems(receiptId: Uuid)
  @Delete suspend fun deleteReceipt(receipt: StoreReceiptEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// AppSettingsDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface AppSettingsDao {
  @Query("SELECT * FROM app_settings WHERE id = '${AppSettingsEntity.SINGLETON_ID}' LIMIT 1")
  fun observe(): Flow<AppSettingsEntity?>
  @Query("SELECT * FROM app_settings WHERE id = '${AppSettingsEntity.SINGLETON_ID}' LIMIT 1")
  suspend fun get(): AppSettingsEntity?
  @Upsert suspend fun upsert(settings: AppSettingsEntity)
}
