package io.github.and19081.mealplanner.data

import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.data.db.entity.*
import io.github.and19081.mealplanner.data.db.relation.*
import io.github.and19081.mealplanner.ingredients.*
import io.github.and19081.mealplanner.settings.AppSettings
import io.github.and19081.mealplanner.settings.DashboardConfig as UiDashboardConfig
import io.github.and19081.mealplanner.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.shoppinglist.ShoppingListItem
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.*

// --- Unit Mappers ---
fun UnitEntity.toModel(): UnitModel =
    UnitModel(
        id = id,
        type = unitType,
        abbreviation = abbreviation ?: "",
        displayName = displayName ?: name,
        isSystemUnit = isSystemUnit,
        factorToBase = factorToBase ?: 1.0,
    )

fun UnitModel.toEntity(): UnitEntity =
    UnitEntity(
        id = id,
        name = displayName,
        abbreviation = abbreviation,
        displayName = displayName,
        isSystemUnit = isSystemUnit,
        factorToBase = factorToBase,
        unitType = type,
    )

// --- Category Mappers ---
fun CategoryEntity.toModel(): Category = Category(id = id, name = name)

fun Category.toEntity(): CategoryEntity = CategoryEntity(id = id, name = name)

// --- Store Mappers ---
fun StoreEntity.toModel(): Store = Store(id = id, name = name)

fun Store.toEntity(): StoreEntity = StoreEntity(id = id, name = name)

// --- Ingredient Mappers ---
fun IngredientWithCategories.toModel(): Ingredient =
    Ingredient(
        id = ingredient.id,
        name = ingredient.name,
        categoryId = categories.firstOrNull()?.id ?: Uuid.NIL,
        preferredUnitId = ingredient.preferredUnitId,
    )

fun Ingredient.toEntity(): IngredientEntity =
    IngredientEntity(id = id, name = name, preferredUnitId = preferredUnitId)

// --- Package Mappers ---
fun PackageOptionEntity.toModel(): Package =
    Package(
        id = id,
        ingredientId = ingredientId,
        storeId = storeId,
        priceCents = priceCents ?: 0,
        quantity = quantity ?: 0.0,
        unitId = unitId,
    )

fun Package.toEntity(): PackageOptionEntity =
    PackageOptionEntity(
        id = id,
        storeId = storeId,
        ingredientId = ingredientId,
        unitId = unitId,
        priceCents = priceCents,
        quantity = quantity,
    )

// --- Bridge Mappers ---
fun UnitConversionBridgeEntity.toModel(): BridgeConversion =
    BridgeConversion(
        id = id,
        ingredientId = ingredientId,
        fromUnitId = fromUnitId,
        fromQuantity = fromQuantity,
        toUnitId = toUnitId,
        toQuantity = toQuantity,
    )

// --- Recipe Mappers ---
fun RecipeWithDetails.toModel(): Recipe =
    Recipe(
        id = recipe.id,
        name = recipe.name,
        description = recipe.description,
        instructions = instructions.sortedBy { it.stepOrder }.map { it.instruction },
        servings = recipe.servings ?: 1.0,
        mealType = recipe.mealType ?: RecipeMealType.Other,
        prepTimeMinutes = recipe.prepTimeMinutes ?: 0,
        cookTimeMinutes = recipe.cookTimeMinutes ?: 0,
        producesIngredientId = recipe.producesIngredientId,
        amountPerServing = recipe.amountPerServing,
        requirementGroups =
            requirementGroups
                .sortedBy { it.group.sortOrder }
                .map { groupWithReqs ->
                  RecipeRequirementGroup(
                      id = groupWithReqs.group.id,
                      requirements =
                          groupWithReqs.requirements.map { req ->
                            RecipeRequirement(
                                id = req.id,
                                ingredientId = req.ingredientId,
                                subRecipeId = req.subRecipeId,
                                quantity = req.quantity,
                                unitId = req.unitId,
                                isPrimary = req.isPrimary,
                            )
                          },
                  )
                },
    )

// --- Pantry Mappers ---
fun PantryInventoryWithDetails.toModel(): PantryItem =
    PantryItem(
        id = pantryItem.id,
        ingredientId = pantryItem.ingredientId,
        quantity = pantryItem.quantity,
        unitId = pantryItem.unitId,
    )

// --- Leftover Mappers ---
fun LeftoverInventoryEntity.toModel(): LeftoverItem =
    LeftoverItem(
        id = id,
        recipeId = recipeId,
        remainingServings = remainingServings,
        dateAdded = dateAdded,
        expirationDate = expirationDate,
    )

// --- Meal Mappers ---
fun PrePlannedMealWithRecipes.toModel(): PrePlannedMeal =
    PrePlannedMeal(
        id = meal.id,
        name = meal.name,
        mealType = meal.mealType,
        recipes = recipes.map { it.id },
        independentIngredients =
            independentIngredients.map { MealIngredient(it.ingredientId, it.quantity, it.unitId) },
    )

fun ScheduledMealWithSource.toModel(): ScheduledMeal =
    ScheduledMeal(
        id = scheduledMeal.id,
        date =
            try {
              LocalDate.parse(scheduledMeal.date)
            } catch (e: Exception) {
              Clock.System.todayIn(TimeZone.currentSystemDefault())
            },
        time =
            try {
              LocalTime.parse(scheduledMeal.time)
            } catch (e: Exception) {
              LocalTime(12, 0)
            },
        mealType = scheduledMeal.mealType ?: RecipeMealType.Other,
        prePlannedMealId = scheduledMeal.prePlannedMealId,
        restaurantId = scheduledMeal.restaurantId,
        peopleCount = scheduledMeal.peopleCount ?: 1,
        isConsumed = scheduledMeal.isConsumed,
        anticipatedCostCents = scheduledMeal.anticipatedCostCents,
    )

// --- Shopping List Mappers ---
fun ShoppingCartItemWithDetails.toModel(): ShoppingListItem =
    ShoppingListItem(
        id = cartItem.id,
        ingredientId = cartItem.ingredientId,
        customName = cartItem.customName,
        storeId = cartItem.storeId ?: Uuid.parse("00000000-0000-0000-0000-000000000000"),
        neededQuantity = cartItem.neededQuantity,
        unitId = cartItem.unitId,
        packageId = cartItem.packageOptionId,
        isPurchased = cartItem.isPurchased,
        isPantryItem = cartItem.isPantryItem,
    )

// --- Receipt Mappers ---
fun StoreReceiptEntity.toModel(): ReceiptHistory =
    ReceiptHistory(
        id = id,
        date =
            try {
              LocalDate.parse(date)
            } catch (e: Exception) {
              Clock.System.todayIn(TimeZone.currentSystemDefault())
            },
        time =
            try {
              LocalTime.parse(time)
            } catch (e: Exception) {
              LocalTime(12, 0)
            },
        storeId = storeId,
        restaurantId = restaurantId,
        projectedTotalCents = projectedTotalCents ?: 0,
        actualTotalCents = actualTotalCents ?: 0,
        taxPaidCents = taxPaidCents ?: 0,
    )

fun StoreReceiptWithLineItems.toModel(): ReceiptHistory =
    receipt.toModel().copy(lineItems = lineItems.map { it.toModel() })

fun ReceiptLineItemEntity.toModel(): io.github.and19081.mealplanner.shoppinglist.ReceiptLineItem =
    io.github.and19081.mealplanner.shoppinglist.ReceiptLineItem(
        id = id,
        receiptId = receiptId,
        ingredientId = ingredientId,
        unitId = unitId,
        customName = customName,
        quantityBought = quantityBought,
        pricePaidCents = pricePaidCents,
    )

fun io.github.and19081.mealplanner.shoppinglist.ReceiptLineItem.toEntity(): ReceiptLineItemEntity =
    ReceiptLineItemEntity(
        id = id,
        receiptId = receiptId,
        ingredientId = ingredientId,
        unitId = unitId,
        customName = customName,
        quantityBought = quantityBought,
        pricePaidCents = pricePaidCents,
    )

// --- Settings Mappers ---
fun AppSettingsEntity.toModel(): AppSettings =
    AppSettings(view = appMode, defaultTaxRatePercentage = defaultTaxRatePercentage ?: 0.0)

fun DashboardConfig.toModel(): UiDashboardConfig =
    UiDashboardConfig(
        showWeeklyCost = showWeeklyCost,
        showShoppingListSummary = showShoppingListSummary,
        showMealPlan = showMealPlan,
    )
