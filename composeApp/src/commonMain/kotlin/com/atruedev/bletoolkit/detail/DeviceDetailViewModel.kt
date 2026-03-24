package com.atruedev.bletoolkit.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atruedev.kmpble.connection.ConnectionOptions
import com.atruedev.kmpble.connection.ReconnectionStrategy
import com.atruedev.kmpble.connection.State
import com.atruedev.kmpble.gatt.BackpressureStrategy
import com.atruedev.kmpble.gatt.Characteristic
import com.atruedev.kmpble.gatt.WriteType
import com.atruedev.kmpble.peripheral.Peripheral
import com.atruedev.kmpble.scanner.Advertisement
import com.atruedev.kmpble.peripheral.toPeripheral
import com.atruedev.bletoolkit.registry.BluetoothUuidNames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class, com.atruedev.kmpble.ExperimentalBleApi::class)
class DeviceDetailViewModel(advertisement: Advertisement) : ViewModel() {

    private val peripheral: Peripheral = advertisement.toPeripheral()

    private val _uiState = MutableStateFlow(
        DeviceDetailUiState(
            deviceName = advertisement.name ?: "Unknown",
            identifier = advertisement.identifier.value,
        )
    )
    val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

    private var rssiJob: Job? = null
    private val notificationJobs = mutableMapOf<String, Job>()

    private companion object {
        const val MAX_NOTIFICATION_VALUES = 50
    }

    init {
        observeConnectionState()
        observeBondState()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            peripheral.state.collect { state ->
                _uiState.update { it.copy(connectionState = state, error = null) }
                when (state) {
                    is State.Connected.Ready -> onConnected()
                    is State.Disconnected -> onDisconnected()
                    else -> Unit
                }
            }
        }
    }

    private fun observeBondState() {
        viewModelScope.launch {
            peripheral.bondState.collect { bondState ->
                _uiState.update { it.copy(bondState = bondState) }
            }
        }
    }

    fun connect() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(error = null) }
                peripheral.connect(
                    ConnectionOptions(
                        timeout = 15.seconds,
                        reconnectionStrategy = ReconnectionStrategy.ExponentialBackoff(
                            initialDelay = 1.seconds,
                            maxDelay = 15.seconds,
                            maxAttempts = 3,
                        ),
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Connection failed: ${e.message}") }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                stopAllNotifications()
                peripheral.disconnect()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Disconnect errors are non-critical
            }
        }
    }

    private fun onConnected() {
        discoverServices()
        startRssiPolling()
    }

    private fun onDisconnected() {
        rssiJob?.cancel()
        rssiJob = null
        stopAllNotifications()
        _uiState.update { it.copy(rssi = null, mtu = null) }
    }

    private fun discoverServices() {
        viewModelScope.launch {
            try {
                val services = peripheral.refreshServices()
                val serviceModels = services.map { service ->
                    val characteristics = service.characteristics.map { char ->
                        val descriptors = char.descriptors.map { desc ->
                            DescriptorUiModel(
                                descriptor = desc,
                                uuid = desc.uuid,
                                displayName = descriptorName(desc.uuid),
                                lastReadValue = null,
                            )
                        }
                        CharacteristicUiModel(
                            characteristic = char,
                            uuid = char.uuid,
                            displayName = BluetoothUuidNames.characteristicName(char.uuid)
                                ?: char.uuid.toString().take(8),
                            properties = char.properties,
                            descriptors = descriptors,
                        )
                    }
                    ServiceUiModel(
                        uuid = service.uuid,
                        displayName = BluetoothUuidNames.serviceName(service.uuid)
                            ?: service.uuid.toString().take(8),
                        characteristics = characteristics,
                    )
                }
                _uiState.update { it.copy(services = serviceModels) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Service discovery failed: ${e.message}") }
            }
        }
    }

    private fun descriptorName(uuid: kotlin.uuid.Uuid): String {
        val short = uuid.toString().replace("-", "").lowercase()
        return when {
            short.startsWith("00002902") -> "Client Characteristic Configuration"
            short.startsWith("00002901") -> "Characteristic User Description"
            short.startsWith("00002900") -> "Characteristic Extended Properties"
            short.startsWith("00002903") -> "Server Characteristic Configuration"
            short.startsWith("00002904") -> "Characteristic Presentation Format"
            else -> uuid.toString().take(8)
        }
    }

    private fun startRssiPolling() {
        rssiJob?.cancel()
        rssiJob = viewModelScope.launch {
            while (true) {
                try {
                    val rssi = peripheral.readRssi()
                    _uiState.update { it.copy(rssi = rssi) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // RSSI read failures are non-critical
                }
                delay(2.seconds)
            }
        }
    }

    fun requestMtu(mtu: Int) {
        viewModelScope.launch {
            try {
                val negotiated = peripheral.requestMtu(mtu)
                _uiState.update { it.copy(mtu = negotiated) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "MTU request failed: ${e.message}") }
            }
        }
    }

    fun toggleService(serviceIndex: Int) {
        _uiState.update { state ->
            val services = state.services.toMutableList()
            val service = services[serviceIndex]
            services[serviceIndex] = service.copy(isExpanded = !service.isExpanded)
            state.copy(services = services)
        }
    }

    fun toggleCharacteristic(serviceIndex: Int, charIndex: Int) {
        _uiState.update { state ->
            val services = state.services.toMutableList()
            val service = services[serviceIndex]
            val chars = service.characteristics.toMutableList()
            val char = chars[charIndex]
            chars[charIndex] = char.copy(isExpanded = !char.isExpanded)
            services[serviceIndex] = service.copy(characteristics = chars)
            state.copy(services = services)
        }
    }

    fun readCharacteristic(serviceIndex: Int, charIndex: Int) {
        val char = _uiState.value.services[serviceIndex].characteristics[charIndex]
        viewModelScope.launch {
            try {
                val value = peripheral.read(char.characteristic)
                updateCharacteristic(serviceIndex, charIndex) {
                    it.copy(lastReadValue = value, error = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateCharacteristic(serviceIndex, charIndex) {
                    it.copy(error = "Read failed: ${e.message}")
                }
            }
        }
    }

    fun showWriteDialog(serviceIndex: Int, charIndex: Int) {
        val char = _uiState.value.services[serviceIndex].characteristics[charIndex]
        _uiState.update { it.copy(writeDialogTarget = char) }
    }

    fun dismissWriteDialog() {
        _uiState.update { it.copy(writeDialogTarget = null) }
    }

    fun writeCharacteristic(
        serviceIndex: Int,
        charIndex: Int,
        data: ByteArray,
        writeType: WriteType,
    ) {
        val char = _uiState.value.services[serviceIndex].characteristics[charIndex]
        viewModelScope.launch {
            try {
                peripheral.write(char.characteristic, data, writeType)
                updateCharacteristic(serviceIndex, charIndex) {
                    it.copy(lastReadValue = data, error = null)
                }
                dismissWriteDialog()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateCharacteristic(serviceIndex, charIndex) {
                    it.copy(error = "Write failed: ${e.message}")
                }
            }
        }
    }

    fun toggleNotifications(serviceIndex: Int, charIndex: Int) {
        val char = _uiState.value.services[serviceIndex].characteristics[charIndex]
        val key = "${serviceIndex}_${charIndex}"

        if (char.isNotifying) {
            notificationJobs[key]?.cancel()
            notificationJobs.remove(key)
            updateCharacteristic(serviceIndex, charIndex) {
                it.copy(isNotifying = false)
            }
        } else {
            updateCharacteristic(serviceIndex, charIndex) {
                it.copy(isNotifying = true, notificationValues = emptyList())
            }
            val job = viewModelScope.launch {
                try {
                    peripheral.observeValues(
                        char.characteristic,
                        BackpressureStrategy.Buffer(capacity = 16),
                    ).collect { value ->
                        updateCharacteristic(serviceIndex, charIndex) { model ->
                            val values = model.notificationValues.toMutableList()
                            values.add(TimestampedValue(value, Clock.System.now()))
                            if (values.size > MAX_NOTIFICATION_VALUES) {
                                values.removeAt(0)
                            }
                            model.copy(
                                notificationValues = values,
                                lastReadValue = value,
                                error = null,
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    updateCharacteristic(serviceIndex, charIndex) {
                        it.copy(isNotifying = false, error = "Notification failed: ${e.message}")
                    }
                }
            }
            notificationJobs[key] = job
        }
    }

    fun setDisplayFormat(serviceIndex: Int, charIndex: Int, format: DisplayFormat) {
        updateCharacteristic(serviceIndex, charIndex) {
            it.copy(displayFormat = format)
        }
    }

    fun dismissCharacteristicError(serviceIndex: Int, charIndex: Int) {
        updateCharacteristic(serviceIndex, charIndex) {
            it.copy(error = null)
        }
    }

    private fun updateCharacteristic(
        serviceIndex: Int,
        charIndex: Int,
        transform: (CharacteristicUiModel) -> CharacteristicUiModel,
    ) {
        _uiState.update { state ->
            val services = state.services.toMutableList()
            if (serviceIndex >= services.size) return@update state
            val service = services[serviceIndex]
            val chars = service.characteristics.toMutableList()
            if (charIndex >= chars.size) return@update state
            chars[charIndex] = transform(chars[charIndex])
            services[serviceIndex] = service.copy(characteristics = chars)
            state.copy(services = services)
        }
    }

    private fun stopAllNotifications() {
        notificationJobs.values.forEach { it.cancel() }
        notificationJobs.clear()
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopAllNotifications()
        rssiJob?.cancel()
        peripheral.close()
    }
}
