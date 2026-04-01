package io.github.and19081.mealplanner.domain.repository

import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.Category
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.FoodItemRequirementGroup
import io.github.and19081.mealplanner.domain.model.PurchaseOption
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.StateFlow

interface FoodItemRepository {
  val foodItems: StateFlow<List<FoodItem>>
  val categories: StateFlow<List<Category>>
  val purchaseOptions: StateFlow<List<PurchaseOption>>
  val conversions: StateFlow<List<BridgeConversion>>

  suspend fun getFoodItem(id: Uuid): FoodItem?

  suspend fun getConversionsForFoodItem(foodItemId: Uuid): List<BridgeConversion>

  suspend fun saveFoodItem(
      item: FoodItem,
      instructions: List<String> = emptyList(),
      requirementGroups: List<FoodItemRequirementGroup> = emptyList(),
  )

  suspend fun deleteFoodItem(id: Uuid)

  // Categories
  suspend fun saveCategory(category: Category)

  suspend fun deleteCategory(id: Uuid)

  // Conversions
  suspend fun saveConversion(bridge: BridgeConversion)

  suspend fun deleteConversion(id: Uuid)

  // PurchaseOptions
  suspend fun savePurchaseOption(purchaseOption: PurchaseOption)

  suspend fun deletePurchaseOption(id: Uuid)
}
