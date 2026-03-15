package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.ingredients.Category
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.Store
import io.github.and19081.mealplanner.ingredients.StoreRepository
import io.github.and19081.mealplanner.meals.MealRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import kotlin.uuid.Uuid
import kotlinx.datetime.*
import kotlin.time.Clock

object MockData {

  suspend fun initialize(
      unitRepository: UnitRepository,
      storeRepository: StoreRepository,
      ingredientRepository: IngredientRepository,
      recipeRepository: RecipeRepository,
      mealRepository: MealRepository,
      mealPlanRepository: MealPlanRepository,
      restaurantRepository: io.github.and19081.mealplanner.ingredients.RestaurantRepository,
  ) =
      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (ingredientRepository.count() > 0) return@withContext

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
        val walmart = Store(id = Uuid.random(), name = "Walmart")
        val winco = Store(id = Uuid.random(), name = "WinCo")
        val costco = Store(id = Uuid.random(), name = "Costco")

        storeRepository.setStores(listOf(walmart, winco, costco))

        // Restaurants
        val mcdonalds = Restaurant(id = Uuid.random(), name = "McDonald's")
        val oliveGarden = Restaurant(id = Uuid.random(), name = "Olive Garden")
        val chipotle = Restaurant(id = Uuid.random(), name = "Chipotle")

        restaurantRepository.setRestaurants(listOf(mcdonalds, oliveGarden, chipotle))

        // Categories (from Spreadsheet)
        val cBaking = Category(id = Uuid.random(), name = "Baking")
        val cBread = Category(id = Uuid.random(), name = "Bread")
        val cCondiment = Category(id = Uuid.random(), name = "Condiment")
        val cDairy = Category(id = Uuid.random(), name = "Dairy")
        val cMeat = Category(id = Uuid.random(), name = "Meat")
        val cSpice = Category(id = Uuid.random(), name = "Spice")
        val cStarch = Category(id = Uuid.random(), name = "Starch")
        val cVegetable = Category(id = Uuid.random(), name = "Vegetable")

        ingredientRepository.setCategories(listOf(cBaking, cBread, cCondiment, cDairy, cMeat, cSpice, cStarch, cVegetable))

        // Helper to make packages
        fun makePackage(
            store: Store,
            cents: Int,
            amount: Double,
            unit: UnitModel,
            ingredientId: Uuid,
        ): Package {
          return Package(
              id = Uuid.random(),
              ingredientId = ingredientId,
              storeId = store.id,
              priceCents = cents,
              quantity = amount,
              unitId = unit.id,
          )
        }

        // --- Ingredients & Packages (Walmart Pricing from Spreadsheet) ---
        
        // Baking
        val iTapiocaStarch = Ingredient(id = Uuid.random(), name = "Tapioca Starch", categoryId = cBaking.id)
        val pTapiocaStarch = makePackage(walmart, 388, 16.0, uOz, iTapiocaStarch.id)
        
        val iRice = Ingredient(id = Uuid.random(), name = "Rice", categoryId = cBaking.id, preferredUnitId = uLb.id)
        val pRice = makePackage(walmart, 398, 80.0, uOz, iRice.id)

        val iRiceFlour = Ingredient(id = Uuid.random(), name = "Rice Flour", categoryId = cBaking.id)
        val pRiceFlour = makePackage(walmart, 300, 16.0, uOz, iRiceFlour.id) // Estimated package
        
        val iSugar = Ingredient(id = Uuid.random(), name = "Sugar", categoryId = cBaking.id, preferredUnitId = uLb.id)
        val pSugar = makePackage(walmart, 1997, 25.0, uLb, iSugar.id)
        
        val iBakingPowder = Ingredient(id = Uuid.random(), name = "Baking Powder", categoryId = cBaking.id)
        val pBakingPowder = makePackage(walmart, 312, 8.1, uOz, iBakingPowder.id)
        
        val iOil = Ingredient(id = Uuid.random(), name = "Oil", categoryId = cBaking.id)
        val pOil = makePackage(walmart, 1917, 51.0, uFlOz, iOil.id)
        
        val iVanilla = Ingredient(id = Uuid.random(), name = "Vanilla", categoryId = cBaking.id)
        val pVanilla = makePackage(walmart, 192, 8.0, uFlOz, iVanilla.id)

        // Bread
        val iGlutenWhiteBread = Ingredient(id = Uuid.random(), name = "Gluten White Bread", categoryId = cBread.id)
        val pGlutenWhiteBread = makePackage(walmart, 142, 22.0, uEach, iGlutenWhiteBread.id)
        
        val iGlutenFreeBread = Ingredient(id = Uuid.random(), name = "Gluten Free Bread", categoryId = cBread.id)
        val pGlutenFreeBread = makePackage(walmart, 647, 15.0, uEach, iGlutenFreeBread.id)

        val iGlutenBurritoShells = Ingredient(id = Uuid.random(), name = "Gluten Burrito Shells", categoryId = cBread.id)
        val pGlutenBurritoShells = makePackage(walmart, 212, 8.0, uEach, iGlutenBurritoShells.id)

        val iCornTortillas = Ingredient(id = Uuid.random(), name = "Corn Tortillas", categoryId = cBread.id)
        val pCornTortillas = makePackage(walmart, 398, 80.0, uEach, iCornTortillas.id)

        // Condiment
        val iSalsa = Ingredient(id = Uuid.random(), name = "Salsa", categoryId = cCondiment.id)
        val pSalsa = makePackage(walmart, 297, 16.0, uOz, iSalsa.id)

        val iEnchiladaSauce = Ingredient(id = Uuid.random(), name = "Enchilada Sauce", categoryId = cCondiment.id)
        val pEnchiladaSauce = makePackage(walmart, 250, 16.0, uOz, iEnchiladaSauce.id)

        // Dairy
        val iMilk = Ingredient(id = Uuid.random(), name = "Milk", categoryId = cDairy.id)
        val pMilk = makePackage(walmart, 437, 0.5, uGallon, iMilk.id)
        
        val iButter = Ingredient(id = Uuid.random(), name = "Butter", categoryId = cDairy.id)
        val pButter = makePackage(walmart, 697, 8.0, uEach, iButter.id) // 8 sticks
        
        val iSourCream = Ingredient(id = Uuid.random(), name = "Sour Cream", categoryId = cDairy.id)
        val pSourCream = makePackage(walmart, 492, 48.0, uOz, iSourCream.id)
        
        val iCheese = Ingredient(id = Uuid.random(), name = "Cheese (Fiesta/Mozzarella)", categoryId = cDairy.id, preferredUnitId = uLb.id)
        val pCheese = makePackage(walmart, 1724, 80.0, uOz, iCheese.id)

        // Meat
        val iSausage = Ingredient(id = Uuid.random(), name = "Sausage", categoryId = cMeat.id, preferredUnitId = uLb.id)
        val pSausage = makePackage(walmart, 397, 16.0, uOz, iSausage.id)
        
        val iEggs = Ingredient(id = Uuid.random(), name = "Eggs", categoryId = cMeat.id, preferredUnitId = uDozen.id)
        val pEggs = makePackage(walmart, 956, 60.0, uEach, iEggs.id)

        val iChicken = Ingredient(id = Uuid.random(), name = "Chicken Breast", categoryId = cMeat.id, preferredUnitId = uLb.id)
        val pChicken = makePackage(walmart, 1200, 48.0, uOz, iChicken.id)

        // Spice
        val iSalt = Ingredient(id = Uuid.random(), name = "Salt", categoryId = cSpice.id)
        val pSalt = makePackage(walmart, 167, 26.0, uOz, iSalt.id)
        
        val iNatureSeasons = Ingredient(id = Uuid.random(), name = "Nature Seasons", categoryId = cSpice.id)
        val pNatureSeasons = makePackage(walmart, 428, 7.5, uOz, iNatureSeasons.id)

        // Vegetable & Starch
        val iOnions = Ingredient(id = Uuid.random(), name = "Onions", categoryId = cVegetable.id)
        val pOnions = makePackage(walmart, 62, 1.0, uEach, iOnions.id)
        
        val iPotatoes = Ingredient(id = Uuid.random(), name = "Potatoes", categoryId = cStarch.id, preferredUnitId = uLb.id)
        val pPotatoes = makePackage(walmart, 394, 10.0, uLb, iPotatoes.id)

        val iRefriedBeans = Ingredient(id = Uuid.random(), name = "Refried Beans", categoryId = cStarch.id)
        val pRefriedBeans = makePackage(walmart, 150, 16.0, uOz, iRefriedBeans.id)

        val allIngredients = listOf(
            iTapiocaStarch, iRice, iRiceFlour, iSugar, iBakingPowder, iOil, iVanilla,
            iGlutenWhiteBread, iGlutenFreeBread, iGlutenBurritoShells, iCornTortillas,
            iSalsa, iEnchiladaSauce, iMilk, iButter, iSourCream, iCheese,
            iSausage, iEggs, iChicken, iSalt, iNatureSeasons, iOnions, iPotatoes, iRefriedBeans
        )
        
        ingredientRepository.setIngredients(allIngredients)
        ingredientRepository.setPackages(
            listOf(
                pTapiocaStarch, pRice, pRiceFlour, pSugar, pBakingPowder, pOil, pVanilla,
                pGlutenWhiteBread, pGlutenFreeBread, pGlutenBurritoShells, pCornTortillas,
                pSalsa, pEnchiladaSauce, pMilk, pButter, pSourCream, pCheese,
                pSausage, pEggs, pChicken, pSalt, pNatureSeasons, pOnions, pPotatoes, pRefriedBeans
            )
        )

        // Bridges
        val bChicken = BridgeConversion(
            id = Uuid.random(),
            ingredientId = iChicken.id,
            fromUnitId = uEach.id,
            fromQuantity = 1.0,
            toUnitId = uOz.id,
            toQuantity = 6.0 // Assume 1 breast = 6 oz
        )
        val bEggs = BridgeConversion(
            id = Uuid.random(),
            ingredientId = iEggs.id,
            fromUnitId = uEach.id,
            fromQuantity = 12.0,
            toUnitId = uDozen.id,
            toQuantity = 1.0
        )
        val bCheese = BridgeConversion(
            id = Uuid.random(),
            ingredientId = iCheese.id,
            fromUnitId = uCup.id,
            fromQuantity = 1.0,
            toUnitId = uOz.id,
            toQuantity = 4.0 // 1 cup shredded cheese approx 4 oz
        )
        val bButter = BridgeConversion(
            id = Uuid.random(),
            ingredientId = iButter.id,
            fromUnitId = uEach.id, // stick
            fromQuantity = 1.0,
            toUnitId = uTbsp.id,
            toQuantity = 8.0
        )

        ingredientRepository.setBridges(listOf(bChicken, bEggs, bCheese, bButter))

        // Helper to make requirement group with one ingredient
        fun req(ingId: Uuid, qty: Double, unitId: Uuid): RecipeRequirementGroup {
            return RecipeRequirementGroup(
                id = Uuid.random(),
                requirements = listOf(
                    RecipeRequirement(ingredientId = ingId, quantity = qty, unitId = unitId, isPrimary = true)
                )
            )
        }

        // --- Recipes ---
        val rFluffyPancakes = Recipe(
            id = Uuid.random(),
            name = "Fluffy Pancakes",
            servings = 4.0,
            mealType = RecipeMealType.Breakfast,
            prepTimeMinutes = 10,
            cookTimeMinutes = 15,
            instructions = listOf("Mix dry ingredients.", "Whisk in milk, eggs, and butter.", "Cook on griddle."),
            requirementGroups = listOf(
                req(iEggs.id, 6.0, uEach.id),
                req(iMilk.id, 1.0, uCup.id),
                req(iRiceFlour.id, 0.5, uCup.id),
                req(iTapiocaStarch.id, 0.5, uCup.id),
                req(iButter.id, 0.25, uEach.id)
            )
        )

        val rEggsAndSausage = Recipe(
            id = Uuid.random(),
            name = "Eggs & Sausage",
            servings = 4.0,
            mealType = RecipeMealType.Breakfast,
            prepTimeMinutes = 5,
            cookTimeMinutes = 10,
            instructions = listOf("Scramble eggs.", "Brown sausage in a separate pan.", "Serve together."),
            requirementGroups = listOf(
                req(iEggs.id, 6.0, uEach.id),
                req(iSausage.id, 16.0, uOz.id)
            )
        )

        val rChickenQuesadilla = Recipe(
            id = Uuid.random(),
            name = "Chicken Quesadilla",
            servings = 2.0,
            mealType = RecipeMealType.Dinner,
            prepTimeMinutes = 10,
            cookTimeMinutes = 10,
            instructions = listOf("Cook chicken with Nature Seasons.", "Assemble in shells with cheese.", "Grill with butter until golden."),
            requirementGroups = listOf(
                req(iGlutenBurritoShells.id, 4.0, uEach.id),
                req(iCheese.id, 1.0, uCup.id),
                req(iChicken.id, 1.0, uEach.id),
                req(iNatureSeasons.id, 1.0, uTsp.id),
                req(iButter.id, 0.25, uTsp.id)
            )
        )

        val rEnchiladas = Recipe(
            id = Uuid.random(),
            name = "Enchiladas",
            servings = 6.0,
            mealType = RecipeMealType.Dinner,
            prepTimeMinutes = 20,
            cookTimeMinutes = 30,
            instructions = listOf("Cook chicken.", "Roll in tortillas with beans and cheese.", "Cover with sauce and remaining cheese. Bake."),
            requirementGroups = listOf(
                req(iCornTortillas.id, 18.0, uEach.id),
                req(iChicken.id, 3.0, uEach.id),
                req(iEnchiladaSauce.id, 2.0, uEach.id),
                req(iCheese.id, 3.0, uCup.id),
                req(iRefriedBeans.id, 0.5, uEach.id)
            )
        )

        recipeRepository.setRecipes(listOf(rFluffyPancakes, rEggsAndSausage, rChickenQuesadilla, rEnchiladas))

        // PrePlanned Meals
        val mPancakes = PrePlannedMeal(id = Uuid.random(), name = "Pancake Breakfast", recipes = listOf(rFluffyPancakes.id))
        val mEggsSausage = PrePlannedMeal(id = Uuid.random(), name = "Classic Eggs & Sausage", recipes = listOf(rEggsAndSausage.id))
        val mQuesadilla = PrePlannedMeal(id = Uuid.random(), name = "Quesadilla Lunch", recipes = listOf(rChickenQuesadilla.id))
        val mEnchilada = PrePlannedMeal(id = Uuid.random(), name = "Enchilada Dinner", recipes = listOf(rEnchiladas.id))

        mealRepository.setMeals(listOf(mPancakes, mEggsSausage, mQuesadilla, mEnchilada))

        // Mock Meal Plan
        mealPlanRepository.clearAll()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val entry1 = ScheduledMeal(
            id = Uuid.random(),
            date = today,
            time = LocalTime(8, 0),
            mealType = RecipeMealType.Breakfast,
            peopleCount = 4,
            prePlannedMealId = mPancakes.id,
        )
        mealPlanRepository.addPlan(entry1)

        val entry2 = ScheduledMeal(
            id = Uuid.random(),
            date = today,
            time = LocalTime(18, 0),
            mealType = RecipeMealType.Dinner,
            peopleCount = 6,
            prePlannedMealId = mEnchilada.id,
        )
        mealPlanRepository.addPlan(entry2)
      }
}
