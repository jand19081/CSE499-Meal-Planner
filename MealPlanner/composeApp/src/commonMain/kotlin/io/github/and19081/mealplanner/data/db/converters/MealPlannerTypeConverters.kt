package io.github.and19081.mealplanner.data.db.converters

import androidx.room.TypeConverter
import io.github.and19081.mealplanner.RecipeMealType
import io.github.and19081.mealplanner.UnitType
import io.github.and19081.mealplanner.settings.AppTheme
import io.github.and19081.mealplanner.settings.Mode
import kotlin.uuid.Uuid

/**
 * Room TypeConverters for domain objects and enumerations.
 */
class MealPlannerTypeConverters {

    // ── UUID ─────────────────────────────────────────────────────────────────

    @TypeConverter
    fun uuidToString(uuid: Uuid): String = uuid.toString()

    @TypeConverter
    fun stringToUuid(string: String): Uuid = Uuid.parse(string)

    // ── UnitType ─────────────────────────────────────────────────────────────

    @TypeConverter
    fun unitTypeToString(value: UnitType): String = value.name

    @TypeConverter
    fun stringToUnitType(value: String): UnitType = UnitType.valueOf(value)

    // ── RecipeMealType ───────────────────────────────────────────────────────

    @TypeConverter
    fun recipeMealTypeToString(value: RecipeMealType?): String? = value?.name

    @TypeConverter
    fun stringToRecipeMealType(value: String?): RecipeMealType? =
        value?.let { RecipeMealType.valueOf(it) }

    // ── View Mode (mapped to Mode) ───────────────────────────────────────────

    @TypeConverter
    fun modeToString(value: Mode): String = value.name

    @TypeConverter
    fun stringToMode(value: String): Mode = Mode.valueOf(value)

    // ── Theme Mode (mapped to AppTheme) ──────────────────────────────────────

    @TypeConverter
    fun appThemeToString(value: AppTheme): String = value.name

    @TypeConverter
    fun stringToAppTheme(value: String): AppTheme = AppTheme.valueOf(value)
}
