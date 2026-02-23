package io.github.and19081.mealplanner.data.repository

import io.github.and19081.mealplanner.SystemUnits
import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.UnitRepository
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.toEntity
import io.github.and19081.mealplanner.data.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.uuid.Uuid

class RoomUnitRepository(
    private val db: MealPlannerDatabase,
    private val scope: CoroutineScope
) : UnitRepository {

    private val unitDao = db.unitDao()

    override val units: StateFlow<List<UnitModel>> = unitDao.observeAll()
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
        val existing = unitDao.getById(unitId.toString())
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
