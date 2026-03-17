package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.ScheduledMeal
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.ReceiptLineItemEntity
import io.github.and19081.mealplanner.data.db.entity.ScheduledMealEntity
import io.github.and19081.mealplanner.data.db.entity.StoreReceiptEntity
import io.github.and19081.mealplanner.data.db.relation.ScheduledMealWithSource
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class RoomMealPlanRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : MealPlanRepository {
  private val dao = db.scheduledMealDao()

  override val entries: StateFlow<List<ScheduledMeal>> =
      dao.observeInRangeWithSource("0000-00-00", "9999-99-99")
          .map { list -> list.map { it.toDomain() } }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  override suspend fun addPlan(entry: ScheduledMeal) {
    dao.upsert(entry.toEntity())
  }

  override suspend fun removePlan(id: Uuid) {
    val existing = dao.getWithSource(id)
    if (existing != null) {
      dao.delete(existing.scheduledMeal)
    }
  }

  override suspend fun setConsumedStatus(id: Uuid, consumed: Boolean) {
    dao.setConsumed(id, consumed)
  }

  override suspend fun addReceipt(
      entryId: Uuid,
      totalCents: Int,
      taxCents: Int,
      lineItems: List<Triple<String, Double, Int>>
  ) {
    val receiptId = Uuid.random()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

    val receiptEntity = StoreReceiptEntity(
        id = receiptId,
        name = "Meal Receipt",
        date = today.toString(),
        time = now.toString(),
        scheduledMealId = entryId,
        actualTotalCents = totalCents,
        taxPaidCents = taxCents
    )

    val lineItemEntities = lineItems.map { (name, qty, price) ->
        ReceiptLineItemEntity(
            receiptId = receiptId,
            customName = name,
            quantityBought = qty,
            pricePaidCents = price
        )
    }

    db.receiptDao().upsertReceiptWithDetails(receiptEntity, lineItemEntities)
  }

  override suspend fun clearAll() {
    dao.clearAll()
  }

  private fun ScheduledMealWithSource.toDomain(): ScheduledMeal =
      ScheduledMeal(
          id = scheduledMeal.id,
          date = try { LocalDate.parse(scheduledMeal.date) } catch (e: Exception) { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
          time = try { LocalTime.parse(scheduledMeal.time) } catch (e: Exception) { LocalTime(12, 0) },
          mealType = scheduledMeal.mealType,
          prePlannedMealId = scheduledMeal.foodItemId,
          restaurantId = scheduledMeal.restaurantId,
          peopleCount = scheduledMeal.peopleCount,
          isConsumed = scheduledMeal.isConsumed,
          anticipatedCostCents = scheduledMeal.anticipatedCostCents,
      )

  private fun ScheduledMeal.toEntity(): ScheduledMealEntity =
      ScheduledMealEntity(
          id = id,
          date = date.toString(),
          time = time.toString(),
          mealType = mealType,
          foodItemId = prePlannedMealId,
          restaurantId = restaurantId,
          peopleCount = peopleCount,
          isConsumed = isConsumed,
          anticipatedCostCents = anticipatedCostCents
      )
}
