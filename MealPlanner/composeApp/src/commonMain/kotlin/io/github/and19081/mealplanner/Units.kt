package io.github.and19081.mealplanner

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

enum class UnitType {
    Weight, Volume, Count, Custom
}

data class UnitModel(
    val id: Uuid = Uuid.random(),
    val type: UnitType,
    val abbreviation: String,
    val displayName: String,
    val isSystemUnit: Boolean
)

// Legacy compatibility helper or Value Object for usage in UI
data class Measure(
    val amount: Double,
    val unitId: Uuid,
    // Optional: cache the unit object for display if needed, or look it up
)

object UnitRepository {
    private val _units = MutableStateFlow<List<UnitModel>>(emptyList())
    val units = _units.asStateFlow()

    fun addUnit(unit: UnitModel) {
        _units.update { it + unit }
    }
    
    fun setUnits(newUnits: List<UnitModel>) {
        _units.value = newUnits
    }
    
    fun getUnit(id: Uuid): UnitModel? {
        return _units.value.find { it.id == id }
    }
}