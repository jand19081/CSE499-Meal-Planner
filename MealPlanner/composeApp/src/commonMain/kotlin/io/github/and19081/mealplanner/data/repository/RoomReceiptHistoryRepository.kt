package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.domain.repository.ReceiptHistoryRepository
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptLineItem
import io.github.and19081.mealplanner.domain.model.ItemMeasurement
import io.github.and19081.mealplanner.data.db.entity.ItemMeasurement as EntityMeasurement
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.ReceiptLineItemEntity
import io.github.and19081.mealplanner.data.db.entity.StoreReceiptEntity
import io.github.and19081.mealplanner.data.db.relation.StoreReceiptWithLineItems
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class RoomReceiptHistoryRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : ReceiptHistoryRepository {
  private val dao = db.receiptDao()

  override val trips: StateFlow<List<ReceiptHistory>> =
      dao.observeAllWithLineItems()
          .map { list -> list.map { it.toDomain() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun getTripWithLineItems(id: Uuid): ReceiptHistory? {
    return dao.observeAllWithLineItems().first().find { it.receipt.id == id }?.toDomain()
  }

  override suspend fun addTrip(trip: ReceiptHistory) {
    dao.upsertReceiptWithDetails(
        trip.toEntity(),
        trip.lineItems.map { it.toEntity() }
    )
  }

  override suspend fun updateTrip(trip: ReceiptHistory) {
    addTrip(trip)
  }

  override suspend fun removeTrip(id: Uuid) {
    val existing = dao.observeAllWithLineItems().first().find { it.receipt.id == id }
    if (existing != null) {
      dao.deleteReceipt(existing.receipt)
    }
  }

  override suspend fun setTrips(history: List<ReceiptHistory>) {
    history.forEach { addTrip(it) }
  }

  private fun StoreReceiptWithLineItems.toDomain(): ReceiptHistory =
      receipt.toDomain().copy(lineItems = lineItems.map { it.toDomain() })

  private fun StoreReceiptEntity.toDomain(): ReceiptHistory =
      ReceiptHistory(
          id = id,
          date = try {
              LocalDate.parse(date)
          } catch (e: Exception) {
              Clock.System.todayIn(TimeZone.currentSystemDefault())
          },
          time = try {
              LocalTime.parse(time)
          } catch (e: Exception) {
              LocalTime(12, 0)
          },
          storeId = storeId,
          restaurantId = restaurantId,
          projectedTotalCents = projectedTotalCents ?: 0,
          actualTotalCents = actualTotalCents ?: 0,
          taxPaidCents = taxPaidCents ?: 0,
      )

  private fun ReceiptLineItemEntity.toDomain(): ReceiptLineItem =
      ReceiptLineItem(
          id = id,
          receiptId = receiptId,
          measurement = ItemMeasurement(
              foodItemId = measurement.foodItemId ?: Uuid.NIL,
              unitId = measurement.unitId ?: Uuid.NIL,
              quantity = measurement.quantity
          ),
          customName = customName,
          pricePaidCents = pricePaidCents,
      )

  private fun ReceiptHistory.toEntity(): StoreReceiptEntity =
      StoreReceiptEntity(
          id = id,
          name = "", // Not used in ReceiptHistory domain
          date = date.toString(),
          time = time.toString(),
          storeId = storeId,
          restaurantId = restaurantId,
          projectedTotalCents = projectedTotalCents,
          actualTotalCents = actualTotalCents,
          taxPaidCents = taxPaidCents
      )

  private fun ReceiptLineItem.toEntity(): ReceiptLineItemEntity =
      ReceiptLineItemEntity(
          id = id,
          receiptId = receiptId,
          measurement = EntityMeasurement(
              foodItemId = measurement.foodItemId,
              unitId = measurement.unitId,
              quantity = measurement.quantity
          ),
          customName = customName,
          pricePaidCents = pricePaidCents
      )
}
