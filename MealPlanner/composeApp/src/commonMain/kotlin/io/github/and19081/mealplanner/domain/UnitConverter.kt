package io.github.and19081.mealplanner.domain

import io.github.and19081.mealplanner.SystemUnits
import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.UnitType
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import kotlin.uuid.Uuid

object UnitConverter {
  fun convert(
      amount: Double,
      fromUnitId: Uuid?,
      toUnitId: Uuid?,
      allUnits: Map<Uuid, UnitModel>,
      bridges: List<BridgeConversion> = emptyList(),
  ): Double? {
    if (fromUnitId == null || toUnitId == null) return null
    if (fromUnitId == toUnitId) return amount

    val fromUnit = allUnits[fromUnitId] ?: return null
    val toUnit = allUnits[toUnitId] ?: return null

    // Convert FROM to Base
    val amountInBase = toBase(amount, fromUnit)

    // If types differ, look for bridge
    val fromType = fromUnit.type
    val toType = toUnit.type

    var convertedBase = amountInBase

    if (fromType != toType) {
      val validBridge =
          bridges.find { bridge ->
            val bridgeFrom = allUnits[bridge.fromUnitId]
            val bridgeTo = allUnits[bridge.toUnitId]

            if (bridgeFrom == null || bridgeTo == null) return@find false

            (bridgeFrom.type == fromType && bridgeTo.type == toType) ||
                (bridgeFrom.type == toType && bridgeTo.type == fromType)
          } ?: return null

      val bFrom = allUnits[validBridge.fromUnitId]!!
      val bTo = allUnits[validBridge.toUnitId]!!

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
    }

    // 3. Convert Base to TO
    return fromBase(convertedBase, toUnit)
  }

  // Convert arbitrary unit to Standard Base Unit (Gram, ML, Each)
  // Returns (Amount, UnitId of Base)
  fun toStandard(
      amount: Double,
      unit: UnitModel,
      allUnits: Map<Uuid, UnitModel>,
  ): Pair<Double, UnitModel?> {
    val baseAmount = toBase(amount, unit)
    // Find base unit for this type using predefined SystemUnits
    val baseUnitId =
        when (unit.type) {
          UnitType.Weight -> SystemUnits.Gram.id
          UnitType.Volume -> SystemUnits.Ml.id
          UnitType.Count -> SystemUnits.Each.id
          else -> return baseAmount to unit
        }
    val baseUnit = allUnits[baseUnitId]
    return baseAmount to baseUnit
  }

  private fun toBase(amount: Double, unit: UnitModel): Double {
    if (!unit.isSystemUnit)
        return amount // Custom units are their own base unless bridged? Assuming 1:1 if not system
                      // for now.
    return amount * unit.factorToBase
  }

  private fun fromBase(amount: Double, unit: UnitModel): Double {
    if (!unit.isSystemUnit) return amount
    return amount / unit.factorToBase
  }

  /**
   * Feature 6: Finds the most readable unit for a given amount in base units. e.g. if amount is
   * 15ml, might return (1.0, "Tablespoon")
   */
  fun getBestUnit(
      baseAmount: Double,
      unitType: UnitType,
      allUnits: Map<Uuid, UnitModel>,
  ): Pair<Double, UnitModel> {
    val candidates =
        allUnits.values
            .filter { it.type == unitType && it.isSystemUnit }
            .sortedByDescending { it.factorToBase } // Start with largest units

    for (unit in candidates) {
      val converted = baseAmount / unit.factorToBase
      if (converted >= 0.99) { // If it fits at least 1 of this unit (with some float tolerance)
        return converted to unit
      }
    }

    // Fallback to smallest unit if none found or amount is very small
    val smallest =
        candidates.lastOrNull()
            ?: return baseAmount to
                UnitModel(
                    id = kotlin.uuid.Uuid.random(),
                    type = unitType,
                    abbreviation = "?",
                    displayName = "Unknown",
                    isSystemUnit = false,
                    factorToBase = 1.0,
                )
    return (baseAmount / smallest.factorToBase) to smallest
  }

  /** Feature 6: Scales a quantity and finds the best unit for the result. */
  fun scaleAndNormalize(
      amount: Double,
      fromUnitId: kotlin.uuid.Uuid?,
      scale: Double,
      allUnits: Map<Uuid, UnitModel>,
  ): Pair<Double, UnitModel> {
    val fromUnit =
        allUnits[fromUnitId]
            ?: return (amount * scale) to
                UnitModel(
                    id = kotlin.uuid.Uuid.random(),
                    type = UnitType.Custom,
                    abbreviation = "?",
                    displayName = "Unknown",
                    isSystemUnit = false,
                    factorToBase = 1.0,
                )
    val baseAmount = toBase(amount, fromUnit) * scale
    return getBestUnit(baseAmount, fromUnit.type, allUnits)
  }
}