package io.github.and19081.mealplanner.data.export

import androidx.room.useWriterConnection
import io.github.and19081.mealplanner.data.db.MealPlannerDatabase
import io.github.and19081.mealplanner.data.export.dto.BackupPayloadV1
import io.github.and19081.mealplanner.data.export.mapper.*
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class DataTransferService(private val database: MealPlannerDatabase) {

  private val json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
  }

  suspend fun exportData(): String {
    val payload =
        BackupPayloadV1(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            units = database.unitDao().getAll().map { it.toDtoV1() },
            categories = database.categoryDao().getAll().map { it.toDtoV1() },
            stores = database.storeDao().getAll().map { it.toDtoV1() },
            restaurants = database.restaurantDao().getAll().map { it.toDtoV1() },
            foodItems = database.foodItemDao().getAll().map { it.toDtoV1() },
            purchasableComponents = database.foodItemDao().getAllPurchasable().map { it.toDtoV1() },
            recipeComponents = database.foodItemDao().getAllRecipeComponents().map { it.toDtoV1() },
            leftoverComponents =
                database.foodItemDao().getAllLeftoverComponents().map { it.toDtoV1() },
            recipeInstructions = database.foodItemDao().getAllInstructions().map { it.toDtoV1() },
            scheduledMeals = database.scheduledMealDao().getAll().map { it.toDtoV1() },
            appSettings = database.appSettingsDao().get()?.toDtoV1(),
            storeReceipts = database.receiptDao().getAllReceipts().map { it.toDtoV1() },
            unitConversionBridges = database.foodItemDao().getAllConversions().map { it.toDtoV1() },
            packageOptions = database.packageOptionDao().getAll().map { it.toDtoV1() },
            recipeRequirementGroups =
                database.foodItemDao().getAllRequirementGroups().map { it.toDtoV1() },
            recipeRequirements = database.foodItemDao().getAllRequirements().map { it.toDtoV1() },
            pantryInventory = database.pantryDao().getAll().map { it.toDtoV1() },
            shoppingCartItems = database.shoppingListDao().getAll().map { it.toDtoV1() },
            receiptLineItems = database.receiptDao().getAllLineItems().map { it.toDtoV1() },
        )
    return json.encodeToString(payload)
  }

  suspend fun importData(jsonString: String): Result<Unit> {
    if (jsonString.isBlank() || jsonString == "{}") {
      return runCatching { database.useWriterConnection { clearAllTables(database) } }
    }

    return runCatching {
      val genericObject = json.decodeFromString<JsonObject>(jsonString)
      // Default to version 1 if missing for backward compatibility with first-gen exports
      val version = genericObject["version"]?.jsonPrimitive?.intOrNull ?: 1

      if (version == 1) {
        val backup = json.decodeFromString<BackupPayloadV1>(jsonString)
        database.useWriterConnection {
          clearAllTables(database)

          // Insert data
          database.unitDao().upsertAll(backup.units.map { it.toEntity() })
          database.categoryDao().upsertAll(backup.categories.map { it.toEntity() })
          database.storeDao().upsertAll(backup.stores.map { it.toEntity() })
          database.restaurantDao().upsertAll(backup.restaurants.map { it.toEntity() })
          database.foodItemDao().upsertAll(backup.foodItems.map { it.toEntity() })
          database
              .foodItemDao()
              .upsertAllPurchasable(backup.purchasableComponents.map { it.toEntity() })
          database
              .foodItemDao()
              .upsertAllRecipeComponents(backup.recipeComponents.map { it.toEntity() })
          database
              .foodItemDao()
              .upsertAllLeftoverComponents(backup.leftoverComponents.map { it.toEntity() })
          database
              .foodItemDao()
              .upsertAllInstructions(backup.recipeInstructions.map { it.toEntity() })
          database.scheduledMealDao().upsertAll(backup.scheduledMeals.map { it.toEntity() })

          backup.appSettings?.let {
            // Force isFirstLaunch to false so mock data doesn't run again
            database.appSettingsDao().upsert(it.toEntity().copy(isFirstLaunch = false))
          }

          database.receiptDao().upsertAllReceipts(backup.storeReceipts.map { it.toEntity() })
          database.receiptDao().upsertAllLineItems(backup.receiptLineItems.map { it.toEntity() })
          database
              .foodItemDao()
              .upsertAllConversions(backup.unitConversionBridges.map { it.toEntity() })
          database.packageOptionDao().upsertAll(backup.packageOptions.map { it.toEntity() })
          database
              .foodItemDao()
              .upsertAllRequirementGroups(backup.recipeRequirementGroups.map { it.toEntity() })
          database
              .foodItemDao()
              .upsertAllRequirements(backup.recipeRequirements.map { it.toEntity() })
          database.pantryDao().upsertAll(backup.pantryInventory.map { it.toEntity() })
          database.shoppingListDao().upsertAll(backup.shoppingCartItems.map { it.toEntity() })
        }
      } else {
        throw IllegalArgumentException("Unsupported backup version: $version")
      }
    }
  }

  private suspend fun clearAllTables(database: MealPlannerDatabase) {
    // 1. Delete leaf nodes / tables with many dependencies first
    database.receiptDao().clearAllLineItems()
    database.shoppingListDao().clearAll()
    database.packageOptionDao().clearAll()
    database.receiptDao().clearAllReceipts()
    database.scheduledMealDao().clearAll()
    database.pantryDao().clearAll()
    database.foodItemDao().clearAllConversions()
    database.foodItemDao().clearAllRequirements()
    database.foodItemDao().clearAllRequirementGroups()
    database.foodItemDao().clearAllInstructions()
    database.foodItemDao().clearAllLeftoverComponents()
    database.foodItemDao().clearAllRecipeComponents()
    database.foodItemDao().clearAllPurchasable()

    // 2. Delete main entities
    database.foodItemDao().clearAll()
    database.unitDao().clearAll()
    database.categoryDao().clearAll()
    database.storeDao().clearAll()
    database.restaurantDao().clearAll()

    // 3. Reset settings but preserve isFirstLaunch = false
    // to honor the requirement that it never re-initializes mock data
    val currentSettings = database.appSettingsDao().get()
    if (currentSettings != null) {
      database.appSettingsDao().upsert(currentSettings.copy(isFirstLaunch = false))
    }
  }
}
