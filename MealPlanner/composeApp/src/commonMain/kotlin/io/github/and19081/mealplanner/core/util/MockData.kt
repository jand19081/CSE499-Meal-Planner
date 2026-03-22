package io.github.and19081.mealplanner.core.util

import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.Category
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.ItemMeasurement
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.model.FoodItemRequirement
import io.github.and19081.mealplanner.domain.model.FoodItemRequirementGroup
import io.github.and19081.mealplanner.domain.repository.MealPlanRepository
import io.github.and19081.mealplanner.domain.model.Package
import io.github.and19081.mealplanner.domain.model.PurchasableInfo
import io.github.and19081.mealplanner.domain.model.RecipeInfo
import io.github.and19081.mealplanner.feature.meals.Restaurant
import io.github.and19081.mealplanner.domain.repository.RestaurantRepository
import io.github.and19081.mealplanner.feature.meals.ScheduledMeal
import io.github.and19081.mealplanner.domain.model.Store
import io.github.and19081.mealplanner.domain.repository.StoreRepository
import io.github.and19081.mealplanner.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlin.uuid.Uuid
import kotlinx.datetime.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.time.Clock

object MockData {

  // Stable UUIDs to prevent duplication on re-initialization
  private val WALMART_ID = Uuid.parse("00000000-0000-0000-0000-000000000001")
  private val WINCO_ID = Uuid.parse("00000000-0000-0000-0000-000000000002")
  private val COSTCO_ID = Uuid.parse("00000000-0000-0000-0000-000000000003")

  private val MCDONALDS_ID = Uuid.parse("00000000-0000-0000-0001-000000000001")
  private val OLIVE_GARDEN_ID = Uuid.parse("00000000-0000-0000-0001-000000000002")
  private val CHIPOTLE_ID = Uuid.parse("00000000-0000-0000-0001-000000000003")

  private val CAT_BAKING = Uuid.parse("00000000-0000-0000-0002-000000000001")
  private val CAT_BREAD = Uuid.parse("00000000-0000-0000-0002-000000000002")
  private val CAT_CONDIMENT = Uuid.parse("00000000-0000-0000-0002-000000000003")
  private val CAT_DAIRY = Uuid.parse("00000000-0000-0000-0002-000000000004")
  private val CAT_MEAT = Uuid.parse("00000000-0000-0000-0002-000000000005")
  private val CAT_SPICE = Uuid.parse("00000000-0000-0000-0002-000000000006")
  private val CAT_STARCH = Uuid.parse("00000000-0000-0000-0002-000000000007")
  private val CAT_VEGETABLE = Uuid.parse("00000000-0000-0000-0002-000000000008")

  private val ING_EGGS = Uuid.parse("00000000-0000-0000-0003-000000000001")
  private val ING_MILK = Uuid.parse("00000000-0000-0000-0003-000000000002")
  private val ING_RICE_FLOUR = Uuid.parse("00000000-0000-0000-0003-000000000003")
  private val ING_TAPIOCA = Uuid.parse("00000000-0000-0000-0003-000000000004")
  private val ING_BUTTER = Uuid.parse("00000000-0000-0000-0003-000000000005")
  private val ING_SAUSAGE = Uuid.parse("00000000-0000-0000-0003-000000000006")
  private val ING_CHICKEN = Uuid.parse("00000000-0000-0000-0003-000000000007")
  private val ING_CHEESE = Uuid.parse("00000000-0000-0000-0003-000000000008")
  private val ING_TORTILLAS = Uuid.parse("00000000-0000-0000-0003-000000000009")
  private val ING_SAUCE = Uuid.parse("00000000-0000-0000-0003-000000000010")
  private val ING_BEANS = Uuid.parse("00000000-0000-0000-0003-000000000011")
  private val ING_BURRITO = Uuid.parse("00000000-0000-0000-0003-000000000012")
  private val ING_NATURE = Uuid.parse("00000000-0000-0000-0003-000000000013")

  private val REC_PANCAKES = Uuid.parse("00000000-0000-0000-0004-000000000001")
  private val REC_ENCHILADAS = Uuid.parse("00000000-0000-0000-0004-000000000002")

  suspend fun initialize(
      unitRepository: UnitRepository,
      storeRepository: StoreRepository,
      foodItemRepository: FoodItemRepository,
      mealPlanRepository: MealPlanRepository,
      restaurantRepository: RestaurantRepository,
      settingsRepository: SettingsRepository,
  ) =
      withContext(Dispatchers.IO) {
        // Use setting to ensure we only ever run this once
        val settings = settingsRepository.appSettings.first()
        if (!settings.isFirstLaunch) return@withContext

        // Mark as initialized first to prevent race conditions
        settingsRepository.updateSettings { it.copy(isFirstLaunch = false) }

        // Double check: if data already exists, don't double-insert
        val existingItems = foodItemRepository.foodItems.first()
        if (existingItems.isNotEmpty()) return@withContext

        // --- Units ---
        val uLb = SystemUnits.Lb
        val uOz = SystemUnits.Oz
        val uCup = SystemUnits.Cup
        val uEach = SystemUnits.Each
        val uDozen = SystemUnits.Dozen
        val uFlOz = SystemUnits.FlOz
        val uGallon = SystemUnits.Gallon
        val uTsp = SystemUnits.Tsp
        val uTbsp = SystemUnits.Tbsp

        unitRepository.setUnits(SystemUnits.all)

        // Stores
        val walmart = Store(id = WALMART_ID, name = "Walmart")
        val winco = Store(id = WINCO_ID, name = "WinCo")
        val costco = Store(id = COSTCO_ID, name = "Costco")

        storeRepository.setStores(listOf(walmart, winco, costco))

        // Restaurants
        val mcdonalds = Restaurant(id = MCDONALDS_ID, name = "McDonald's")
        val oliveGarden = Restaurant(id = OLIVE_GARDEN_ID, name = "Olive Garden")
        val chipotle = Restaurant(id = CHIPOTLE_ID, name = "Chipotle")

        restaurantRepository.setRestaurants(listOf(mcdonalds, oliveGarden, chipotle))

        // Categories
        val cBaking = Category(id = CAT_BAKING, name = "Baking")
        val cBread = Category(id = CAT_BREAD, name = "Bread")
        val cCondiment = Category(id = CAT_CONDIMENT, name = "Condiment")
        val cDairy = Category(id = CAT_DAIRY, name = "Dairy")
        val cMeat = Category(id = CAT_MEAT, name = "Meat")
        val cSpice = Category(id = CAT_SPICE, name = "Spice")
        val cStarch = Category(id = CAT_STARCH, name = "Starch")
        val cVegetable = Category(id = CAT_VEGETABLE, name = "Vegetable")

        listOf(cBaking, cBread, cCondiment, cDairy, cMeat, cSpice, cStarch, cVegetable).forEach {
            foodItemRepository.saveCategory(it)
        }

        // --- Ingredients ---
        fun createIng(id: Uuid, name: String, cat: Category, unit: UnitModel? = null): FoodItem {
            return FoodItem(
                id = id,
                name = name,
                preferredUnitId = unit?.id,
                purchasableInfo = PurchasableInfo(
                    categoryId = cat.id,
                    expectedPriceCents = 0
                )
            )
        }

        val iEggs = createIng(ING_EGGS, "Eggs", cMeat, uDozen)
        val iMilk = createIng(ING_MILK, "Milk", cDairy, uGallon)
        val iRiceFlour = createIng(ING_RICE_FLOUR, "Rice Flour", cBaking)
        val iTapiocaStarch = createIng(ING_TAPIOCA, "Tapioca Starch", cBaking)
        val iButter = createIng(ING_BUTTER, "Butter", cDairy)
        val iSausage = createIng(ING_SAUSAGE, "Sausage", cMeat, uLb)
        val iChicken = createIng(ING_CHICKEN, "Chicken Breast", cMeat, uLb)
        val iCheese = createIng(ING_CHEESE, "Cheese", cDairy, uLb)
        val iCornTortillas = createIng(ING_TORTILLAS, "Corn Tortillas", cBread)
        val iEnchiladaSauce = createIng(ING_SAUCE, "Enchilada Sauce", cCondiment)
        val iRefriedBeans = createIng(ING_BEANS, "Refried Beans", cStarch)
        val iBurritoShells = createIng(ING_BURRITO, "Burrito Shells", cBread)
        val iNatureSeasons = createIng(ING_NATURE, "Nature Seasons", cSpice)

        val ingredients = listOf(iEggs, iMilk, iRiceFlour, iTapiocaStarch, iButter, iSausage, iChicken, iCheese, iCornTortillas, iEnchiladaSauce, iRefriedBeans, iBurritoShells, iNatureSeasons)
        ingredients.forEach { foodItemRepository.saveFoodItem(it) }

        // --- Packages ---
        fun pkg(idString: String, item: FoodItem, store: Store, cents: Int, qty: Double, unit: UnitModel) =
            Package(
                id = Uuid.parse(idString),
                foodItemId = item.id,
                storeId = store.id,
                priceCents = cents,
                quantity = qty,
                unitId = unit.id
            )

        listOf(
            pkg("00000000-0000-0000-0005-000000000001", iEggs, walmart, 956, 60.0, uEach),
            pkg("00000000-0000-0000-0005-000000000002", iMilk, walmart, 437, 0.5, uGallon),
            pkg("00000000-0000-0000-0005-000000000003", iRiceFlour, walmart, 300, 16.0, uOz),
            pkg("00000000-0000-0000-0005-000000000004", iTapiocaStarch, walmart, 388, 16.0, uOz),
            pkg("00000000-0000-0000-0005-000000000005", iButter, walmart, 697, 8.0, uEach),
            pkg("00000000-0000-0000-0005-000000000006", iSausage, walmart, 397, 16.0, uOz),
            pkg("00000000-0000-0000-0005-000000000007", iChicken, walmart, 1200, 48.0, uOz),
            pkg("00000000-0000-0000-0005-000000000008", iCheese, walmart, 1724, 80.0, uOz),
            pkg("00000000-0000-0000-0005-000000000009", iCornTortillas, walmart, 398, 80.0, uEach),
            pkg("00000000-0000-0000-0005-000000000010", iEnchiladaSauce, walmart, 250, 16.0, uOz),
            pkg("00000000-0000-0000-0005-000000000011", iRefriedBeans, walmart, 150, 16.0, uOz),
            pkg("00000000-0000-0000-0005-000000000012", iBurritoShells, walmart, 212, 8.0, uEach),
            pkg("00000000-0000-0000-0005-000000000013", iNatureSeasons, walmart, 428, 7.5, uOz)
        ).forEach { foodItemRepository.savePackage(it) }

        // --- Bridges ---
        foodItemRepository.saveConversion(
            BridgeConversion(
                id = Uuid.parse("00000000-0000-0000-0006-000000000001"),
                foodItemId = iChicken.id,
                fromUnitId = uEach.id,
                fromQuantity = 1.0,
                toUnitId = uOz.id,
                toQuantity = 6.0
            )
        )
        foodItemRepository.saveConversion(
            BridgeConversion(
                id = Uuid.parse("00000000-0000-0000-0006-000000000002"),
                foodItemId = iEggs.id,
                fromUnitId = uEach.id,
                fromQuantity = 12.0,
                toUnitId = uDozen.id,
                toQuantity = 1.0
            )
        )
        foodItemRepository.saveConversion(
            BridgeConversion(
                id = Uuid.parse("00000000-0000-0000-0006-000000000003"),
                foodItemId = iCheese.id,
                fromUnitId = uCup.id,
                fromQuantity = 1.0,
                toUnitId = uOz.id,
                toQuantity = 4.0
            )
        )
        foodItemRepository.saveConversion(
            BridgeConversion(
                id = Uuid.parse("00000000-0000-0000-0006-000000000004"),
                foodItemId = iButter.id,
                fromUnitId = uEach.id,
                fromQuantity = 1.0,
                toUnitId = uTbsp.id,
                toQuantity = 8.0
            )
        )

        // --- Recipes ---
        fun req(idString: String, item: FoodItem, qty: Double, unit: UnitModel) =
            FoodItemRequirement(
                id = Uuid.parse(idString),
                measurement = ItemMeasurement(
                    foodItemId = item.id,
                    unitId = unit.id,
                    quantity = qty
                )
            )

        val rPancakes = FoodItem(
            id = REC_PANCAKES,
            name = "Fluffy Pancakes",
            recipeInfo = RecipeInfo(
                servings = 4.0,
                mealType = RecipeMealType.Breakfast,
                instructions = listOf("Mix dry.", "Add wet.", "Cook."),
                requirementGroups = listOf(
                    FoodItemRequirementGroup(
                        requirements = listOf(
                            req("00000000-0000-0000-0007-000000000001", iEggs, 6.0, uEach),
                            req("00000000-0000-0000-0007-000000000002", iMilk, 1.0, uCup),
                            req("00000000-0000-0000-0007-000000000003", iRiceFlour, 0.5, uCup),
                            req("00000000-0000-0000-0007-000000000004", iTapiocaStarch, 0.5, uCup),
                            req("00000000-0000-0000-0007-000000000005", iButter, 0.25, uEach)
                        )
                    )
                )
            )
        )

        val rEnchiladas = FoodItem(
            id = REC_ENCHILADAS,
            name = "Enchiladas",
            recipeInfo = RecipeInfo(
                servings = 6.0,
                mealType = RecipeMealType.Dinner,
                instructions = listOf("Cook chicken.", "Roll.", "Bake."),
                requirementGroups = listOf(
                    FoodItemRequirementGroup(
                        requirements = listOf(
                            req("00000000-0000-0000-0008-000000000001", iCornTortillas, 18.0, uEach),
                            req("00000000-0000-0000-0008-000000000002", iChicken, 3.0, uEach),
                            req("00000000-0000-0000-0008-000000000003", iEnchiladaSauce, 2.0, uEach),
                            req("00000000-0000-0000-0008-000000000004", iCheese, 3.0, uCup),
                            req("00000000-0000-0000-0008-000000000005", iRefriedBeans, 0.5, uEach)
                        )
                    )
                )
            )
        )

        listOf(rPancakes, rEnchiladas).forEach { recipe ->
            foodItemRepository.saveFoodItem(
                item = recipe,
                instructions = recipe.recipeInfo?.instructions ?: emptyList(),
                requirementGroups = recipe.recipeInfo?.requirementGroups ?: emptyList()
            )
        }

        // --- Mock Meal Plan ---
        mealPlanRepository.clearAll()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        mealPlanRepository.addPlan(
            ScheduledMeal(
                id = Uuid.parse("00000000-0000-0000-0009-000000000001"),
                date = today,
                time = LocalTime(8, 0),
                mealType = RecipeMealType.Breakfast,
                peopleCount = 4,
                prePlannedMealId = rPancakes.id
            )
        )

        mealPlanRepository.addPlan(
            ScheduledMeal(
                id = Uuid.parse("00000000-0000-0000-0009-000000000002"),
                date = today,
                time = LocalTime(18, 0),
                mealType = RecipeMealType.Dinner,
                peopleCount = 6,
                prePlannedMealId = rEnchiladas.id
            )
        )
      }
}
