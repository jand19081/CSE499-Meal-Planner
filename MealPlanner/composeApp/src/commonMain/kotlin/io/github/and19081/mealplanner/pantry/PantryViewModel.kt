package io.github.and19081.mealplanner.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.LeftoverItem
import io.github.and19081.mealplanner.LeftoverRepository
import io.github.and19081.mealplanner.PantryItem
import io.github.and19081.mealplanner.PantryRepository
import io.github.and19081.mealplanner.Recipe
import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.UnitRepository
import io.github.and19081.mealplanner.ingredients.Category
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import io.github.and19081.mealplanner.domain.UnitConverter
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PantryViewModel(
    private val pantryRepository: PantryRepository,
    private val leftoverRepository: LeftoverRepository,
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val unitRepository: UnitRepository,
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")

  val uiState =
      combine(
              listOf(
                  pantryRepository.pantryItems,
                  leftoverRepository.leftovers,
                  recipeRepository.recipes,
                  ingredientRepository.ingredients,
                  ingredientRepository.categories,
                  unitRepository.units,
                  _searchQuery,
                  ingredientRepository.bridges,
              )
          ) { array ->
            val pantryItems = array[0] as List<PantryItem>
            val leftovers = array[1] as List<LeftoverItem>
            val allRecipes = array[2] as List<Recipe>
            val allIngredients = array[3] as List<Ingredient>
            val allCategories = array[4] as List<Category>
            val allUnits = array[5] as List<UnitModel>
            val query = array[6] as String
            val bridges = array[7] as List<io.github.and19081.mealplanner.ingredients.BridgeConversion>

            val ingredientsMap = allIngredients.associateBy { it.id }
            val categoryMap = allCategories.associateBy { it.id }
            val unitMap = allUnits.associateBy { it.id }
            val recipesMap = allRecipes.associateBy { it.id }

            val joinedPantry =
                pantryItems.mapNotNull { item ->
                  val ing = ingredientsMap[item.ingredientId]
                  val unit = unitMap[item.unitId]
                  if (ing != null && unit != null) {
                    val catName = categoryMap[ing.categoryId]?.name ?: "Uncategorized"

                    val displayUnit = ing.preferredUnitId?.let { unitMap[it] } ?: unit
                    val displayQty =
                        if (displayUnit.id != unit.id) {
                          UnitConverter.convert(
                              item.quantity,
                              unit.id,
                              displayUnit.id,
                              unitMap,
                              bridges,
                          ) ?: 0.0
                        } else {
                          item.quantity
                        }

                    PantryItemUi(
                        id = item.ingredientId,
                        batchId = item.id,
                        name = ing.name,
                        category = catName,
                        quantity = displayQty,
                        unit = displayUnit,
                    )
                  } else null
                }

            val joinedLeftovers =
                leftovers.map { item ->
                  LeftoverItemUi(
                      id = item.id,
                      recipeName = recipesMap[item.recipeId]?.name ?: "Unknown Recipe",
                      remainingServings = item.remainingServings,
                      dateAdded = item.dateAdded,
                      expirationDate = item.expirationDate,
                  )
                }

            val filteredPantry =
                if (query.isBlank()) joinedPantry
                else {
                  joinedPantry.filter { it.name.contains(query, ignoreCase = true) }
                }

            val filteredLeftovers =
                if (query.isBlank()) joinedLeftovers
                else {
                  joinedLeftovers.filter { it.recipeName.contains(query, ignoreCase = true) }
                }

            PantryUiState(
                items = filteredPantry.sortedBy { it.name },
                leftovers = filteredLeftovers.sortedByDescending { it.dateAdded },
                allIngredients = allIngredients.sortedBy { it.name },
                allUnits = allUnits,
            )
          }
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5000),
              PantryUiState(emptyList(), emptyList(), emptyList(), emptyList()),
          )

  fun onSearchQueryChange(query: String) {
    _searchQuery.value = query
  }

  fun updateQuantity(
      ingredientId: Uuid,
      newAmount: Double,
      unitId: Uuid,
  ) {
    viewModelScope.launch {
      pantryRepository.updateQuantity(ingredientId, newAmount, unitId)
    }
  }

  fun deleteItem(batchId: Uuid) {
    viewModelScope.launch { pantryRepository.removeBatch(batchId) }
  }

  fun updateLeftoverQuantity(id: Uuid, servings: Double) {
    viewModelScope.launch {
      val existing = leftoverRepository.leftovers.value.find { it.id == id } ?: return@launch
      leftoverRepository.consumeLeftover(id, existing.remainingServings - servings)
    }
  }

  fun deleteLeftover(id: Uuid) {
    viewModelScope.launch { leftoverRepository.removeLeftover(id) }
  }
}

data class PantryUiState(
    val items: List<PantryItemUi>,
    val leftovers: List<LeftoverItemUi>,
    val allIngredients: List<Ingredient>,
    val allUnits: List<UnitModel>,
)

data class PantryItemUi(
    val id: Uuid,
    val batchId: Uuid,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: UnitModel,
)

data class LeftoverItemUi(
    val id: Uuid,
    val recipeName: String,
    val remainingServings: Double,
    val dateAdded: String,
    val expirationDate: String? = null,
)
