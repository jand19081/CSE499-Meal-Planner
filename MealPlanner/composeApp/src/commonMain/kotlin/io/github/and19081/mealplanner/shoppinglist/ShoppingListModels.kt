package io.github.and19081.mealplanner.shoppinglist

import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class ReceiptItem(
    val name: String,
    val quantity: Double,
    val unit: String,
    val priceCents: Long,
    val ingredientId: Uuid?
)

@OptIn(ExperimentalUuidApi::class)
data class ShoppingTrip(
    val id: Uuid = Uuid.random(),
    val date: LocalDate,
    val storeName: String,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalPaidCents: Long,
    val items: List<ReceiptItem>
)
