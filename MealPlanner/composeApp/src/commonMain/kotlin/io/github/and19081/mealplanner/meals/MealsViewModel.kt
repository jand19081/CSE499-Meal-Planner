package io.github.and19081.mealplanner.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.DataQualityValidator
import io.github.and19081.mealplanner.domain.DataWarning
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.kitchen.*
import io.github.and19081.mealplanner.recipes.RecipeRepository
import kotlin.math.max
import kotlin.uuid.Uuid
import io.github.and19081.mealplanner.domain.UnitConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MealsViewModel(
    private val mealRepository: MealRepository,
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val pantryRepository: PantryRepository,
    private val unitRepository: UnitRepository,
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  private val _sortByAlpha = MutableStateFlow(true)
  private val _errorMessage = MutableStateFlow<String?>(null)

  private val _ingredientBundle =
      combine(
          ingredientRepository.ingredients,
          ingredientRepository.packages,
          ingredientRepository.bridges,
      ) { i, p, b ->
        IngredientBundle(i, p, b)
      }

  private val _uiSettings =
      combine(_searchQuery, _sortByAlpha, _errorMessage) { q, s, e -> UiSettings(q, s, e) }

  val uiState =
      combine(
              mealRepository.meals,
              recipeRepository.recipes,
              unitRepository.units,
              _ingredientBundle,
              _uiSettings,
          ) { meals, recipes, units, ingBundle, settings ->
            val ingredients = ingBundle.ingredients
            val packages = ingBundle.packages
            val bridges = ingBundle.bridges
            val allUnits = units
            val query = settings.query
            val isAlpha = settings.isAlpha
            val error = settings.error

            // Pre-calculate Maps
            val recipesById = recipes.associateBy { it.id }
            val ingredientsById = ingredients.associateBy { it.id }

            // Validate Meals
            val warningsMap =
                meals.associate { meal ->
                  meal.id to
                      DataQualityValidator.validateMeal(
                          meal,
                          recipesById,
                          ingredientsById,
                          packages,
                          bridges,
                          allUnits,
                      )
                }

            // 1. Filter
            val filtered =
                if (query.isBlank()) meals
                else {
                  meals.filter { it.name.contains(query, ignoreCase = true) }
                }

            // 2. Sort
            val sorted =
                if (isAlpha) filtered.sortedBy { it.name }
                else filtered.sortedByDescending { it.name }

            // Group (Just "All" for now)
            val grouped = mapOf("All Meals" to sorted)

            MealsUiState(
                groupedMeals = grouped,
                searchQuery = query,
                allRecipes = recipes,
                allIngredients = ingredients,
                allPackages = packages,
                allBridges = bridges,
                allUnits = allUnits,
                errorMessage = error,
                mealWarnings = warningsMap,
            )
          }
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5000),
              MealsUiState(
                  emptyMap(),
                  "",
                  emptyList(),
                  emptyList(),
                  emptyList(),
                  emptyList(),
                  emptyList(),
                  null,
                  emptyMap(),
              ),
          )

  fun onSearchQueryChange(query: String) {
    _searchQuery.value = query
    _errorMessage.value = null
  }

  fun clearError() {
    _errorMessage.value = null
  }

  fun saveMeal(meal: PrePlannedMeal) {
    val nameVal = io.github.and19081.mealplanner.domain.Validators.validateMealName(meal.name)
    if (nameVal.isFailure) {
      _errorMessage.value = nameVal.exceptionOrNull()?.message
      return
    }

    _errorMessage.value = null
    viewModelScope.launch { mealRepository.upsertMeal(meal) }
  }

  fun deleteMeal(meal: PrePlannedMeal) {
    viewModelScope.launch { mealRepository.removeMeal(meal.id) }
  }

  fun createMakeTransaction(
      meal: PrePlannedMeal,
      multiplier: Double = 1.0,
      yieldIngredientId: Uuid? = null,
      yieldQuantity: Double? = null,
      yieldUnitId: Uuid? = null
  ): KitchenTransaction {
    val changes = mutableListOf<InventoryChange>()
    val allUnits = uiState.value.allUnits
    val ingredientsMap = uiState.value.allIngredients.associateBy { it.id }
    val recipesMap = uiState.value.allRecipes.associateBy { it.id }

    // Helper to add changes
    fun addChange(ingId: Uuid, qty: Double, unitId: Uuid?) {
      val name = ingredientsMap[ingId]?.name ?: "Unknown"
      val unit = allUnits.find { it.id == unitId }
      changes.add(
          InventoryChange(
              ingredientId = ingId,
              ingredientName = name,
              quantity = qty,
              unitId = unitId,
              unitAbbreviation = unit?.abbreviation ?: "?",
              direction = TransactionDirection.OUT
          )
      )
    }

    // 1. Recipes
    meal.recipes.forEach { rId ->
      val recipe = recipesMap[rId]
      if (recipe != null) {
        recipe.ingredients.forEach { ri ->
          if (ri.ingredientId != null) {
            addChange(ri.ingredientId, ri.quantity * multiplier, ri.unitId)
          }
        }
      }
    }

    // 2. Independent Ingredients
    meal.independentIngredients.forEach { item ->
      addChange(item.ingredientId, item.quantity * multiplier, item.unitId)
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
        title = "Making: ${meal.name}",
        changes = changes
    )
  }

  fun commitTransaction(transaction: KitchenTransaction) {
    viewModelScope.launch {
      val allUnits = uiState.value.allUnits
      val pantryItems = pantryRepository.pantryItems.value

      transaction.changes.forEach { change ->
        val currentPantryItem = pantryItems.find { it.ingredientId == change.ingredientId }
        val currentQty = currentPantryItem?.quantity ?: 0.0
        val currentUnitId = currentPantryItem?.unitId ?: change.unitId

        if (currentUnitId != null) {
            val convertedChangeQty = UnitConverter.convert(
                amount = change.quantity,
                fromUnitId = change.unitId,
                toUnitId = currentUnitId,
                allUnits = allUnits.associateBy { it.id }
            ) ?: 0.0

            val newQty = if (change.direction == TransactionDirection.IN) {
                currentQty + convertedChangeQty
            } else {
                max(0.0, currentQty - convertedChangeQty)
            }

            pantryRepository.updateQuantity(change.ingredientId, newQty, currentUnitId)
        }
      }
    }
  }
}

private data class IngredientBundle(
    val ingredients: List<Ingredient>,
    val packages: List<Package>,
    val bridges: List<BridgeConversion>,
)

private data class UiSettings(val query: String, val isAlpha: Boolean, val error: String?)

data class MealsUiState(
    val groupedMeals: Map<String, List<PrePlannedMeal>>,
    val searchQuery: String,
    val allRecipes: List<Recipe>,
    val allIngredients: List<Ingredient>,
    val allPackages: List<Package>,
    val allBridges: List<BridgeConversion>,
    val allUnits: List<UnitModel>,
    val errorMessage: String? = null,
    val mealWarnings: Map<Uuid, List<DataWarning>> = emptyMap(),
)
