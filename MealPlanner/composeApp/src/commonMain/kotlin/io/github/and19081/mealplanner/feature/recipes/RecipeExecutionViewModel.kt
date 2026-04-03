package io.github.and19081.mealplanner.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.domain.logic.CommitRecipeExecutionUseCase
import io.github.and19081.mealplanner.domain.model.Recipe
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeExecutionViewModel(
    private val commitRecipeExecutionUseCase: CommitRecipeExecutionUseCase,
    private val foodItemRepository: FoodItemRepository,
    private val unitRepository: UnitRepository,
) : ViewModel() {

  private val _state = MutableStateFlow<RecipeExecutionState?>(null)
  val state: StateFlow<RecipeExecutionState?> = _state.asStateFlow()

  private val _isFinishing = MutableStateFlow(false)
  val isFinishing: StateFlow<Boolean> = _isFinishing.asStateFlow()

  val allItemNames: StateFlow<Map<Uuid, String>> =
      foodItemRepository.foodItems
          .map { items -> items.associate { it.id to it.name } }
          .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

  val allUnitAbbr: StateFlow<Map<Uuid, String>> =
      unitRepository.units
          .map { units -> units.associate { it.id to it.abbreviation } }
          .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

  fun start(recipe: Recipe) {
    _state.value =
        RecipeExecutionState(
            recipe = recipe,
            targetServings = recipe.recipeInfo.servings,
            currentStepIndex = -1,
            view = RecipeExecutionView.INSTRUCTIONS,
        )
  }

  fun setTargetServings(servings: Double) = _state.update {
    it?.copy(targetServings = servings.coerceAtLeast(0.5))
  }

  fun nextStep() = _state.update { s ->
    s?.copy(currentStepIndex = (s.currentStepIndex + 1).coerceAtMost(s.totalSteps - 1))
  }

  fun prevStep() = _state.update { s ->
    s?.copy(currentStepIndex = (s.currentStepIndex - 1).coerceAtLeast(-1))
  }

  fun toggleView() = _state.update { s ->
    s?.copy(
        view =
            when (s.view) {
              RecipeExecutionView.INSTRUCTIONS -> RecipeExecutionView.INGREDIENTS
              RecipeExecutionView.INGREDIENTS -> RecipeExecutionView.INSTRUCTIONS
            }
    )
  }

  fun showFinishSheet() {
    _isFinishing.value = true
  }

  fun hideFinishSheet() {
    _isFinishing.value = false
  }

  fun finish(yieldServings: Double) {
    val s = _state.value ?: return
    viewModelScope.launch {
      commitRecipeExecutionUseCase(s.recipe, s.scaleFactor, yieldServings)
      _state.value = null
      _isFinishing.value = false
    }
  }

  fun cancel() {
    _state.value = null
    _isFinishing.value = false
  }
}
