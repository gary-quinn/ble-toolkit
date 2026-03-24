package com.atruedev.bletoolkit.scanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atruedev.bletoolkit.permission.PermissionResult
import com.atruedev.bletoolkit.permission.rememberBlePermissionLauncher
import com.atruedev.kmpble.adapter.BluetoothAdapterState
import com.atruedev.kmpble.scanner.Advertisement
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class, ExperimentalLayoutApi::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onDeviceSelected: (Advertisement) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    val requestPermission = rememberBlePermissionLauncher { result ->
        if (result is PermissionResult.Granted) {
            viewModel.onPermissionGranted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Toolkit") },
                actions = {
                    SortModeSelector(
                        currentMode = state.sortMode,
                        onModeSelected = viewModel::setSortMode,
                    )
                    IconButton(onClick = viewModel::toggleFilterBar) {
                        Text(
                            "⊞",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.isFilterBarVisible) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ScanToggleButton(
                        scanState = state.scanState,
                        adapterState = state.adapterState,
                        onToggle = viewModel::toggleScan,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (state.scanState == ScanState.Scanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            AnimatedVisibility(
                visible = state.isFilterBarVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                FilterBar(
                    filters = state.filters,
                    onFiltersChanged = viewModel::updateFilters,
                )
            }

            when {
                state.adapterState == BluetoothAdapterState.Unavailable -> InitializingContent()
                state.adapterState == BluetoothAdapterState.Off -> BluetoothOffContent()
                state.adapterState == BluetoothAdapterState.Unauthorized -> PermissionDeniedContent(
                    onRequestPermission = requestPermission,
                )
                state.adapterState == BluetoothAdapterState.Unsupported -> UnsupportedContent()
                state.scanState is ScanState.Error -> ErrorContent(
                    message = (state.scanState as ScanState.Error).message,
                    onRetry = viewModel::startScan,
                )
                state.advertisements.isEmpty() && state.scanState == ScanState.Scanning -> EmptyScanningContent()
                state.advertisements.isEmpty() && state.scanState == ScanState.Idle -> EmptyIdleContent(
                    onStartScan = viewModel::startScan,
                )
                else -> DeviceList(
                    devices = state.advertisements,
                    onDeviceClick = { onDeviceSelected(it.advertisement) },
                )
            }
        }
    }
}

@Composable
private fun ScanToggleButton(
    scanState: ScanState,
    adapterState: BluetoothAdapterState,
    onToggle: () -> Unit,
) {
    TextButton(
        onClick = onToggle,
        enabled = adapterState == BluetoothAdapterState.On,
    ) {
        Text(
            when (scanState) {
                ScanState.Scanning -> "Stop"
                else -> "Scan"
            }
        )
    }
}

@Composable
private fun SortModeSelector(
    currentMode: SortMode,
    onModeSelected: (SortMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                when (currentMode) {
                    SortMode.RSSI -> "RSSI"
                    SortMode.NAME -> "Name"
                    SortMode.LAST_SEEN -> "Recent"
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (mode) {
                                SortMode.RSSI -> "Sort by RSSI"
                                SortMode.NAME -> "Sort by Name"
                                SortMode.LAST_SEEN -> "Sort by Recent"
                            }
                        )
                    },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun FilterBar(
    filters: ScanFilters,
    onFiltersChanged: (ScanFilters) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = filters.nameQuery,
            onValueChange = { onFiltersChanged(filters.copy(nameQuery = it)) },
            label = { Text("Filter by name or ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Min RSSI: ${filters.minRssi} dBm",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(130.dp),
            )
            Slider(
                value = filters.minRssi.toFloat(),
                onValueChange = { onFiltersChanged(filters.copy(minRssi = it.toInt())) },
                valueRange = -100f..-30f,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Hide unnamed", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = filters.hideUnnamed,
                onCheckedChange = { onFiltersChanged(filters.copy(hideUnnamed = it)) },
            )
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun DeviceList(
    devices: List<DiscoveredDevice>,
    onDeviceClick: (DiscoveredDevice) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(
            items = devices,
            key = { _, device -> device.identifier },
        ) { _, device ->
            DeviceCard(device = device, onClick = { onDeviceClick(device) })
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalLayoutApi::class)
@Composable
private fun DeviceCard(
    device: DiscoveredDevice,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SignalStrengthIndicator(rssi = device.rssi)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (device.advertisement.name != null)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Text(
                    text = device.identifier,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (device.deviceCategory != DeviceCategory.UNKNOWN) {
                        CategoryChip(category = device.deviceCategory)
                    }
                    if (device.manufacturerName != null) {
                        SmallChip(text = device.manufacturerName)
                    }
                    if (device.serviceUuids.isNotEmpty()) {
                        SmallChip(text = "${device.serviceUuids.size} services")
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = rssiColor(device.rssi),
                )
                Text(
                    text = relativeTime(device.lastSeen),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SignalStrengthIndicator(rssi: Int) {
    val bars = when {
        rssi >= -50 -> 4
        rssi >= -65 -> 3
        rssi >= -80 -> 2
        rssi >= -90 -> 1
        else -> 0
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.size(20.dp, 16.dp),
    ) {
        for (i in 0 until 4) {
            val barHeight = (4 + i * 3).dp
            val isActive = i < bars
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .align(Alignment.Bottom)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (isActive) rssiColor(rssi)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    ),
            )
        }
    }
}

@Composable
private fun rssiColor(rssi: Int) = when {
    rssi >= -50 -> MaterialTheme.colorScheme.primary
    rssi >= -65 -> MaterialTheme.colorScheme.tertiary
    rssi >= -80 -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun CategoryChip(category: DeviceCategory) {
    val label = when (category) {
        DeviceCategory.HEART_RATE -> "Heart Rate"
        DeviceCategory.BATTERY -> "Battery"
        DeviceCategory.BLOOD_PRESSURE -> "Blood Pressure"
        DeviceCategory.GLUCOSE -> "Glucose"
        DeviceCategory.CYCLING -> "Cycling"
        DeviceCategory.DEVICE_INFO -> "Device Info"
        DeviceCategory.NORDIC_DK -> "Nordic DK"
        DeviceCategory.UNKNOWN -> return
    }
    SmallChip(text = label)
}

@Composable
private fun SmallChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private fun relativeTime(instant: kotlin.time.Instant): String {
    val elapsed = kotlin.time.Clock.System.now() - instant
    return when {
        elapsed.inWholeSeconds < 5 -> "now"
        elapsed.inWholeSeconds < 60 -> "${elapsed.inWholeSeconds}s ago"
        elapsed.inWholeMinutes < 60 -> "${elapsed.inWholeMinutes}m ago"
        else -> "${elapsed.inWholeHours}h ago"
    }
}

@Composable
private fun InitializingContent() {
    CenteredMessage(
        title = "Initializing...",
        subtitle = "Checking Bluetooth adapter state.",
    )
}

@Composable
private fun BluetoothOffContent() {
    CenteredMessage(
        title = "Bluetooth is turned off",
        subtitle = "Enable Bluetooth in system settings to scan for devices.",
    )
}

@Composable
private fun PermissionDeniedContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Bluetooth permission required", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Grant Bluetooth permission to scan for devices.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun UnsupportedContent() {
    CenteredMessage(
        title = "BLE not supported",
        subtitle = "This device does not support Bluetooth Low Energy.",
    )
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun EmptyScanningContent() {
    CenteredMessage(
        title = "Scanning...",
        subtitle = "Looking for BLE devices nearby.",
    )
}

@Composable
private fun EmptyIdleContent(onStartScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No devices found", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Check that Bluetooth is on and devices are nearby.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onStartScan) { Text("Scan again") }
    }
}

@Composable
private fun CenteredMessage(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
