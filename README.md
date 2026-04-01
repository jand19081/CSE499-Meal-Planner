# 🍽️ Meal Planner

A comprehensive, cross-platform meal planning and kitchen management application built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. 

Developed as a Senior Project (CSE 499), this application goes beyond simple recipe tracking by integrating smart unit conversions, automated shopping list generation, pantry inventory management, and a robust data-oriented architecture.

## ✨ Core Features

* **Smart Recipe Execution**: Step-by-step cooking state management (`RecipeExecutionState`) that tracks active steps, automatically scales ingredient quantities, and prevents state synchronization bugs.
* **Intelligent Unit Conversion**: A graph-based conversion system (BFS routing) that seamlessly translates between weight, volume, and unit measurements (e.g., grams to cups, ml to tablespoons) using ingredient-specific density bridges.
* **Pantry & Inventory Tracking**: Keep a real-time ledger of available ingredients to prevent over-purchasing and track leftover consumption.
* **Automated Shopping Lists**: Dynamically aggregate required ingredients based on your scheduled meal calendar, automatically subtracting what is already in the pantry.
* **Meal Calendar & Dashboard**: Plan breakfasts, lunches, and dinners across the week with a clear, at-a-glance dashboard.
* **Local-First & Offline Ready**: All data is stored locally using Room Database, with support for JSON-based backup and restore functionalities (`BackupPayloadV1`).

## 🛠️ Tech Stack

* **Language**: Kotlin
* **Framework**: Kotlin Multiplatform (targeting Android and JVM/Desktop)
* **UI**: Jetpack Compose / Compose Multiplatform
* **Database**: Room (SQLite) with Coroutines & Flows for reactive data streams
* **Serialization**: `kotlinx.serialization`
* **Date & Time**: `kotlinx.datetime`

## 🏗️ Architecture

This project adheres to **Clean Architecture** principles, strictly separating concerns across Data, Domain, and UI/Feature layers:

### Data-Oriented Design & ECS Database
To handle the inherently polymorphic nature of food items (where an item can be a purchasable ingredient, a recipe, a complete meal, or a leftover), the database utilizes an **Entity Component System (ECS)** approach. 
* **Identity Node**: `FoodItemEntity` acts as the pure identity (UUID and name).
* **Components**: Behaviors are attached via 1-to-1 foreign key relationships (e.g., `PurchasableComponentEntity`, `RecipeComponentEntity`).
* **Reconstruction**: Room's `@Relation` and `ComposedFoodItemRelation` reconstruct the full domain models efficiently without bloating tables with nullable columns.

### Domain Modeling
The domain layer bridges the ECS database into an Algebraic Data Type hierarchy (`Ingredient`, `Recipe`, `Meal`, `Leftover`), encapsulating business logic in highly testable Use Cases (e.g., `ConsumePantryItemUseCase`, `CookingTimeCalculator`).

## 📂 Project Structure

```text
MealPlanner/
├── androidApp/              # Android application entry point and manifest
└── composeApp/              # Shared KMP module
    └── src/
        ├── commonMain/      # Core application logic and UI
        │   ├── core/        # DI, Navigation, Theme, Utils (Unit Conversions, Validators)
        │   ├── data/        # Room DAOs, Entities, Relations, Repositories, Export logic
        │   ├── domain/      # Business logic (Use Cases, Domain Models)
        │   ├── feature/     # UI Features (Calendar, Dashboard, Pantry, Recipes, Shopping List)
        │   └── ui/          # Shared Compose UI components
        ├── androidMain/     # Android-specific implementations (e.g., Notifications, File I/O)
        └── jvmMain/         # Desktop-specific implementations
```

## 🚀 Getting Started

### Prerequisites
* [Android Studio](https://developer.android.com/studio) (Koala or newer recommended) or IntelliJ IDEA.
* JDK 17+

### Build & Run
1.  Clone the repository.
2.  Open the project in Android Studio or IntelliJ.
3.  Sync the Gradle project.
4.  **To run on Android**: Select the `androidApp` run configuration and deploy to an emulator or physical device.
5.  **To run on Desktop (JVM)**: Execute the following Gradle task:
    ```bash
    ./gradlew :composeApp:run
    ```

## 🗺️ Roadmap & Upcoming Refactors

* **Domain Type Safety**: Transitioning the `FoodItem` interface to a `sealed interface` to guarantee exhaustive compile-time checking across the app.
* **Query Optimization**: Replacing in-memory list filtering (`.first().find { ... }`) with targeted O(1) / O(log N) Room DAO queries for better performance and concurrency safety.
* **Data Integrity Enhancements**: Updating DTOs for backup payloads to ensure full feature parity (e.g., preserving `isMeal` flags during round-trip exports).
* **UI Consolidation**: Extracting complex Flow combinations (like the shopping list calculation) into dedicated, independently testable calculator classes.

---
*Developed for CSE 499 Senior Project.*
