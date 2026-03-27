package com.atruedev.bletoolkit.profiles

import com.atruedev.kmpble.peripheral.Peripheral
import com.atruedev.kmpble.profiles.battery.batteryLevelNotifications
import com.atruedev.kmpble.profiles.battery.readBatteryLevel
import com.atruedev.kmpble.profiles.deviceinfo.readDeviceInformation
import com.atruedev.kmpble.profiles.heartrate.heartRateMeasurements
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProfileOperations(
    private val peripheral: Peripheral,
    private val _uiState: MutableStateFlow<ProfileUiState>,
    private val scope: CoroutineScope,
) {
    private val subscriptionJobs = mutableMapOf<ProfileType, Job>()

    fun startHeartRate() {
        if (subscriptionJobs.containsKey(ProfileType.HEART_RATE)) return
        _uiState.update { it.copy(activeSubscriptions = it.activeSubscriptions + ProfileType.HEART_RATE) }

        subscriptionJobs[ProfileType.HEART_RATE] = scope.launch {
            try {
                peripheral.heartRateMeasurements().collect { measurement ->
                    val data = ProfileData.HeartRate(
                        bpm = measurement.heartRate,
                        sensorContact = measurement.sensorContactDetected,
                        rrIntervals = measurement.rrIntervals,
                    )
                    _uiState.update { it.copy(readings = it.readings + (ProfileType.HEART_RATE to data)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(activeSubscriptions = it.activeSubscriptions - ProfileType.HEART_RATE) }
            }
        }
    }

    fun stopProfile(type: ProfileType) {
        subscriptionJobs[type]?.cancel()
        subscriptionJobs.remove(type)
        _uiState.update { it.copy(activeSubscriptions = it.activeSubscriptions - type) }
    }

    fun readBattery() {
        scope.launch {
            try {
                val level = peripheral.readBatteryLevel()
                if (level != null) {
                    _uiState.update { it.copy(readings = it.readings + (ProfileType.BATTERY to ProfileData.Battery(level))) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Battery read failed silently
            }
        }
    }

    fun startBatteryNotifications() {
        if (subscriptionJobs.containsKey(ProfileType.BATTERY)) return
        _uiState.update { it.copy(activeSubscriptions = it.activeSubscriptions + ProfileType.BATTERY) }

        subscriptionJobs[ProfileType.BATTERY] = scope.launch {
            try {
                peripheral.batteryLevelNotifications().collect { level ->
                    _uiState.update { it.copy(readings = it.readings + (ProfileType.BATTERY to ProfileData.Battery(level))) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(activeSubscriptions = it.activeSubscriptions - ProfileType.BATTERY) }
            }
        }
    }

    fun readDeviceInfo() {
        scope.launch {
            try {
                val info = peripheral.readDeviceInformation()
                val data = ProfileData.DeviceInfo(
                    manufacturer = info.manufacturerName,
                    model = info.modelNumber,
                    serial = info.serialNumber,
                    firmware = info.firmwareRevision,
                    hardware = info.hardwareRevision,
                    software = info.softwareRevision,
                )
                _uiState.update { it.copy(readings = it.readings + (ProfileType.DEVICE_INFO to data)) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Device info read failed silently
            }
        }
    }

    fun stopAll() {
        subscriptionJobs.values.forEach { it.cancel() }
        subscriptionJobs.clear()
        _uiState.update { it.copy(activeSubscriptions = emptySet()) }
    }
}
