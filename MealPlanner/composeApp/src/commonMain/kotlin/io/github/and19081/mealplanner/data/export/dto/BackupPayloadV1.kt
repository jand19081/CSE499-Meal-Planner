package io.github.and19081.mealplanner.data.export.dto

import io.github.and19081.mealplanner.core.theme.AppTheme
import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.core.util.UnitType
import io.github.and19081.mealplanner.feature.settings.Mode
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayloadV1(
    val version: Int = 1,
    val timestamp: Long,
    val units: List<UnitDtoV1> = emptyList(),
    val categories: List<CategoryDtoV1> = emptyList(),
    val stores: List<StoreDtoV1> = emptyList(),
    val restaurants: List<RestaurantDtoV1> = emptyList(),
    val foodItems: List<FoodItemDtoV1> = emptyList(),
    val purchasableComponents: List<PurchasableComponentDtoV1> = emptyList(),
    val recipeComponents: List<RecipeComponentDtoV1> = emptyList(),
    val leftoverComponents: List<LeftoverComponentDtoV1> = emptyList(),
    val recipeInstructions: List<RecipeInstructionDtoV1> = emptyList(),
    val scheduledMeals: List<ScheduledMealDtoV1> = emptyList(),
    val appSettings: AppSettingsDtoV1? = null,
    val storeReceipts: List<StoreReceiptDtoV1> = emptyList(),
    val unitConversionBridges: List<UnitConversionBridgeDtoV1> = emptyList(),
    val purchaseOptions: List<PurchaseOptionDtoV1> = emptyList(),
    val recipeRequirementGroups: List<RecipeRequirementGroupDtoV1> = emptyList(),
    val recipeRequirements: List<RecipeRequirementDtoV1> = emptyList(),
    val pantryInventory: List<PantryInventoryDtoV1> = emptyList(),
    val shoppingCartItems: List<ShoppingCartItemDtoV1> = emptyList(),
    val receiptLineItems: List<ReceiptLineItemDtoV1> = emptyList(),
)

@Serializable
data class UnitDtoV1(
    val id: String,
    val name: String,
    val abbreviation: String?,
    val displayName: String?,
    val isSystemUnit: Boolean,
    val factorToBase: Double?,
    val unitType: UnitType,
)

@Serializable
data class CategoryDtoV1(
    val id: String,
    val name: String,
)

@Serializable
data class StoreDtoV1(
    val id: String,
    val name: String,
)

@Serializable
data class RestaurantDtoV1(
    val id: String,
    val name: String,
)

@Serializable
data class FoodItemDtoV1(
    val id: String,
    val name: String,
    val preferredUnitId: String?,
    val unitType: UnitType = UnitType.Count,
    val createdAt: Long,
    val updatedAt: Long?,
)

@Serializable
data class PurchasableComponentDtoV1(
    val foodItemId: String,
    val expectedPriceCents: Int?,
    val categoryId: String?,
)

@Serializable
data class RecipeComponentDtoV1(
    val foodItemId: String,
    val description: String?,
    val servings: Double,
    val mealType: RecipeMealType,
    val isMeal: Boolean = false,
    val prepTimeMinutes: Int?,
    val cookTimeMinutes: Int?,
)

@Serializable
data class LeftoverComponentDtoV1(
    val foodItemId: String,
    val remainingServings: Double,
    val dateAdded: String,
    val expirationDate: String?,
)

@Serializable
data class RecipeInstructionDtoV1(
    val id: String,
    val foodItemId: String,
    val stepOrder: Int,
    val instruction: String,
)

@Serializable
data class ScheduledMealDtoV1(
    val id: String,
    val foodItemId: String? = null,
    val restaurantId: String? = null,
    val date: String,
    val time: String,
    val mealType: RecipeMealType,
    val peopleCount: Int,
    val isConsumed: Boolean,
    val anticipatedCostCents: Int?,
    val prePlannedMealId: String? = null,
    val standaloneRecipeId: String? = null,
    val standaloneIngredientId: String? = null,
    val standaloneIngredientQuantity: Double? = null,
    val standaloneIngredientUnitId: String? = null,
)

@Serializable
data class AppSettingsDtoV1(
    val id: String,
    val isFirstLaunch: Boolean = false,
    val notificationDelayMinutes: Int?,
    val defaultTaxRatePercentage: Double?,
    val appMode: Mode,
    val themePreference: AppTheme,
    val cornerStyle: String,
    val accentColor: String,
    val dashboard: DashboardConfigDtoV1,
)

@Serializable
data class DashboardConfigDtoV1(
    val showWeeklyCost: Boolean,
    val showShoppingListSummary: Boolean,
    val showMealPlan: Boolean,
)

@Serializable
data class StoreReceiptDtoV1(
    val id: String,
    val name: String,
    val date: String,
    val time: String,
    val storeId: String?,
    val restaurantId: String?,
    val scheduledMealId: String?,
    val projectedTotalCents: Int?,
    val actualTotalCents: Int?,
    val taxPaidCents: Int?,
)

@Serializable
data class UnitConversionBridgeDtoV1(
    val id: String,
    val foodItemId: String,
    val fromUnitId: String,
    val toUnitId: String,
    val fromQuantity: Double,
    val toQuantity: Double,
)

@Serializable
data class PurchaseOptionDtoV1(
    val id: String,
    val storeId: String,
    val foodItemId: String,
    val unitId: String,
    val priceCents: Int?,
    val quantity: Double?,
)

@Serializable
data class RecipeRequirementGroupDtoV1(
    val id: String,
    val foodItemId: String,
    val sortOrder: Int,
)

@Serializable
data class RecipeRequirementDtoV1(
    val id: String,
    val groupId: String,
    val measurement: ItemMeasurementDtoV1,
    val isPrimary: Boolean,
)

@Serializable
data class ItemMeasurementDtoV1(
    val foodItemId: String?,
    val unitId: String?,
    val quantity: Double,
)

@Serializable
data class PantryInventoryDtoV1(
    val id: String,
    val measurement: ItemMeasurementDtoV1,
)

@Serializable
data class ShoppingCartItemDtoV1(
    val id: String,
    val storeId: String?,
    val purchaseOptionId: String?,
    val customName: String?,
    val measurement: ItemMeasurementDtoV1,
    val isPurchased: Boolean,
    val isPantryItem: Boolean,
)

@Serializable
data class ReceiptLineItemDtoV1(
    val id: String,
    val receiptId: String,
    val customName: String?,
    val measurement: ItemMeasurementDtoV1,
    val pricePaidCents: Int,
)
