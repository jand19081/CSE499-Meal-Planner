package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.feature.meals.Restaurant
import io.github.and19081.mealplanner.domain.repository.RestaurantRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.RestaurantEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomRestaurantRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : RestaurantRepository {
  private val dao = db.restaurantDao()

  override val restaurants: StateFlow<List<Restaurant>> =
      dao.observeAll()
          .map { list -> list.map { it.toDomain() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun addRestaurant(restaurant: Restaurant) {
    dao.upsert(restaurant.toEntity())
  }

  override suspend fun updateRestaurant(restaurant: Restaurant) {
    dao.upsert(restaurant.toEntity())
  }

  override suspend fun deleteRestaurant(id: Uuid) {
    val existing = dao.observeAll().first().find { it.id == id }
    if (existing != null) dao.delete(existing)
  }

  override suspend fun setRestaurants(restaurants: List<Restaurant>) {
    restaurants.forEach { addRestaurant(it) }
  }

  private fun RestaurantEntity.toDomain(): Restaurant = Restaurant(id = id, name = name)
  private fun Restaurant.toEntity(): RestaurantEntity = RestaurantEntity(id = id, name = name)
}
