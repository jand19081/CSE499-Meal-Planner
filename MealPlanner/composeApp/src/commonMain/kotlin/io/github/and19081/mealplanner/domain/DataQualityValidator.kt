package io.github.and19081.mealplanner.domain

import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.domain.BridgeConversion
import io.github.and19081.mealplanner.domain.Package
import kotlin.uuid.Uuid

sealed class DataWarning(val message: String) {
  class MissingPackage(val itemName: String) :
      DataWarning("Missing purchase options for '$itemName'")

  class MissingBridge(val itemName: String, val fromUnit: String, val toUnit: String) :
      DataWarning("Missing conversion for '$itemName' ($fromUnit to $toUnit)")

  class MissingUnit(val itemName: String) : DataWarning("Missing unit for '$itemName'")
}

object DataQualityValidator {

  fun validateFoodItem(
      item: FoodItem,
      allItemsMap: Map<Uuid, FoodItem>,
      allPackages: List<Package>,
      allBridges: List<BridgeConversion>,
      allUnits: List<UnitModel>,
  ): List<DataWarning> {
    val warnings = mutableListOf<DataWarning>()
    val recipeInfo = item.recipeInfo ?: return emptyList()

    for (req in recipeInfo.requirements) {
      val subItem = allItemsMap[req.foodItemId] ?: continue
      
      if (subItem.isRecipe) {
        warnings.addAll(
            validateFoodItem(
                subItem,
                allItemsMap,
                allPackages,
                allBridges,
                allUnits,
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
            if (pkg.unitId == req.unitId) return@any true

            val pkgUnit = allUnits.find { it.id == pkg.unitId }
            if (pkgUnit == null) return@any false

            // If same type, simple factor conversion works
            if (pkgUnit.type == reqUnit.type) return@any true

            // If different types, need a bridge
            bridges.any { b ->
              val bFrom = allUnits.find { it.id == b.fromUnitId }
              val bTo = allUnits.find { it.id == b.toUnitId }
              if (bFrom == null || bTo == null) return@any false

              (bFrom.type == pkgUnit.type && bTo.type == reqUnit.type) ||
                  (bFrom.type == reqUnit.type && bTo.type == pkgUnit.type)
            }
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
}
