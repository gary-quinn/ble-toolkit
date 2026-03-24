package com.atruedev.bletoolkit.detail

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atruedev.kmpble.connection.State
import com.atruedev.kmpble.gatt.WriteType
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceDetailScreen(
    viewModel: DeviceDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.connect()
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    if (state.writeDialogTarget != null) {
        WriteDialog(
            characteristicName = state.writeDialogTarget!!.displayName,
            onDismiss = viewModel::dismissWriteDialog,
            onWrite = { data, writeType ->
                val target = state.writeDialogTarget!!
                val serviceIndex = state.services.indexOfFirst { svc ->
                    svc.characteristics.any { it.uuid == target.uuid }
                }
                if (serviceIndex >= 0) {
                    val charIndex = state.services[serviceIndex].characteristics
                        .indexOfFirst { it.uuid == target.uuid }
                    if (charIndex >= 0) {
                        viewModel.writeCharacteristic(serviceIndex, charIndex, data, writeType)
                    }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.deviceName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            state.identifier,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onBack()
                    }) {
                        Text("<", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    ConnectionStateIndicator(state.connectionState)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            ConnectionControlBar(
                connectionState = state.connectionState,
                rssi = state.rssi,
                mtu = state.mtu,
                bondState = state.bondState,
                onConnect = viewModel::connect,
                onDisconnect = viewModel::disconnect,
                onRequestMtu = { viewModel.requestMtu(512) },
            )

            if (state.connectionState is State.Connected) {
                ServiceList(
                    services = state.services,
                    onToggleService = viewModel::toggleService,
                    onToggleCharacteristic = viewModel::toggleCharacteristic,
                    onRead = viewModel::readCharacteristic,
                    onWrite = viewModel::showWriteDialog,
                    onToggleNotify = viewModel::toggleNotifications,
                    onFormatChange = viewModel::setDisplayFormat,
                    onDismissError = viewModel::dismissCharacteristicError,
                )
            } else if (state.connectionState is State.Disconnected) {
                DisconnectedContent(onConnect = viewModel::connect)
            } else {
                ConnectingContent()
            }
        }
    }
}

@Composable
private fun ConnectionStateIndicator(state: State) {
    val color = when (state) {
        is State.Connected -> Color(0xFF4CAF50)
        is State.Connecting -> Color(0xFFFFC107)
        is State.Disconnecting -> Color(0xFFFFC107)
        is State.Disconnected -> Color(0xFFF44336)
    }
    val label = when (state) {
        is State.Connected.Ready -> "Connected"
        is State.Connected -> "Connected"
        is State.Connecting -> "Connecting"
        is State.Disconnecting -> "Disconnecting"
        is State.Disconnected -> "Disconnected"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ConnectionControlBar(
    connectionState: State,
    rssi: Int?,
    mtu: Int?,
    bondState: com.atruedev.kmpble.bonding.BondState?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRequestMtu: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (connectionState is State.Connected) {
                    OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
                } else if (connectionState is State.Disconnected) {
                    Button(onClick = onConnect) { Text("Connect") }
                } else {
                    OutlinedButton(onClick = {}, enabled = false) { Text("Connecting...") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (rssi != null) {
                        InfoLabel(label = "RSSI", value = "$rssi dBm")
                    }
                    if (mtu != null) {
                        InfoLabel(label = "MTU", value = "$mtu")
                    }
                    if (bondState != null) {
                        InfoLabel(
                            label = "Bond",
                            value = when (bondState) {
                                is com.atruedev.kmpble.bonding.BondState.Bonded -> "Bonded"
                                is com.atruedev.kmpble.bonding.BondState.Bonding -> "Bonding"
                                is com.atruedev.kmpble.bonding.BondState.NotBonded -> "None"
                                else -> "?"
                            },
                        )
                    }
                }
            }

            if (connectionState is State.Connected) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRequestMtu) { Text("Request MTU") }
                }
            }
        }
    }
}

@Composable
private fun InfoLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ServiceList(
    services: List<ServiceUiModel>,
    onToggleService: (Int) -> Unit,
    onToggleCharacteristic: (Int, Int) -> Unit,
    onRead: (Int, Int) -> Unit,
    onWrite: (Int, Int) -> Unit,
    onToggleNotify: (Int, Int) -> Unit,
    onFormatChange: (Int, Int, DisplayFormat) -> Unit,
    onDismissError: (Int, Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        services.forEachIndexed { serviceIndex, service ->
            item(key = "service_${service.uuid}") {
                ServiceHeader(
                    service = service,
                    onClick = { onToggleService(serviceIndex) },
                )
            }

            if (service.isExpanded) {
                service.characteristics.forEachIndexed { charIndex, char ->
                    item(key = "char_${service.uuid}_${char.uuid}") {
                        CharacteristicItem(
                            characteristic = char,
                            onToggle = { onToggleCharacteristic(serviceIndex, charIndex) },
                            onRead = { onRead(serviceIndex, charIndex) },
                            onWrite = { onWrite(serviceIndex, charIndex) },
                            onToggleNotify = { onToggleNotify(serviceIndex, charIndex) },
                            onFormatChange = { onFormatChange(serviceIndex, charIndex, it) },
                            onDismissError = { onDismissError(serviceIndex, charIndex) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ServiceHeader(service: ServiceUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (service.isExpanded) "v" else ">",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(service.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    service.uuid.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${service.characteristics.size} chars",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CharacteristicItem(
    characteristic: CharacteristicUiModel,
    onToggle: () -> Unit,
    onRead: () -> Unit,
    onWrite: () -> Unit,
    onToggleNotify: () -> Unit,
    onFormatChange: (DisplayFormat) -> Unit,
    onDismissError: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, top = 1.dp, bottom = 1.dp)
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        characteristic.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        characteristic.uuid.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PropertyChips(properties = characteristic.properties)
            }

            AnimatedVisibility(
                visible = characteristic.isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (characteristic.properties.read) {
                            OutlinedButton(onClick = onRead) { Text("Read", style = MaterialTheme.typography.labelSmall) }
                        }
                        if (characteristic.properties.write || characteristic.properties.writeWithoutResponse) {
                            OutlinedButton(onClick = onWrite) { Text("Write", style = MaterialTheme.typography.labelSmall) }
                        }
                        if (characteristic.properties.notify || characteristic.properties.indicate) {
                            OutlinedButton(onClick = onToggleNotify) {
                                Text(
                                    if (characteristic.isNotifying) "Unsubscribe" else "Subscribe",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }

                    if (characteristic.lastReadValue != null || characteristic.notificationValues.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FormatSelector(
                            currentFormat = characteristic.displayFormat,
                            onFormatChange = onFormatChange,
                        )
                    }

                    if (characteristic.lastReadValue != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Value: ${ValueFormatter.format(characteristic.lastReadValue, characteristic.displayFormat)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                        )
                    }

                    if (characteristic.notificationValues.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Notification Log:", style = MaterialTheme.typography.labelSmall)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            characteristic.notificationValues.forEach { tv ->
                                val elapsed = Clock.System.now() - tv.timestamp
                                val timeStr = when {
                                    elapsed.inWholeSeconds < 60 -> "${elapsed.inWholeSeconds}s"
                                    else -> "${elapsed.inWholeMinutes}m"
                                }
                                Text(
                                    "[$timeStr] ${ValueFormatter.format(tv.value, characteristic.displayFormat)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                )
                            }
                        }
                    }

                    if (characteristic.error != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                characteristic.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onDismissError) { Text("Dismiss") }
                        }
                    }

                    if (characteristic.descriptors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Descriptors:", style = MaterialTheme.typography.labelSmall)
                        characteristic.descriptors.forEach { desc ->
                            Text(
                                "${desc.displayName} (${desc.uuid.toString().take(8)})",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PropertyChips(properties: com.atruedev.kmpble.gatt.Characteristic.Properties) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (properties.read) PropertyChip("R", MaterialTheme.colorScheme.primary)
        if (properties.write) PropertyChip("W", Color(0xFF4CAF50))
        if (properties.writeWithoutResponse) PropertyChip("WNR", Color(0xFF8BC34A))
        if (properties.notify) PropertyChip("N", Color(0xFFFF9800))
        if (properties.indicate) PropertyChip("I", Color(0xFF9C27B0))
    }
}

@Composable
private fun PropertyChip(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatSelector(
    currentFormat: DisplayFormat,
    onFormatChange: (DisplayFormat) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        DisplayFormat.entries.forEachIndexed { index, format ->
            SegmentedButton(
                selected = format == currentFormat,
                onClick = { onFormatChange(format) },
                shape = SegmentedButtonDefaults.itemShape(index, DisplayFormat.entries.size),
            ) {
                Text(format.name, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun WriteDialog(
    characteristicName: String,
    onDismiss: () -> Unit,
    onWrite: (ByteArray, WriteType) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    var isHexMode by remember { mutableStateOf(true) }
    var writeType by remember { mutableStateOf(WriteType.WithResponse) }
    var parseError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write to $characteristicName") },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = { isHexMode = true }) {
                        Text("HEX", color = if (isHexMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { isHexMode = false }) {
                        Text("UTF-8", color = if (!isHexMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        parseError = null
                    },
                    label = { Text(if (isHexMode) "Hex value (e.g., 01 FF A3)" else "Text value") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = parseError != null,
                    supportingText = parseError?.let { { Text(it) } },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Type:", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { writeType = WriteType.WithResponse }) {
                        Text(
                            "With Response",
                            color = if (writeType == WriteType.WithResponse) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { writeType = WriteType.WithoutResponse }) {
                        Text(
                            "Without Response",
                            color = if (writeType == WriteType.WithoutResponse) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val data = if (isHexMode) {
                    val parsed = ValueFormatter.parseHexInput(inputText)
                    if (parsed == null) {
                        parseError = "Invalid hex format"
                        return@TextButton
                    }
                    parsed
                } else {
                    inputText.encodeToByteArray()
                }
                onWrite(data, writeType)
            }) { Text("Write") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DisconnectedContent(onConnect: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Disconnected", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onConnect) { Text("Reconnect") }
    }
}

@Composable
private fun ConnectingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Connecting...", style = MaterialTheme.typography.bodyLarge)
    }
}
