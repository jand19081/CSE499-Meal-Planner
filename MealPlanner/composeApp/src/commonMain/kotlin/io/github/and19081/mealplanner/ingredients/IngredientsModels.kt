@file:OptIn(ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.ingredients

import io.github.and19081.mealplanner.Measure
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Store(
    val id: Uuid = Uuid.random(),
    val name: String
)

data class PurchaseOption(
    val id: Uuid = Uuid.random(),
    val ingredientId: Uuid,
    val storeId: Uuid,

    val priceCents: Long,     // 350 = $3.50
    val quantity: Measure,    // "12 Count" or "5 Lbs"

    val label: String? = null // "Organic", "Family Pack", etc.
) {
    val unitPrice: Double
        get() = if (quantity.amount > 0) priceCents / quantity.amount else 0.0
}

data class UnitBridge(
    val id: Uuid = Uuid.random(),
    val ingredientId: Uuid,

    val from: Measure, // e.g. 1.0 CUP
    val to: Measure,   // e.g. 120.0 GRAM
) {
    val densityRatio: Double
        get() = from.normalizedAmount / to.normalizedAmount
}

data class Ingredient(
    val id : Uuid = Uuid.random(),
    val name: String,
    val category: String,

    val purchaseOptions: List<PurchaseOption> = emptyList(),
    val conversionBridges: List<UnitBridge> = emptyList()
)