package io.github.and19081.mealplanner.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    var selectedRailIndex = mutableIntStateOf(0)
        private set

    fun onRailItemClicked(index: Int) {
        selectedRailIndex.intValue = index
    }
}