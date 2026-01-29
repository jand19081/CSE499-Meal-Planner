package io.github.and19081.mealplanner.meals

import io.github.and19081.mealplanner.Meal
import io.github.and19081.mealplanner.MealComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

object MealRepository {
    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals = _meals.asStateFlow()

    private val _mealComponents = MutableStateFlow<List<MealComponent>>(emptyList())
    val mealComponents = _mealComponents.asStateFlow()

    fun addMeal(meal: Meal) {
        _meals.update { it + meal }
    }

    fun updateMeal(meal: Meal) {
        _meals.update { list -> list.map { if (it.id == meal.id) meal else it } }
    }

    fun upsertMeal(meal: Meal) {
        _meals.update { current ->
            val list = current.toMutableList()
            list.removeAll { it.id == meal.id }
            list.add(meal)
            list
        }
    }

    fun removeMeal(id: Uuid) {
        _meals.update { list -> list.filter { it.id != id } }
        _mealComponents.update { list -> list.filter { it.mealId != id } }
    }

    fun removeComponentsForMeal(mealId: Uuid) {
        _mealComponents.update { list -> list.filter { it.mealId != mealId } }
    }

    fun addMealComponent(component: MealComponent) {
        _mealComponents.update { it + component }
    }

    fun setMeals(newMeals: List<Meal>) {
        _meals.value = newMeals
    }
    
    fun setMealComponents(newComponents: List<MealComponent>) {
        _mealComponents.value = newComponents
    }
}
