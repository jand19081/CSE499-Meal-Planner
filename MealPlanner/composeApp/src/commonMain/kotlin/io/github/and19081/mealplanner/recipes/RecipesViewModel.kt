package io.github.and19081.mealplanner.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.DataQualityValidator
import io.github.and19081.mealplanner.domain.DataWarning
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import io.github.and19081.mealplanner.ingredients.Category
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.kitchen.*
import kotlin.math.max
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipesViewModel(
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val pantryRepository: PantryRepository,
    private val unitRepository: UnitRepository,
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  private val _sortByAlpha = MutableStateFlow(true)
  private val _filterCanMakeNow = MutableStateFlow(false)
  private val _errorMessage = MutableStateFlow<String?>(null)

  val uiState =
      combine(
              listOf(
                  recipeRepository.recipes,
                  _searchQuery,
                  _sortByAlpha,
                  _filterCanMakeNow,
                  ingredientRepository.ingredients,
                  pantryRepository.pantryItems,
                  ingredientRepository.packages,
                  ingredientRepository.bridges,
                  unitRepository.units,
                  _errorMessage,
              )
          ) { array ->
            val recipes = array[0] as List<Recipe>
            val query = array[1] as String
            val isAlpha = array[2] as Boolean
            val canMakeNow = array[3] as Boolean
            val ingredients = array[4] as List<Ingredient>
            val pantry = array[5] as List<PantryItem>
            val packages = array[6] as List<Package>
            val bridges = array[7] as List<BridgeConversion>
            val allUnits = array[8] as List<UnitModel>
            val error = array[9] as String?

            // Pre-calculate Maps for O(1) lookups
            val pantryMap = pantry.associateBy { it.ingredientId }
            val ingredientsMap = ingredients.associateBy { it.id }

            // Validate Recipes
            val warningsMap =
                recipes.associate { recipe ->
                  recipe.id to
                      DataQualityValidator.validateRecipe(
                          recipe,
                          ingredientsMap,
                          packages,
                          bridges,
                          allUnits,
                      )
                }

            // 1. Filter by Search Query
            var filtered =
                if (query.isBlank()) recipes
                else {
                  recipes.filter { it.name.contains(query, ignoreCase = true) }
                }

            // 2. Filter by "Can Make Now"
            if (canMakeNow) {
              filtered =
                  filtered.filter { recipe ->
                    val needed = recipe.ingredients
                    needed.all { ri ->
                      val ingId =
                          ri.ingredientId
                              ?: return@all true // Assume sub-recipes are okay for now or recursion
                                                 // needed
                      val pantryItem = pantryMap[ingId]
                      if (pantryItem == null) false
                      else {
                        // Check quantity
                        val ingredientBridges = bridges.filter { it.ingredientId == ingId }

                        val pantryInRiUnit =
                            UnitConverter.convert(
                                amount = pantryItem.quantity,
                                fromUnitId = pantryItem.unitId,
                                toUnitId = ri.unitId,
                                allUnits = allUnits.associateBy { it.id },
                                bridges = ingredientBridges,
                            ) ?: 0.0
                        pantryInRiUnit >= ri.quantity
                      }
                    }
                  }
            }

            // 3. Group
            val grouped: Map<String, List<Recipe>> =
                if (isAlpha) {
                  filtered
                      .sortedBy { it.name }
                      .groupBy { if (it.name.isNotEmpty()) it.name.first().uppercase() else "?" }
                      .toSortedMap()
                } else {
                  mapOf("All Recipes" to filtered.sortedBy { it.name })
                }

            val exactMatch = recipes.any { it.name.equals(query, ignoreCase = true) }

            RecipesUiState(
                groupedRecipes = grouped,
                allRecipes = recipes,
                searchQuery = query,
                doesExactMatchExist = exactMatch,
                allIngredients = ingredients,
                pantryItems = pantry,
                allPackages = packages,
                allBridges = bridges,
                allUnits = allUnits,
                isCanMakeNowFilterActive = canMakeNow,
                errorMessage = error,
                recipeWarnings = warningsMap,
            )
          }
          .flowOn(kotlinx.coroutines.Dispatchers.Default)
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5000),
              RecipesUiState(
                  emptyMap(),
                  emptyList(),
                  "",
                  false,
                  emptyList(),
                  emptyList(),
                  emptyList(),
                  emptyList(),
                  emptyList(),
                  false,
                  null,
                  emptyMap(),
              ),
          )

  fun onSearchQueryChange(query: String) {
    _searchQuery.value = query
    _errorMessage.value = null
  }

  fun toggleCanMakeNowFilter() {
    _filterCanMakeNow.update { !it }
  }

  fun clearError() {
    _errorMessage.value = null
  }

  private fun hasCircularDependency(
      recipe: Recipe,
      allRecipesMap: Map<Uuid, Recipe>,
      visited: Set<Uuid> = emptySet(),
  ): Boolean {
    if (visited.contains(recipe.id)) return true
    val newVisited = visited + recipe.id
    for (ri in recipe.ingredients) {
      if (ri.subRecipeId != null) {
        if (ri.subRecipeId == recipe.id) return true
        val subRecipe = allRecipesMap[ri.subRecipeId]
        if (subRecipe != null) {
          if (hasCircularDependency(subRecipe, allRecipesMap, newVisited)) {
            return true
          }
        }
      }
    }
    return false
  }

  fun saveRecipe(recipe: Recipe) {
    val nameVal = io.github.and19081.mealplanner.domain.Validators.validateRecipeName(recipe.name)
    if (nameVal.isFailure) {
      _errorMessage.value = nameVal.exceptionOrNull()?.message
      return
    }

    val servingsVal =
        io.github.and19081.mealplanner.domain.Validators.validateServings(recipe.servings)
    if (servingsVal.isFailure) {
      _errorMessage.value = servingsVal.exceptionOrNull()?.message
      return
    }

    val allRecipesMap = recipeRepository.recipes.value.associateBy { it.id }
    if (hasCircularDependency(recipe, allRecipesMap)) {
      _errorMessage.value = "Cannot save recipe: Circular dependency detected in sub-recipes."
      return
    }

    _errorMessage.value = null
    viewModelScope.launch { recipeRepository.upsertRecipe(recipe) }
  }

  fun deleteRecipe(id: Uuid) {
    viewModelScope.launch { recipeRepository.removeRecipe(id) }
  }

  fun addIngredient(name: String) {
    viewModelScope.launch {
      val categories = ingredientRepository.categories.value
      var miscCategory = categories.find { it.name.equals("Miscellaneous", ignoreCase = true) }

      if (miscCategory == null) {
        val newCat = Category(id = Uuid.random(), name = "Miscellaneous")
        ingredientRepository.addCategory(newCat)
        miscCategory = newCat
      }

      val newIngredient = Ingredient(id = Uuid.random(), name = name, categoryId = miscCategory.id)
      ingredientRepository.addIngredient(newIngredient)
    }
  }

  fun createMakeTransaction(
      recipe: Recipe,
      multiplier: Double = 1.0,
      yieldIngredientId: Uuid? = null,
      yieldQuantity: Double? = null,
      yieldUnitId: Uuid? = null
  ): KitchenTransaction {
    val changes = mutableListOf<InventoryChange>()
    val allUnits = uiState.value.allUnits
    val ingredientsMap = uiState.value.allIngredients.associateBy { it.id }

    // OUT: Ingredients
    recipe.ingredients.forEach { ri ->
      if (ri.ingredientId != null) {
        val name = ingredientsMap[ri.ingredientId]?.name ?: "Unknown"
        val unit = allUnits.find { it.id == ri.unitId }
        changes.add(
            InventoryChange(
                ingredientId = ri.ingredientId,
                ingredientName = name,
                quantity = ri.quantity * multiplier,
                unitId = ri.unitId,
                unitAbbreviation = unit?.abbreviation ?: "?",
                direction = TransactionDirection.OUT
            )
        )
      }
    }

    // IN: Dynamically Produced Item
    if (yieldIngredientId != null && yieldQuantity != null) {
      val producedIng = ingredientsMap[yieldIngredientId]
      if (producedIng != null) {
        val yieldUnit = allUnits.find { it.id == yieldUnitId }
        changes.add(
            InventoryChange(
                ingredientId = yieldIngredientId,
                ingredientName = producedIng.name,
                quantity = yieldQuantity,
                unitId = yieldUnit?.id,
                unitAbbreviation = yieldUnit?.abbreviation ?: "each",
                direction = TransactionDirection.IN
            )
        )
      }
    }

    return KitchenTransaction(
        type = TransactionType.Production,
        title = "Making: ${recipe.name}",
        changes = changes
    )
  }

  fun commitTransaction(transaction: KitchenTransaction) {
    viewModelScope.launch {
      val allUnits = uiState.value.allUnits
      val allIngredients = ingredientRepository.ingredients.value
      val allBridges = ingredientRepository.bridges.value
      val pantryItems = pantryRepository.pantryItems.value

      transaction.changes.forEach { change ->
        val ingredient = allIngredients.find { it.id == change.ingredientId }
        val unit = allUnits.find { it.id == change.unitId }

        if (ingredient != null && unit != null) {
          val preferredUnitId = ingredient.preferredUnitId
          val targetUnitId = preferredUnitId ?: when (unit.type) {
              UnitType.Weight -> SystemUnits.Gram.id
              UnitType.Volume -> SystemUnits.Ml.id
              UnitType.Count -> SystemUnits.Each.id
              else -> unit.id
          }

          val targetUnit = allUnits.find { it.id == targetUnitId }

          if (targetUnit != null) {
            val bridges = allBridges.filter { it.ingredientId == change.ingredientId }
            val changeInTargetUnit = UnitConverter.convert(
                amount = change.quantity,
                fromUnitId = change.unitId,
                toUnitId = targetUnitId,
                allUnits = allUnits.associateBy { it.id },
                bridges = bridges
            ) ?: 0.0

            val currentPantryItem = pantryItems.find { it.ingredientId == change.ingredientId }
            val currentQtyInTargetUnit = if (currentPantryItem != null) {
                UnitConverter.convert(
                    amount = currentPantryItem.quantity,
                    fromUnitId = currentPantryItem.unitId,
                    toUnitId = targetUnitId,
                    allUnits = allUnits.associateBy { it.id },
                    bridges = bridges
                ) ?: 0.0
            } else 0.0

            val newQty = if (change.direction == TransactionDirection.IN) {
                currentQtyInTargetUnit + changeInTargetUnit
            } else {
                max(0.0, currentQtyInTargetUnit - changeInTargetUnit)
            }

            pantryRepository.updateQuantity(change.ingredientId, newQty, targetUnitId)
          }
        }
      }
    }
  }
}

data class RecipesUiState(
    val groupedRecipes: Map<String, List<Recipe>>,
    val allRecipes: List<Recipe>,
    val searchQuery: String,
    val doesExactMatchExist: Boolean,
    val allIngredients: List<Ingredient>,
    val pantryItems: List<PantryItem>,
    val allPackages: List<Package>,
    val allBridges: List<BridgeConversion>,
    val allUnits: List<UnitModel>,
    val isCanMakeNowFilterActive: Boolean,
    val errorMessage: String? = null,
    val recipeWarnings: Map<Uuid, List<DataWarning>> = emptyMap(),
)
