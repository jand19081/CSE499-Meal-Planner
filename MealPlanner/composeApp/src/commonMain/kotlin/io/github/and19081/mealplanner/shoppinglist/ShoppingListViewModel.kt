package io.github.and19081.mealplanner.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.*
import io.github.and19081.mealplanner.calendar.MealPlanRepository
import io.github.and19081.mealplanner.domain.DataQualityValidator
import io.github.and19081.mealplanner.domain.DataWarning
import io.github.and19081.mealplanner.domain.UnitConverter
import io.github.and19081.mealplanner.ingredients.BridgeConversion
import io.github.and19081.mealplanner.ingredients.Ingredient
import io.github.and19081.mealplanner.ingredients.IngredientRepository
import io.github.and19081.mealplanner.kitchen.*
import io.github.and19081.mealplanner.ingredients.Package
import io.github.and19081.mealplanner.ingredients.Store
import io.github.and19081.mealplanner.ingredients.StoreRepository
import io.github.and19081.mealplanner.meals.MealRepository
import io.github.and19081.mealplanner.recipes.RecipeRepository
import io.github.and19081.mealplanner.settings.AppSettings
import io.github.and19081.mealplanner.settings.SettingsRepository
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

class ShoppingListViewModel(
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val storeRepository: StoreRepository,
    private val unitRepository: UnitRepository,
    private val settingsRepository: SettingsRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val pantryRepository: PantryRepository,
    private val shoppingListItemRepository: ShoppingListItemRepository,
    private val receiptHistoryRepository: ReceiptHistoryRepository,
) : ViewModel() {

  private val _inCartItems = MutableStateFlow<Set<Uuid>>(emptySet())
  val inCartItems = _inCartItems.asStateFlow()

  // Helper data class to group static/repo data
  data class CoreData(
      val recipes: List<Recipe>,
      val allIngredients: List<Ingredient>,
      val allStores: List<Store>,
      val allPackages: List<Package>,
      val allBridges: List<BridgeConversion>,
      val allUnits: List<UnitModel>,
      val taxRate: Double,
  )

  // Helper data class to group user/dynamic data
  data class UserData(
      val entries: List<ScheduledMeal>,
      val meals: List<PrePlannedMeal>,
      val overrides: Map<Uuid, ShoppingListOverride>,
      val pantryItems: List<PantryItem>,
      val customItems: List<ShoppingListItem>,
  )

  // Helper for intermediate calculation result
  data class CalculatedList(
      val sections: List<ShoppingListSection>,
      val ownedItems: List<ShoppingListItemUi>,
      val allStores: List<Store>,
      val allUnits: List<UnitModel>,
      val taxRate: Double,
      val globalWarnings: List<DataWarning> = emptyList(),
  )

  private val coreDataFlow =
      combine(
          recipeRepository.recipes,
          ingredientRepository.ingredients,
          storeRepository.stores,
          ingredientRepository.packages,
          ingredientRepository.bridges,
          unitRepository.units,
          settingsRepository.appSettings,
      ) { args: Array<Any?> ->
        CoreData(
            args[0] as? List<Recipe> ?: emptyList(),
            args[1] as? List<Ingredient> ?: emptyList(),
            args[2] as? List<Store> ?: emptyList(),
            args[3] as? List<Package> ?: emptyList(),
            args[4] as? List<BridgeConversion> ?: emptyList(),
            args[5] as? List<UnitModel> ?: emptyList(),
            (args[6] as? AppSettings)?.defaultTaxRatePercentage ?: 0.0,
        )
      }

  private val userDataFlow =
      combine(
          mealPlanRepository.entries,
          mealRepository.meals,
          shoppingListRepository.overrides,
          pantryRepository.pantryItems,
          shoppingListItemRepository.items,
      ) { args: Array<Any?> ->
        UserData(
            entries = args[0] as? List<ScheduledMeal> ?: emptyList(),
            meals = args[1] as? List<PrePlannedMeal> ?: emptyList(),
            overrides = args[2] as? Map<Uuid, ShoppingListOverride> ?: emptyMap(),
            pantryItems = args[3] as? List<PantryItem> ?: emptyList(),
            customItems = args[4] as? List<ShoppingListItem> ?: emptyList(),
        )
      }

  // Heavy Calculation: Depends on everything EXCEPT cart status
  private val calculatedListFlow =
      combine(coreDataFlow, userDataFlow) { core, user ->
        val (recipes, allIngredients, allStores, allPackages, allBridges, allUnits, taxRate) = core
        val (entries, meals, overrides, pantryItems, customItems) = user

        // Pre-calculate Maps for O(1) Lookups
        val mealsMap = meals.associateBy { it.id }
        val recipesMap = recipes.associateBy { it.id }
        val ingredientsMap = allIngredients.associateBy { it.id }

        // 1. Calculate Requirements (Grouped by UnitType to avoid mixing bases)
        val grossRequirementsByType = mutableMapOf<Uuid, MutableMap<UnitType, Double>>()
        val activeWarnings = mutableListOf<DataWarning>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        entries.filter { !it.isConsumed && it.date >= today }.forEach { entry: ScheduledMeal ->
          val meal = entry.prePlannedMealId?.let { mealsMap[it] }
          if (meal != null) {
            activeWarnings.addAll(
                DataQualityValidator.validateMeal(
                    meal,
                    recipesMap,
                    ingredientsMap,
                    allPackages,
                    allBridges,
                    allUnits,
                )
            )

            fun add(ingId: Uuid, qty: Double, unitId: Uuid?) {
              val unit = allUnits.find { it.id == unitId } ?: return
              val (baseQty, _) = UnitConverter.toStandard(qty, unit, allUnits.associateBy { it.id })
              val types = grossRequirementsByType.getOrPut(ingId) { mutableMapOf() }
              types[unit.type] = (types[unit.type] ?: 0.0) + baseQty
            }

            // Recipes
            meal.recipes.forEach { rId ->
              val recipe = recipesMap[rId]
              if (recipe != null) {
                val scale = if (recipe.servings > 0) entry.peopleCount / recipe.servings else 1.0
                recipe.ingredients.forEach { ri ->
                  ri.ingredientId?.let { add(it, ri.quantity * scale, ri.unitId) }
                }
              }
            }

            // Independent Ingredients
            meal.independentIngredients.forEach { item ->
              // Assuming qty is per person
              add(item.ingredientId, item.quantity * entry.peopleCount, item.unitId)
            }
          }
        }

        // 1b. Consolidate Grouped Requirements to a single Consistent Base Unit using Bridges
        val grossRequirements = mutableMapOf<Uuid, Double>()
        grossRequirementsByType.forEach { (ingId, types) ->
          if (types.isEmpty()) return@forEach
          val ing = ingredientsMap[ingId] ?: return@forEach

          // Determine target unit type (prefer preferred unit or largest requirement)
          val preferredType = ing.preferredUnitId?.let { pid -> allUnits.find { it.id == pid }?.type }
          val targetType = preferredType ?: types.maxBy { it.value }.key

          var totalInTargetBase = 0.0
          val targetBaseUnitId =
              when (targetType) {
                UnitType.Weight -> SystemUnits.Gram.id
                UnitType.Volume -> SystemUnits.Ml.id
                UnitType.Count -> SystemUnits.Each.id
                else -> null
              }

          types.forEach { (type, baseQty) ->
            if (type == targetType) {
              totalInTargetBase += baseQty
            } else {
              val fromBaseUnitId =
                  when (type) {
                    UnitType.Weight -> SystemUnits.Gram.id
                    UnitType.Volume -> SystemUnits.Ml.id
                    UnitType.Count -> SystemUnits.Each.id
                    else -> null
                  }

              // Use converter to bridge between different base types (e.g. Count Each to Weight
              // Grams)
              val converted =
                  UnitConverter.convert(
                      baseQty,
                      fromBaseUnitId,
                      targetBaseUnitId,
                      allUnits.associateBy { it.id },
                      allBridges,
                  ) ?: 0.0
              totalInTargetBase += converted
            }
          }
          grossRequirements[ingId] = totalInTargetBase
        }

        // 2. Apply Pantry
        val netRequirements = mutableMapOf<Uuid, Double>()
        val fullyOwnedIngredients = mutableListOf<Uuid>()

        grossRequirements.forEach { (ingId, reqQty) ->
          val ing = ingredientsMap[ingId] ?: return@forEach
          val pantryItem = pantryItems.find { it.ingredientId == ingId }

          // We must ensure the pantry quantity is in the same Base Unit as our reqQty
          // reqQty is in the base unit of the 'targetType' identified in consolidation step
          val preferredType = ing.preferredUnitId?.let { pid -> allUnits.find { it.id == pid }?.type }
          val grossRequirementsTargetType = preferredType ?: (grossRequirementsByType[ingId]?.maxBy { it.value }?.key ?: UnitType.Count)

          val ownedQty =
              if (pantryItem != null) {
                val pUnit = allUnits.find { it.id == pantryItem.unitId }
                if (pUnit != null) {
                    val targetBaseUnitId = when (grossRequirementsTargetType) {
                        UnitType.Weight -> SystemUnits.Gram.id
                        UnitType.Volume -> SystemUnits.Ml.id
                        UnitType.Count -> SystemUnits.Each.id
                        else -> null
                    }
                    UnitConverter.convert(pantryItem.quantity, pUnit.id, targetBaseUnitId, allUnits.associateBy { it.id }, allBridges) ?: 0.0
                }
                else 0.0
              } else 0.0

          val needed = max(0.0, reqQty - ownedQty)
          if (needed > 0.001) {
            netRequirements[ingId] = needed
          } else {
            fullyOwnedIngredients.add(ingId)
          }
        }

        // 3. Build Lists
        val shoppingLists = mutableMapOf<Uuid, ShoppingListSection>()

        allStores.forEach { store ->
          shoppingLists[store.id] = ShoppingListSection(store.id, store.name, emptyList(), 0, 0, 0)
        }
        val anyStoreId = Uuid.parse("00000000-0000-0000-0000-000000000000")
        if (!shoppingLists.containsKey(anyStoreId)) {
          shoppingLists[anyStoreId] =
              ShoppingListSection(anyStoreId, "Other / Custom", emptyList(), 0, 0, 0)
        }

        netRequirements.forEach { (ingId, baseQtyNeeded) ->
          val ing = ingredientsMap[ingId] ?: return@forEach
          val override = overrides[ingId]

          // If explicit override isOwned is true, treat as owned (legacy support or manual
          // override)
          if (override?.inPantry == true) {
            fullyOwnedIngredients.add(ingId)
            return@forEach
          }

          val packages = allPackages.filter { it.ingredientId == ingId }

          val forcedStoreId = override?.forceStoreId
          val bestOption =
              if (forcedStoreId != null) {
                packages.find { it.storeId == forcedStoreId }
                    ?: packages.minByOrNull {
                      if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE
                    }
              } else {
                packages.minByOrNull {
                  if (it.quantity > 0) it.priceCents / it.quantity else Double.MAX_VALUE
                }
              }

          val calculatedStoreId = bestOption?.storeId ?: forcedStoreId ?: anyStoreId
          val targetStoreId =
              if (shoppingLists.containsKey(calculatedStoreId)) calculatedStoreId else anyStoreId

          var purchaseQty = baseQtyNeeded
          var price = 0L
          var displayUnit = "Units"

          if (bestOption != null && bestOption.quantity > 0) {
            val optUnit = allUnits.find { it.id == bestOption.unitId }
            if (optUnit != null) {
              val (baseOptionQty, _) =
                  UnitConverter.toStandard(bestOption.quantity, optUnit, allUnits.associateBy { it.id })
              if (baseOptionQty > 0) {
                val packs = ceil(baseQtyNeeded / baseOptionQty).toLong()
                purchaseQty = packs * bestOption.quantity
                price = packs * bestOption.priceCents.toLong()
                displayUnit = optUnit.abbreviation
              }
            }
          } else {
            val preferredUnit = ing.preferredUnitId?.let { id -> allUnits.find { it.id == id } }
            if (preferredUnit != null) {
              // We need to know which base unit baseQtyNeeded is in to convert correctly.
              // Since we don't track it per-sum, we try to convert from the base unit of the
              // preferred unit's type.
              val baseUnitId =
                  when (preferredUnit.type) {
                    UnitType.Weight -> SystemUnits.Gram.id
                    UnitType.Volume -> SystemUnits.Ml.id
                    UnitType.Count -> SystemUnits.Each.id
                    else -> null
                  }

              if (baseUnitId != null) {
                purchaseQty =
                    UnitConverter.convert(
                        baseQtyNeeded,
                        baseUnitId,
                        preferredUnit.id,
                        allUnits.associateBy { it.id },
                        allBridges,
                    ) ?: 0.0
                displayUnit = preferredUnit.abbreviation
              }
            } else {
              // Safe fallback for unit
              val countUnit = allUnits.find { it.type == UnitType.Count }
              if (countUnit != null) {
                val (_, stdUnit) = UnitConverter.toStandard(baseQtyNeeded, countUnit, allUnits.associateBy { it.id })
                displayUnit = stdUnit?.abbreviation ?: "?"
              }
            }
          }

          val item =
              ShoppingListItemUi(
                  id = ingId,
                  name = ing.name,
                  quantity = purchaseQty,
                  requiredQuantity = baseQtyNeeded,
                  unit = displayUnit,
                  priceCents = price,
                  isOwned = false,
                  isCustom = false,
                  isPantryItem = true,
                  isInCart = false, // Placeholder, filled later
              )

          val currentSection = shoppingLists[targetStoreId]
          if (currentSection != null) {
            shoppingLists[targetStoreId] =
                currentSection.copy(
                    items = currentSection.items + item,
                    subtotalCents = currentSection.subtotalCents + price,
                )
          }
        }

        customItems.forEach { custom ->
          if (!custom.isPurchased) {
            val unit = allUnits.find { it.id == custom.unitId }
            val item =
                ShoppingListItemUi(
                    id = custom.id,
                    name = custom.customName ?: "Unknown",
                    quantity = custom.neededQuantity ?: 0.0,
                    requiredQuantity = custom.neededQuantity ?: 0.0,
                    unit = unit?.abbreviation ?: "?",
                    priceCents = 0,
                    isOwned = false,
                    isCustom = true,
                    isPantryItem = custom.isPantryItem,
                    isInCart = false, // Placeholder
                )
            val section = shoppingLists[anyStoreId]
            if (section != null) {
              shoppingLists[anyStoreId] = section.copy(items = section.items + item)
            }
          }
        }

        val ownedItemsUi =
            fullyOwnedIngredients.distinct().mapNotNull { id ->
              val ing = ingredientsMap[id]
              if (ing != null) {
                ShoppingListItemUi(
                    id = id,
                    name = ing.name,
                    quantity = 0.0,
                    requiredQuantity = 0.0,
                    unit = "",
                    priceCents = 0,
                    isOwned = true,
                    isCustom = false,
                    isInCart = false,
                )
              } else null
            } +
                customItems
                    .filter { it.isPurchased }
                    .map { item ->
                      val unit = allUnits.find { it.id == item.unitId }
                      ShoppingListItemUi(
                          id = item.id,
                          name = item.customName ?: "Unknown Item",
                          quantity = item.neededQuantity ?: 0.0,
                          requiredQuantity = item.neededQuantity ?: 0.0,
                          unit = unit?.abbreviation ?: "?",
                          priceCents = 0,
                          isOwned = true,
                          isCustom = true,
                          isInCart = false,
                      )
                    }

        val finalSections =
            shoppingLists.values
                .filter { it.items.isNotEmpty() }
                .map { section ->
                  val tax = (section.subtotalCents * taxRate).toLong()
                  section.copy(taxCents = tax, totalCents = section.subtotalCents + tax)
                }
                .sortedBy { if (it.storeName == "Other / Custom") "ZZZ" else it.storeName }

        CalculatedList(
            finalSections,
            ownedItemsUi,
            allStores,
            allUnits,
            taxRate,
            activeWarnings.distinctBy { it.message },
        )
      }

  // Merge Calculation with Cart Status (Fast)
  val uiState =
      combine(calculatedListFlow, _inCartItems) { list, inCartItems ->
            // Update Sections
            val updatedSections =
                list.sections.map { section ->
                  section.copy(
                      items =
                          section.items.map { item ->
                            item.copy(isInCart = inCartItems.contains(item.id))
                          }
                  )
                }

            // Update Owned Items
            val updatedOwned =
                list.ownedItems.map { item -> item.copy(isInCart = inCartItems.contains(item.id)) }

            ShoppingListUiState(
                updatedSections,
                updatedOwned,
                list.allStores,
                list.allUnits,
                list.taxRate,
                list.globalWarnings,
            )
          }
          .flowOn(kotlinx.coroutines.Dispatchers.Default)
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5000),
              ShoppingListUiState(emptyList(), emptyList(), emptyList(), emptyList(), 0.0),
          )

  // ... ensureNeeded ...
  private suspend fun ensureNeeded(ingredientId: Uuid) {
    val entries = mealPlanRepository.entries.value
    val meals = mealRepository.meals.value
    val recipes = recipeRepository.recipes.value
    val allUnits = unitRepository.units.value
    val allIngredients = ingredientRepository.ingredients.value
    val allBridges = ingredientRepository.bridges.value

    val ing = allIngredients.find { it.id == ingredientId } ?: return

    val reqByType = mutableMapOf<UnitType, Double>()

    fun addReq(qty: Double, unit: UnitModel) {
      val (baseQty, _) = UnitConverter.toStandard(qty, unit, allUnits.associateBy { it.id })
      reqByType[unit.type] = (reqByType[unit.type] ?: 0.0) + baseQty
    }

    entries.forEach { entry: ScheduledMeal ->
      val meal = entry.prePlannedMealId?.let { id -> meals.find { it.id == id } }
      if (meal != null) {
        // Recipes
        meal.recipes.forEach { rId ->
          val recipe = recipes.find { it.id == rId }
          if (recipe != null) {
            val scale = if (recipe.servings > 0) entry.peopleCount / recipe.servings else 1.0
            recipe.ingredients
                .filter { it.ingredientId == ingredientId }
                .forEach { ri ->
                  val unit = allUnits.find { it.id == ri.unitId }
                  if (unit != null) {
                    addReq(ri.quantity * scale, unit)
                  }
                }
          }
        }
        // Independent
        meal.independentIngredients
            .filter { it.ingredientId == ingredientId }
            .forEach { item ->
              val unit = allUnits.find { it.id == item.unitId }
              if (unit != null) {
                addReq(item.quantity * entry.peopleCount, unit)
              }
            }
      }
    }

    // Consolidate to preferred unit type if possible
    val preferredType = ing.preferredUnitId?.let { pid -> allUnits.find { it.id == pid }?.type }
    val targetType = preferredType ?: reqByType.maxByOrNull { it.value }?.key ?: UnitType.Count
    val targetBaseUnitId =
        when (targetType) {
          UnitType.Weight -> SystemUnits.Gram.id
          UnitType.Volume -> SystemUnits.Ml.id
          UnitType.Count -> SystemUnits.Each.id
          else -> null
        }

    var totalReqInTargetBase = 0.0
    reqByType.forEach { (type, baseQty) ->
      if (type == targetType) {
        totalReqInTargetBase += baseQty
      } else {
        val fromBaseUnitId =
            when (type) {
              UnitType.Weight -> SystemUnits.Gram.id
              UnitType.Volume -> SystemUnits.Ml.id
              UnitType.Count -> SystemUnits.Each.id
              else -> null
            }
        totalReqInTargetBase +=
            UnitConverter.convert(baseQty, fromBaseUnitId, targetBaseUnitId, allUnits.associateBy { it.id }, allBridges) ?: 0.0
      }
    }

    val currentPantryItem =
        pantryRepository.pantryItems.value.find { it.ingredientId == ingredientId }
    if (currentPantryItem != null) {
      val pUnit = allUnits.find { it.id == currentPantryItem.unitId }
      if (pUnit != null) {
        val ownedInTargetBase =
            UnitConverter.convert(
                currentPantryItem.quantity,
                pUnit.id,
                targetBaseUnitId,
                allUnits.associateBy { it.id },
                allBridges,
            ) ?: 0.0
        val newBase = max(0.0, ownedInTargetBase - totalReqInTargetBase)

        // Convert newBase (which is in targetBaseUnit) back to standard base for update
        // (Though targetBase IS the standard base for that type)
        if (targetBaseUnitId != null) {
          pantryRepository.updateQuantity(ingredientId, newBase, targetBaseUnitId)
        }
      }
    }
  }

  fun toggleCart(id: Uuid) {
    _inCartItems.update { current -> if (current.contains(id)) current - id else current + id }
  }

  private val _showReceiptDialog = MutableStateFlow(false)
  val showReceiptDialog = _showReceiptDialog.asStateFlow()

  private val _showDiscrepancyDialog = MutableStateFlow(false)
  val showDiscrepancyDialog = _showDiscrepancyDialog.asStateFlow()

  private val _pendingActualTotal = MutableStateFlow<Long?>(null)
  val pendingActualTotal = _pendingActualTotal.asStateFlow()

  private var _pendingTime: LocalTime? = null
  private var _pendingStoreId: Uuid? = null

  fun openReceiptDialog(storeId: Uuid? = null) {
    _pendingStoreId = storeId
    _showReceiptDialog.value = true
  }

  fun dismissReceiptDialog() {
    _showReceiptDialog.value = false
    _pendingStoreId = null
  }

  fun submitReceiptTotal(
      actualTotalCents: Long,
      time: LocalTime?,
      forcePriceReview: Boolean = false,
  ) {
    _showReceiptDialog.value = false
    _pendingTime = time

    val currentState = uiState.value
    val cartItems =
        if (_pendingStoreId == null) {
          currentState.sections.flatMap { it.items }.filter { it.isInCart }
        } else {
          currentState.sections
              .find { it.storeId == _pendingStoreId }
              ?.items
              ?.filter { it.isInCart } ?: emptyList()
        }

    val projectedTotal = cartItems.sumOf { it.priceCents }
    val tax = (projectedTotal * currentState.taxRate).toLong()
    val projectedWithTax = projectedTotal + tax

    val isDifferent = actualTotalCents != projectedWithTax

    if (forcePriceReview || isDifferent) {
      _pendingActualTotal.value = actualTotalCents
      _showDiscrepancyDialog.value = true
    } else {
      val transaction = createShoppingTransaction(_pendingStoreId)
      commitTransaction(transaction, actualTotalCents, _pendingTime, _pendingStoreId)
      _pendingTime = null
      _pendingStoreId = null
    }
  }

  fun updatePricesAndFinalize(updates: List<PriceUpdate>) {
    updates.forEach { update -> updatePrice(update.ingredientId, update.newPriceCents) }

    _showDiscrepancyDialog.value = false
    _pendingActualTotal.value?.let { 
        val transaction = createShoppingTransaction(_pendingStoreId)
        commitTransaction(transaction, it, _pendingTime, _pendingStoreId)
    }
    _pendingActualTotal.value = null
    _pendingTime = null
    _pendingStoreId = null
  }

  fun skipPriceUpdate() {
    _showDiscrepancyDialog.value = false
    _pendingActualTotal.value?.let { 
        val transaction = createShoppingTransaction(_pendingStoreId)
        commitTransaction(transaction, it, _pendingTime, _pendingStoreId)
    }
    _pendingActualTotal.value = null
    _pendingTime = null
    _pendingStoreId = null
  }

  fun updatePrice(ingredientId: Uuid, newPriceCents: Int) {
    viewModelScope.launch {
      val packages = ingredientRepository.packages.value
      val pkg = packages.filter { it.ingredientId == ingredientId }.minByOrNull { it.priceCents }

      if (pkg != null) {
        ingredientRepository.updatePackage(pkg.copy(priceCents = newPriceCents))
      }
    }
  }

  fun markOwned(item: ShoppingListItemUi) {
    viewModelScope.launch {
      if (item.isCustom) {
        shoppingListItemRepository.toggleItem(item.id)
      } else {
        val allUnits = unitRepository.units.value
        val unit = allUnits.find { it.abbreviation == item.unit }
        if (unit != null) {
          val (baseQtyToAdd, _) = UnitConverter.toStandard(item.quantity, unit, allUnits.associateBy { it.id })

          val currentPantryItem =
              pantryRepository.pantryItems.value.find { it.ingredientId == item.id }
          val currentBaseQty =
              if (currentPantryItem != null) {
                val pUnit = allUnits.find { it.id == currentPantryItem.unitId }
                if (pUnit != null)
                    UnitConverter.toStandard(currentPantryItem.quantity, pUnit, allUnits.associateBy { it.id }).first
                else 0.0
              } else 0.0

          val newQty = currentBaseQty + baseQtyToAdd

          val (_, stdUnit) = UnitConverter.toStandard(0.0, unit, allUnits.associateBy { it.id })
          if (stdUnit != null) {
            pantryRepository.updateQuantity(item.id, newQty, stdUnit.id)
          }
        }
      }
    }
  }

  fun markUnowned(item: ShoppingListItemUi) {
    viewModelScope.launch {
      if (item.isCustom) {
        shoppingListItemRepository.toggleItem(item.id)
      } else {
        ensureNeeded(item.id)
      }
    }
  }

  fun moveToStore(ingredientId: Uuid, storeId: Uuid) {
    shoppingListRepository.setStoreOverride(ingredientId, storeId)
    viewModelScope.launch { ensureNeeded(ingredientId) }
  }

  fun addCustomItem(name: String, qty: Double, unitId: Uuid, isPantry: Boolean = true) {
    viewModelScope.launch {
      shoppingListItemRepository.addItem(
          ShoppingListItem(
              customName = name,
              neededQuantity = qty,
              unitId = unitId,
              storeId = Uuid.parse("00000000-0000-0000-0000-000000000000"), // Default Any Store
              isPurchased = false,
              isPantryItem = isPantry,
          )
      )
    }
  }

  fun createShoppingTransaction(storeId: Uuid?): KitchenTransaction {
    val currentState = uiState.value
    val cartItems =
        if (storeId == null) {
          currentState.sections.flatMap { it.items }.filter { it.isInCart }
        } else {
          currentState.sections
              .find { it.storeId == storeId }
              ?.items
              ?.filter { it.isInCart } ?: emptyList()
        }

    val storeName = if (storeId != null) {
        currentState.allStores.find { it.id == storeId }?.name ?: "Unknown Store"
    } else "Mixed Stores"

    val changes = cartItems.map { item ->
        val unit = currentState.allUnits.find { it.abbreviation == item.unit }
        InventoryChange(
            ingredientId = item.id,
            ingredientName = item.name,
            quantity = item.quantity,
            unitId = unit?.id,
            unitAbbreviation = item.unit,
            direction = TransactionDirection.IN,
            priceCents = item.priceCents,
            isPantryItem = item.isPantryItem
        )
    }

    return KitchenTransaction(
        type = TransactionType.Acquisition,
        title = "Shopping Trip: $storeName",
        changes = changes
    )
  }

  fun commitTransaction(
      transaction: KitchenTransaction,
      actualTotalCents: Long? = null,
      time: LocalTime? = null,
      storeId: Uuid? = null,
  ) {
    viewModelScope.launch {
      val currentState = uiState.value
      val allUnits = currentState.allUnits
      val finalTime = time ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

      // 1. Capture Trip Data
      val subtotalCents = transaction.changes.sumOf { it.priceCents ?: 0L }
      val taxCents = (subtotalCents * currentState.taxRate).toLong()
      val totalPaidCents = actualTotalCents ?: (subtotalCents + taxCents)
      val actualTaxCents = max(0L, totalPaidCents - subtotalCents)

      val dominantStoreId = storeId ?: currentState.sections.maxByOrNull { it.items.count { i -> i.isInCart } }?.storeId

      val trip = ReceiptHistory(
          date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
          time = finalTime,
          storeId = dominantStoreId,
          projectedTotalCents = subtotalCents.toInt(),
          actualTotalCents = totalPaidCents.toInt(),
          taxPaidCents = actualTaxCents.toInt(),
      )
      receiptHistoryRepository.addTrip(trip)

      val allIngredients = ingredientRepository.ingredients.value

      // 2. Process Changes
      transaction.changes.forEach { change ->
        val isCustom = uiState.value.ownedItems.any { it.id == change.ingredientId && it.isCustom } ||
                       uiState.value.sections.flatMap { it.items }.any { it.id == change.ingredientId && it.isCustom }
        
        if (change.isPantryItem) {
            val unit = allUnits.find { it.id == change.unitId }
            if (unit != null) {
              val ingredient = allIngredients.find { it.id == change.ingredientId }
              
              // Target unit: preferredUnitId, otherwise base unit of the change's type
              val preferredUnitId = ingredient?.preferredUnitId
              val targetUnitId = preferredUnitId ?: when (unit.type) {
                  UnitType.Weight -> SystemUnits.Gram.id
                  UnitType.Volume -> SystemUnits.Ml.id
                  UnitType.Count -> SystemUnits.Each.id
                  else -> unit.id
              }
              
              val targetUnit = allUnits.find { it.id == targetUnitId }
              
              if (targetUnit != null) {
                  val allBridges = ingredientRepository.bridges.value
                  val qtyInTargetUnit = UnitConverter.convert(change.quantity, unit.id, targetUnitId, allUnits.associateBy { it.id }, allBridges) ?: 0.0
                  
                  val currentPantryItem = pantryRepository.pantryItems.value.find { it.ingredientId == change.ingredientId }
                  val currentQtyInTargetUnit = if (currentPantryItem != null) {
                      UnitConverter.convert(currentPantryItem.quantity, currentPantryItem.unitId, targetUnitId, allUnits.associateBy { it.id }, allBridges) ?: 0.0
                  } else 0.0
                  
                  val newQty = currentQtyInTargetUnit + qtyInTargetUnit
                  pantryRepository.updateQuantity(change.ingredientId, newQty, targetUnitId)
              }
            }
        }

        if (isCustom) {
            shoppingListItemRepository.toggleItem(change.ingredientId)
        }
      }

      // 3. Clear Cart
      _inCartItems.value = emptySet()
    }
  }
}

data class ShoppingListUiState(
    val sections: List<ShoppingListSection>,
    val ownedItems: List<ShoppingListItemUi>,
    val allStores: List<Store>,
    val allUnits: List<UnitModel>,
    val taxRate: Double = 0.0,
    val warnings: List<DataWarning> = emptyList(),
)

data class ShoppingListSection(
    val storeId: Uuid,
    val storeName: String,
    val items: List<ShoppingListItemUi>,
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val warnings: List<DataWarning> = emptyList(),
)

data class ShoppingListItemUi(
    val id: Uuid,
    val name: String,
    val quantity: Double,
    val requiredQuantity: Double,
    val unit: String,
    val priceCents: Long,
    val isOwned: Boolean,
    val isCustom: Boolean,
    val isPantryItem: Boolean = true,
    val isInCart: Boolean = false,
)
