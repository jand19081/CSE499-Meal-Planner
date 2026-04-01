package io.github.and19081.mealplanner.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.domain.logic.CommitRecipeExecutionUseCase
import io.github.and19081.mealplanner.domain.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeExecutionViewModel(
    private val commitRecipeExecutionUseCase: CommitRecipeExecutionUseCase,
) : ViewModel() {

  private val _state = MutableStateFlow<RecipeExecutionState?>(null)
  val state: StateFlow<RecipeExecutionState?> = _state.asStateFlow()

  private val _isFinishing = MutableStateFlow(false)
  val isFinishing: StateFlow<Boolean> = _isFinishing.asStateFlow()

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
