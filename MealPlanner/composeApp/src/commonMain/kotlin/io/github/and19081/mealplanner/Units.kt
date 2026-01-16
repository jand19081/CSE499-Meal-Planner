package io.github.and19081.mealplanner

enum class MeasureType { MASS, VOLUME, COUNT }

enum class MeasureUnit(val type: MeasureType, val baseFactor: Double) {
    // Mass Base: Gram
    GRAM(MeasureType.MASS, 1.0),
    KG(MeasureType.MASS, 1000.0),
    OZ(MeasureType.MASS, 28.3495),
    LB(MeasureType.MASS, 453.592),

    // Volume Base: Milliliter
    ML(MeasureType.VOLUME, 1.0),
    LITER(MeasureType.VOLUME, 1000.0),
    TSP(MeasureType.VOLUME, 4.92892),
    TBSP(MeasureType.VOLUME, 14.7868),
    CUP(MeasureType.VOLUME, 236.588), // US Cup

    // Count Base: Each
    EACH(MeasureType.COUNT, 1.0)
}

data class Measure(
    val amount: Double,
    val unit: MeasureUnit
) {
    // Normalize to base unit
    val normalizedAmount: Double get() = amount * unit.baseFactor

    override fun toString(): String = "$amount ${unit.name}"
}