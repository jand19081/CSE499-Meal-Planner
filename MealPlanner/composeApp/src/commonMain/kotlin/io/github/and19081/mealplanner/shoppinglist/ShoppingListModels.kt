package io.github.and19081.mealplanner.shoppinglist

import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

data class ShoppingListItem(
    val id: Uuid = Uuid.random(),
    val ingredientId: Uuid? = null,
    val customName: String? = null,
    val storeId: Uuid,
    val neededQuantity: Double? = null,
    val unitId: Uuid? = null,
    val packageId: Uuid? = null,
    val isPurchased: Boolean = false
)

data class ReceiptHistory(
    val id: Uuid = Uuid.random(),
    val date: LocalDate,
    val storeId: Uuid,
    val projectedTotalCents: Int,
    val actualTotalCents: Int,
    val taxPaidCents: Int
)

data class PriceUpdate(
    val ingredientId: Uuid,
    val newPriceCents: Int
)