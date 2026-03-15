package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.LeftoverItem
import io.github.and19081.mealplanner.LeftoverRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.LeftoverInventoryEntity
import io.github.and19081.mealplanner.data.toModel
import kotlin.math.max
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomLeftoverRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : LeftoverRepository {

  private val leftoverDao = db.leftoverDao()

  override val leftovers: StateFlow<List<LeftoverItem>> =
      leftoverDao
          .observeAll()
          .map { list -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun addLeftover(
      recipeId: Uuid,
      servings: Double,
      dateAdded: String,
      expirationDate: String?,
  ) {
    leftoverDao.upsert(
        LeftoverInventoryEntity(
            recipeId = recipeId,
            remainingServings = servings,
            dateAdded = dateAdded,
            expirationDate = expirationDate,
        )
    )
  }

  override suspend fun removeLeftover(id: Uuid) {
    leftoverDao.removeById(id)
  }

  override suspend fun consumeLeftover(id: Uuid, servings: Double) {
    val existing = leftovers.value.find { it.id == id } ?: return
    val newServings = max(0.0, existing.remainingServings - servings)
    if (newServings <= 0.001) {
      removeLeftover(id)
    } else {
      leftoverDao.upsert(
          LeftoverInventoryEntity(
              id = existing.id,
              recipeId = existing.recipeId,
              remainingServings = newServings,
              dateAdded = existing.dateAdded,
              expirationDate = existing.expirationDate,
          )
      )
    }
  }
}
