@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)

package io.github.and19081.mealplanner.feature.receipts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.and19081.mealplanner.feature.settings.Mode
import io.github.and19081.mealplanner.feature.shoppinglist.ReceiptHistory
import io.github.and19081.mealplanner.ui.components.ReceiptForm
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.*
import kotlin.time.Clock

@Composable
fun ReceiptsView(viewModel: ReceiptsViewModel, mode: Mode, isExpanded: Boolean) {
  val uiState by viewModel.uiState.collectAsState()
  var selectedTrip by remember { mutableStateOf<ReceiptHistory?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }

  val actualIsExpanded =
      when (mode) {
        Mode.AUTO -> isExpanded
        Mode.DESKTOP -> true
        Mode.MOBILE -> false
      }

  Scaffold(
      floatingActionButton = {
        if (selectedTrip == null && !isAddingNew) {
          FloatingActionButton(onClick = { isAddingNew = true }) {
            Icon(Icons.Default.Add, contentDescription = "Add Receipt")
          }
        }
      }
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      if (actualIsExpanded) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
          // List Column
          Column(modifier = Modifier.weight(0.4f)) {
            Text(
                "Receipts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            ReceiptList(
                receipts = uiState.receipts,
                allStores = uiState.allStores,
                allRestaurants = uiState.allRestaurants,
                onTripClick = { selectedTrip = it },
            )
          }

          VerticalDivider(modifier = Modifier.width(1.dp).padding(horizontal = 16.dp))

          // Detail/Form Column
          Box(modifier = Modifier.weight(0.6f)) {
            when {
              isAddingNew -> {
                LocationSelectionForm(
                    allStores = uiState.allStores,
                    allRestaurants = uiState.allRestaurants,
                onLocationSelected = { storeId, restaurantId, _ ->
                  val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                  val now =
                      Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
                      selectedTrip =
                          ReceiptHistory(
                              date = today,
                              time = now,
                              storeId = storeId,
                              restaurantId = restaurantId,
                              projectedTotalCents = 0,
                              actualTotalCents = 0,
                              taxPaidCents = 0,
                          )
                      isAddingNew = false
                    },
                    onCancel = { isAddingNew = false },
                )
              }
              selectedTrip != null -> {
                val trip = selectedTrip!!
                var fullTrip by remember(trip.id) { mutableStateOf<ReceiptHistory?>(null) }
                LaunchedEffect(trip.id) { fullTrip = viewModel.getTripDetails(trip.id) }

                if (fullTrip != null) {
                  val currentFullTrip = fullTrip!!
                  val locName =
                      if (currentFullTrip.restaurantId != null) {
                        uiState.allRestaurants.find { it.id == currentFullTrip.restaurantId }?.name
                            ?: "Unknown Restaurant"
                      } else {
                        uiState.allStores.find { it.id == currentFullTrip.storeId }?.name
                            ?: "Unknown Store"
                      }
                  ReceiptForm(
                      trip = currentFullTrip,
                      locationName = locName,
                      allIngredients = uiState.allIngredients,
                      allUnits = uiState.allUnits,
                      onClose = { selectedTrip = null },
                      onSave = { updatedTrip ->
                        viewModel.updateReceipt(updatedTrip)
                        selectedTrip = null
                      },
                      onDelete = {
                        viewModel.deleteReceipt(trip.id)
                        selectedTrip = null
                      },
                  )
                } else {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                  }
                }
              }
              else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text("Select a receipt to view details or add a new one.")
                }
              }
            }
          }
        }
      } else {
        // Mobile View
        if (isAddingNew) {
          LocationSelectionForm(
              allStores = uiState.allStores,
              allRestaurants = uiState.allRestaurants,
              onLocationSelected = { storeId, restaurantId, _ ->
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
                selectedTrip =
                    ReceiptHistory(
                        date = today,
                        time = now,
                        storeId = storeId,
                        restaurantId = restaurantId,
                        projectedTotalCents = 0,
                        actualTotalCents = 0,
                        taxPaidCents = 0,
                    )
                isAddingNew = false
              },
              onCancel = { isAddingNew = false },
          )
        } else if (selectedTrip != null) {
          val trip = selectedTrip!!
          var fullTrip by remember(trip.id) { mutableStateOf<ReceiptHistory?>(null) }
          LaunchedEffect(trip.id) { fullTrip = viewModel.getTripDetails(trip.id) }
          if (fullTrip != null) {
            val currentFullTrip = fullTrip!!
            val locName =
                if (currentFullTrip.restaurantId != null) {
                  uiState.allRestaurants.find { it.id == currentFullTrip.restaurantId }?.name
                      ?: "Unknown Restaurant"
                } else {
                  uiState.allStores.find { it.id == currentFullTrip.storeId }?.name ?: "Unknown Store"
                }
            ReceiptForm(
                trip = currentFullTrip,
                locationName = locName,
                allIngredients = uiState.allIngredients,
                allUnits = uiState.allUnits,
                onClose = { selectedTrip = null },
                onSave = { updatedTrip ->
                  viewModel.updateReceipt(updatedTrip)
                  selectedTrip = null
                },
                onDelete = {
                  viewModel.deleteReceipt(trip.id)
                  selectedTrip = null
                },
            )
          } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
        } else {
          Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Receipts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            ReceiptList(
                receipts = uiState.receipts,
                allStores = uiState.allStores,
                allRestaurants = uiState.allRestaurants,
                onTripClick = { selectedTrip = it },
            )
          }
        }
      }
    }
  }
}

@Composable
fun ReceiptList(
    receipts: List<ReceiptHistory>,
    allStores: List<io.github.and19081.mealplanner.domain.model.Store>,
    allRestaurants: List<io.github.and19081.mealplanner.feature.meals.Restaurant>,
    onTripClick: (ReceiptHistory) -> Unit,
) {
  LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(bottom = 80.dp),
  ) {
    items(receipts) { trip ->
      val name =
          if (trip.restaurantId != null) {
            allRestaurants.find { it.id == trip.restaurantId }?.name ?: "Unknown Restaurant"
          } else {
            allStores.find { it.id == trip.storeId }?.name ?: "Unknown Store"
          }
      Card(
          modifier = Modifier.fillMaxWidth().clickable { onTripClick(trip) },
      ) {
        ListItem(
            headlineContent = { Text(name) },
            supportingContent = { Text(trip.date.toString()) },
            trailingContent = {
              Text(
                  "$${String.format("%.2f", trip.actualTotalCents / 100.0)}",
                  fontWeight = FontWeight.Bold,
              )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
      }
    }
    if (receipts.isEmpty()) {
      item {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
          Text("No receipts found.")
        }
      }
    }
  }
}

@Composable
fun LocationSelectionForm(
    allStores: List<io.github.and19081.mealplanner.domain.model.Store>,
    allRestaurants: List<io.github.and19081.mealplanner.feature.meals.Restaurant>,
    onLocationSelected: (Uuid?, Uuid?, String) -> Unit,
    onCancel: () -> Unit,
) {
  var selectedType by remember { mutableStateOf(0) } // 0 for Store, 1 for Restaurant

  Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text("Add New Receipt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("Select Location Type")

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      SegmentedButton(
          selected = selectedType == 0,
          onClick = { selectedType = 0 },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
      ) {
        Text("Grocery Store")
      }
      SegmentedButton(
          selected = selectedType == 1,
          onClick = { selectedType = 1 },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
      ) {
        Text("Restaurant")
      }
    }

    Text("Select Location")
    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
      if (selectedType == 0) {
        items(allStores) { store ->
          ListItem(
              headlineContent = { Text(store.name) },
              modifier = Modifier.clickable { onLocationSelected(store.id, null, store.name) }
          )
          HorizontalDivider()
        }
      } else {
        items(allRestaurants) { restaurant ->
          ListItem(
              headlineContent = { Text(restaurant.name) },
              modifier = Modifier.clickable { onLocationSelected(null, restaurant.id, restaurant.name) }
          )
          HorizontalDivider()
        }
      }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      TextButton(onClick = onCancel) { Text("Cancel") }
    }
  }
}
