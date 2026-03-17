package io.github.and19081.mealplanner.core.util

import io.github.and19081.mealplanner.feature.settings.AppSettings
import io.github.and19081.mealplanner.data.db.entity.AppSettingsEntity
import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.Category
import io.github.and19081.mealplanner.data.db.entity.CategoryEntity
import io.github.and19081.mealplanner.data.db.relation.ComposedFoodItemRelation
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.data.db.entity.FoodItemEntity
import io.github.and19081.mealplanner.domain.model.FoodItemRequirement
import io.github.and19081.mealplanner.domain.model.LeftoverInfo
import io.github.and19081.mealplanner.domain.model.Package
import io.github.and19081.mealplanner.data.db.entity.PackageOptionEntity
import io.github.and19081.mealplanner.data.db.relation.PantryInventoryWithDetails
import io.github.and19081.mealplanner.feature.meals.PantryItem
import io.github.and19081.mealplanner.domain.model.PurchasableInfo
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptLineItem
import io.github.and19081.mealplanner.data.db.entity.ReceiptLineItemEntity
import io.github.and19081.mealplanner.domain.model.RecipeInfo
import io.github.and19081.mealplanner.feature.meals.ScheduledMeal
import io.github.and19081.mealplanner.data.db.relation.ScheduledMealWithSource
import io.github.and19081.mealplanner.data.db.relation.ShoppingCartItemWithDetails
import io.github.and19081.mealplanner.feature.shoppinglist.ShoppingListItem
import io.github.and19081.mealplanner.domain.model.Store
import io.github.and19081.mealplanner.data.db.entity.StoreEntity
import io.github.and19081.mealplanner.data.db.entity.StoreReceiptEntity
import io.github.and19081.mealplanner.data.db.relation.StoreReceiptWithLineItems
import io.github.and19081.mealplanner.data.db.entity.UnitConversionBridgeEntity
import io.github.and19081.mealplanner.data.db.entity.UnitEntity
import io.github.and19081.mealplanner.data.db.entity.DashboardConfig as UiDashboardConfig
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

// --- Food Item Mappers (ECS) ---
fun ComposedFoodItemRelation.toDomainModel(): FoodItem =
    FoodItem(
        id = item.id,
        name = item.name,
        preferredUnitId = item.preferredUnitId,
        purchasableInfo = purchasable?.let {
            PurchasableInfo(
                expectedPriceCents = it.expectedPriceCents,
                categoryId = it.categoryId
            )
        },
        recipeInfo = recipe?.let {
            RecipeInfo(
                description = it.description,
                instructions = instructions.sortedBy { it.stepOrder }.map { it.instruction },
                servings = it.servings,
                mealType = it.mealType,
                prepTimeMinutes = it.prepTimeMinutes,
                cookTimeMinutes = it.cookTimeMinutes,
                requirements = requirementGroups.flatMap { group ->
                    group.requirements.map { req ->
                        FoodItemRequirement(
                            id = req.id,
                            foodItemId = req.foodItemId,
                            quantity = req.quantity,
                            unitId = req.unitId,
                            isPrimary = req.isPrimary
                        )
                    }
                }
            )
        },
        leftoverInfo = leftover?.let {
            LeftoverInfo(
                remainingServings = it.remainingServings,
                dateAdded = it.dateAdded,
                expirationDate = it.expirationDate
            )
        }
    )

fun FoodItem.toEntity(): FoodItemEntity =
    FoodItemEntity(id = id, name = name, preferredUnitId = preferredUnitId)

// --- Package Mappers ---
fun PackageOptionEntity.toModel(): Package =
    Package(
        id = id,
        foodItemId = foodItemId,
        storeId = storeId,
        priceCents = priceCents ?: 0,
        quantity = quantity ?: 0.0,
        unitId = unitId,
    )

fun Package.toEntity(): PackageOptionEntity =
    PackageOptionEntity(
        id = id,
        storeId = storeId,
        foodItemId = foodItemId,
        unitId = unitId,
        priceCents = priceCents,
        quantity = quantity,
    )

// --- Bridge Mappers ---
fun UnitConversionBridgeEntity.toModel(): BridgeConversion =
    BridgeConversion(
        id = id,
        foodItemId = foodItemId,
        fromUnitId = fromUnitId,
        fromQuantity = fromQuantity,
        toUnitId = toUnitId,
        toQuantity = toQuantity,
    )

// --- Pantry Mappers ---
fun PantryInventoryWithDetails.toModel(): PantryItem =
    PantryItem(
        id = pantryItem.id,
        foodItemId = pantryItem.foodItemId,
        quantity = pantryItem.quantity,
        unitId = pantryItem.unitId,
    )

// --- Scheduled Meal Mappers ---
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
        mealType = scheduledMeal.mealType,
        prePlannedMealId = scheduledMeal.foodItemId,
        restaurantId = scheduledMeal.restaurantId,
        peopleCount = scheduledMeal.peopleCount,
        isConsumed = scheduledMeal.isConsumed,
        anticipatedCostCents = scheduledMeal.anticipatedCostCents,
    )

// --- Shopping List Mappers ---
fun ShoppingCartItemWithDetails.toModel(): ShoppingListItem =
    ShoppingListItem(
        id = cartItem.id,
        foodItemId = cartItem.foodItemId,
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

fun ReceiptLineItemEntity.toModel(): ReceiptLineItem =
    ReceiptLineItem(
        id = id,
        receiptId = receiptId,
        foodItemId = foodItemId ?: Uuid.NIL,
        unitId = unitId ?: Uuid.NIL,
        customName = customName,
        quantityBought = quantityBought,
        pricePaidCents = pricePaidCents,
    )

fun ReceiptLineItem.toEntity(): ReceiptLineItemEntity =
    ReceiptLineItemEntity(
        id = id,
        receiptId = receiptId,
        foodItemId = foodItemId,
        unitId = unitId,
        customName = customName,
        quantityBought = quantityBought,
        pricePaidCents = pricePaidCents,
    )

// --- Settings Mappers ---
fun AppSettingsEntity.toModel(): AppSettings =
    AppSettings(view = appMode, defaultTaxRatePercentage = defaultTaxRatePercentage ?: 0.0)

fun UiDashboardConfig.toModel(): UiDashboardConfig =
    UiDashboardConfig(
        showWeeklyCost = showWeeklyCost,
        showShoppingListSummary = showShoppingListSummary,
        showMealPlan = showMealPlan,
    )
