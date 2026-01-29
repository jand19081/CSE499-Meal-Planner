package io.github.and19081.mealplanner.domain

import io.github.and19081.mealplanner.MeasureUnit
import io.github.and19081.mealplanner.ingredients.UnitBridge

object UnitConverter {
    // Standard conversions to a base unit within type
    // Weight Base: Grams
    // Volume Base: Milliliters
    
    fun convert(amount: Double, from: MeasureUnit, to: MeasureUnit, bridges: List<UnitBridge> = emptyList()): Double {
        if (from == to) return amount
        
        // 1. Convert FROM to Base
        val amountInBase = toBase(amount, from)
        
        // 2. If types differ, look for bridge
        val fromType = getType(from)
        val toType = getType(to)
        
        var convertedBase = amountInBase
        
        if (fromType != toType) {
            // Find a bridge that connects these two types
            // A bridge connects SideA (Unit A) and SideB (Unit B)
            // We need a bridge where SideA type == fromType AND SideB type == toType
            // OR SideB type == fromType AND SideA type == toType
            
            val validBridge = bridges.find { bridge ->
                val typeA = getType(bridge.from.unit)
                val typeB = getType(bridge.to.unit)
                
                (typeA == fromType && typeB == toType) || (typeA == toType && typeB == fromType)
            }
            
            if (validBridge != null) {
                // Convert bridge sides to base
                val (sideABase, _) = toStandard(validBridge.from.amount, validBridge.from.unit)
                val (sideBBase, _) = toStandard(validBridge.to.amount, validBridge.to.unit)
                
                val sideAType = getType(validBridge.from.unit)
                // val sideBType = getType(validBridge.to.unit)
                
                if (sideAType == fromType) {
                    // Bridge is From -> To
                    // ratio = To / From
                    if (sideABase > 0) {
                        convertedBase = convertedBase * (sideBBase / sideABase)
                    }
                } else {
                    // Bridge is To -> From
                    // We have Amount in 'To' type (which is 'fromType' here)
                    // Wait. Bridge: A (Weight) = B (Volume)
                    // We have Weight.
                    // If A is Weight, we match A. We want B.
                    // Ratio = B / A.
                    // If B is Weight, we match B. We want A.
                    // Ratio = A / B.
                    
                    // Logic check:
                    // sideAType is ToType (target).
                    // So we matched Side B (FromType).
                    // We want Side A (ToType).
                    // Ratio = Side A / Side B.
                    if (sideBBase > 0) {
                        convertedBase = convertedBase * (sideABase / sideBBase)
                    }
                }
            } else {
                return 0.0 // No bridge found
            }
        }
        
        // 3. Convert Base to TO
        return fromBase(convertedBase, to)
    }
    
    // Simplification for standard conversions
    fun toStandard(amount: Double, from: MeasureUnit): Pair<Double, MeasureUnit> {
         // Convert to a standard unit like GRAM or ML, or EACH
         return when(getType(from)) {
             UnitType.WEIGHT -> toBase(amount, from) to MeasureUnit.GRAM
             UnitType.VOLUME -> toBase(amount, from) to MeasureUnit.ML
             UnitType.COUNT -> amount to MeasureUnit.EACH
             UnitType.UNKNOWN -> amount to from
         }
    }

    private fun getType(unit: MeasureUnit): UnitType {
        return when (unit) {
            MeasureUnit.GRAM, MeasureUnit.KG, MeasureUnit.OZ, MeasureUnit.LB -> UnitType.WEIGHT
            MeasureUnit.ML, MeasureUnit.LITER, MeasureUnit.TSP, MeasureUnit.TBSP, MeasureUnit.CUP, MeasureUnit.PINT, MeasureUnit.QUART, MeasureUnit.GALLON, MeasureUnit.FL_OZ -> UnitType.VOLUME
            MeasureUnit.EACH, MeasureUnit.DOZEN -> UnitType.COUNT
        }
    }

    private enum class UnitType { WEIGHT, VOLUME, COUNT, UNKNOWN }

    private fun toBase(amount: Double, unit: MeasureUnit): Double {
        return when (unit) {
            // Weight -> Gram
            MeasureUnit.GRAM -> amount
            MeasureUnit.KG -> amount * 1000.0
            MeasureUnit.OZ -> amount * 28.3495
            MeasureUnit.LB -> amount * 453.592
            
            // Volume -> Milliliters
            MeasureUnit.ML -> amount
            MeasureUnit.LITER -> amount * 1000.0
            MeasureUnit.TSP -> amount * 4.92892
            MeasureUnit.TBSP -> amount * 14.7868
            MeasureUnit.FL_OZ -> amount * 29.5735
            MeasureUnit.CUP -> amount * 236.588
            MeasureUnit.PINT -> amount * 473.176
            MeasureUnit.QUART -> amount * 946.353
            MeasureUnit.GALLON -> amount * 3785.41
            
            // Count -> Each
            MeasureUnit.EACH -> amount
            MeasureUnit.DOZEN -> amount * 12.0
            

        }
    }

    private fun fromBase(amount: Double, unit: MeasureUnit): Double {
        return when (unit) {
            // Gram -> Weight
            MeasureUnit.GRAM -> amount
            MeasureUnit.KG -> amount / 1000.0
            MeasureUnit.OZ -> amount / 28.3495
            MeasureUnit.LB -> amount / 453.592

            // Milliliters -> Volume
            MeasureUnit.ML -> amount
            MeasureUnit.LITER -> amount / 1000.0
            MeasureUnit.TSP -> amount / 4.92892
            MeasureUnit.TBSP -> amount / 14.7868
            MeasureUnit.FL_OZ -> amount / 29.5735
            MeasureUnit.CUP -> amount / 236.588
            MeasureUnit.PINT -> amount / 473.176
            MeasureUnit.QUART -> amount / 946.353
            MeasureUnit.GALLON -> amount / 3785.41
            
            // Each -> Count
            MeasureUnit.EACH -> amount
            MeasureUnit.DOZEN -> amount / 12.0
            

        }
    }
}
