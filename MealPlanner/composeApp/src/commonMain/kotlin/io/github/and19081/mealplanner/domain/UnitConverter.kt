package io.github.and19081.mealplanner.domain

import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.UnitType
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import kotlin.uuid.Uuid

object UnitConverter {
    // Hardcoded factors for system units (Standard Base: Gram for Weight, ML for Volume)
    // Map abbreviation -> Factor to Base
    private val weightFactors = mapOf(
        "g" to 1.0,
        "kg" to 1000.0,
        "oz" to 28.3495,
        "lb" to 453.592
    )
    
    private val volumeFactors = mapOf(
        "ml" to 1.0,
        "l" to 1000.0,
        "tsp" to 4.92892,
        "tbsp" to 14.7868,
        "fl oz" to 29.5735,
        "cup" to 236.588,
        "pt" to 473.176,
        "qt" to 946.353,
        "gal" to 3785.41
    )
    
    private val countFactors = mapOf(
        "each" to 1.0,
        "dozen" to 12.0
    )

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
        
        // 1. Convert FROM to Base
        val amountInBase = toBase(amount, fromUnit)
        
        // 2. If types differ, look for bridge
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
                        convertedBase = convertedBase * (sideBBase / sideABase)
                    }
                } else {
                    // Bridge: ToType -> FromType
                    if (sideBBase > 0) {
                        convertedBase = convertedBase * (sideABase / sideBBase)
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
        // Find base unit for this type
        val baseAbbr = when(unit.type) {
            UnitType.Weight -> "g"
            UnitType.Volume -> "ml"
            UnitType.Count -> "each"
            else -> return baseAmount to unit
        }
        val baseUnit = allUnits.find { it.abbreviation == baseAbbr && it.type == unit.type }
        return baseAmount to baseUnit
    }

    private fun toBase(amount: Double, unit: UnitModel): Double {
        if (!unit.isSystemUnit) return amount // Custom units are their own base unless bridged? Assuming 1:1 if not system for now.
        
        return when (unit.type) {
            UnitType.Weight -> amount * (weightFactors[unit.abbreviation.lowercase()] ?: 1.0)
            UnitType.Volume -> amount * (volumeFactors[unit.abbreviation.lowercase()] ?: 1.0)
            UnitType.Count -> amount * (countFactors[unit.abbreviation.lowercase()] ?: 1.0)
            UnitType.Custom -> amount
        }
    }

    private fun fromBase(amount: Double, unit: UnitModel): Double {
        if (!unit.isSystemUnit) return amount
        
        return when (unit.type) {
            UnitType.Weight -> amount / (weightFactors[unit.abbreviation.lowercase()] ?: 1.0)
            UnitType.Volume -> amount / (volumeFactors[unit.abbreviation.lowercase()] ?: 1.0)
            UnitType.Count -> amount / (countFactors[unit.abbreviation.lowercase()] ?: 1.0)
            UnitType.Custom -> amount
        }
    }
}
