package io.github.and19081.mealplanner.shoppinglist

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid



@Serializable

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
    val lineItems: List<ReceiptLineItem> = emptyList()
)

@Serializable
data class ReceiptLineItem(
    val id: Long = 0,
    val receiptId: Uuid,
    val ingredientId: Uuid? = null,
    val unitId: Uuid? = null,
    val customName: String? = null,
    val quantityBought: Double,
    val pricePaidCents: Int
)

@Serializable

data class PriceUpdate(

    val ingredientId: Uuid,

    val newPriceCents: Int

)
