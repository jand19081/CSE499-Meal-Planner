package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.Category
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.model.FoodItemRequirementGroup
import io.github.and19081.mealplanner.domain.model.Package
import io.github.and19081.mealplanner.core.util.toDomainModel
import io.github.and19081.mealplanner.core.util.toEntity
import io.github.and19081.mealplanner.core.util.toModel
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.LeftoverComponentEntity
import io.github.and19081.mealplanner.data.db.entity.PurchasableComponentEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeComponentEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeInstructionEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeRequirementEntity
import io.github.and19081.mealplanner.data.db.entity.RecipeRequirementGroupEntity
import io.github.and19081.mealplanner.data.db.entity.UnitConversionBridgeEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomFoodItemRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : FoodItemRepository {

    private val foodItemDao = db.foodItemDao()
    private val categoryDao = db.categoryDao()
    private val packageDao = db.packageOptionDao()

    override val foodItems: StateFlow<List<FoodItem>> =
        foodItemDao.observeAllComposed()
            .map { list -> list.map { it.toDomainModel() } }
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val categories: StateFlow<List<Category>> =
        categoryDao.observeAll()
            .map { list -> list.map { it.toModel() } }
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val packages: StateFlow<List<Package>> =
        packageDao.observeAll()
            .map { list -> list.map { it.toModel() } }
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val conversions: StateFlow<List<BridgeConversion>> =
        foodItemDao.observeAllConversions()
            .map { list -> list.map { it.toModel() } }
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override suspend fun getFoodItem(id: Uuid): FoodItem? {
        return foodItemDao.getComposedById(id)?.toDomainModel()
    }

    override suspend fun saveFoodItem(
        item: FoodItem,
        instructions: List<String>,
        requirementGroups: List<FoodItemRequirementGroup>
    ) {
        val entity = item.toEntity()
        val purchasable = item.purchasableInfo?.let {
            PurchasableComponentEntity(
                foodItemId = item.id,
                expectedPriceCents = it.expectedPriceCents,
                categoryId = it.categoryId
            )
        }
        val recipe = item.recipeInfo?.let {
            RecipeComponentEntity(
                foodItemId = item.id,
                description = it.description,
                servings = it.servings,
                mealType = it.mealType,
                prepTimeMinutes = it.prepTimeMinutes,
                cookTimeMinutes = it.cookTimeMinutes
            )
        }
        val leftover = item.leftoverInfo?.let {
            LeftoverComponentEntity(
                foodItemId = item.id,
                remainingServings = it.remainingServings,
                dateAdded = it.dateAdded,
                expirationDate = it.expirationDate
            )
        }

        val instructionEntities = instructions.mapIndexed { index, text ->
            RecipeInstructionEntity(
                foodItemId = item.id,
                stepOrder = index,
                instruction = text
            )
        }

        val groupEntities = requirementGroups.map { group ->
            RecipeRequirementGroupEntity(
                id = group.id,
                foodItemId = item.id,
                sortOrder = group.sortOrder
            )
        }

        val requirementEntities = requirementGroups.flatMap { group ->
            group.requirements.map { req ->
                RecipeRequirementEntity(
                    id = req.id,
                    groupId = group.id,
                    foodItemId = req.foodItemId,
                    unitId = req.unitId,
                    quantity = req.quantity,
                    isPrimary = req.isPrimary
                )
            }
        }

        foodItemDao.upsertFoodItem(
            item = entity,
            purchasable = purchasable,
            recipe = recipe,
            leftover = leftover,
            instructions = instructionEntities,
            requirementGroups = groupEntities,
            requirements = requirementEntities
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
                toQuantity = bridge.toQuantity
            )
        )
    }

    override suspend fun deleteConversion(id: Uuid) {
        foodItemDao.deleteConversion(id)
    }

    override suspend fun savePackage(pkg: Package) {
        packageDao.upsert(pkg.toEntity())
    }

    override suspend fun deletePackage(id: Uuid) {
        packageDao.deleteById(id)
    }
}
