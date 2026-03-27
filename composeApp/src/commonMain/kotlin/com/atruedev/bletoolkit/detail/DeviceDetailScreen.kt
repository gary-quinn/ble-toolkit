package com.atruedev.bletoolkit.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atruedev.bletoolkit.detail.bonding.BondingSection
import com.atruedev.bletoolkit.dfu.DfuSection
import com.atruedev.bletoolkit.l2cap.L2capSection
import com.atruedev.bletoolkit.profiles.ProfileSection
import com.atruedev.kmpble.bonding.BondState
import com.atruedev.kmpble.connection.State
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
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
                        Text("←", style = MaterialTheme.typography.titleLarge)
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

            when (state.connectionState) {
                is State.Connected -> {
                    BondingSection(
                        bondState = state.bondState,
                        selectedRecipe = state.selectedRecipe,
                        onPair = viewModel::pair,
                        onRemoveBond = viewModel::removeBond,
                        onSelectRecipe = viewModel::selectRecipe,
                        onConnectWithRecipe = viewModel::connectWithRecipe,
                    )

                    DfuSection(
                        dfuState = state.dfuState,
                        onFileSelected = viewModel::selectFirmware,
                        onStartDfu = viewModel::startDfu,
                        onCancelDfu = viewModel::cancelDfu,
                        onReset = viewModel::resetDfu,
                    )

                    ProfileSection(
                        profileState = state.profileState,
                        onStartHeartRate = viewModel::startHeartRate,
                        onStopProfile = viewModel::stopProfile,
                        onReadBattery = viewModel::readBattery,
                        onStartBatteryNotifications = viewModel::startBatteryNotifications,
                        onReadDeviceInfo = viewModel::readDeviceInfo,
                    )

                    L2capSection(
                        l2capState = state.l2capState,
                        messages = state.l2capMessages,
                        onOpenChannel = viewModel::openL2capChannel,
                        onSend = viewModel::sendL2capData,
                        onCloseChannel = viewModel::closeL2capChannel,
                    )

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
                }
                is State.Disconnected -> DisconnectedContent(onConnect = viewModel::connect)
                else -> ConnectingContent()
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
    bondState: BondState?,
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
                when (connectionState) {
                    is State.Connected -> OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
                    is State.Disconnected -> Button(onClick = onConnect) { Text("Connect") }
                    else -> OutlinedButton(onClick = {}, enabled = false) { Text("Connecting...") }
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
                                is BondState.Bonded -> "Bonded"
                                is BondState.Bonding -> "Bonding"
                                is BondState.NotBonded -> "None"
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
