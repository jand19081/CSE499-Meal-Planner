package io.github.and19081.mealplanner.main

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    var selectedRailIndex = mutableIntStateOf(0)
        private set

    var isNavRailVisible = mutableStateOf(true)
        private set

    fun onRailItemClicked(index: Int) {
        selectedRailIndex.intValue = index
    }

    fun toggleRailVisibility() {
        isNavRailVisible.value = !isNavRailVisible.value
    }
}