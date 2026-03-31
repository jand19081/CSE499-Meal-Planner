package io.github.and19081.mealplanner.data.db.converters

import androidx.room.TypeConverter
import io.github.and19081.mealplanner.domain.model.ConsumptionResult
import io.github.and19081.mealplanner.domain.model.MealSource
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.core.theme.AppTheme
import io.github.and19081.mealplanner.core.util.RecipeMealType
import io.github.and19081.mealplanner.core.util.UnitType
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.feature.kitchen.KitchenTransaction
import io.github.and19081.mealplanner.feature.main.Routes
import io.github.and19081.mealplanner.feature.meals.MealPlan
import io.github.and19081.mealplanner.feature.recipes.RecipeExecutionState
import io.github.and19081.mealplanner.feature.settings.SettingsState
import io.github.and19081.mealplanner.feature.shoppinglist.ShoppingList
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.uuid.Uuid

/** Room TypeConverters for domain objects and enumerations. */
class MealPlannerTypeConverters {

    // ── UUID ─────────────────────────────────────────────────────────────────

    @TypeConverter
    fun uuidToString(uuid: Uuid): String = uuid.toString()

    @TypeConverter
    fun stringToUuid(string: String): Uuid = Uuid.Companion.parse(string)

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

    // ── MealSource (serialized JSON) ────────────────────────────────────────────────

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @TypeConverter
    fun mealSourceToJson(source: MealSource): String = json.encodeToString(MealSource.serializer(), source)

    @TypeConverter
    fun jsonToMealSource(jsonString: String): MealSource =
        jsonString.ifEmpty { "{}" }
            .let { json.decodeFromString(MealSource.serializer(), it) }

    // ── ConsumptionResult (serialized JSON) ──────────────────────────────────────────

    @TypeConverter
    fun consumptionResultToJson(result: ConsumptionResult): String =
        json.encodeToString(ConsumptionResult.serializer(), result)

    @TypeConverter
    fun jsonToConsumptionResult(jsonString: String): ConsumptionResult =
        jsonString.ifEmpty { "{}" }
            .let { json.decodeFromString(ConsumptionResult.serializer(), it) }
}
