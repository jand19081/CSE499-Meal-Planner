package io.github.and19081.mealplanner.ingredients

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

object IngredientRepository {
    private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredients = _ingredients.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _packages = MutableStateFlow<List<Package>>(emptyList())
    val packages = _packages.asStateFlow()

    private val _bridges = MutableStateFlow<List<BridgeConversion>>(emptyList())
    val bridges = _bridges.asStateFlow()

    // --- Ingredients ---
    fun addIngredient(ingredient: Ingredient) {
        _ingredients.update { it + ingredient }
    }

    fun updateIngredient(ingredient: Ingredient) {
        _ingredients.update { list -> list.map { if (it.id == ingredient.id) ingredient else it } }
    }

    fun removeIngredient(id: Uuid) {
        _ingredients.update { list -> list.filter { it.id != id } }
        // Cascade delete packages and bridges
        _packages.update { list -> list.filter { it.ingredientId != id } }
        _bridges.update { list -> list.filter { it.ingredientId != id } }
    }

    fun setIngredients(newIngredients: List<Ingredient>) {
        _ingredients.value = newIngredients
    }

    // --- Categories ---
    fun addCategory(category: Category) {
        _categories.update { it + category }
    }
    
    fun setCategories(newCategories: List<Category>) {
        _categories.value = newCategories
    }

    // --- Packages ---
    fun addPackage(pkg: Package) {
        _packages.update { it + pkg }
    }
    
    fun updatePackage(pkg: Package) {
        _packages.update { list -> list.map { if (it.id == pkg.id) pkg else it } }
    }
    
    fun removePackage(id: Uuid) {
        _packages.update { list -> list.filter { it.id != id } }
    }

    fun setPackages(newPackages: List<Package>) {
        _packages.value = newPackages
    }

    fun removePackagesForStore(storeId: Uuid) {
        _packages.update { list -> list.filter { it.storeId != storeId } }
    }

    // --- Bridges ---
    fun addBridge(bridge: BridgeConversion) {
        _bridges.update { it + bridge }
    }
    
    fun setBridges(newBridges: List<BridgeConversion>) {
        _bridges.value = newBridges
    }
}