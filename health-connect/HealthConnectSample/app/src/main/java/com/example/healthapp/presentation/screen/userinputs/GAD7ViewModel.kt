package com.example.healthapp.presentation.screen.userinputs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.HealthConnectManager
import kotlinx.coroutines.launch

class GAD7ViewModel (private val healthConnectManager: HealthConnectManager): ViewModel(){
    fun saveGAD7Info(score: Int) {
        viewModelScope.launch {
            healthConnectManager.saveFormular(score)
            healthConnectManager.calculateStress()
        }
    }
}

class GAD7ViewModelFactory (private val healthConnectManager: HealthConnectManager): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GAD7ViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GAD7ViewModel(
                healthConnectManager = healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}