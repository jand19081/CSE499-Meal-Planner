package io.github.and19081.mealplanner.domain

object Validators {
    fun validateIngredientName(name: String): Result<String> {
        return when {
            name.isBlank() -> Result.failure(Exception("Ingredient name cannot be empty"))
            name.length > 100 -> Result.failure(Exception("Name too long (max 100 characters)"))
            else -> Result.success(name.trim())
        }
    }
    
    fun validatePrice(priceCents: Long): Result<Long> {
        return when {
            priceCents < 0 -> Result.failure(Exception("Price cannot be negative"))
            priceCents > 10000000 -> Result.failure(Exception("Price too large (max $100,000.00)"))
            else -> Result.success(priceCents)
        }
    }
    
    fun validateQuantity(quantity: Double): Result<Double> {
        return when {
            quantity <= 0 -> Result.failure(Exception("Quantity must be greater than zero"))
            quantity > 10000 -> Result.failure(Exception("Quantity too large (max 10,000)"))
            else -> Result.success(quantity)
        }
    }
    
    fun validateServings(servings: Double): Result<Double> {
        return when {
            servings <= 0 -> Result.failure(Exception("Servings must be greater than zero"))
            servings > 100 -> Result.failure(Exception("Too many servings (max 100)"))
            else -> Result.success(servings)
        }
    }

    fun validateRecipeName(name: String): Result<String> {
        return when {
            name.isBlank() -> Result.failure(Exception("Recipe name cannot be empty"))
            name.length > 100 -> Result.failure(Exception("Name too long (max 100 characters)"))
            else -> Result.success(name.trim())
        }
    }

    fun validateMealName(name: String): Result<String> {
        return when {
            name.isBlank() -> Result.failure(Exception("Meal name cannot be empty"))
            name.length > 100 -> Result.failure(Exception("Name too long (max 100 characters)"))
            else -> Result.success(name.trim())
        }
    }
}
