package io.github.and19081.mealplanner.data.db.dao

import androidx.room.*
import io.github.and19081.mealplanner.data.db.entity.*
import io.github.and19081.mealplanner.data.db.relation.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// UnitDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface UnitDao {

  @Query("SELECT * FROM units ORDER BY name ASC") fun observeAll(): Flow<List<UnitEntity>>

  @Query("SELECT * FROM units WHERE id = :id")
  suspend fun getById(id: kotlin.uuid.Uuid): UnitEntity?

  @Query("SELECT * FROM units WHERE unit_type = :type ORDER BY name ASC")
  suspend fun getByType(type: String): List<UnitEntity>

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

  @Query("SELECT * FROM stores WHERE id = :id")
  suspend fun getById(id: kotlin.uuid.Uuid): StoreEntity?

  @Transaction
  @Query("SELECT * FROM stores WHERE id = :storeId")
  fun observeStoreWithReceipts(storeId: kotlin.uuid.Uuid): Flow<StoreWithReceipts?>

  @Upsert suspend fun upsert(store: StoreEntity)

  @Delete suspend fun delete(store: StoreEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// RestaurantDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface RestaurantDao {

  @Query("SELECT * FROM restaurants ORDER BY name ASC")
  fun observeAll(): Flow<List<RestaurantEntity>>

  @Upsert suspend fun upsert(restaurant: RestaurantEntity)

  @Delete suspend fun delete(restaurant: RestaurantEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// IngredientDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface IngredientDao {

  @Query("SELECT COUNT(*) FROM ingredients") suspend fun count(): Int

  @Query("SELECT * FROM ingredients ORDER BY name ASC")
  fun observeAll(): Flow<List<IngredientEntity>>

  @Query("SELECT * FROM ingredients WHERE id = :id")
  suspend fun getById(id: kotlin.uuid.Uuid): IngredientEntity?

  @Query("SELECT * FROM ingredients WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
  suspend fun search(query: String): List<IngredientEntity>

  @Transaction
  @Query("SELECT * FROM ingredients WHERE id = :id")
  suspend fun getWithCategories(id: kotlin.uuid.Uuid): IngredientWithCategories?

  @Transaction
  @Query("SELECT * FROM ingredients ORDER BY name ASC")
  fun observeAllWithCategories(): Flow<List<IngredientWithCategories>>

  @Transaction
  @Query("SELECT * FROM ingredients WHERE id = :id")
  suspend fun getWithConversions(id: kotlin.uuid.Uuid): IngredientWithConversions?

  @Upsert suspend fun upsert(ingredient: IngredientEntity)

  @Delete suspend fun delete(ingredient: IngredientEntity)

  @Transaction
  suspend fun upsertIngredientWithDetails(
      ingredient: IngredientEntity,
      categoryId: kotlin.uuid.Uuid?,
      packages: List<PackageOptionEntity>,
      bridges: List<UnitConversionBridgeEntity>,
  ) {
    upsert(ingredient)

    // Categories
    clearCategories(ingredient.id)
    if (categoryId != null && categoryId != kotlin.uuid.Uuid.NIL) {
      addToCategory(IngredientCategoryEntity(ingredient.id, categoryId))
    }

    // Packages
    clearPackages(ingredient.id)
    insertPackages(packages)

    // Bridges
    clearConversions(ingredient.id)
    insertConversions(bridges)
  }

  @Query("DELETE FROM package_options WHERE ingredient_id = :ingredientId")
  suspend fun clearPackages(ingredientId: kotlin.uuid.Uuid)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPackages(packages: List<PackageOptionEntity>)

  @Query("DELETE FROM unit_conversion_bridges WHERE ingredient_id = :ingredientId")
  suspend fun clearConversions(ingredientId: kotlin.uuid.Uuid)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversions(bridges: List<UnitConversionBridgeEntity>)

  // ── Category associations ──────────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun addToCategory(cross: IngredientCategoryEntity)

  @Delete suspend fun removeFromCategory(cross: IngredientCategoryEntity)

  @Query("DELETE FROM ingredient_categories WHERE ingredient_id = :ingredientId")
  suspend fun clearCategories(ingredientId: kotlin.uuid.Uuid)

  // ── Unit conversion bridges ────────────────────────────────────────────

  @Upsert suspend fun upsertConversionBridge(bridge: UnitConversionBridgeEntity)

  @Delete suspend fun deleteConversionBridge(bridge: UnitConversionBridgeEntity)

  @Query("SELECT * FROM unit_conversion_bridges")
  fun observeAllConversions(): Flow<List<UnitConversionBridgeEntity>>

  @Query("DELETE FROM unit_conversion_bridges WHERE id = :id")
  suspend fun deleteConversionById(id: kotlin.uuid.Uuid)
}

// ─────────────────────────────────────────────────────────────────────────────
// RecipeDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface RecipeDao {

  @Query("SELECT * FROM recipes ORDER BY name ASC") fun observeAll(): Flow<List<RecipeEntity>>

  @Query("SELECT * FROM recipes WHERE id = :id")
  suspend fun getById(id: kotlin.uuid.Uuid): RecipeEntity?

  @Query(
      "SELECT * FROM recipes WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'"
  )
  suspend fun search(query: String): List<RecipeEntity>

  @Query("SELECT * FROM recipes WHERE meal_type = :mealType ORDER BY name ASC")
  fun observeByMealType(mealType: String): Flow<List<RecipeEntity>>

  @Transaction
  @Query("SELECT * FROM recipes WHERE id = :id")
  suspend fun getWithDetails(id: kotlin.uuid.Uuid): RecipeWithDetails?

  @Transaction
  @Query("SELECT * FROM recipes ORDER BY name ASC")
  fun observeAllWithDetails(): Flow<List<RecipeWithDetails>>

  @Query(
      """
        SELECT rrg.recipe_id as recipe_id, 
               SUM(rr.quantity * (SELECT MIN(po.price_cents / po.quantity) FROM package_options po WHERE po.ingredient_id = rr.ingredient_id)) as total_cost_cents
        FROM recipe_requirements rr
        JOIN recipe_requirement_groups rrg ON rr.group_id = rrg.id
        WHERE rr.is_primary = 1
        GROUP BY rrg.recipe_id
    """
  )
  fun observeRecipeCosts(): Flow<List<RecipeCost>>

  @Upsert suspend fun upsertRecipe(recipe: RecipeEntity)

  @Delete suspend fun deleteRecipe(recipe: RecipeEntity)

  @Transaction
  suspend fun upsertRecipeWithDetails(
      recipe: RecipeEntity,
      instructions: List<RecipeInstructionEntity>,
      requirementGroups: List<RecipeRequirementGroupEntity>,
      requirements: List<RecipeRequirementEntity>,
  ) {
    upsertRecipe(recipe)
    replaceInstructions(recipe.id, instructions)
    clearRequirementGroups(recipe.id)
    insertRequirementGroups(requirementGroups)
    insertRequirements(requirements)
  }

  // ── Instructions ──────────────────────────────────────────────────────

  /** Replace all instructions for a recipe atomically. */
  @Transaction
  suspend fun replaceInstructions(
      recipeId: kotlin.uuid.Uuid,
      instructions: List<RecipeInstructionEntity>,
  ) {
    deleteInstructions(recipeId)
    insertInstructions(instructions)
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertInstructions(instructions: List<RecipeInstructionEntity>)

  @Query("DELETE FROM recipe_instructions WHERE recipe_id = :recipeId")
  suspend fun deleteInstructions(recipeId: kotlin.uuid.Uuid)

  @Query("SELECT * FROM recipe_instructions WHERE recipe_id = :recipeId ORDER BY step_order ASC")
  suspend fun getInstructions(recipeId: kotlin.uuid.Uuid): List<RecipeInstructionEntity>

  // ── Requirements ──────────────────────────────────────────────────────

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRequirementGroups(groups: List<RecipeRequirementGroupEntity>)

  @Query("DELETE FROM recipe_requirement_groups WHERE recipe_id = :recipeId")
  suspend fun clearRequirementGroups(recipeId: kotlin.uuid.Uuid)

  @Upsert suspend fun upsertRequirement(requirement: RecipeRequirementEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRequirements(requirements: List<RecipeRequirementEntity>)

  @Delete suspend fun deleteRequirement(requirement: RecipeRequirementEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// PrePlannedMealDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface PrePlannedMealDao {

  @Query("SELECT * FROM pre_planned_meals ORDER BY name ASC")
  fun observeAll(): Flow<List<PrePlannedMealEntity>>

  @Query("SELECT * FROM pre_planned_meals WHERE id = :id")
  suspend fun getById(id: kotlin.uuid.Uuid): PrePlannedMealEntity?

  @Transaction
  @Query("SELECT * FROM pre_planned_meals WHERE id = :id")
  suspend fun getWithDetails(id: kotlin.uuid.Uuid): PrePlannedMealWithRecipes?

  @Transaction
  @Query("SELECT * FROM pre_planned_meals ORDER BY name ASC")
  fun observeAllWithDetails(): Flow<List<PrePlannedMealWithRecipes>>

  @Query(
      """
        SELECT m.id as meal_id,
               (COALESCE((SELECT SUM(rc.total_cost_cents) 
                 FROM meal_recipes mr 
                 JOIN (SELECT rrg.recipe_id, SUM(rr.quantity * (SELECT MIN(po.price_cents / po.quantity) FROM package_options po WHERE po.ingredient_id = rr.ingredient_id)) as total_cost_cents
                       FROM recipe_requirements rr
                       JOIN recipe_requirement_groups rrg ON rr.group_id = rrg.id
                       WHERE rr.is_primary = 1
                       GROUP BY rrg.recipe_id) rc ON mr.recipe_id = rc.recipe_id
                 WHERE mr.pre_planned_meal_id = m.id), 0) +
                COALESCE((SELECT SUM(mii.quantity * (SELECT MIN(po.price_cents / po.quantity) FROM package_options po WHERE po.ingredient_id = mii.ingredient_id))
                 FROM meal_independent_ingredients mii
                 WHERE mii.pre_planned_meal_id = m.id), 0)) as total_cost_cents
        FROM pre_planned_meals m
    """
  )
  fun observeMealCosts(): Flow<List<MealCost>>

  @Upsert suspend fun upsert(meal: PrePlannedMealEntity)

  @Delete suspend fun delete(meal: PrePlannedMealEntity)

  @Transaction
  suspend fun upsertMealWithDetails(
      meal: PrePlannedMealEntity,
      recipes: List<MealRecipeEntity>,
      independentIngredients: List<MealIndependentIngredientEntity>,
  ) {
    upsert(meal)
    // Note: Currently there is no clearRecipes or clearIndependentIngredients.
    // I should probably add them if I want true sync, but the prompt didn't mention it for meals.
    // However, it's safer to have them.
    clearRecipes(meal.id)
    clearIndependentIngredients(meal.id)
    addRecipes(recipes)
    upsertIndependentIngredients(independentIngredients)
  }

  @Query("DELETE FROM meal_recipes WHERE pre_planned_meal_id = :mealId")
  suspend fun clearRecipes(mealId: kotlin.uuid.Uuid)

  @Query("DELETE FROM meal_independent_ingredients WHERE pre_planned_meal_id = :mealId")
  suspend fun clearIndependentIngredients(mealId: kotlin.uuid.Uuid)

  @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addRecipe(cross: MealRecipeEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun addRecipes(crosses: List<MealRecipeEntity>)

  @Delete suspend fun removeRecipe(cross: MealRecipeEntity)

  @Upsert suspend fun upsertIndependentIngredient(item: MealIndependentIngredientEntity)

  @Upsert suspend fun upsertIndependentIngredients(items: List<MealIndependentIngredientEntity>)

  @Delete suspend fun deleteIndependentIngredient(item: MealIndependentIngredientEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// ScheduledMealDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface ScheduledMealDao {

  /** Observe all scheduled meals in a date range (ISO-8601 strings, inclusive). */
  @Query(
      "SELECT * FROM scheduled_meals WHERE date BETWEEN :from AND :to ORDER BY date ASC, time ASC"
  )
  fun observeInRange(from: String, to: String): Flow<List<ScheduledMealEntity>>

  @Transaction
  @Query("SELECT * FROM scheduled_meals WHERE id = :id")
  suspend fun getWithSource(id: kotlin.uuid.Uuid): ScheduledMealWithSource?

  @Transaction
  @Query(
      "SELECT * FROM scheduled_meals WHERE date BETWEEN :from AND :to ORDER BY date ASC, time ASC"
  )
  fun observeInRangeWithSource(
      from: String,
      to: String,
  ): Flow<List<ScheduledMealWithSource>>

  @Query("DELETE FROM scheduled_meals") suspend fun clearAll()

  @Upsert suspend fun upsert(meal: ScheduledMealEntity)

  @Delete suspend fun delete(meal: ScheduledMealEntity)

  @Query("UPDATE scheduled_meals SET is_consumed = :consumed WHERE id = :id")
  suspend fun setConsumed(id: kotlin.uuid.Uuid, consumed: Boolean)
}

// ─────────────────────────────────────────────────────────────────────────────
// PackageOptionDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface PackageOptionDao {

  @Query(
      "SELECT * FROM package_options WHERE ingredient_id = :ingredientId ORDER BY price_cents ASC"
  )
  fun observeForIngredient(ingredientId: kotlin.uuid.Uuid): Flow<List<PackageOptionEntity>>

  @Query("SELECT * FROM package_options WHERE store_id = :storeId ORDER BY ingredient_id ASC")
  suspend fun getForStore(storeId: kotlin.uuid.Uuid): List<PackageOptionEntity>

  @Query("SELECT * FROM package_options WHERE id = :id")
  suspend fun getById(id: kotlin.uuid.Uuid): PackageOptionEntity?

  @Query("SELECT * FROM package_options ORDER BY ingredient_id ASC")
  fun observeAll(): Flow<List<PackageOptionEntity>>

  @Query("DELETE FROM package_options WHERE store_id = :storeId")
  suspend fun removeByStore(storeId: kotlin.uuid.Uuid)

  @Upsert suspend fun upsert(option: PackageOptionEntity)

  @Delete suspend fun delete(option: PackageOptionEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// PantryDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface PantryDao {

  @Query("SELECT * FROM pantry_inventory") fun observeAll(): Flow<List<PantryInventoryEntity>>

  @Transaction
  @Query("SELECT * FROM pantry_inventory")
  fun observeAllWithDetails(): Flow<List<PantryInventoryWithDetails>>

  @Query("SELECT * FROM pantry_inventory WHERE ingredient_id = :ingredientId")
  suspend fun getForIngredient(ingredientId: kotlin.uuid.Uuid): List<PantryInventoryEntity>

  @Upsert suspend fun upsert(item: PantryInventoryEntity)

  @Delete suspend fun delete(item: PantryInventoryEntity)

  @Query("DELETE FROM pantry_inventory WHERE ingredient_id = :ingredientId AND unit_id = :unitId")
  suspend fun remove(ingredientId: kotlin.uuid.Uuid, unitId: kotlin.uuid.Uuid)
}

// ─────────────────────────────────────────────────────────────────────────────
// ShoppingListDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface ShoppingListDao {

  @Query("SELECT * FROM shopping_cart_items ORDER BY is_purchased ASC, store_id ASC")
  fun observeAll(): Flow<List<ShoppingCartItemEntity>>

  @Transaction
  @Query("SELECT * FROM shopping_cart_items ORDER BY is_purchased ASC, store_id ASC")
  fun observeAllWithDetails(): Flow<List<ShoppingCartItemWithDetails>>

  @Query("SELECT * FROM shopping_cart_items WHERE store_id = :storeId ORDER BY is_purchased ASC")
  fun observeForStore(storeId: kotlin.uuid.Uuid): Flow<List<ShoppingCartItemEntity>>

  @Query("DELETE FROM shopping_cart_items WHERE id = :id")
  suspend fun deleteById(id: kotlin.uuid.Uuid)

  @Upsert suspend fun upsert(item: ShoppingCartItemEntity)

  @Delete suspend fun delete(item: ShoppingCartItemEntity)

  @Query("UPDATE shopping_cart_items SET is_purchased = :purchased WHERE id = :id")
  suspend fun setPurchased(id: kotlin.uuid.Uuid, purchased: Boolean)

  @Query("DELETE FROM shopping_cart_items WHERE is_purchased = 1") suspend fun clearPurchased()

  @Query("DELETE FROM shopping_cart_items") suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────────────────────
// ReceiptDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface ReceiptDao {

  @Query("SELECT * FROM store_receipts ORDER BY date DESC")
  fun observeAll(): Flow<List<StoreReceiptEntity>>

  @Query("SELECT * FROM store_receipts WHERE store_id = :storeId ORDER BY date DESC")
  fun observeForStore(storeId: kotlin.uuid.Uuid): Flow<List<StoreReceiptEntity>>

  @Transaction
  @Query("SELECT * FROM store_receipts WHERE id = :id")
  suspend fun getWithLineItems(id: kotlin.uuid.Uuid): StoreReceiptWithLineItems?

  @Transaction
  @Query("SELECT * FROM store_receipts ORDER BY date DESC")
  fun observeAllWithLineItems(): Flow<List<StoreReceiptWithLineItems>>

  @Upsert suspend fun upsertReceipt(receipt: StoreReceiptEntity)

  @Upsert suspend fun upsertLineItem(item: ReceiptLineItemEntity)

  @Upsert suspend fun upsertLineItems(items: List<ReceiptLineItemEntity>)

  @Transaction
  suspend fun upsertReceiptWithDetails(
      receipt: StoreReceiptEntity,
      lineItems: List<ReceiptLineItemEntity>,
  ) {
    upsertReceipt(receipt)
    clearLineItems(receipt.id)
    upsertLineItems(lineItems)
  }

  @Query("DELETE FROM receipt_line_items WHERE receipt_id = :receiptId")
  suspend fun clearLineItems(receiptId: kotlin.uuid.Uuid)

  @Delete suspend fun deleteReceipt(receipt: StoreReceiptEntity)

  @Delete suspend fun deleteLineItem(item: ReceiptLineItemEntity)

  /** Recalculate projected total from package options and store it. */
  @Query(
      """
        UPDATE store_receipts
        SET projected_total_cents = (
            SELECT SUM(po.price_cents)
            FROM shopping_cart_items sci
            JOIN package_options po ON sci.package_option_id = po.id
            WHERE sci.store_id = store_receipts.store_id
        )
        WHERE id = :receiptId
        """
  )
  suspend fun refreshProjectedTotal(receiptId: kotlin.uuid.Uuid)
}

// ─────────────────────────────────────────────────────────────────────────────
// AppSettingsDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface AppSettingsDao {

  /**
   * Returns the single settings row or null if not yet initialized. Call [upsert] with a default
   * [AppSettingsEntity] to seed on first launch.
   */
  @Query("SELECT * FROM app_settings WHERE id = '${AppSettingsEntity.SINGLETON_ID}' LIMIT 1")
  fun observe(): Flow<AppSettingsEntity?>

  @Query("SELECT * FROM app_settings WHERE id = '${AppSettingsEntity.SINGLETON_ID}' LIMIT 1")
  suspend fun get(): AppSettingsEntity?

  /** Inserts or fully replaces the singleton settings row. */
  @Upsert suspend fun upsert(settings: AppSettingsEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// LeftoverDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface LeftoverDao {
  @Query("SELECT * FROM leftover_inventory") fun observeAll(): Flow<List<LeftoverInventoryEntity>>

  @Upsert suspend fun upsert(leftover: LeftoverInventoryEntity)

  @Delete suspend fun delete(leftover: LeftoverInventoryEntity)

  @Query("DELETE FROM leftover_inventory WHERE id = :id")
  suspend fun removeById(id: kotlin.uuid.Uuid)
}
