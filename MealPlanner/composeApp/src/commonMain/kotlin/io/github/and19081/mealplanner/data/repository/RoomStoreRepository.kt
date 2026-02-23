package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.toEntity
import io.github.and19081.mealplanner.data.toModel
import io.github.and19081.mealplanner.ingredients.Store
import io.github.and19081.mealplanner.ingredients.StoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.uuid.Uuid

class RoomStoreRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope
) : StoreRepository {

    private val storeDao = db.storeDao()

    override val stores: StateFlow<List<Store>> = storeDao.observeAll()
        .map { list -> list.map { it.toModel() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override suspend fun addStore(store: Store) {
        storeDao.upsert(store.toEntity())
    }

    override suspend fun updateStore(store: Store) {
        storeDao.upsert(store.toEntity())
    }

    override suspend fun deleteStore(storeId: Uuid) {
        val entity = storeDao.getById(storeId.toString())
        if (entity != null) storeDao.delete(entity)
    }

    override suspend fun setStores(newStores: List<Store>) {
        newStores.forEach { addStore(it) }
    }
}
