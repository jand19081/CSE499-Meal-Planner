package io.github.and19081.mealplanner.domain.model

import io.github.and19081.mealplanner.core.util.RecipeMealType
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

interface NamedReference {
    val id: Uuid
    val name: String
}

@Serializable
data class Category(override val id: Uuid = Uuid.random(), override val name: String) : NamedReference

@Serializable
data class Store(override val id: Uuid = Uuid.random(), override val name: String) : NamedReference

@Serializable
data class ItemMeasurement(
    val foodItemId: Uuid?,
    val unitId: Uuid? = null,
    val quantity: Double = 0.0,
)

@Serializable
data class Package(
    val id: Uuid = Uuid.random(),
    val foodItemId: Uuid,
    val storeId: Uuid,
    val priceCents: Int,
    val quantity: Double,
    val unitId: Uuid,
)

@Serializable
data class BridgeConversion(
    val id: Uuid = Uuid.random(),
    val foodItemId: Uuid,
    val fromUnitId: Uuid,
    val fromQuantity: Double,
    val toUnitId: Uuid,
    val toQuantity: Double,
)

@Serializable
data class FoodItem(
    val id: Uuid = Uuid.random(),
    val name: String,
    val preferredUnitId: Uuid? = null,
    val purchasableInfo: PurchasableInfo? = null,
    val recipeInfo: RecipeInfo? = null,
    val leftoverInfo: LeftoverInfo? = null,
) {
    val isIngredient: Boolean get() = purchasableInfo != null
    val isRecipe: Boolean get() = recipeInfo != null
    val isLeftover: Boolean get() = leftoverInfo != null
}

@Serializable
data class PurchasableInfo(
    val expectedPriceCents: Int? = null,
    val categoryId: Uuid? = null,
)

@Serializable
data class RecipeInfo(
    val description: String? = null,
    val instructions: List<String> = emptyList(),
    val servings: Double = 1.0,
    val mealType: RecipeMealType = RecipeMealType.Other,
    val prepTimeMinutes: Int? = null,
    val cookTimeMinutes: Int? = null,
    val requirementGroups: List<FoodItemRequirementGroup> = emptyList(),
)

@Serializable
data class FoodItemRequirement(
    val id: Uuid = Uuid.random(),
    val measurement: ItemMeasurement,
    val isPrimary: Boolean = true,
)

@Serializable
data class FoodItemRequirementGroup(
    val id: Uuid = Uuid.random(),
    val sortOrder: Int = 0,
    val requirements: List<FoodItemRequirement> = emptyList()
)

@Serializable
data class LeftoverInfo(
    val remainingServings: Double,
    val dateAdded: String,
    val expirationDate: String? = null,
)
