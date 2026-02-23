package io.github.and19081.mealplanner

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
enum class UnitType {
    Weight, Volume, Count, Custom
}

@Serializable
data class UnitModel(
    val id: Uuid,
    val type: UnitType,
    val abbreviation: String,
    val displayName: String,
    val isSystemUnit: Boolean,
    val factorToBase: Double = 1.0
)

object SystemUnits {
    // Weight Base: Gram (g)
    val Gram = UnitModel(Uuid.parse("00000000-0000-0000-0001-000000000001"), UnitType.Weight, "g", "Grams", true, 1.0)
    val Kg = UnitModel(Uuid.parse("00000000-0000-0000-0001-000000000002"), UnitType.Weight, "kg", "Kilograms", true, 1000.0)
    val Oz = UnitModel(Uuid.parse("00000000-0000-0000-0001-000000000003"), UnitType.Weight, "oz", "Ounces", true, 28.3495)
    val Lb = UnitModel(Uuid.parse("00000000-0000-0000-0001-000000000004"), UnitType.Weight, "lb", "Pounds", true, 453.592)

    // Volume Base: Milliliter (ml)
    val Ml = UnitModel(Uuid.parse("00000000-0000-0000-0002-000000000001"), UnitType.Volume, "ml", "Milliliters", true, 1.0)
    val Liter = UnitModel(Uuid.parse("00000000-0000-0000-0002-000000000002"), UnitType.Volume, "l", "Liters", true, 1000.0)
    val Tsp = UnitModel(Uuid.parse("00000000-0000-0000-0002-000000000003"), UnitType.Volume, "tsp", "Teaspoons", true, 4.92892)
    val Tbsp = UnitModel(Uuid.parse("00000000-0000-0000-0002-000000000004"), UnitType.Volume, "tbsp", "Tablespoons", true, 14.7868)
    val FlOz = UnitModel(Uuid.parse("00000000-0000-0000-0002-000000000005"), UnitType.Volume, "fl oz", "Fluid Ounces", true, 29.5735)
    val Cup = UnitModel(Uuid.parse("00000000-0000-0000-0002-000000000006"), UnitType.Volume, "cup", "Cups", true, 236.588)
    val Pint = UnitModel(Uuid.parse("00000000-0000-0000-0002-000000000007"), UnitType.Volume, "pt", "Pints", true, 473.176)
    val Quart = UnitModel(Uuid.parse("00000000-0000-0000-0002-000000000008"), UnitType.Volume, "qt", "Quarts", true, 946.353)
    val Gallon = UnitModel(Uuid.parse("00000000-0000-0000-0002-000000000009"), UnitType.Volume, "gal", "Gallons", true, 3785.41)

    // Count Base: Each
    val Each = UnitModel(Uuid.parse("00000000-0000-0000-0003-000000000001"), UnitType.Count, "each", "Each", true, 1.0)
    val Dozen = UnitModel(Uuid.parse("00000000-0000-0000-0003-000000000002"), UnitType.Count, "dozen", "Dozen", true, 12.0)

    val all = listOf(
        Gram, Kg, Oz, Lb,
        Ml, Liter, Tsp, Tbsp, FlOz, Cup, Pint, Quart, Gallon,
        Each, Dozen
    )
}

// Legacy compatibility helper or Value Object for usage in UI
data class Measure(
    val amount: Double,
    val unitId: Uuid,
    // Optional: cache the unit object for display if needed, or look it up
)

interface UnitRepository {
    val units: StateFlow<List<UnitModel>>
    suspend fun addUnit(unit: UnitModel)
    suspend fun updateUnit(unit: UnitModel)
    suspend fun deleteUnit(unitId: Uuid)
    suspend fun setUnits(newUnits: List<UnitModel>)
    fun getUnit(id: Uuid): UnitModel?
}

class InMemoryUnitRepository : UnitRepository {
    private val _units = MutableStateFlow<List<UnitModel>>(SystemUnits.all)
    override val units = _units.asStateFlow()

    override suspend fun addUnit(unit: UnitModel) {
        if (_units.value.any { it.id == unit.id }) return
        _units.update { it + unit }
    }

    override suspend fun updateUnit(unit: UnitModel) {
        _units.update { list -> 
            list.map { if (it.id == unit.id) unit else it }
        }
    }

    override suspend fun deleteUnit(unitId: Uuid) {
        _units.update { list ->
            list.filter { it.id != unitId || it.isSystemUnit }
        }
    }
    
    override suspend fun setUnits(newUnits: List<UnitModel>) {
        // Always ensure system units are present
        val systemIds = SystemUnits.all.map { it.id }.toSet()
        val customUnits = newUnits.filter { it.id !in systemIds }
        _units.value = SystemUnits.all + customUnits
    }
    
    override fun getUnit(id: Uuid): UnitModel? {
        return _units.value.find { it.id == id }
    }
}