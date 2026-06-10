package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.core.util.DateConstants
import io.github.and19081.mealplanner.core.util.DateConstants.LATEST_DATE
import io.github.and19081.mealplanner.core.util.DateConstants.EARLIEST_DATE
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.db.entity.ItemMeasurement
import io.github.and19081.mealplanner.data.db.entity.ReceiptLineItemEntity
import io.github.and19081.mealplanner.data.db.entity.ScheduledMealEntity
import io.github.and19081.mealplanner.data.db.entity.StoreReceiptEntity
import io.github.and19081.mealplanner.data.db.relation.ScheduledMealWithSource
import io.github.and19081.mealplanner.domain.model.MealSource
import io.github.and19081.mealplanner.domain.repository.MealPlanRepository
import io.github.and19081.mealplanner.feature.meals.ScheduledMeal
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json

class RoomMealPlanRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope,
) : MealPlanRepository {
    private val dao = db.scheduledMealDao()

    override val entries: StateFlow<List<ScheduledMeal>> =
        dao.observeInRangeWithSource(EARLIEST_DATE, LATEST_DATE)
            .map { list -> list.map { it.toDomain() } }
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    override suspend fun getMealById(id: Uuid): ScheduledMeal? {
        return dao.getWithSource(id)?.toDomain()
    }

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
        lineItems: List<Triple<String, Double, Int>>,
    ) {
        val receiptId = Uuid.random()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

        val receiptEntity =
            StoreReceiptEntity(
                id = receiptId,
                name = "Meal Receipt",
                date = today.toString(),
                time = now.toString(),
                scheduledMealId = entryId,
                actualTotalCents = totalCents,
                taxPaidCents = taxCents,
            )

        val lineItemEntities = lineItems.map { (name, qty, price) ->
            ReceiptLineItemEntity(
                receiptId = receiptId,
                customName = name,
                measurement = ItemMeasurement(foodItemId = null, unitId = null, quantity = qty),
                pricePaidCents = price,
            )
        }

        db.receiptDao().upsertReceiptWithDetails(receiptEntity, lineItemEntities)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun ScheduledMealWithSource.toDomain(): ScheduledMeal {
        val source =
            try {
                json.decodeFromString(MealSource.serializer(), scheduledMeal.mealSource)
            } catch (e: Exception) {
                null
            }

        val parsedDate =
            try {
                LocalDate.parse(scheduledMeal.date)
            } catch (e: Exception) {
                // Log the parse error and return null instead of silently substituting
                null
            }

        val parsedTime =
            try {
                LocalTime.parse(scheduledMeal.time)
            } catch (e: Exception) {
                null
            }

        return ScheduledMeal(
            id = scheduledMeal.id,
            date = parsedDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
            time = parsedTime ?: LocalTime(12, 0),
            mealType = scheduledMeal.mealType,
            source = source,
            peopleCount = scheduledMeal.peopleCount,
            isConsumed = scheduledMeal.isConsumed,
            anticipatedCostCents = scheduledMeal.anticipatedCostCents,
        )
    }

    private fun ScheduledMeal.toEntity(): ScheduledMealEntity {
        val src = source ?: MealSource.PrePlannedMeal(Uuid.random())
        val foodItemId =
            when (src) {
                is MealSource.PrePlannedMeal -> src.id
                is MealSource.StandaloneRecipe -> src.id
                is MealSource.StandaloneIngredient -> src.id
                else -> null
            }
        val restaurantId = (src as? MealSource.Restaurant)?.restaurantId

        return ScheduledMealEntity(
            id = id,
            foodItemId = foodItemId,
            restaurantId = restaurantId,
            date = date.toString(),
            time = time.toString(),
            mealType = mealType,
            mealSource = json.encodeToString(MealSource.serializer(), src),
            peopleCount = peopleCount,
            isConsumed = isConsumed,
            anticipatedCostCents = anticipatedCostCents,
        )
    }
}
