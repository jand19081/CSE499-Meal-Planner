package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.StoreEntity
import io.github.and19081.mealplanner.domain.Store
import io.github.and19081.mealplanner.ingredients.StoreRepository
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomStoreRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : StoreRepository {
  private val dao = db.storeDao()

  override val stores: StateFlow<List<Store>> =
      dao.observeAll()
          .map { list -> list.map { it.toDomain() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun addStore(store: Store) {
    dao.upsert(store.toEntity())
  }

  override suspend fun updateStore(store: Store) {
    dao.upsert(store.toEntity())
  }

  override suspend fun deleteStore(storeId: Uuid) {
    val existing = dao.observeAll().first().find { it.id == storeId }
    if (existing != null) dao.delete(existing)
  }

  override suspend fun setStores(newStores: List<Store>) {
    newStores.forEach { addStore(it) }
  }

  private fun StoreEntity.toDomain(): Store = Store(id = id, name = name)
  private fun Store.toEntity(): StoreEntity = StoreEntity(id = id, name = name)
}
