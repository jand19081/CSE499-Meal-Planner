package io.github.and19081.mealplanner.feature.shoppinglist

import io.github.and19081.mealplanner.domain.model.ItemMeasurement
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingListItem(
    val id: Uuid = Uuid.random(),
    val customName: String? = null,
    val storeId: Uuid,
    val measurement: ItemMeasurement,
    val packageId: Uuid? = null,
    val isPurchased: Boolean = false,
    val isPantryItem: Boolean = true,
)

@Serializable
data class ReceiptHistory(
    val id: Uuid = Uuid.random(),
    val date: LocalDate,
    val time: LocalTime,
    val storeId: Uuid? = null,
    val restaurantId: Uuid? = null,
    val projectedTotalCents: Int,
    val actualTotalCents: Int,
    val taxPaidCents: Int,
    val lineItems: List<ReceiptLineItem> = emptyList(),
)

@Serializable
data class ReceiptLineItem(
    val id: Uuid = Uuid.random(),
    val receiptId: Uuid,
    val customName: String? = null,
    val measurement: ItemMeasurement,
    val pricePaidCents: Int,
)

@Serializable data class PriceUpdate(val foodItemId: Uuid, val priceCents: Int)
