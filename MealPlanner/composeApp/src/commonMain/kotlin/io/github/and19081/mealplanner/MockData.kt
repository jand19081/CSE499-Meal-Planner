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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.Uuid

object MockData {

    fun initialize() {
        // --- Units ---
        val uLb = UnitModel(id = Uuid.random(), type = UnitType.Weight, abbreviation = "lb", displayName = "Pounds", isSystemUnit = true)
        val uOz = UnitModel(id = Uuid.random(), type = UnitType.Weight, abbreviation = "oz", displayName = "Ounces", isSystemUnit = true)
        val uGram = UnitModel(id = Uuid.random(), type = UnitType.Weight, abbreviation = "g", displayName = "Grams", isSystemUnit = true)
        val uKg = UnitModel(id = Uuid.random(), type = UnitType.Weight, abbreviation = "kg", displayName = "Kilograms", isSystemUnit = true)
        
        val uCup = UnitModel(id = Uuid.random(), type = UnitType.Volume, abbreviation = "cup", displayName = "Cups", isSystemUnit = true)
        val uMl = UnitModel(id = Uuid.random(), type = UnitType.Volume, abbreviation = "ml", displayName = "Milliliters", isSystemUnit = true)
        val uLiter = UnitModel(id = Uuid.random(), type = UnitType.Volume, abbreviation = "l", displayName = "Liters", isSystemUnit = true)
        val uTbsp = UnitModel(id = Uuid.random(), type = UnitType.Volume, abbreviation = "tbsp", displayName = "Tablespoons", isSystemUnit = true)
        
        val uEach = UnitModel(id = Uuid.random(), type = UnitType.Count, abbreviation = "each", displayName = "Each", isSystemUnit = true)
        val uDozen = UnitModel(id = Uuid.random(), type = UnitType.Count, abbreviation = "dozen", displayName = "Dozen", isSystemUnit = true)

        UnitRepository.setUnits(listOf(uLb, uOz, uGram, uKg, uCup, uMl, uLiter, uTbsp, uEach, uDozen))

        // Stores
        val walmart = Store(id = Uuid.random(), name = "Walmart")
        val winco = Store(id = Uuid.random(), name = "WinCo")
        val costco = Store(id = Uuid.random(), name = "Costco")
        
        StoreRepository.setStores(listOf(walmart, winco, costco))

        // Categories
        val meat = Category(id = Uuid.random(), name = "Meat")
        val pantry = Category(id = Uuid.random(), name = "Pantry")
        val dairy = Category(id = Uuid.random(), name = "Dairy")
        val produce = Category(id = Uuid.random(), name = "Produce")
        
        IngredientRepository.setCategories(listOf(meat, pantry, dairy, produce))

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
            makePackage(costco, 899, 72.0, uOz, marinara.id) // 3x24oz
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
        IngredientRepository.setIngredients(allIngredients)
        IngredientRepository.setPackages(groundBeefPkgs + pastaPkgs + marinaraPkgs + cheesePkgs + tortillasPkgs + eggsPkgs + onionPkgs)
        
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
                RecipeIngredient(groundBeef.id, 1.0, uLb.id),
                RecipeIngredient(pasta.id, 12.0, uOz.id),
                RecipeIngredient(marinara.id, 24.0, uOz.id),
                RecipeIngredient(cheese.id, 2.0, uCup.id),
                RecipeIngredient(onion.id, 1.0, uEach.id)
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
                RecipeIngredient(groundBeef.id, 1.0, uLb.id),
                RecipeIngredient(tortillas.id, 12.0, uEach.id),
                RecipeIngredient(cheese.id, 1.0, uCup.id),
                RecipeIngredient(onion.id, 0.5, uEach.id)
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
                RecipeIngredient(eggs.id, 3.0, uEach.id),
                RecipeIngredient(cheese.id, 0.5, uCup.id)
            )
        )

        RecipeRepository.setRecipes(listOf(rLasagna, rTacos, rOmelette))

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

        MealRepository.setMeals(listOf(mLasagna, mTacos, mOmelette))

        // Mock Meal Plan
        MealPlanRepository.clearAll()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // Plan Tacos for Today
        val entry1 = ScheduledMeal(
            id = Uuid.random(),
            date = today,
            mealType = RecipeMealType.Dinner,
            peopleCount = 4,
            prePlannedMealId = mTacos.id
        )
        MealPlanRepository.addPlan(entry1)

        // Plan Omelette for Tomorrow Breakfast
        val tomorrow = today + DatePeriod(days = 1)
        val entry2 = ScheduledMeal(
            id = Uuid.random(),
            date = tomorrow,
            mealType = RecipeMealType.Breakfast,
            peopleCount = 2,
            prePlannedMealId = mOmelette.id
        )
        MealPlanRepository.addPlan(entry2)
    }
}
