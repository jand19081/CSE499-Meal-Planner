package io.github.and19081.mealplanner.feature.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.and19081.mealplanner.core.util.UnitModel
import io.github.and19081.mealplanner.core.util.UnitRepository
import io.github.and19081.mealplanner.domain.model.FoodItem
import io.github.and19081.mealplanner.domain.model.Store
import io.github.and19081.mealplanner.domain.model.isIngredient
import io.github.and19081.mealplanner.domain.repository.FoodItemRepository
import io.github.and19081.mealplanner.domain.repository.ReceiptHistoryRepository
import io.github.and19081.mealplanner.domain.repository.RestaurantRepository
import io.github.and19081.mealplanner.domain.repository.StoreRepository
import io.github.and19081.mealplanner.feature.meals.Restaurant
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptHistory
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReceiptsViewModel(
    private val receiptHistoryRepository: ReceiptHistoryRepository,
    private val foodItemRepository: FoodItemRepository,
    private val storeRepository: StoreRepository,
    private val restaurantRepository: RestaurantRepository,
    private val unitRepository: UnitRepository,
) : ViewModel() {

  data class ReceiptsUiState(
      val receipts: List<ReceiptHistory> = emptyList(),
      val allStores: List<Store> = emptyList(),
      val allRestaurants: List<Restaurant> = emptyList(),
      val allIngredients: List<FoodItem> = emptyList(),
      val allUnits: List<UnitModel> = emptyList(),
      val isLoading: Boolean = true,
  )

  val uiState: StateFlow<ReceiptsUiState> =
      combine(
              receiptHistoryRepository.trips,
              storeRepository.stores,
              restaurantRepository.restaurants,
              foodItemRepository.foodItems,
              unitRepository.units,
          ) { trips, stores, restaurants, items, units ->
            ReceiptsUiState(
                receipts = trips.sortedByDescending { it.date },
                allStores = stores,
                allRestaurants = restaurants,
                allIngredients = items.filter { it.isIngredient() },
                allUnits = units,
                isLoading = false,
            )
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReceiptsUiState())

  suspend fun getTripDetails(id: Uuid): ReceiptHistory? {
    return receiptHistoryRepository.getTripWithLineItems(id)
  }

  fun addReceipt(receipt: ReceiptHistory) {
    viewModelScope.launch { receiptHistoryRepository.addTrip(receipt) }
  }

  fun updateReceipt(receipt: ReceiptHistory) {
    viewModelScope.launch { receiptHistoryRepository.updateTrip(receipt) }
  }

  fun deleteReceipt(id: Uuid) {
    viewModelScope.launch { receiptHistoryRepository.removeTrip(id) }
  }
}
