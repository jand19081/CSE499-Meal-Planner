package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.core.util.UnitConverter
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.core.util.toDomainModel
import io.github.and19081.mealplanner.core.util.toEntity
import io.github.and19081.mealplanner.core.util.toModel
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.RecipeInstructionEntity
import io.github.and19081.mealplanner.data.db.entity.UnitConversionBridgeEntity
import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.Category
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.FoodItemRequirementGroup
import io.github.and19081.mealplanner.domain.model.PurchaseOption
import io.github.and19081.mealplanner.domain.model.leftoverInfo
import io.github.and19081.mealplanner.domain.model.purchasableInfo
import io.github.and19081.mealplanner.domain.model.recipeInfo
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomFoodItemRepository(
    private val db: MealPlannerDatabase,
    private val unitRepository: UnitRepository,
    private val scope: CoroutineScope,
) : FoodItemRepository {

  private val foodItemDao = db.foodItemDao()
  private val categoryDao = db.categoryDao()
  private val packageDao = db.purchaseOptionDao()

  override val foodItems: StateFlow<List<FoodItem>> =
      foodItemDao
          .observeAllComposed()
          .map { list -> list.map { it.toDomainModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override val categories: StateFlow<List<Category>> =
      categoryDao
          .observeAll()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override val purchaseOptions: StateFlow<List<PurchaseOption>> =
      packageDao
          .observeAll()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override val conversions: StateFlow<List<BridgeConversion>> =
      foodItemDao
          .observeAllConversions()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun getFoodItem(id: Uuid): FoodItem? {
    return foodItemDao.getComposedById(id)?.toDomainModel()
  }

  override suspend fun getCheapestPurchaseOption(foodItemId: Uuid): PurchaseOption? {
    return packageDao.getCheapestOption(foodItemId)?.toModel()
  }

  override suspend fun getCheapestPurchaseOptionsBatch(foodItemIds: List<Uuid>): List<PurchaseOption> {
    return packageDao.getCheapestOptionsBatch(foodItemIds).map { it.toModel() }
  }

  override suspend fun getRecursiveIngredients(recipeId: Uuid): List<io.github.and19081.mealplanner.domain.model.ItemMeasurement> {
    return foodItemDao.getRecursiveIngredients(recipeId).map { 
        io.github.and19081.mealplanner.domain.model.ItemMeasurement(
            foodItemId = it.foodItemId,
            quantity = it.requiredQty,
            unitId = it.unitId
        )
    }
  }

  override suspend fun getMakeableRecipes(): List<FoodItem> {
    return foodItemDao.getMakeableRecipes().mapNotNull { getFoodItem(it.id) }
  }

  override suspend fun getConversionsForFoodItem(foodItemId: Uuid): List<BridgeConversion> {
    return foodItemDao.getConversionsByFoodItemId(foodItemId).map { it.toModel() }
  }

  override suspend fun saveFoodItem(
      item: FoodItem,
      instructions: List<String>,
      requirementGroups: List<FoodItemRequirementGroup>,
  ) {
    val allUnits = unitRepository.units.value.associateBy { it.id }
    val instructionEntities = instructions.mapIndexed { index, text ->
      RecipeInstructionEntity(foodItemId = item.id, stepOrder = index, instruction = text)
    }

    val normalizedGroups = requirementGroups.map { group ->
        group.copy(
            requirements = group.requirements.map { req ->
                val fromUnit = allUnits[req.measurement.unitId]
                if (fromUnit != null) {
                    val (baseQty, baseUnit) = UnitConverter.toStandard(req.measurement.quantity, fromUnit, allUnits)
                    req.copy(
                        measurement = req.measurement.copy(
                            quantity = baseQty,
                            unitId = baseUnit?.id ?: req.measurement.unitId
                        )
                    )
                } else req
            }
        )
    }

    foodItemDao.upsertFoodItem(
        item = item.toEntity(),
        purchasable = item.purchasableInfo?.toEntity(item.id),
        recipe = item.recipeInfo?.toEntity(item.id),
        leftover = item.leftoverInfo?.toEntity(item.id),
        instructions = instructionEntities,
        requirementGroups = normalizedGroups.map { it.toEntity(item.id) },
        requirements =
            normalizedGroups.flatMap { group -> group.requirements.map { it.toEntity(group.id) } },
    )
  }

  override suspend fun deleteFoodItem(id: Uuid) {
    foodItemDao.deleteFoodItem(id)
  }

  override suspend fun saveCategory(category: Category) {
    categoryDao.upsert(category.toEntity())
  }

  override suspend fun deleteCategory(id: Uuid) {
    val existing = categoryDao.observeAll().first().find { it.id == id }
    if (existing != null) categoryDao.delete(existing)
  }

  override suspend fun saveConversion(bridge: BridgeConversion) {
    foodItemDao.upsertConversion(
        UnitConversionBridgeEntity(
            id = bridge.id,
            foodItemId = bridge.foodItemId,
            fromUnitId = bridge.fromUnitId,
            toUnitId = bridge.toUnitId,
            fromQuantity = bridge.fromQuantity,
            toQuantity = bridge.toQuantity,
        )
    )
  }

  override suspend fun deleteConversion(id: Uuid) {
    foodItemDao.deleteConversion(id)
  }

  override suspend fun savePurchaseOption(purchaseOption: PurchaseOption) {
    val allUnits = unitRepository.units.value.associateBy { it.id }
    val fromUnit = allUnits[purchaseOption.unitId]
    val normalizedPO = if (fromUnit != null) {
        val (baseQty, baseUnit) = UnitConverter.toStandard(purchaseOption.quantity, fromUnit, allUnits)
        purchaseOption.copy(
            quantity = baseQty,
            unitId = baseUnit?.id ?: purchaseOption.unitId
        )
    } else purchaseOption
    
    packageDao.upsert(normalizedPO.toEntity())
  }

  override suspend fun deletePurchaseOption(id: Uuid) {
    packageDao.deleteById(id)
  }
}
