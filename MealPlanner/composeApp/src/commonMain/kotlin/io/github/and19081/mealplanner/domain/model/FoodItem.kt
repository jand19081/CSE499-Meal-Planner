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

/**
 * Marker interface for all FoodItem types.
 */
interface FoodItem {
    val id: Uuid
    val name: String
    val preferredUnitId: Uuid?
}

/**
 * Variant for purchasable ingredients.
 */
@Serializable
data class Ingredient(
    override val id: Uuid = Uuid.random(),
    override val name: String,
    override val preferredUnitId: Uuid? = null,
    val purchasableInfo: PurchasableInfo? = null,
) : FoodItem

/**
 * Variant for recipe-based items (meals and recipes share storage).
 */
@Serializable
data class Recipe(
    override val id: Uuid = Uuid.random(),
    override val name: String,
    override val preferredUnitId: Uuid? = null,
    val recipeInfo: RecipeInfo,
) : FoodItem

/**
 * Variant for pre-planned meals (shares storage with Recipe but marked as meal).
 */
@Serializable
data class Meal(
    override val id: Uuid = Uuid.random(),
    override val name: String,
    override val preferredUnitId: Uuid? = null,
    val recipeInfo: RecipeInfo,
) : FoodItem

/**
 * Variant for leftover/prepared food items.
 */
@Serializable
data class Leftover(
    override val id: Uuid = Uuid.random(),
    override val name: String,
    override val preferredUnitId: Uuid? = null,
    val leftoverInfo: LeftoverInfo,
) : FoodItem

// Extension functions for FoodItem type checking
fun FoodItem.isIngredient(): Boolean = this is Ingredient
fun FoodItem.isRecipe(): Boolean = this is Recipe
fun FoodItem.isMeal(): Boolean = this is Meal
fun FoodItem.isLeftover(): Boolean = this is Leftover

// Extension properties to access type-specific info
val FoodItem.purchasableInfo: PurchasableInfo?
    get() = (this as? Ingredient)?.purchasableInfo

val FoodItem.recipeInfo: RecipeInfo?
    get() = when (this) {
        is Recipe -> this.recipeInfo
        is Meal -> this.recipeInfo
        else -> null
    }

val FoodItem.leftoverInfo: LeftoverInfo?
    get() = (this as? Leftover)?.leftoverInfo

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
