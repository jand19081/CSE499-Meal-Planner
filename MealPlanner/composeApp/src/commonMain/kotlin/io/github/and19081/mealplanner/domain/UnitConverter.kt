package io.github.and19081.mealplanner.domain

import io.github.and19081.mealplanner.SystemUnits
import io.github.and19081.mealplanner.UnitModel
import io.github.and19081.mealplanner.UnitType
import kotlin.uuid.Uuid

object UnitConverter {

  fun toStandard(
      amount: Double,
      fromUnit: UnitModel,
      allUnits: Map<Uuid, UnitModel>
  ): Pair<Double, UnitModel?> {
    val baseUnit =
        when (fromUnit.type) {
          UnitType.Mass -> SystemUnits.Gram
          UnitType.Volume -> SystemUnits.Ml
          UnitType.Count -> SystemUnits.Each
          else -> fromUnit
        }
    return (amount * fromUnit.factorToBase) to baseUnit
  }

  fun convert(
      amount: Double,
      fromUnitId: Uuid,
      toUnitId: Uuid?,
      allUnits: Map<Uuid, UnitModel>,
      bridges: List<BridgeConversion> = emptyList(),
  ): Double? {
    if (toUnitId == null || fromUnitId == toUnitId) return amount

    val fromUnit = allUnits[fromUnitId] ?: return null
    val toUnit = allUnits[toUnitId] ?: return null

    if (fromUnit.type == toUnit.type) {
      val (baseAmount, _) = toStandard(amount, fromUnit, allUnits)
      return baseAmount / toUnit.factorToBase
    }

    val bridge =
        bridges.find { b ->
          val bFrom = allUnits[b.fromUnitId]
          val bTo = allUnits[b.toUnitId]
          if (bFrom == null || bTo == null) return@find false

          (bFrom.type == fromUnit.type && bTo.type == toUnit.type) ||
              (bFrom.type == toUnit.type && bTo.type == fromUnit.type)
        } ?: return null

    val bridgeFromUnit = allUnits[bridge.fromUnitId]!!
    val bridgeToUnit = allUnits[bridge.toUnitId]!!

    val amountInBridgeFrom =
        if (bridgeFromUnit.type == fromUnit.type) {
          convert(amount, fromUnitId, bridgeFromUnit.id, allUnits) ?: return null
        } else {
          convert(amount, fromUnitId, bridgeToUnit.id, allUnits) ?: return null
        }

    val targetBridgeUnit = if (bridgeFromUnit.type == fromUnit.type) bridgeToUnit else bridgeFromUnit
    val sourceBridgeUnit = if (bridgeFromUnit.type == fromUnit.type) bridgeFromUnit else bridgeToUnit
    val sourceBridgeQty = if (bridgeFromUnit.type == fromUnit.type) bridge.fromQuantity else bridge.toQuantity
    val targetBridgeQty = if (bridgeFromUnit.type == fromUnit.type) bridge.toQuantity else bridge.fromQuantity

    val bridgedAmount = (amountInBridgeFrom / sourceBridgeQty) * targetBridgeQty

    return convert(bridgedAmount, targetBridgeUnit.id, toUnitId, allUnits)
  }
}
