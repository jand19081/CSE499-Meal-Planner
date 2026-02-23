package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.ingredients.Category
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.Store
import io.github.and19081.mealplanner.ingredients.StoreRepository
import io.github.and19081.mealplanner.meals.MealRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.Uuid

object MockData {

    suspend fun initialize(
        unitRepository: UnitRepository,
        storeRepository: StoreRepository,
        ingredientRepository: IngredientRepository,
        recipeRepository: RecipeRepository,
        mealRepository: MealRepository,
        mealPlanRepository: MealPlanRepository,
        restaurantRepository: io.github.and19081.mealplanner.ingredients.RestaurantRepository
    ) {
        if (ingredientRepository.count() > 0) return

        // --- Units ---
        val uLb = SystemUnits.Lb
        val uOz = SystemUnits.Oz
        val uCup = SystemUnits.Cup
        val uEach = SystemUnits.Each

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

        // Categories
        val meat = Category(id = Uuid.random(), name = "Meat")
        val pantry = Category(id = Uuid.random(), name = "Pantry")
        val dairy = Category(id = Uuid.random(), name = "Dairy")
        val produce = Category(id = Uuid.random(), name = "Produce")
        
        ingredientRepository.setCategories(listOf(meat, pantry, dairy, produce))

        // Helper to make packages
        fun makePackage(store: Store, cents: Int, amount: Double, unit: UnitModel, ingredientId: Uuid): Package {
            return Package(
                id = Uuid.random(),
                ingredientId = ingredientId,
                storeId = store.id,
                priceCents = cents,
                quantity = amount,
                unitId = unit.id
            )
        }

        // --- Ground Beef ---
        val groundBeef = Ingredient(id = Uuid.random(), name = "Ground Beef", categoryId = meat.id)
        val groundBeefPkgs = listOf(
            makePackage(walmart, 598, 1.0, uLb, groundBeef.id),
            makePackage(winco, 450, 1.0, uLb, groundBeef.id),
            makePackage(costco, 2499, 5.0, uLb, groundBeef.id)
        )

        // --- Pasta Noodles ---
        val pasta = Ingredient(id = Uuid.random(), name = "Lasagna Noodles", categoryId = pantry.id)
        val pastaPkgs = listOf(
            makePackage(walmart, 198, 16.0, uOz, pasta.id),
            makePackage(winco, 148, 16.0, uOz, pasta.id)
        )

        // --- Marinara Sauce ---
        val marinara = Ingredient(id = Uuid.random(), name = "Marinara Sauce", categoryId = pantry.id)
        val marinaraPkgs = listOf(
            makePackage(walmart, 250, 24.0, uOz, marinara.id),
            makePackage(winco, 198, 24.0, uOz, marinara.id),
            makePackage(costco, 899, 72.0, uOz, marinara.id)
        )

        // --- Cheese (Mozzarella) ---
        val cheese = Ingredient(id = Uuid.random(), name = "Mozzarella Cheese", categoryId = dairy.id)
        val cheesePkgs = listOf(
            makePackage(walmart, 398, 8.0, uOz, cheese.id),
            makePackage(winco, 350, 8.0, uOz, cheese.id),
            makePackage(costco, 1299, 32.0, uOz, cheese.id)
        )

        // --- Tortillas ---
        val tortillas = Ingredient(id = Uuid.random(), name = "Corn Tortillas", categoryId = pantry.id)
        val tortillasPkgs = listOf(
            makePackage(walmart, 298, 30.0, uEach, tortillas.id),
            makePackage(winco, 250, 30.0, uEach, tortillas.id)
        )

        // --- Eggs ---
        val eggs = Ingredient(id = Uuid.random(), name = "Eggs", categoryId = dairy.id)
        val eggsPkgs = listOf(
            makePackage(walmart, 250, 12.0, uEach, eggs.id),
            makePackage(winco, 220, 12.0, uEach, eggs.id),
            makePackage(costco, 999, 60.0, uEach, eggs.id)
        )

        // --- Onions ---
        val onion = Ingredient(id = Uuid.random(), name = "Onion", categoryId = produce.id)
        val onionPkgs = listOf(
            makePackage(walmart, 80, 1.0, uEach, onion.id),
            makePackage(winco, 58, 1.0, uEach, onion.id)
        )

        val allIngredients = listOf(groundBeef, pasta, marinara, cheese, tortillas, eggs, onion)
        ingredientRepository.setIngredients(allIngredients)
        ingredientRepository.setPackages(groundBeefPkgs + pastaPkgs + marinaraPkgs + cheesePkgs + tortillasPkgs + eggsPkgs + onionPkgs)
        
        // Recipes
        val rLasagna = Recipe(
            id = Uuid.parse("60e8d022-7772-4638-8924-111111111111"),
            name = "Lasagna",
            servings = 6.0,
            mealType = RecipeMealType.Dinner,
            prepTimeMinutes = 20,
            cookTimeMinutes = 45,
            instructions = listOf(
                "Cook ground beef with onions.",
                "Layer noodles, sauce, beef, and cheese.",
                "Bake at 375F for 45 mins."
            ),
            ingredients = listOf(
                RecipeIngredient(ingredientId = groundBeef.id, quantity = 1.0, unitId = uLb.id),
                RecipeIngredient(ingredientId = pasta.id, quantity = 12.0, unitId = uOz.id),
                RecipeIngredient(ingredientId = marinara.id, quantity = 24.0, unitId = uOz.id),
                RecipeIngredient(ingredientId = cheese.id, quantity = 2.0, unitId = uCup.id),
                RecipeIngredient(ingredientId = onion.id, quantity = 1.0, unitId = uEach.id)
            )
        )
        
        val rTacos = Recipe(
            id = Uuid.random(),
            name = "Street Tacos",
            servings = 4.0,
            mealType = RecipeMealType.Dinner,
            prepTimeMinutes = 15,
            cookTimeMinutes = 15,
            instructions = listOf(
                "Cook beef with spices.",
                "Warm tortillas.",
                "Serve with onions and cheese."
            ),
            ingredients = listOf(
                RecipeIngredient(ingredientId = groundBeef.id, quantity = 1.0, unitId = uLb.id),
                RecipeIngredient(ingredientId = tortillas.id, quantity = 12.0, unitId = uEach.id),
                RecipeIngredient(ingredientId = cheese.id, quantity = 1.0, unitId = uCup.id),
                RecipeIngredient(ingredientId = onion.id, quantity = 0.5, unitId = uEach.id)
            )
        )

        val rOmelette = Recipe(
            id = Uuid.random(),
            name = "Cheese Omelette",
            servings = 1.0,
            mealType = RecipeMealType.Breakfast,
            prepTimeMinutes = 5,
            cookTimeMinutes = 5,
            instructions = listOf(
                "Whisk eggs.",
                "Cook in pan.",
                "Add cheese and fold."
            ),
            ingredients = listOf(
                RecipeIngredient(ingredientId = eggs.id, quantity = 3.0, unitId = uEach.id),
                RecipeIngredient(ingredientId = cheese.id, quantity = 0.5, unitId = uCup.id)
            )
        )

        recipeRepository.setRecipes(listOf(rLasagna, rTacos, rOmelette))

        // PrePlanned Meals
        val mLasagna = PrePlannedMeal(
            id = Uuid.random(), 
            name = "Lasagna Dinner",
            recipes = listOf(rLasagna.id)
        )
        val mTacos = PrePlannedMeal(
            id = Uuid.random(), 
            name = "Taco Night",
            recipes = listOf(rTacos.id)
        )
        val mOmelette = PrePlannedMeal(
            id = Uuid.random(), 
            name = "Quick Omelette",
            recipes = listOf(rOmelette.id)
        )

        mealRepository.setMeals(listOf(mLasagna, mTacos, mOmelette))

        // Mock Meal Plan
        mealPlanRepository.clearAll()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // Plan Tacos for Today
        val entry1 = ScheduledMeal(
            id = Uuid.random(),
            date = today,
            time = LocalTime(18, 0),
            mealType = RecipeMealType.Dinner,
            peopleCount = 4,
            prePlannedMealId = mTacos.id
        )
        mealPlanRepository.addPlan(entry1)

        // Plan Omelette for Tomorrow Breakfast
        val tomorrow = today + DatePeriod(days = 1)
        val entry2 = ScheduledMeal(
            id = Uuid.random(),
            date = tomorrow,
            time = LocalTime(8, 0),
            mealType = RecipeMealType.Breakfast,
            peopleCount = 2,
            prePlannedMealId = mOmelette.id
        )
        mealPlanRepository.addPlan(entry2)

        // Plan Restaurant for Lunch
        val entry3 = ScheduledMeal(
            id = Uuid.random(),
            date = today,
            time = LocalTime(12, 0),
            mealType = RecipeMealType.Lunch,
            peopleCount = 2,
            restaurantId = chipotle.id,
            anticipatedCostCents = 2500
        )
        mealPlanRepository.addPlan(entry3)
    }
}
