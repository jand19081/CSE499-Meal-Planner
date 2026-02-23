package io.github.and19081.mealplanner.data

import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.data.db.entity.*
import io.github.and19081.mealplanner.data.db.relation.*
import io.github.and19081.mealplanner.ingredients.*
import io.github.and19081.mealplanner.shoppinglist.ShoppingListItem
import io.github.and19081.mealplanner.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.settings.AppSettings
import io.github.and19081.mealplanner.settings.DashboardConfig as UiDashboardConfig
import kotlin.uuid.Uuid

// --- Unit Mappers ---
fun UnitEntity.toModel(): UnitModel = UnitModel(
    id = id,
    type = unitType,
    abbreviation = abbreviation ?: "",
    displayName = displayName ?: name,
    isSystemUnit = isSystemUnit,
    factorToBase = factorToBase ?: 1.0
)

fun UnitModel.toEntity(): UnitEntity = UnitEntity(
    id = id,
    name = displayName,
    abbreviation = abbreviation,
    displayName = displayName,
    isSystemUnit = isSystemUnit,
    factorToBase = factorToBase,
    unitType = type
)

// --- Category Mappers ---
fun CategoryEntity.toModel(): Category = Category(
    id = id,
    name = name
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name
)

// --- Store Mappers ---
fun StoreEntity.toModel(): Store = Store(
    id = id,
    name = name
)

fun Store.toEntity(): StoreEntity = StoreEntity(
    id = id,
    name = name
)

// --- Ingredient Mappers ---
fun IngredientWithCategories.toModel(): Ingredient = Ingredient(
    id = ingredient.id,
    name = ingredient.name,
    categoryId = categories.firstOrNull()?.id ?: Uuid.NIL
)

fun Ingredient.toEntity(): IngredientEntity = IngredientEntity(
    id = id,
    name = name
)

// --- Package Mappers ---
fun PackageOptionEntity.toModel(): Package = Package(
    id = id,
    ingredientId = ingredientId,
    storeId = storeId,
    priceCents = priceCents ?: 0,
    quantity = quantity ?: 0.0,
    unitId = unitId
)

fun Package.toEntity(): PackageOptionEntity = PackageOptionEntity(
    id = id,
    storeId = storeId,
    ingredientId = ingredientId,
    unitId = unitId,
    priceCents = priceCents,
    quantity = quantity
)

// --- Bridge Mappers ---
fun UnitConversionBridgeEntity.toModel(): BridgeConversion = BridgeConversion(
    id = Uuid.parse(id.toString().padStart(32, '0').let { 
        "${it.substring(0,8)}-${it.substring(8,12)}-${it.substring(12,16)}-${it.substring(16,20)}-${it.substring(20)}"
    }),
    ingredientId = ingredientId,
    fromUnitId = fromUnitId,
    fromQuantity = fromQuantity,
    toUnitId = toUnitId,
    toQuantity = toQuantity
)

// --- Recipe Mappers ---
fun RecipeWithDetails.toModel(): Recipe = Recipe(
    id = recipe.id,
    name = recipe.name,
    description = recipe.description,
    instructions = instructions.sortedBy { it.stepOrder }.map { it.instruction },
    servings = recipe.servings ?: 1.0,
    mealType = recipe.mealType ?: RecipeMealType.Other,
    prepTimeMinutes = recipe.prepTimeMinutes ?: 0,
    cookTimeMinutes = recipe.cookTimeMinutes ?: 0,
    ingredients = requirements.map { req ->
        RecipeIngredient(
            ingredientId = req.ingredientId,
            subRecipeId = req.subRecipeId,
            quantity = req.quantity,
            unitId = req.unitId
        )
    }
)

// --- Pantry Mappers ---
fun PantryInventoryWithDetails.toModel(): PantryItem = PantryItem(
    id = pantryItem.ingredientId,
    ingredientId = pantryItem.ingredientId,
    quantity = pantryItem.quantity,
    unitId = pantryItem.unitId
)

// --- Meal Mappers ---
fun PrePlannedMealWithRecipes.toModel(): PrePlannedMeal = PrePlannedMeal(
    id = meal.id,
    name = meal.name,
    recipes = recipes.map { it.id },
    independentIngredients = independentIngredients.map { 
        MealIngredient(it.ingredientId, it.quantity, it.unitId)
    }
)

fun ScheduledMealWithSource.toModel(): ScheduledMeal = ScheduledMeal(
    id = scheduledMeal.id,
    date = kotlinx.datetime.LocalDate.parse(scheduledMeal.date),
    time = kotlinx.datetime.LocalTime.parse(scheduledMeal.time ?: "12:00:00"),
    mealType = scheduledMeal.mealType ?: RecipeMealType.Other,
    prePlannedMealId = scheduledMeal.prePlannedMealId,
    restaurantId = scheduledMeal.restaurantId,
    peopleCount = scheduledMeal.peopleCount ?: 1,
    isConsumed = scheduledMeal.isConsumed,
    anticipatedCostCents = scheduledMeal.anticipatedCostCents
)

// --- Shopping List Mappers ---
fun ShoppingCartItemWithDetails.toModel(): ShoppingListItem = ShoppingListItem(
    id = cartItem.id,
    ingredientId = cartItem.ingredientId,
    customName = cartItem.customName,
    storeId = cartItem.storeId,
    neededQuantity = cartItem.neededQuantity,
    unitId = cartItem.unitId,
    packageId = cartItem.packageOptionId,
    isPurchased = cartItem.isPurchased
)

// --- Receipt Mappers ---
fun StoreReceiptEntity.toModel(): ReceiptHistory = ReceiptHistory(
    id = id,
    date = kotlinx.datetime.LocalDate.parse(date),
    time = kotlinx.datetime.LocalTime.parse(time ?: "12:00:00"),
    storeId = storeId,
    restaurantId = restaurantId,
    projectedTotalCents = projectedTotalCents ?: 0,
    actualTotalCents = actualTotalCents ?: 0,
    taxPaidCents = taxPaidCents ?: 0
)

fun StoreReceiptWithLineItems.toModel(): ReceiptHistory = receipt.toModel().copy(
    lineItems = lineItems.map { it.toModel() }
)

fun ReceiptLineItemEntity.toModel(): io.github.and19081.mealplanner.shoppinglist.ReceiptLineItem = io.github.and19081.mealplanner.shoppinglist.ReceiptLineItem(
    id = id,
    receiptId = receiptId,
    ingredientId = ingredientId,
    unitId = unitId,
    customName = customName,
    quantityBought = quantityBought,
    pricePaidCents = pricePaidCents
)

fun io.github.and19081.mealplanner.shoppinglist.ReceiptLineItem.toEntity(): ReceiptLineItemEntity = ReceiptLineItemEntity(
    id = id,
    receiptId = receiptId,
    ingredientId = ingredientId,
    unitId = unitId,
    customName = customName,
    quantityBought = quantityBought,
    pricePaidCents = pricePaidCents
)

// --- Settings Mappers ---
fun AppSettingsEntity.toModel(): AppSettings = AppSettings(
    view = appMode,
    defaultTaxRatePercentage = defaultTaxRatePercentage ?: 0.0
)

fun DashboardConfig.toModel(): UiDashboardConfig = UiDashboardConfig(
    showWeeklyCost = showWeeklyCost,
    showShoppingListSummary = showShoppingListSummary,
    showMealPlan = showMealPlan
)
