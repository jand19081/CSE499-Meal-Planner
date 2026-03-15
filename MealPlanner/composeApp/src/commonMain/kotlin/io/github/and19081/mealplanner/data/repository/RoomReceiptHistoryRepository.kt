package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.ReceiptHistoryRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.StoreReceiptEntity
import io.github.and19081.mealplanner.data.toEntity
import io.github.and19081.mealplanner.data.toModel
import io.github.and19081.mealplanner.shoppinglist.ReceiptHistory
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomReceiptHistoryRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : ReceiptHistoryRepository {

  private val receiptDao = db.receiptDao()

  override val trips: StateFlow<List<ReceiptHistory>> =
      receiptDao
          .observeAll()
          .map { list: List<StoreReceiptEntity> -> list.map { it.toModel() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun getTripWithLineItems(id: Uuid): ReceiptHistory? {
    return receiptDao.getWithLineItems(id)?.toModel()
  }

  override suspend fun addTrip(trip: ReceiptHistory) {
    val anyId = Uuid.parse("00000000-0000-0000-0000-000000000000")
    val receiptEntity =
        StoreReceiptEntity(
            id = trip.id,
            name = "Trip on ${trip.date}",
            date = trip.date.toString(),
            time = trip.time.toString(),
            storeId = if (trip.storeId == anyId) null else trip.storeId,
            restaurantId = if (trip.restaurantId == anyId) null else trip.restaurantId,
            projectedTotalCents = trip.projectedTotalCents,
            actualTotalCents = trip.actualTotalCents,
            taxPaidCents = trip.taxPaidCents,
        )
    val lineItems = trip.lineItems.map { it.toEntity() }
    receiptDao.upsertReceiptWithDetails(receiptEntity, lineItems)
  }

  override suspend fun updateTrip(trip: ReceiptHistory) {
    addTrip(trip)
  }

  override suspend fun removeTrip(id: Uuid) {
    val trip = receiptDao.getWithLineItems(id)
    if (trip != null) {
      receiptDao.deleteReceipt(trip.receipt)
    }
  }

  override suspend fun setTrips(history: List<ReceiptHistory>) {
    history.forEach { addTrip(it) }
  }
}
