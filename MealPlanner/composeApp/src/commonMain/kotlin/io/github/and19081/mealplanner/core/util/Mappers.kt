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
import io.github.and19081.mealplanner.data.db.entity.ItemMeasurement as EntityMeasurement
import io.github.and19081.mealplanner.domain.model.ItemMeasurement as DomainMeasurement
import io.github.and19081.mealplanner.data.db.entity.DashboardConfig as UiDashboardConfig
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.*

// --- Measurement Mappers ---
fun EntityMeasurement.toDomain(): DomainMeasurement =
    DomainMeasurement(foodItemId = foodItemId, unitId = unitId, quantity = quantity)

fun DomainMeasurement.toEntity(): EntityMeasurement =
    EntityMeasurement(foodItemId = foodItemId, unitId = unitId, quantity = quantity)

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
                requirementGroups = requirementGroups.map { group ->
                    io.github.and19081.mealplanner.domain.model.FoodItemRequirementGroup(
                        id = group.group.id,
                        sortOrder = group.group.sortOrder,
                        requirements = group.requirements.map { req ->
                            FoodItemRequirement(
                                id = req.id,
                                measurement = req.measurement.toDomain(),
                                isPrimary = req.isPrimary
                            )
                        }
                    )
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

fun PurchasableInfo.toEntity(foodItemId: Uuid): io.github.and19081.mealplanner.data.db.entity.PurchasableComponentEntity =
    io.github.and19081.mealplanner.data.db.entity.PurchasableComponentEntity(
        foodItemId = foodItemId,
        expectedPriceCents = expectedPriceCents,
        categoryId = categoryId
    )

fun RecipeInfo.toEntity(foodItemId: Uuid): io.github.and19081.mealplanner.data.db.entity.RecipeComponentEntity =
    io.github.and19081.mealplanner.data.db.entity.RecipeComponentEntity(
        foodItemId = foodItemId,
        description = description,
        servings = servings,
        mealType = mealType,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes
    )

fun LeftoverInfo.toEntity(foodItemId: Uuid): io.github.and19081.mealplanner.data.db.entity.LeftoverComponentEntity =
    io.github.and19081.mealplanner.data.db.entity.LeftoverComponentEntity(
        foodItemId = foodItemId,
        remainingServings = remainingServings,
        dateAdded = dateAdded,
        expirationDate = expirationDate
    )

fun io.github.and19081.mealplanner.domain.model.FoodItemRequirementGroup.toEntity(foodItemId: Uuid): io.github.and19081.mealplanner.data.db.entity.RecipeRequirementGroupEntity =
    io.github.and19081.mealplanner.data.db.entity.RecipeRequirementGroupEntity(
        id = id,
        foodItemId = foodItemId,
        sortOrder = sortOrder
    )

fun io.github.and19081.mealplanner.domain.model.FoodItemRequirement.toEntity(groupId: Uuid): io.github.and19081.mealplanner.data.db.entity.RecipeRequirementEntity =
    io.github.and19081.mealplanner.data.db.entity.RecipeRequirementEntity(
        id = id,
        groupId = groupId,
        measurement = measurement.toEntity(),
        isPrimary = isPrimary
    )

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
        measurement = pantryItem.measurement.toDomain(),
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
        customName = cartItem.customName,
        storeId = cartItem.storeId ?: Uuid.parse("00000000-0000-0000-0000-000000000000"),
        measurement = cartItem.measurement.toDomain(),
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
        customName = customName,
        measurement = measurement.toDomain(),
        pricePaidCents = pricePaidCents,
    )

fun ReceiptLineItem.toEntity(): ReceiptLineItemEntity =
    ReceiptLineItemEntity(
        id = id,
        receiptId = receiptId,
        customName = customName,
        measurement = measurement.toEntity(),
        pricePaidCents = pricePaidCents,
    )

// --- Settings Mappers ---
fun AppSettingsEntity.toModel(): AppSettings =
    AppSettings(
        isFirstLaunch = isFirstLaunch,
        view = appMode,
        defaultTaxRatePercentage = defaultTaxRatePercentage ?: 0.0,
        mealConsumedNotificationDelayMinutes = notificationDelayMinutes ?: 30
    )

fun UiDashboardConfig.toModel(): UiDashboardConfig =
    UiDashboardConfig(
        showWeeklyCost = showWeeklyCost,
        showShoppingListSummary = showShoppingListSummary,
        showMealPlan = showMealPlan,
    )
