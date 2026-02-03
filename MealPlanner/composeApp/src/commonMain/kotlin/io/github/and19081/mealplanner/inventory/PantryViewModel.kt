package io.github.and19081.mealplanner.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.PantryItem
import io.github.and19081.mealplanner.PantryRepository
import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.UnitRepository
import io.github.and19081.mealplanner.ingredients.Ingredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.uuid.Uuid

class PantryViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState = combine(
        PantryRepository.pantryItems,
        IngredientRepository.ingredients,
        IngredientRepository.categories,
        UnitRepository.units,
        _searchQuery
    ) { pantryItems, allIngredients, allCategories, allUnits, query ->
        
        val ingredientsMap = allIngredients.associateBy { it.id }
        val categoryMap = allCategories.associateBy { it.id }
        val unitMap = allUnits.associateBy { it.id }

        val joined = pantryItems.mapNotNull { item ->
            val ing = ingredientsMap[item.ingredientId]
            val unit = unitMap[item.unitId]
            if (ing != null && unit != null) {
                val catName = categoryMap[ing.categoryId]?.name ?: "Uncategorized"
                PantryItemUi(
                    id = item.ingredientId,
                    name = ing.name,
                    category = catName,
                    quantity = item.quantity,
                    unit = unit
                )
            } else null
        }

        val filtered = if (query.isBlank()) joined else {
            joined.filter { it.name.contains(query, ignoreCase = true) }
        }

        PantryUiState(
            items = filtered.sortedBy { it.name },
            allIngredients = allIngredients.sortedBy { it.name },
            allUnits = allUnits
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PantryUiState(emptyList(), emptyList(), emptyList()))

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun updateQuantity(ingredientId: Uuid, newAmount: Double, unitId: Uuid) {
        PantryRepository.updateQuantity(ingredientId, newAmount, unitId)
    }
}

data class PantryUiState(
    val items: List<PantryItemUi>,
    val allIngredients: List<Ingredient>,
    val allUnits: List<UnitModel>
)

data class PantryItemUi(
    val id: Uuid,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: UnitModel
)
