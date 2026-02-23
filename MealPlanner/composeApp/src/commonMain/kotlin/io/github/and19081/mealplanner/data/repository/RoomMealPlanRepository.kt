package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.ScheduledMeal
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.ScheduledMealEntity
import io.github.and19081.mealplanner.data.db.entity.StoreReceiptEntity
import io.github.and19081.mealplanner.data.db.entity.ReceiptLineItemEntity
import io.github.and19081.mealplanner.data.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.uuid.Uuid

class RoomMealPlanRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope
) : MealPlanRepository {

    private val scheduledMealDao = db.scheduledMealDao()

    override val entries: StateFlow<List<ScheduledMeal>> = scheduledMealDao.observeInRangeWithSource("1900-01-01", "2100-12-31")
        .map { list -> list.map { it.toModel() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override suspend fun addPlan(entry: ScheduledMeal) {
        scheduledMealDao.upsert(ScheduledMealEntity(
            id = entry.id,
            date = entry.date.toString(),
            time = entry.time.toString(),
            mealType = entry.mealType,
            peopleCount = entry.peopleCount,
            isConsumed = entry.isConsumed,
            prePlannedMealId = entry.prePlannedMealId,
            restaurantId = entry.restaurantId,
            anticipatedCostCents = entry.anticipatedCostCents
        ))
    }

    override suspend fun removePlan(entryId: Uuid) {
        val entity = scheduledMealDao.getWithSource(entryId.toString())?.scheduledMeal
        if (entity != null) scheduledMealDao.delete(entity)
    }

    override suspend fun markConsumed(entryId: Uuid) {
        scheduledMealDao.setConsumed(entryId.toString(), true)
    }

    override suspend fun addReceipt(
        mealId: Uuid,
        actualTotalCents: Int,
        taxCents: Int,
        lineItems: List<Triple<String, Double, Int>>
    ) {
        val entry = scheduledMealDao.getWithSource(mealId.toString())?.scheduledMeal ?: return
        
        val receiptId = Uuid.random()
        val receipt = StoreReceiptEntity(
            id = receiptId,
            name = "Restaurant Meal",
            date = entry.date,
            time = entry.time,
            restaurantId = entry.restaurantId,
            scheduledMealId = mealId,
            actualTotalCents = actualTotalCents,
            taxPaidCents = taxCents
        )

        db.receiptDao().upsertReceipt(receipt)

        if (lineItems.isNotEmpty()) {
            val entities = lineItems.map { (name: String, qty: Double, price: Int) ->
                ReceiptLineItemEntity(
                    receiptId = receiptId,
                    customName = name,
                    quantityBought = qty,
                    pricePaidCents = price
                )
            }
            db.receiptDao().upsertLineItems(entities)
        }
        
        // Also mark as consumed if it wasn't
        markConsumed(mealId)
    }

    override suspend fun clearAll() {
        scheduledMealDao.clearAll()
    }
}
