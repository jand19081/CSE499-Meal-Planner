package io.github.and19081.mealplanner.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.PantryItem
import io.github.and19081.mealplanner.PantryRepository
import io.github.and19081.mealplanner.Measure
import io.github.and19081.mealplanner.MeasureUnit
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.Ingredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PantryViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState = combine(
        PantryRepository.pantryItems,
        IngredientRepository.ingredients,
        _searchQuery
    ) { pantryItems, allIngredients, query ->
        
        val ingredientsMap = allIngredients.associateBy { it.id }

        val joined = pantryItems.mapNotNull { item ->
            val ing = ingredientsMap[item.ingredientId]
            if (ing != null) {
                // Determine display string (convert back to best guess? No, just show raw for now)
                // Ideally we show "1 lb" instead of "453 g" if possible, but we stored as normalized if we used my logic?
                // Actually my logic in CalendarViewModel stored in base unit.
                
                // Let's stick to base units for display for now, or simple heuristic
                PantryItemUi(
                    id = item.ingredientId,
                    name = ing.name,
                    category = ing.category,
                    quantity = item.quantityOnHand
                )
            } else null
        }

        val filtered = if (query.isBlank()) joined else {
            joined.filter { it.name.contains(query, ignoreCase = true) }
        }

        PantryUiState(
            items = filtered.sortedBy { it.name },
            allIngredients = allIngredients.sortedBy { it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PantryUiState(emptyList(), emptyList()))

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun updateQuantity(ingredientId: java.util.UUID, newAmount: Double, unit: MeasureUnit) {
        // Must convert Uuid
        // Wait, Uuid vs java.util.UUID mismatch? 
        // My project uses kotlin.uuid.Uuid.
    }
    
    // Fix UUID type
    fun updateQuantity(ingredientId: kotlin.uuid.Uuid, newAmount: Double, unit: MeasureUnit) {
        PantryRepository.updateQuantity(ingredientId, Measure(newAmount, unit))
    }
}

data class PantryUiState(
    val items: List<PantryItemUi>,
    val allIngredients: List<Ingredient>
)

data class PantryItemUi(
    val id: kotlin.uuid.Uuid,
    val name: String,
    val category: String,
    val quantity: Measure
)
