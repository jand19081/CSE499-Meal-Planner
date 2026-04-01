package io.github.and19081.mealplanner.feature.shoppinglist

import kotlin.uuid.Uuid

data class ReceiptLineItemDraft(
    val foodItemId: Uuid?,
    val name: String,
    val quantity: Double,
    val unitAbbreviation: String,
    val unitId: Uuid? = null,
    val priceCents: Long, // editable by user
    val isCustom: Boolean = false,
)

data class ShoppingReceiptDraft(
    val storeId: Uuid,
    val storeName: String,
    val lineItems: List<ReceiptLineItemDraft>,
    val taxRatePercent: Double,
    val userEnteredTotalCents: Long? = null, // null until user types a total
) {
  val subtotalCents: Long
    get() = lineItems.sumOf { it.priceCents }

  val taxCents: Long
    get() = (subtotalCents * taxRatePercent / 100.0).toLong()

  val computedTotalCents: Long
    get() = subtotalCents + taxCents

  val hasDiscrepancy: Boolean
    get() = userEnteredTotalCents != null && userEnteredTotalCents != computedTotalCents
}
