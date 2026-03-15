package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.*
import io.github.and19081.mealplanner.data.toEntity
import io.github.and19081.mealplanner.data.toModel
import io.github.and19081.mealplanner.ingredients.*
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomIngredientRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : IngredientRepository {

  private val ingredientDao = db.ingredientDao()
  private val categoryDao = db.categoryDao()
  private val packageDao = db.packageOptionDao()

  override val ingredients: StateFlow<List<Ingredient>> =
      ingredientDao
          .observeAllWithCategories()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override val categories: StateFlow<List<Category>> =
      categoryDao
          .observeAll()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override val packages: StateFlow<List<Package>> =
      packageDao
          .observeAll()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override val bridges: StateFlow<List<BridgeConversion>> =
      ingredientDao
          .observeAllConversions()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun count(): Int = ingredientDao.count()

  override suspend fun addIngredient(ingredient: Ingredient) {
    ingredientDao.upsertIngredientWithDetails(
        ingredient.toEntity(),
        ingredient.categoryId,
        emptyList(),
        emptyList(),
    )
  }

  override suspend fun updateIngredient(ingredient: Ingredient) {
    ingredientDao.upsertIngredientWithDetails(
        ingredient.toEntity(),
        ingredient.categoryId,
        emptyList(),
        emptyList(),
    )
  }

  override suspend fun removeIngredient(id: Uuid) {
    val entity = ingredientDao.getById(id)
    if (entity != null) ingredientDao.delete(entity)
  }

  override suspend fun setIngredients(newIngredients: List<Ingredient>) {
    newIngredients.forEach { addIngredient(it) }
  }

  override suspend fun upsertIngredientWithDetails(
      ingredient: Ingredient,
      packages: List<Package>,
      bridges: List<BridgeConversion>,
  ) {
    ingredientDao.upsertIngredientWithDetails(
        ingredient = ingredient.toEntity(),
        categoryId = ingredient.categoryId,
        packages = packages.map { it.toEntity() },
        bridges =
            bridges.map {
              UnitConversionBridgeEntity(
                  id = it.id,
                  ingredientId = it.ingredientId,
                  fromUnitId = it.fromUnitId,
                  toUnitId = it.toUnitId,
                  fromQuantity = it.fromQuantity,
                  toQuantity = it.toQuantity,
              )
            },
    )
  }

  override suspend fun addCategory(category: Category) {
    categoryDao.upsert(category.toEntity())
  }

  override suspend fun updateCategory(category: Category) {
    categoryDao.upsert(category.toEntity())
  }

  override suspend fun removeCategory(id: Uuid) {
    val existing = categoryDao.observeAll().first().find { it.id == id }
    if (existing != null) categoryDao.delete(existing)
  }

  override suspend fun setCategories(newCategories: List<Category>) {
    newCategories.forEach { addCategory(it) }
  }

  override suspend fun addPackage(pkg: Package) {
    packageDao.upsert(pkg.toEntity())
  }

  override suspend fun updatePackage(pkg: Package) {
    packageDao.upsert(pkg.toEntity())
  }

  override suspend fun removePackage(id: Uuid) {
    val entity = packageDao.getById(id)
    if (entity != null) packageDao.delete(entity)
  }

  override suspend fun setPackages(newPackages: List<Package>) {
    newPackages.forEach { addPackage(it) }
  }

  override suspend fun removePackagesForStore(storeId: Uuid) {
    packageDao.removeByStore(storeId)
  }

  override suspend fun addBridge(bridge: BridgeConversion) {
    ingredientDao.upsertConversionBridge(
        UnitConversionBridgeEntity(
            id = bridge.id,
            ingredientId = bridge.ingredientId,
            fromUnitId = bridge.fromUnitId,
            toUnitId = bridge.toUnitId,
            fromQuantity = bridge.fromQuantity,
            toQuantity = bridge.toQuantity,
        )
    )
  }

  override suspend fun updateBridge(bridge: BridgeConversion) {
    addBridge(bridge)
  }

  override suspend fun removeBridge(id: Uuid) {
    ingredientDao.deleteConversionById(id)
  }

  override suspend fun setBridges(newBridges: List<BridgeConversion>) {
    newBridges.forEach { addBridge(it) }
  }
}
