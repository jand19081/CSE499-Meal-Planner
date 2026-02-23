package io.github.and19081.mealplanner.ingredients

import io.github.and19081.mealplanner.Restaurant
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.Uuid

interface RestaurantRepository {
    val restaurants: StateFlow<List<Restaurant>>
    suspend fun addRestaurant(restaurant: Restaurant)
    suspend fun updateRestaurant(restaurant: Restaurant)
    suspend fun deleteRestaurant(id: Uuid)
    suspend fun setRestaurants(newRestaurants: List<Restaurant>)
}
