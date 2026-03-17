package io.github.and19081.mealplanner.feature.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.core.util.DataQualityValidator
import io.github.and19081.mealplanner.core.util.DataWarning
import io.github.and19081.mealplanner.core.util.SystemUnits
import io.github.and19081.mealplanner.core.util.UnitConverter
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.core.util.UnitType
import io.github.and19081.mealplanner.domain.model.BridgeConversion
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.Package
import io.github.and19081.mealplanner.domain.model.Store
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.repository.MealPlanRepository
import io.github.and19081.mealplanner.domain.repository.PantryRepository
import io.github.and19081.mealplanner.domain.repository.ReceiptHistoryRepository
import io.github.and19081.mealplanner.domain.repository.SettingsRepository
import io.github.and19081.mealplanner.domain.repository.ShoppingListItemRepository
import io.github.and19081.mealplanner.domain.repository.ShoppingListOverride
import io.github.and19081.mealplanner.domain.repository.ShoppingListRepository
import io.github.and19081.mealplanner.domain.repository.StoreRepository
import io.github.and19081.mealplanner.feature.kitchen.InventoryChange
import io.github.and19081.mealplanner.feature.kitchen.KitchenTransaction
import io.github.and19081.mealplanner.feature.kitchen.TransactionDirection
import io.github.and19081.mealplanner.feature.kitchen.TransactionType
import io.github.and19081.mealplanner.feature.meals.PantryItem
import io.github.and19081.mealplanner.feature.meals.PriceUpdate
import io.github.and19081.mealplanner.feature.meals.ScheduledMeal
import io.github.and19081.mealplanner.feature.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlin.math.ceil
import kotlin.math.max
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.collections.get
import kotlin.time.Clock

class ShoppingListViewModel(
    private val foodItemRepository: FoodItemRepository,
    private val storeRepository: StoreRepository,
    private val unitRepository: UnitRepository,
    private val settingsRepository: SettingsRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val pantryRepository: PantryRepository,
    private val shoppingListItemRepository: ShoppingListItemRepository,
    private val receiptHistoryRepository: ReceiptHistoryRepository,
) : ViewModel() {

  private val _inCartItems = MutableStateFlow<Set<Uuid>>(emptySet())
  val inCartItems = _inCartItems.asStateFlow()

  private val _activeStoreId = MutableStateFlow<Uuid?>(null)
  val activeStoreId = _activeStoreId.asStateFlow()

  private val _showReceiptDialog = MutableStateFlow(false)
  val showReceiptDialog = _showReceiptDialog.asStateFlow()

  private val _showDiscrepancyDialog = MutableStateFlow(false)
  val showDiscrepancyDialog = _showDiscrepancyDialog.asStateFlow()

  private val _pendingActualTotal = MutableStateFlow<Long?>(null)
  val pendingActualTotal = _pendingActualTotal.asStateFlow()

  data class CoreData(
      val allItems: List<FoodItem>,
      val allStores: List<Store>,
      val allPackages: List<Package>,
      val allBridges: List<BridgeConversion>,
      val allUnits: List<UnitModel>,
      val taxRate: Double,
  )

  data class UserData(
      val entries: List<ScheduledMeal>,
      val overrides: Map<Uuid, ShoppingListOverride>,
      val pantryItems: List<PantryItem>,
      val customItems: List<ShoppingListItem>,
  )

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
          foodItemRepository.foodItems,
          storeRepository.stores,
          foodItemRepository.packages,
          foodItemRepository.conversions,
          unitRepository.units,
          settingsRepository.appSettings,
      ) { args: Array<Any?> ->
        CoreData(
            allItems = args[0] as List<FoodItem>,
            allStores = args[1] as List<Store>,
            allPackages = args[2] as List<Package>,
            allBridges = args[3] as List<BridgeConversion>,
            allUnits = args[4] as List<UnitModel>,
            taxRate = (args[5] as? AppSettings)?.defaultTaxRatePercentage ?: 0.0,
        )
      }

  private val userDataFlow =
      combine(
          mealPlanRepository.entries,
          shoppingListRepository.overrides,
          pantryRepository.pantryItems,
          shoppingListItemRepository.items,
      ) { args: Array<Any?> ->
        UserData(
            entries = args[0] as List<ScheduledMeal>,
            overrides = args[1] as Map<Uuid, ShoppingListOverride>,
            pantryItems = args[2] as List<PantryItem>,
            customItems = args[3] as List<ShoppingListItem>,
        )
      }

  private val calculatedListFlow =
      combine(coreDataFlow, userDataFlow) { core, user ->
        val (allItems, allStores, allPackages, allBridges, allUnits, taxRate) = core
        val (entries, overrides, pantryItems, customItems) = user

        val itemsMap = allItems.associateBy { it.id }
        val unitsMap = allUnits.associateBy { it.id }

        val grossRequirementsByType = mutableMapOf<Uuid, MutableMap<UnitType, Double>>()
        val activeWarnings = mutableListOf<DataWarning>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        fun addRecursive(itemId: Uuid, qty: Double, unitId: Uuid?, multiplier: Double) {
          val item = itemsMap[itemId] ?: return
          
          if (item.isRecipe) {
              val recipeInfo = item.recipeInfo!!
              val scale = if (recipeInfo.servings > 0) (qty * multiplier) / recipeInfo.servings else 1.0
              recipeInfo.requirements.forEach { req ->
                  addRecursive(req.foodItemId, req.quantity, req.unitId, scale)
              }
          } else {
              val unit = unitsMap[unitId ?: item.preferredUnitId] ?: return
              val (baseQty, _) = UnitConverter.toStandard(qty * multiplier, unit, unitsMap)
              val types = grossRequirementsByType.getOrPut(itemId) { mutableMapOf() }
              types[unit.type] = (types[unit.type] ?: 0.0) + baseQty
          }
        }

        entries.filter { !it.isConsumed && it.date >= today }.forEach { entry ->
          val rootItem = entry.prePlannedMealId?.let { itemsMap[it] }
          if (rootItem != null) {
            activeWarnings.addAll(
                DataQualityValidator.validateFoodItem(
                    rootItem,
                    itemsMap,
                    allPackages,
                    allBridges,
                    allUnits,
                )
            )

            addRecursive(rootItem.id, entry.peopleCount.toDouble(), null, 1.0)
          }
        }

        val remainingCustomTextItems = mutableListOf<ShoppingListItem>()

        customItems.filter { !it.isPurchased }.forEach { customItem ->
            val linkedFoodItem = itemsMap[customItem.foodItemId]
            
            if (linkedFoodItem != null) {
                // SCENARIO A: It's a real item from the database
                if (linkedFoodItem.isRecipe) {
                    // It's an ad-hoc recipe! Run it through the recursive engine.
                    val batches = customItem.neededQuantity ?: 1.0
                    val defaultServings = linkedFoodItem.recipeInfo?.servings ?: 1.0
                    addRecursive(linkedFoodItem.id, defaultServings, null, batches)
                } else {
                    // It's an ad-hoc raw ingredient! Add it directly to the math pile.
                    val unit = unitsMap[customItem.unitId ?: linkedFoodItem.preferredUnitId]
                    if (unit != null) {
                        val (baseQty, _) = UnitConverter.toStandard(customItem.neededQuantity ?: 1.0, unit, unitsMap)
                        val types = grossRequirementsByType.getOrPut(linkedFoodItem.id) { mutableMapOf() }
                        types[unit.type] = (types[unit.type] ?: 0.0) + baseQty
                    }
                }
            } else {
                // SCENARIO B: It's a true custom text item (e.g., "Trash Bags")
                remainingCustomTextItems.add(customItem)
            }
        }

        val grossRequirements = mutableMapOf<Uuid, Double>()
        grossRequirementsByType.forEach { (itemId, types) ->
          if (types.isEmpty()) return@forEach
          val item = itemsMap[itemId] ?: return@forEach

          val preferredType = item.preferredUnitId?.let { pid -> unitsMap[pid]?.type }
          val targetType = preferredType ?: types.maxBy { it.value }.key

          var totalInTargetBase = 0.0
          val targetBaseUnitId =
              when (targetType) {
                UnitType.Mass -> SystemUnits.Gram.id
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
                    UnitType.Mass -> SystemUnits.Gram.id
                    UnitType.Volume -> SystemUnits.Ml.id
                    UnitType.Count -> SystemUnits.Each.id
                    else -> null
                  }

              val converted =
                  UnitConverter.convert(
                      baseQty,
                      fromBaseUnitId ?: Uuid.NIL,
                      targetBaseUnitId,
                      unitsMap,
                      allBridges,
                  ) ?: 0.0
              totalInTargetBase += converted
            }
          }
          grossRequirements[itemId] = totalInTargetBase
        }

        val netRequirements = mutableMapOf<Uuid, Double>()
        val fullyOwnedItems = mutableListOf<Uuid>()

        grossRequirements.forEach { (itemId, reqQty) ->
          val item = itemsMap[itemId] ?: return@forEach
          val pantryItem = pantryItems.find { it.foodItemId == itemId }

          val preferredType = item.preferredUnitId?.let { pid -> unitsMap[pid]?.type }
          val grossRequirementsTargetType = preferredType ?: (grossRequirementsByType[itemId]?.maxBy { it.value }?.key ?: UnitType.Count)

          val ownedQty =
              if (pantryItem != null) {
                val pUnit = unitsMap[pantryItem.unitId]
                if (pUnit != null) {
                    val targetBaseUnitId = when (grossRequirementsTargetType) {
                        UnitType.Mass -> SystemUnits.Gram.id
                        UnitType.Volume -> SystemUnits.Ml.id
                        UnitType.Count -> SystemUnits.Each.id
                        else -> null
                    }
                    UnitConverter.convert(pantryItem.quantity, pUnit.id, targetBaseUnitId, unitsMap, allBridges) ?: 0.0
                }
                else 0.0
              } else 0.0

          val needed = max(0.0, reqQty - ownedQty)
          if (needed > 0.001) {
            netRequirements[itemId] = needed
          } else {
            fullyOwnedItems.add(itemId)
          }
        }

        val shoppingLists = mutableMapOf<Uuid, ShoppingListSection>()

        allStores.forEach { store ->
          shoppingLists[store.id] = ShoppingListSection(store.id, store.name, emptyList(), 0, 0, 0)
        }
        val anyStoreId = Uuid.parse("00000000-0000-0000-0000-000000000000")
        if (!shoppingLists.containsKey(anyStoreId)) {
          shoppingLists[anyStoreId] =
              ShoppingListSection(anyStoreId, "Other / Custom", emptyList(), 0, 0, 0)
        }

        netRequirements.forEach { (itemId, baseQtyNeeded) ->
          val item = itemsMap[itemId] ?: return@forEach
          val override = overrides[itemId]

          if (override?.inPantry == true) {
            fullyOwnedItems.add(itemId)
            return@forEach
          }

          val packages = allPackages.filter { it.foodItemId == itemId }

          val forcedStoreId = override?.forceStoreId
          val bestOption =
              if (forcedStoreId != null) {
                packages.find { it.storeId == forcedStoreId }
                    ?: packages.minByOrNull {
                      if (it.quantity > 0) it.priceCents.toDouble() / it.quantity else Double.MAX_VALUE
                    }
              } else {
                packages.minByOrNull {
                  if (it.quantity > 0) it.priceCents.toDouble() / it.quantity else Double.MAX_VALUE
                }
              }

          val calculatedStoreId = bestOption?.storeId ?: forcedStoreId ?: anyStoreId
          val targetStoreId =
              if (shoppingLists.containsKey(calculatedStoreId)) calculatedStoreId else anyStoreId

          var purchaseQty = baseQtyNeeded
          var price = 0L
          var displayUnit = "Units"

          if (bestOption != null && bestOption.quantity > 0) {
            val optUnit = unitsMap[bestOption.unitId]
            if (optUnit != null) {
              val (baseOptionQty, _) = UnitConverter.toStandard(bestOption.quantity, optUnit, unitsMap)
              if (baseOptionQty > 0) {
                val packs = ceil(baseQtyNeeded / baseOptionQty).toLong()
                purchaseQty = packs * bestOption.quantity
                price = packs * bestOption.priceCents.toLong()
                displayUnit = optUnit.abbreviation
              }
            }
          } else {
            val preferredUnit = item.preferredUnitId?.let { id -> unitsMap[id] }
            if (preferredUnit != null) {
              val baseUnitId = when (preferredUnit.type) {
                    UnitType.Mass -> SystemUnits.Gram.id
                    UnitType.Volume -> SystemUnits.Ml.id
                    UnitType.Count -> SystemUnits.Each.id
                    else -> null
              }

              if (baseUnitId != null) {
                purchaseQty = UnitConverter.convert(baseQtyNeeded, baseUnitId, preferredUnit.id, unitsMap, allBridges) ?: 0.0
                displayUnit = preferredUnit.abbreviation
              }
            } else {
              val countUnit = allUnits.find { it.type == UnitType.Count }
              if (countUnit != null) {
                val converted = UnitConverter.convert(baseQtyNeeded, SystemUnits.Each.id, countUnit.id, unitsMap, allBridges) ?: baseQtyNeeded
                purchaseQty = converted
                displayUnit = countUnit.abbreviation
              }
            }
          }

          val shoppingItem =
              ShoppingListItemUi(
                  id = itemId,
                  name = item.name,
                  quantity = purchaseQty,
                  requiredQuantity = baseQtyNeeded,
                  unit = displayUnit,
                  priceCents = price,
                  isOwned = false,
                  isCustom = false,
                  isPantryItem = true,
                  isInCart = false,
              )

          val currentSection = shoppingLists[targetStoreId]
          if (currentSection != null) {
            shoppingLists[targetStoreId] =
                currentSection.copy(
                    items = currentSection.items + shoppingItem,
                    subtotalCents = currentSection.subtotalCents + price,
                )
          }
        }

        remainingCustomTextItems.forEach { custom ->
            val unit = unitsMap[custom.unitId]
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
                    isInCart = false,
                )
            val section = shoppingLists[anyStoreId]
            if (section != null) {
              shoppingLists[anyStoreId] = section.copy(items = section.items + item)
            }
        }

        val ownedItemsUi =
            fullyOwnedItems.distinct().mapNotNull { id ->
              val item = itemsMap[id]
              if (item != null) {
                ShoppingListItemUi(
                    id = id,
                    name = item.name,
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
                    .filter { it.isPurchased && itemsMap[it.foodItemId] == null }
                    .map { item ->
                      val unit = unitsMap[item.unitId]
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

  val uiState =
      combine(calculatedListFlow, _inCartItems) { list, inCartItems ->
            val updatedSections =
                list.sections.map { section ->
                  section.copy(
                      items =
                          section.items.map { item ->
                            item.copy(isInCart = inCartItems.contains(item.id))
                          }
                  )
                }

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
          .flowOn(Dispatchers.Default)
          .stateIn(
              viewModelScope,
              SharingStarted.WhileSubscribed(5000),
              ShoppingListUiState(emptyList(), emptyList(), emptyList(), emptyList(), 0.0),
          )

  fun toggleCart(id: Uuid) {
    _inCartItems.update { current -> if (current.contains(id)) current - id else current + id }
  }

  fun dismissReceiptDialog() {
    _showReceiptDialog.value = false
  }

  fun skipPriceUpdate() {
    _showDiscrepancyDialog.value = false
    _pendingActualTotal.value = null
  }

  fun createShoppingTransaction(storeId: Uuid?): KitchenTransaction {
    _activeStoreId.value = storeId
    val itemsInCart = uiState.value.sections
        .find { it.storeId == storeId }
        ?.items
        ?.filter { it.isInCart && !it.isCustom } ?: emptyList()
    
    val changes = itemsInCart.map { item ->
        val unit = uiState.value.allUnits.find { it.abbreviation == item.unit }
        InventoryChange(
            foodItemId = item.id,
            ingredientName = item.name,
            quantity = item.quantity,
            unitId = unit?.id ?: Uuid.NIL,
            unitAbbreviation = item.unit,
            direction = TransactionDirection.IN
        )
    }

    return KitchenTransaction(
        type = TransactionType.Acquisition,
        title = "Shopping Trip",
        changes = changes
    )
  }

  fun commitTransaction(transaction: KitchenTransaction) {
    viewModelScope.launch {
      val allUnits = unitRepository.units.value
      val pantryItems = pantryRepository.pantryItems.value

      transaction.changes.forEach { change ->
        val currentPantryItem = pantryItems.find { it.foodItemId == change.foodItemId }
        val currentQty = currentPantryItem?.quantity ?: 0.0
        val currentUnitId = currentPantryItem?.unitId ?: change.unitId

        if (currentUnitId != null) {
            val convertedChangeQty = UnitConverter.convert(
                amount = change.quantity ?: 0.0,
                fromUnitId = change.unitId ?: Uuid.NIL,
                toUnitId = currentUnitId,
                allUnits = allUnits.associateBy { it.id }
            ) ?: 0.0

            val newQty = currentQty + convertedChangeQty
            pantryRepository.updateQuantity(change.foodItemId, newQty, currentUnitId)
        }
      }

      // Mark custom items that were in the cart as purchased
      val customItemsInCart = uiState.value.sections
          .flatMap { it.items }
          .filter { it.isInCart && it.isCustom }

      customItemsInCart.forEach { customItem ->
          shoppingListItemRepository.toggleItem(customItem.id)
      }

      val processedIds = transaction.changes.map { it.foodItemId }.toSet() + customItemsInCart.map { it.id }.toSet()
      _inCartItems.update { it - processedIds }
    }
  }

  fun submitReceiptTotal(
      actualTotalCents: Long,
      time: LocalTime,
      forcePriceReview: Boolean,
  ) {
    if (forcePriceReview) {
        _pendingActualTotal.value = actualTotalCents
        _showDiscrepancyDialog.value = true
        _showReceiptDialog.value = false
    } else {
        _showReceiptDialog.value = false
        saveShoppingReceipt(_activeStoreId.value, actualTotalCents)
        _activeStoreId.value = null
    }
  }

  fun updatePricesAndFinalize(priceUpdates: List<PriceUpdate>) {
    viewModelScope.launch {
        val storeId = _activeStoreId.value
        priceUpdates.forEach { update ->
            if (storeId != null) {
                updatePrice(update.foodItemId, storeId, update.priceCents)
            } else {
                updatePrice(update.foodItemId, update.priceCents)
            }
        }
        saveShoppingReceipt(storeId, _pendingActualTotal.value ?: 0L)
        _showDiscrepancyDialog.value = false
        _pendingActualTotal.value = null
        _activeStoreId.value = null
    }
  }

  fun updatePrice(foodItemId: Uuid, newPriceCents: Int) {
    viewModelScope.launch {
      val packages = foodItemRepository.packages.value
      val pkg = packages.filter { it.foodItemId == foodItemId }.minByOrNull { it.priceCents }

      if (pkg != null) {
        foodItemRepository.savePackage(pkg.copy(priceCents = newPriceCents))
      }
    }
  }

  fun updatePrice(foodItemId: Uuid, storeId: Uuid, newPriceCents: Int) {
      viewModelScope.launch {
          val packages = foodItemRepository.packages.value
          val pkg = packages.find { it.foodItemId == foodItemId && it.storeId == storeId }

          if (pkg != null) {
              foodItemRepository.savePackage(pkg.copy(priceCents = newPriceCents))
          }
      }
  }

  private fun saveShoppingReceipt(storeId: Uuid?, actualTotalCents: Long) {
      viewModelScope.launch {
          val section = uiState.value.sections.find { it.storeId == storeId } ?: return@launch
          val itemsInCart = section.items.filter { it.isInCart }

          val receiptId = Uuid.random()
          val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
          val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

          val lineItems = itemsInCart.map { item ->
              ReceiptLineItem(
                  receiptId = receiptId,
                  foodItemId = if (item.isCustom) null else item.id,
                  customName = if (item.isCustom) item.name else null,
                  quantityBought = item.quantity,
                  pricePaidCents = item.priceCents.toInt(),
                  unitId = uiState.value.allUnits.find { it.abbreviation == item.unit }?.id
              )
          }

          val receipt = ReceiptHistory(
              id = receiptId,
              date = today,
              time = now,
              storeId = storeId,
              projectedTotalCents = section.totalCents.toInt(),
              actualTotalCents = actualTotalCents.toInt(),
              taxPaidCents = section.taxCents.toInt(),
              lineItems = lineItems
          )

          receiptHistoryRepository.addTrip(receipt)
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
          val converted = UnitConverter.convert(item.quantity, unit.id, unit.id, allUnits.associateBy { it.id }) ?: item.quantity
          val (baseQtyToAdd, _) = UnitConverter.toStandard(converted, unit, allUnits.associateBy { it.id })

          val currentPantryItem =
              pantryRepository.pantryItems.value.find { it.foodItemId == item.id }
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
      }
    }
  }

  fun moveToStore(foodItemId: Uuid, storeId: Uuid) {
    shoppingListRepository.setStoreOverride(foodItemId, storeId)
  }

  fun addCustomItem(name: String, qty: Double, unitId: Uuid, isPantry: Boolean = true) {
    viewModelScope.launch {
      shoppingListItemRepository.addItem(
          ShoppingListItem(
              customName = name,
              neededQuantity = qty,
              unitId = unitId,
              storeId = Uuid.parse("00000000-0000-0000-0000-000000000000"),
              isPurchased = false,
              isPantryItem = isPantry,
          )
      )
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
