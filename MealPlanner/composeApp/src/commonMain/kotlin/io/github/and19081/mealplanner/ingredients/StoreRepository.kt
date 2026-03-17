package io.github.and19081.mealplanner.ingredients

import io.github.and19081.mealplanner.domain.Store
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.StateFlow

interface StoreRepository {
  val stores: StateFlow<List<Store>>

  suspend fun addStore(store: Store)

  suspend fun updateStore(store: Store)

  suspend fun deleteStore(storeId: Uuid)

  suspend fun setStores(newStores: List<Store>)
}
