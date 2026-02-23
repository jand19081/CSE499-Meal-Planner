package io.github.and19081.mealplanner.domain

import io.github.and19081.mealplanner.SystemUnits
import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.UnitType
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import kotlin.uuid.Uuid

object UnitConverter {
    fun convert(
        amount: Double, 
        fromUnitId: Uuid, 
        toUnitId: Uuid, 
        allUnits: List<UnitModel>,
        bridges: List<BridgeConversion> = emptyList()
    ): Double {
        if (fromUnitId == toUnitId) return amount
        
        val fromUnit = allUnits.find { it.id == fromUnitId } ?: return 0.0
        val toUnit = allUnits.find { it.id == toUnitId } ?: return 0.0
        
        // Convert FROM to Base
        val amountInBase = toBase(amount, fromUnit)
        
        // If types differ, look for bridge
        val fromType = fromUnit.type
        val toType = toUnit.type
        
        var convertedBase = amountInBase
        
        if (fromType != toType) {
            val validBridge = bridges.find { bridge ->
                val bridgeFrom = allUnits.find { it.id == bridge.fromUnitId }
                val bridgeTo = allUnits.find { it.id == bridge.toUnitId }
                
                if (bridgeFrom == null || bridgeTo == null) return@find false
                
                (bridgeFrom.type == fromType && bridgeTo.type == toType) || 
                (bridgeFrom.type == toType && bridgeTo.type == fromType)
            }
            
            if (validBridge != null) {
                val bFrom = allUnits.find { it.id == validBridge.fromUnitId }!!
                val bTo = allUnits.find { it.id == validBridge.toUnitId }!!
                
                val sideABase = toBase(validBridge.fromQuantity, bFrom)
                val sideBBase = toBase(validBridge.toQuantity, bTo)
                
                if (bFrom.type == fromType) {
                    // Bridge: FromType -> ToType
                    if (sideABase > 0) {
                        convertedBase *= (sideBBase / sideABase)
                    }
                } else {
                    // Bridge: ToType -> FromType
                    if (sideBBase > 0) {
                        convertedBase *= (sideABase / sideBBase)
                    }
                }
            } else {
                return 0.0
            }
        }
        
        // 3. Convert Base to TO
        return fromBase(convertedBase, toUnit)
    }
    
    // Convert arbitrary unit to Standard Base Unit (Gram, ML, Each)
    // Returns (Amount, UnitId of Base)
    fun toStandard(amount: Double, unit: UnitModel, allUnits: List<UnitModel>): Pair<Double, UnitModel?> {
        val baseAmount = toBase(amount, unit)
        // Find base unit for this type using predefined SystemUnits
        val baseUnitId = when(unit.type) {
            UnitType.Weight -> SystemUnits.Gram.id
            UnitType.Volume -> SystemUnits.Ml.id
            UnitType.Count -> SystemUnits.Each.id
            else -> return baseAmount to unit
        }
        val baseUnit = allUnits.find { it.id == baseUnitId }
        return baseAmount to baseUnit
    }

    private fun toBase(amount: Double, unit: UnitModel): Double {
        if (!unit.isSystemUnit) return amount // Custom units are their own base unless bridged? Assuming 1:1 if not system for now.
        return amount * unit.factorToBase
    }

    private fun fromBase(amount: Double, unit: UnitModel): Double {
        if (!unit.isSystemUnit) return amount
        return amount / unit.factorToBase
    }
}
