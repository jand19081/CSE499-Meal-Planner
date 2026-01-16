package io.github.and19081.mealplanner

import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.Measure
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.ingredients.PurchaseOption
import io.github.and19081.mealplanner.ingredients.Store
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.Uuid

object MockData {

    fun initialize() {
        // Stores
        val walmart = Store(id = Uuid.random(), name = "Walmart")
        val winco = Store(id = Uuid.random(), name = "WinCo")
        val costco = Store(id = Uuid.random(), name = "Costco")
        
        MealPlannerRepository.setStores(listOf(walmart, winco, costco))

        // Helper to make options
        fun makeOption(store: Store, cents: Long, amount: Double, unit: MeasureUnit): PurchaseOption {
            return PurchaseOption(
                ingredientId = Uuid.random(), // overwritten when attached to ingredient
                storeId = store.id,
                priceCents = cents,
                quantity = Measure(amount, unit)
            )
        }

        // --- Ground Beef ---
        val groundBeef = Ingredient(
            id = Uuid.random(),
            name = "Ground Beef",
            category = "Meat",
            purchaseOptions = listOf(
                makeOption(walmart, 598, 1.0, MeasureUnit.LB),
                makeOption(winco, 450, 1.0, MeasureUnit.LB),
                makeOption(costco, 2499, 5.0, MeasureUnit.LB)
            )
        )

        // --- Pasta Noodles ---
        val pasta = Ingredient(
            id = Uuid.random(),
            name = "Lasagna Noodles",
            category = "Pantry",
            purchaseOptions = listOf(
                makeOption(walmart, 198, 16.0, MeasureUnit.OZ),
                makeOption(winco, 148, 16.0, MeasureUnit.OZ)
            )
        )

        // --- Marinara Sauce ---
        val marinara = Ingredient(
            id = Uuid.random(),
            name = "Marinara Sauce",
            category = "Pantry",
            purchaseOptions = listOf(
                makeOption(walmart, 250, 24.0, MeasureUnit.OZ),
                makeOption(winco, 198, 24.0, MeasureUnit.OZ),
                makeOption(costco, 899, 3.0 * 24.0, MeasureUnit.OZ) // 3 pack
            )
        )

        // --- Cheese (Mozzarella) ---
        val cheese = Ingredient(
            id = Uuid.random(),
            name = "Mozzarella Cheese",
            category = "Dairy",
            purchaseOptions = listOf(
                makeOption(walmart, 398, 8.0, MeasureUnit.OZ),
                makeOption(winco, 350, 8.0, MeasureUnit.OZ),
                makeOption(costco, 1299, 2.0, MeasureUnit.LB) // 32 oz
            )
        )

        // --- Tortillas ---
        val tortillas = Ingredient(
            id = Uuid.random(),
            name = "Corn Tortillas",
            category = "Pantry",
            purchaseOptions = listOf(
                makeOption(walmart, 298, 30.0, MeasureUnit.EACH),
                makeOption(winco, 250, 30.0, MeasureUnit.EACH)
            )
        )

        // --- Eggs ---
        val eggs = Ingredient(
            id = Uuid.random(),
            name = "Eggs",
            category = "Dairy",
            purchaseOptions = listOf(
                makeOption(walmart, 250, 12.0, MeasureUnit.EACH),
                makeOption(winco, 220, 12.0, MeasureUnit.EACH),
                makeOption(costco, 999, 60.0, MeasureUnit.EACH)
            )
        )

        // --- Onions ---
        val onion = Ingredient(
            id = Uuid.random(),
            name = "Onion",
            category = "Produce",
            purchaseOptions = listOf(
                makeOption(walmart, 80, 1.0, MeasureUnit.EACH),
                makeOption(winco, 58, 1.0, MeasureUnit.EACH)
            )
        )

        val ingredients = listOf(groundBeef, pasta, marinara, cheese, tortillas, eggs, onion)
        MealPlannerRepository.setIngredients(ingredients)
        
        // Recipes
        val rLasagna = Recipe(
            id = Uuid.parse("60e8d022-7772-4638-8924-111111111111"),
            name = "Lasagna",
            baseServings = 6.0,
            instructions = listOf(
                "Cook ground beef with onions.",
                "Layer noodles, sauce, beef, and cheese.",
                "Bake at 375F for 45 mins."
            )
        )
        
        val rTacos = Recipe(
            id = Uuid.random(),
            name = "Street Tacos",
            baseServings = 4.0,
            instructions = listOf(
                "Cook beef with spices.",
                "Warm tortillas.",
                "Serve with onions and cheese."
            )
        )

        val rOmelette = Recipe(
            id = Uuid.random(),
            name = "Cheese Omelette",
            baseServings = 1.0,
            instructions = listOf(
                "Whisk eggs.",
                "Cook in pan.",
                "Add cheese and fold."
            )
        )

        RecipeRepository.recipes.value = listOf(rLasagna, rTacos, rOmelette)

        MealPlannerRepository.setRecipeIngredients(listOf(
            RecipeIngredient(recipeId = rLasagna.id, ingredientId = groundBeef.id, quantity = Measure(1.0, MeasureUnit.LB)),
            RecipeIngredient(recipeId = rLasagna.id, ingredientId = pasta.id, quantity = Measure(12.0, MeasureUnit.OZ)),
            RecipeIngredient(recipeId = rLasagna.id, ingredientId = marinara.id, quantity = Measure(24.0, MeasureUnit.OZ)),
            RecipeIngredient(recipeId = rLasagna.id, ingredientId = cheese.id, quantity = Measure(2.0, MeasureUnit.CUP)),
            RecipeIngredient(recipeId = rLasagna.id, ingredientId = onion.id, quantity = Measure(1.0, MeasureUnit.EACH)),
            
            RecipeIngredient(recipeId = rTacos.id, ingredientId = groundBeef.id, quantity = Measure(1.0, MeasureUnit.LB)),
            RecipeIngredient(recipeId = rTacos.id, ingredientId = tortillas.id, quantity = Measure(12.0, MeasureUnit.EACH)),
            RecipeIngredient(recipeId = rTacos.id, ingredientId = cheese.id, quantity = Measure(1.0, MeasureUnit.CUP)),
            RecipeIngredient(recipeId = rTacos.id, ingredientId = onion.id, quantity = Measure(0.5, MeasureUnit.EACH)),
            
            RecipeIngredient(recipeId = rOmelette.id, ingredientId = eggs.id, quantity = Measure(3.0, MeasureUnit.EACH)),
            RecipeIngredient(recipeId = rOmelette.id, ingredientId = cheese.id, quantity = Measure(0.5, MeasureUnit.CUP))
        ))

        // Meals
        val mLasagna = Meal(id = Uuid.random(), name = "Lasagna Dinner", description = "Classic family dinner")
        val mTacos = Meal(id = Uuid.random(), name = "Taco Night", description = "Fun taco Tuesday")
        val mOmelette = Meal(id = Uuid.random(), name = "Quick Omelette", description = "Fast breakfast")

        MealPlannerRepository.meals.value = listOf(mLasagna, mTacos, mOmelette)

        // Meal Components (Link Meals to Recipes)
        val comps = mutableListOf<MealComponent>()
        comps.add(MealComponent(mealId = mLasagna.id, recipeId = rLasagna.id))
        comps.add(MealComponent(mealId = mTacos.id, recipeId = rTacos.id))
        comps.add(MealComponent(mealId = mOmelette.id, recipeId = rOmelette.id))
        
        MealPlannerRepository.mealComponents.value = comps

        // Mock Meal Plan
        MealPlanRepository.clearAll()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // Plan Tacos for Today
        val entry1 = MealPlanEntry(
            id = Uuid.random(),
            date = today,
            mealType = MealType.DINNER,
            targetServings = 4.0,
            mealId = mTacos.id
        )
        MealPlanRepository.addPlan(entry1)

        // Plan Omelette for Tomorrow Breakfast
        val tomorrow = today + DatePeriod(days = 1)
        val entry2 = MealPlanEntry(
            id = Uuid.random(),
            date = tomorrow,
            mealType = MealType.BREAKFAST,
            targetServings = 2.0,
            mealId = mOmelette.id
        )
        MealPlanRepository.addPlan(entry2)
    }
}
