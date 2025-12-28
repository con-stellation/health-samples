/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.healthapp.presentation.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.HealthServicesRepository
import com.example.healthapp.data.ServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MonitoringViewModel
@Inject
constructor(
    private val healthServicesRepository: HealthServicesRepository
) : ViewModel() {

    private val exerciseEndedMutableFlow = MutableStateFlow(false)
    val exerciseEndedFlow = exerciseEndedMutableFlow.asStateFlow()
    val hrMonitoringExercisePaused by lazy {
        healthServicesRepository.exerciseClientManager.hrMonitoringExercisePaused
    }


    val uiState: StateFlow<MonitoringScreenState> =
        healthServicesRepository.serviceState
            .map {
                MonitoringScreenState(
                    hasExerciseCapabilities = healthServicesRepository.hasExerciseCapability(),
                    isTrackingAnotherExercise =
                    healthServicesRepository
                        .isTrackingExerciseInAnotherApp(),
                    serviceState = it,
                    exerciseState = (it as? ServiceState.Connected)?.exerciseServiceState
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(3_000),
                healthServicesRepository.serviceState.value.let {
                    MonitoringScreenState(
                        true,
                        false,
                        it,
                        (it as? ServiceState.Connected)?.exerciseServiceState
                    )
                }
            )

    fun startMonitoring() {
        exerciseEndedMutableFlow.value = false
        healthServicesRepository.startMonitoring()
    }

    fun pauseMonitoring() {
        healthServicesRepository.pauseExercise()
    }

    fun endMonitoring() {
        exerciseEndedMutableFlow.value = true
        healthServicesRepository.endMonitoring()
    }

    fun resumeMonitoring() {
        healthServicesRepository.resumeExercise()
    }
}
