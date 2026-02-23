package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.Restaurant
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.RestaurantEntity
import io.github.and19081.mealplanner.ingredients.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class RoomRestaurantRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope
) : RestaurantRepository {

    private val restaurantDao = db.restaurantDao()

    override val restaurants: StateFlow<List<Restaurant>> = restaurantDao.observeAll()
        .map { list -> list.map { Restaurant(it.id, it.name) } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override suspend fun addRestaurant(restaurant: Restaurant) {
        restaurantDao.upsert(RestaurantEntity(restaurant.id, restaurant.name))
    }

    override suspend fun updateRestaurant(restaurant: Restaurant) {
        restaurantDao.upsert(RestaurantEntity(restaurant.id, restaurant.name))
    }

    override suspend fun deleteRestaurant(id: Uuid) {
        val entity = restaurantDao.observeAll().first().find { it.id == id }
        if (entity != null) restaurantDao.delete(entity)
    }

    override suspend fun setRestaurants(newRestaurants: List<Restaurant>) {
        newRestaurants.forEach { addRestaurant(it) }
    }
}
