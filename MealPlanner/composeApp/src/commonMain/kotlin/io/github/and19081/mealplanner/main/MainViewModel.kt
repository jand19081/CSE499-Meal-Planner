package io.github.and19081.mealplanner.main

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    var selectedRailIndex = mutableIntStateOf(0)
        private set

    fun onRailItemClicked(index: Int) {
        selectedRailIndex.intValue = index
    }
}