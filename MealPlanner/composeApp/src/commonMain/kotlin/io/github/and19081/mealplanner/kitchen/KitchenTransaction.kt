package io.github.and19081.mealplanner.kitchen

import io.github.and19081.mealplanner.UnitModel
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    Acquisition, // Buying (Shopping)
    Production,  // Making (Prep Batch)
    Consumption, // Eating (Meal)
}

@Serializable
enum class TransactionDirection {
    IN,
    OUT
}

@Serializable
data class InventoryChange(
    val foodItemId: Uuid,
    val ingredientName: String,
    val quantity: Double?,
    val unitId: Uuid?,
    val unitAbbreviation: String,
    val direction: TransactionDirection,
    val priceCents: Long? = null,
    val isAdjusted: Boolean = false,
    val isPantryItem: Boolean = true,
)

@Serializable
data class KitchenTransaction(
    val id: Uuid = Uuid.random(),
    val type: TransactionType,
    val title: String,
    val changes: List<InventoryChange>,
    val timestamp: Long = System.currentTimeMillis()
)
