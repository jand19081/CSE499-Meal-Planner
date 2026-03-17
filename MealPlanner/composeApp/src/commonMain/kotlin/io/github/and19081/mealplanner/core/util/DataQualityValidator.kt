package io.github.and19081.mealplanner.core.util

import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.Package
import kotlin.uuid.Uuid

sealed class DataWarning(val message: String) {
  class MissingPackage(val itemName: String) :
      DataWarning("Missing purchase options for '$itemName'")

  class MissingBridge(val itemName: String, val fromUnit: String, val toUnit: String) :
      DataWarning("Missing conversion for '$itemName' ($fromUnit to $toUnit)")

  class MissingUnit(val itemName: String) : DataWarning("Missing unit for '$itemName'")

  class CircularDependency(val itemName: String) :
      DataWarning("Circular dependency detected in recipe: $itemName")
}

object DataQualityValidator {

  fun validateFoodItem(
      item: FoodItem,
      allItemsMap: Map<Uuid, FoodItem>,
      allPackages: List<Package>,
      allBridges: List<BridgeConversion>,
      allUnits: List<UnitModel>,
      currentPath: Set<Uuid> = emptySet()
  ): List<DataWarning> {
    val warnings = mutableListOf<DataWarning>()

    if (currentPath.contains(item.id)) {
      warnings.add(DataWarning.CircularDependency(item.name))
      return warnings
    }

    val newPath = currentPath + item.id
    val recipeInfo = item.recipeInfo ?: return emptyList()

    for (req in recipeInfo.requirements) {
      val subItem = allItemsMap[req.foodItemId] ?: continue
      
      if (subItem.isRecipe) {
        warnings.addAll(
            validateFoodItem(
                item = subItem,
                allItemsMap = allItemsMap,
                allPackages = allPackages,
                allBridges = allBridges,
                allUnits = allUnits,
                currentPath = newPath
            )
        )
        continue
      }

      // It's an ingredient (purchasable)
      val packages = allPackages.filter { it.foodItemId == subItem.id }
      val bridges = allBridges.filter { it.foodItemId == subItem.id }

      if (packages.isEmpty()) {
        warnings.add(DataWarning.MissingPackage(subItem.name))
        continue
      }

      val reqUnit = allUnits.find { it.id == req.unitId }
      if (req.unitId == null || reqUnit == null) {
        warnings.add(DataWarning.MissingUnit(subItem.name))
        continue
      }

      val canConvert =
          packages.any { pkg ->
            hasConversionPath(pkg.unitId, req.unitId!!, bridges, allUnits)
          }

      if (!canConvert) {
        val reqUnitName = reqUnit.abbreviation
        val pkgUnitName =
            allUnits.find { it.id == packages.first().unitId }?.abbreviation ?: "Unknown"
        warnings.add(DataWarning.MissingBridge(subItem.name, reqUnitName, pkgUnitName))
      }
    }

    return warnings.distinctBy { it.message }
  }

  fun hasConversionPath(
      startUnitId: Uuid,
      targetUnitId: Uuid,
      bridges: List<BridgeConversion>,
      units: List<UnitModel>
  ): Boolean {
    if (startUnitId == targetUnitId) return true

    val startUnit = units.find { it.id == startUnitId } ?: return false
    val targetUnit = units.find { it.id == targetUnitId } ?: return false

    // Base factor math works if they share a type (e.g. both Volume)
    if (startUnit.type == targetUnit.type) return true

    val queue = mutableListOf(startUnitId)
    val visited = mutableSetOf(startUnitId)

    while (queue.isNotEmpty()) {
      val currentId = queue.removeFirst()
      if (currentId == targetUnitId) return true

      // Find all bridges connected to the current unit
      val connectedBridges = bridges.filter {
        it.fromUnitId == currentId || it.toUnitId == currentId
      }

      for (bridge in connectedBridges) {
        val nextUnitId = if (bridge.fromUnitId == currentId) bridge.toUnitId else bridge.fromUnitId
        
        // If the next unit's type matches target unit's type, we found a path
        val nextUnit = units.find { it.id == nextUnitId }
        if (nextUnit?.type == targetUnit.type) return true

        if (visited.add(nextUnitId)) {
          queue.add(nextUnitId)
        }
      }
    }
    return false
  }
}
