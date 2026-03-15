package io.github.and19081.mealplanner.ingredients

import io.github.and19081.mealplanner.Restaurant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.StateFlow

interface RestaurantRepository {
  val restaurants: StateFlow<List<Restaurant>>

  suspend fun addRestaurant(restaurant: Restaurant)

  suspend fun updateRestaurant(restaurant: Restaurant)

  suspend fun deleteRestaurant(id: Uuid)

  suspend fun setRestaurants(newRestaurants: List<Restaurant>)
}
