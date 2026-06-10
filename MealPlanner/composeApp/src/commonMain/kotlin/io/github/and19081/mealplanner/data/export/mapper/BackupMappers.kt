package io.github.and19081.mealplanner.data.export.mapper

import io.github.and19081.mealplanner.data.db.entity.*
import io.github.and19081.mealplanner.data.export.dto.*
import kotlin.uuid.Uuid

fun UnitEntity.toDtoV1() =
    UnitDtoV1(
        id = id.toString(),
        name = name,
        abbreviation = abbreviation,
        displayName = displayName,
        isSystemUnit = isSystemUnit,
        factorToBase = factorToBase,
        unitType = unitType,
    )

fun UnitDtoV1.toEntity() =
    UnitEntity(
        id = Uuid.parse(id),
        name = name,
        abbreviation = abbreviation,
        displayName = displayName,
        isSystemUnit = isSystemUnit,
        factorToBase = factorToBase,
        unitType = unitType,
    )

fun CategoryEntity.toDtoV1() = CategoryDtoV1(id = id.toString(), name = name)

fun CategoryDtoV1.toEntity() = CategoryEntity(id = Uuid.parse(id), name = name)

fun StoreEntity.toDtoV1() = StoreDtoV1(id = id.toString(), name = name)

fun StoreDtoV1.toEntity() = StoreEntity(id = Uuid.parse(id), name = name)

fun RestaurantEntity.toDtoV1() = RestaurantDtoV1(id = id.toString(), name = name)

fun RestaurantDtoV1.toEntity() = RestaurantEntity(id = Uuid.parse(id), name = name)

fun FoodItemEntity.toDtoV1() =
    FoodItemDtoV1(
        id = id.toString(),
        name = name,
        preferredUnitId = preferredUnitId?.toString(),
        createdAt = createdAt ?: 0L,
        updatedAt = updatedAt,
    )

fun FoodItemDtoV1.toEntity() =
    FoodItemEntity(
        id = Uuid.parse(id),
        name = name,
        preferredUnitId = preferredUnitId?.let { Uuid.parse(it) },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun PurchasableComponentEntity.toDtoV1() =
    PurchasableComponentDtoV1(
        foodItemId = foodItemId.toString(),
        expectedPriceCents = expectedPriceCents,
        categoryId = categoryId?.toString(),
    )

fun PurchasableComponentDtoV1.toEntity() =
    PurchasableComponentEntity(
        foodItemId = Uuid.parse(foodItemId),
        expectedPriceCents = expectedPriceCents,
        categoryId = categoryId?.let { Uuid.parse(it) },
    )

fun RecipeComponentEntity.toDtoV1() =
    RecipeComponentDtoV1(
        foodItemId = foodItemId.toString(),
        description = description,
        servings = servings,
        mealType = mealType,
        isMeal = isMeal,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
    )

fun RecipeComponentDtoV1.toEntity() =
    RecipeComponentEntity(
        foodItemId = Uuid.parse(foodItemId),
        description = description,
        servings = servings,
        mealType = mealType,
        isMeal = isMeal,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
    )

fun LeftoverComponentEntity.toDtoV1() =
    LeftoverComponentDtoV1(
        foodItemId = foodItemId.toString(),
        remainingServings = remainingServings,
        dateAdded = dateAdded,
        expirationDate = expirationDate,
    )

fun LeftoverComponentDtoV1.toEntity() =
    LeftoverComponentEntity(
        foodItemId = Uuid.parse(foodItemId),
        remainingServings = remainingServings,
        dateAdded = dateAdded,
        expirationDate = expirationDate,
    )

fun RecipeInstructionEntity.toDtoV1() =
    RecipeInstructionDtoV1(
        id = id.toString(),
        foodItemId = foodItemId.toString(),
        stepOrder = stepOrder,
        instruction = instruction,
    )

fun RecipeInstructionDtoV1.toEntity() =
    RecipeInstructionEntity(
        id = Uuid.parse(id),
        foodItemId = Uuid.parse(foodItemId),
        stepOrder = stepOrder,
        instruction = instruction,
    )

fun ScheduledMealEntity.toDtoV1() =
    ScheduledMealDtoV1(
        id = id.toString(),
        foodItemId = foodItemId?.toString(),
        restaurantId = restaurantId?.toString(),
        date = date,
        time = time,
        mealType = mealType,
        peopleCount = peopleCount,
        isConsumed = isConsumed,
        anticipatedCostCents = anticipatedCostCents,
        prePlannedMealId = prePlannedMealId?.toString(),
        standaloneRecipeId = standaloneRecipeId?.toString(),
        standaloneIngredientId = standaloneIngredientId?.toString(),
        standaloneIngredientQuantity = standaloneIngredientQuantity,
        standaloneIngredientUnitId = standaloneIngredientUnitId?.toString(),
    )

fun ScheduledMealDtoV1.toEntity() =
    ScheduledMealEntity(
        id = Uuid.parse(id),
        foodItemId = foodItemId?.let { Uuid.parse(it) },
        restaurantId = restaurantId?.let { Uuid.parse(it) },
        date = date,
        time = time,
        mealType = mealType,
        peopleCount = peopleCount,
        isConsumed = isConsumed,
        anticipatedCostCents = anticipatedCostCents,
        prePlannedMealId = prePlannedMealId?.let { Uuid.parse(it) },
        standaloneRecipeId = standaloneRecipeId?.let { Uuid.parse(it) },
        standaloneIngredientId = standaloneIngredientId?.let { Uuid.parse(it) },
        standaloneIngredientQuantity = standaloneIngredientQuantity,
        standaloneIngredientUnitId = standaloneIngredientUnitId?.let { Uuid.parse(it) },
    )

fun DashboardConfig.toDtoV1() =
    DashboardConfigDtoV1(
        showWeeklyCost = showWeeklyCost,
        showShoppingListSummary = showShoppingListSummary,
        showMealPlan = showMealPlan,
    )

fun DashboardConfigDtoV1.toEntity() =
    DashboardConfig(
        showWeeklyCost = showWeeklyCost,
        showShoppingListSummary = showShoppingListSummary,
        showMealPlan = showMealPlan,
    )

fun AppSettingsEntity.toDtoV1() =
    AppSettingsDtoV1(
        id = id,
        isFirstLaunch = isFirstLaunch,
        notificationDelayMinutes = notificationDelayMinutes,
        defaultTaxRatePercentage = defaultTaxRatePercentage,
        appMode = appMode,
        themePreference = themePreference,
        cornerStyle = cornerStyle,
        accentColor = accentColor,
        dashboard = dashboard.toDtoV1(),
    )

fun AppSettingsDtoV1.toEntity() =
    AppSettingsEntity(
        id = id,
        isFirstLaunch = isFirstLaunch,
        notificationDelayMinutes = notificationDelayMinutes,
        defaultTaxRatePercentage = defaultTaxRatePercentage,
        appMode = appMode,
        themePreference = themePreference,
        cornerStyle = cornerStyle,
        accentColor = accentColor,
        dashboard = dashboard.toEntity(),
    )

fun StoreReceiptEntity.toDtoV1() =
    StoreReceiptDtoV1(
        id = id.toString(),
        name = name,
        date = date,
        time = time,
        storeId = storeId?.toString(),
        restaurantId = restaurantId?.toString(),
        scheduledMealId = scheduledMealId?.toString(),
        projectedTotalCents = projectedTotalCents,
        actualTotalCents = actualTotalCents,
        taxPaidCents = taxPaidCents,
    )

fun StoreReceiptDtoV1.toEntity() =
    StoreReceiptEntity(
        id = Uuid.parse(id),
        name = name,
        date = date,
        time = time,
        storeId = storeId?.let { Uuid.parse(it) },
        restaurantId = restaurantId?.let { Uuid.parse(it) },
        scheduledMealId = scheduledMealId?.let { Uuid.parse(it) },
        projectedTotalCents = projectedTotalCents,
        actualTotalCents = actualTotalCents,
        taxPaidCents = taxPaidCents,
    )

fun UnitConversionBridgeEntity.toDtoV1() =
    UnitConversionBridgeDtoV1(
        id = id.toString(),
        foodItemId = foodItemId.toString(),
        fromUnitId = fromUnitId.toString(),
        toUnitId = toUnitId.toString(),
        fromQuantity = fromQuantity,
        toQuantity = toQuantity,
    )

fun UnitConversionBridgeDtoV1.toEntity() =
    UnitConversionBridgeEntity(
        id = Uuid.parse(id),
        foodItemId = Uuid.parse(foodItemId),
        fromUnitId = Uuid.parse(fromUnitId),
        toUnitId = Uuid.parse(toUnitId),
        fromQuantity = fromQuantity,
        toQuantity = toQuantity,
    )

fun PurchaseOptionEntity.toDtoV1() =
    PurchaseOptionDtoV1(
        id = id.toString(),
        storeId = storeId.toString(),
        foodItemId = foodItemId.toString(),
        unitId = unitId.toString(),
        priceCents = priceCents,
        quantity = quantity,
    )

fun PurchaseOptionDtoV1.toEntity() =
    PurchaseOptionEntity(
        id = Uuid.parse(id),
        storeId = Uuid.parse(storeId),
        foodItemId = Uuid.parse(foodItemId),
        unitId = Uuid.parse(unitId),
        priceCents = priceCents,
        quantity = quantity,
    )

fun RecipeRequirementGroupEntity.toDtoV1() =
    RecipeRequirementGroupDtoV1(
        id = id.toString(),
        foodItemId = foodItemId.toString(),
        sortOrder = sortOrder,
    )

fun RecipeRequirementGroupDtoV1.toEntity() =
    RecipeRequirementGroupEntity(
        id = Uuid.parse(id),
        foodItemId = Uuid.parse(foodItemId),
        sortOrder = sortOrder,
    )

fun ItemMeasurement.toDtoV1() =
    ItemMeasurementDtoV1(
        foodItemId = foodItemId?.toString(),
        unitId = unitId?.toString(),
        quantity = quantity,
    )

fun ItemMeasurementDtoV1.toEntity() =
    ItemMeasurement(
        foodItemId = foodItemId?.let { Uuid.parse(it) },
        unitId = unitId?.let { Uuid.parse(it) },
        quantity = quantity,
    )

fun RecipeRequirementEntity.toDtoV1() =
    RecipeRequirementDtoV1(
        id = id.toString(),
        groupId = groupId.toString(),
        measurement = measurement.toDtoV1(),
        isPrimary = isPrimary,
    )

fun RecipeRequirementDtoV1.toEntity() =
    RecipeRequirementEntity(
        id = Uuid.parse(id),
        groupId = Uuid.parse(groupId),
        measurement = measurement.toEntity(),
        isPrimary = isPrimary,
    )

fun PantryInventoryEntity.toDtoV1() =
    PantryInventoryDtoV1(id = id.toString(), measurement = measurement.toDtoV1())

fun PantryInventoryDtoV1.toEntity() =
    PantryInventoryEntity(id = Uuid.parse(id), measurement = measurement.toEntity())

fun ShoppingCartItemEntity.toDtoV1() =
    ShoppingCartItemDtoV1(
        id = id.toString(),
        storeId = storeId?.toString(),
        purchaseOptionId = purchaseOptionId?.toString(),
        customName = customName,
        measurement = measurement.toDtoV1(),
        isPurchased = isPurchased,
        isPantryItem = isPantryItem,
    )

fun ShoppingCartItemDtoV1.toEntity() =
    ShoppingCartItemEntity(
        id = Uuid.parse(id),
        storeId = storeId?.let { Uuid.parse(it) },
        purchaseOptionId = purchaseOptionId?.let { Uuid.parse(it) },
        customName = customName,
        measurement = measurement.toEntity(),
        isPurchased = isPurchased,
        isPantryItem = isPantryItem,
    )

fun ReceiptLineItemEntity.toDtoV1() =
    ReceiptLineItemDtoV1(
        id = id.toString(),
        receiptId = receiptId.toString(),
        customName = customName,
        measurement = measurement.toDtoV1(),
        pricePaidCents = pricePaidCents,
    )

fun ReceiptLineItemDtoV1.toEntity() =
    ReceiptLineItemEntity(
        id = Uuid.parse(id),
        receiptId = Uuid.parse(receiptId),
        customName = customName,
        measurement = measurement.toEntity(),
        pricePaidCents = pricePaidCents,
    )
