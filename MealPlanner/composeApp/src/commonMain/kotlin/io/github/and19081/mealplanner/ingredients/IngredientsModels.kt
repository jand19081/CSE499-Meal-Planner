package io.github.and19081.mealplanner.ingredients

import kotlin.uuid.Uuid

data class Category(
    val id: Uuid = Uuid.random(),
    val name: String
)

data class Store(
    val id: Uuid = Uuid.random(),
    val name: String
)

data class Package(
    val id: Uuid = Uuid.random(),
    val ingredientId: Uuid,
    val storeId: Uuid,

    val priceCents: Int,
    val quantity: Double,
    val unitId: Uuid
)

data class BridgeConversion(
    val id: Uuid = Uuid.random(),
    val ingredientId: Uuid,
    val fromUnitId: Uuid,
    val fromQuantity: Double,
    val toUnitId: Uuid,
    val toQuantity: Double
)

data class Ingredient(
    val id : Uuid = Uuid.random(),
    val name: String,
    val categoryId: Uuid
)