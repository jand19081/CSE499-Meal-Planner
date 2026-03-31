package io.github.and19081.mealplanner.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class ReceiptLineItemDraft(
    val foodItemId: Uuid,
    val name: String,
    val plannedQuantity: Double,
    val plannedUnitId: Uuid,
    var unitPriceCents: Int? = null
)

@OptIn(ExperimentalUuidApi::class)
data class ShoppingReceiptDraft(
    val id: Uuid = Uuid.random(),
    val storeId: Uuid,
    val storeName: String,
    val lineItems: List<ReceiptLineItemDraft>,
    val taxRate: Double,
    val userEnteredActualTotalCents: Int? = null
) {
    val computedSubtotalCents: Int
        get() = lineItems.sumOf {
            it.unitPriceCents?.toInt() ?: 0
        }

    val computedTaxCents: Int
        get() = (computedSubtotalCents * taxRate).toInt()

    val computedTotalCents: Int
        get() = computedSubtotalCents + computedTaxCents

    val userEnteredTotalDiffers: Boolean
        get() = userEnteredActualTotalCents?.let {
            it != computedTotalCents
        } ?: false
}
