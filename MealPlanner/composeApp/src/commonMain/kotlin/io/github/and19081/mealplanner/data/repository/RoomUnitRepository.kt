package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.core.util.SystemUnits
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.core.util.toEntity
import io.github.and19081.mealplanner.core.util.toModel
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class RoomUnitRepository(private val db: MealPlannerDatabase, private val scope: CoroutineScope) :
    UnitRepository {

  private val unitDao = db.unitDao()

  override val units: StateFlow<List<UnitModel>> =
      unitDao
          .observeAll()
          .map { list ->
            val dbUnits = list.map { it.toModel() }
            val systemIds = SystemUnits.all.map { it.id }.toSet()
            SystemUnits.all + dbUnits.filter { it.id !in systemIds }
          }
          .stateIn(scope, SharingStarted.WhileSubscribed(5000), SystemUnits.all)

  override suspend fun addUnit(unit: UnitModel) {
    unitDao.upsert(unit.toEntity())
  }

  override suspend fun updateUnit(unit: UnitModel) {
    unitDao.upsert(unit.toEntity())
  }

  override suspend fun deleteUnit(unitId: Uuid) {
    val existing = unitDao.getById(unitId)
    if (existing != null && !existing.isSystemUnit) {
      unitDao.delete(existing)
    }
  }

  override suspend fun setUnits(newUnits: List<UnitModel>) {
    newUnits.forEach { addUnit(it) }
  }

  override fun getUnit(id: Uuid): UnitModel? {
    return units.value.find { it.id == id }
  }
}
