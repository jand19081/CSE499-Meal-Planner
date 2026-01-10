package io.github.and19081.mealplanner

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform