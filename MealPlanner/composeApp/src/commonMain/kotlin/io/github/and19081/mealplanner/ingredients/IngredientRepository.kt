package io.github.and19081.mealplanner.ingredients

import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.Uuid

interface IngredientRepository {
    val ingredients: StateFlow<List<Ingredient>>
    val categories: StateFlow<List<Category>>
    val packages: StateFlow<List<Package>>
    val bridges: StateFlow<List<BridgeConversion>>

    suspend fun count(): Int
    suspend fun addIngredient(ingredient: Ingredient)
    suspend fun updateIngredient(ingredient: Ingredient)
    suspend fun removeIngredient(id: Uuid)
    suspend fun setIngredients(newIngredients: List<Ingredient>)

    suspend fun addCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun removeCategory(id: Uuid)
    suspend fun setCategories(newCategories: List<Category>)

    suspend fun addPackage(pkg: Package)
    suspend fun updatePackage(pkg: Package)
    suspend fun removePackage(id: Uuid)
    suspend fun setPackages(newPackages: List<Package>)
    suspend fun removePackagesForStore(storeId: Uuid)

    suspend fun addBridge(bridge: BridgeConversion)
    suspend fun updateBridge(bridge: BridgeConversion)
    suspend fun removeBridge(id: Uuid)
    suspend fun setBridges(newBridges: List<BridgeConversion>)
}
