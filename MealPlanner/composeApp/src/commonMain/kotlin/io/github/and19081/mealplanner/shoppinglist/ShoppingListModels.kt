package io.github.and19081.mealplanner.shoppinglist

import io.github.and19081.mealplanner.Measure
import kotlin.uuid.Uuid

data class ShoppingItem(
    val id: Uuid = Uuid.random(),

    val ingredientId: Uuid,
    val requiredQuantity: Measure, // "Need 200g"

    // If null, the user hasn't decided WHICH store/pack to buy yet.
    val selectedOptionId: Uuid? = null,

    val isPurchased: Boolean = false
)
