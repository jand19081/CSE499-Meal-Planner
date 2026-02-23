package io.github.and19081.mealplanner.domain

import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import kotlin.uuid.Uuid

sealed class DataWarning(val message: String) {
    class MissingPackage(val ingredientName: String) : DataWarning("Missing purchase options for '$ingredientName'")
    class MissingBridge(val ingredientName: String, val fromUnit: String, val toUnit: String) : 
        DataWarning("Missing conversion for '$ingredientName' ($fromUnit to $toUnit)")
}

object DataQualityValidator {

    fun validateRecipe(
        recipe: Recipe,
        ingredientsMap: Map<Uuid, Ingredient>,
        allPackages: List<Package>,
        allBridges: List<BridgeConversion>,
        allUnits: List<UnitModel>,
        recipesMap: Map<Uuid, Recipe> = emptyMap()
    ): List<DataWarning> {
        val warnings = mutableListOf<DataWarning>()

        for (ri in recipe.ingredients) {
            if (ri.subRecipeId != null) {
                val subRecipe = recipesMap[ri.subRecipeId]
                if (subRecipe != null) {
                    warnings.addAll(validateRecipe(subRecipe, ingredientsMap, allPackages, allBridges, allUnits, recipesMap))
                }
                continue
            }

            val ingredient = ri.ingredientId?.let { ingredientsMap[it] } ?: continue
            val packages = allPackages.filter { it.ingredientId == ingredient.id }
            val bridges = allBridges.filter { it.ingredientId == ingredient.id }
            
            if (packages.isEmpty()) {
                warnings.add(DataWarning.MissingPackage(ingredient.name))
                continue
            }

            // Check if ANY package can be converted to the required unit
            val canConvert = packages.any { pkg ->
                if (pkg.unitId == ri.unitId) return@any true
                
                val pkgUnit = allUnits.find { it.id == pkg.unitId }
                val riUnit = allUnits.find { it.id == ri.unitId }
                
                if (pkgUnit == null || riUnit == null) return@any false
                
                // If same type, simple factor conversion works
                if (pkgUnit.type == riUnit.type) return@any true
                
                // If different types, need a bridge
                bridges.any { b ->
                    val bFrom = allUnits.find { it.id == b.fromUnitId }
                    val bTo = allUnits.find { it.id == b.toUnitId }
                    if (bFrom == null || bTo == null) return@any false
                    
                    (bFrom.type == pkgUnit.type && bTo.type == riUnit.type) ||
                    (bFrom.type == riUnit.type && bTo.type == pkgUnit.type)
                }
            }

            if (!canConvert) {
                val riUnitName = allUnits.find { it.id == ri.unitId }?.abbreviation ?: "Unknown"
                val pkgUnitName = allUnits.find { it.id == packages.first().unitId }?.abbreviation ?: "Unknown"
                warnings.add(DataWarning.MissingBridge(ingredient.name, riUnitName, pkgUnitName))
            }
        }

        return warnings.distinctBy { it.message }
    }

    fun validateMeal(
        meal: PrePlannedMeal,
        recipesMap: Map<Uuid, Recipe>,
        ingredientsMap: Map<Uuid, Ingredient>,
        allPackages: List<Package>,
        allBridges: List<BridgeConversion>,
        allUnits: List<UnitModel>
    ): List<DataWarning> {
        val warnings = mutableListOf<DataWarning>()

        // Validate Recipes
        for (recipeId in meal.recipes) {
            val recipe = recipesMap[recipeId] ?: continue
            warnings.addAll(validateRecipe(recipe, ingredientsMap, allPackages, allBridges, allUnits, recipesMap))
        }

        // Validate Independent Ingredients
        for (comp in meal.independentIngredients) {
            val ingredient = ingredientsMap[comp.ingredientId] ?: continue
            val packages = allPackages.filter { it.ingredientId == ingredient.id }
            val bridges = allBridges.filter { it.ingredientId == ingredient.id }

            if (packages.isEmpty()) {
                warnings.add(DataWarning.MissingPackage(ingredient.name))
                continue
            }

            val canConvert = packages.any { pkg ->
                if (pkg.unitId == comp.unitId) return@any true
                val pkgUnit = allUnits.find { it.id == pkg.unitId }
                val compUnit = allUnits.find { it.id == comp.unitId }
                if (pkgUnit == null || compUnit == null) return@any false
                if (pkgUnit.type == compUnit.type) return@any true
                bridges.any { b ->
                    val bFrom = allUnits.find { it.id == b.fromUnitId }
                    val bTo = allUnits.find { it.id == b.toUnitId }
                    if (bFrom == null || bTo == null) return@any false
                    (bFrom.type == pkgUnit.type && bTo.type == compUnit.type) ||
                    (bFrom.type == compUnit.type && bTo.type == pkgUnit.type)
                }
            }

            if (!canConvert) {
                val compUnitName = allUnits.find { it.id == comp.unitId }?.abbreviation ?: "Unknown"
                val pkgUnitName = allUnits.find { it.id == packages.first().unitId }?.abbreviation ?: "Unknown"
                warnings.add(DataWarning.MissingBridge(ingredient.name, compUnitName, pkgUnitName))
            }
        }

        return warnings.distinctBy { it.message }
    }
}
