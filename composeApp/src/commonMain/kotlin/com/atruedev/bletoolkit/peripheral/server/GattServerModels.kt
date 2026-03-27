package com.atruedev.bletoolkit.peripheral.server

sealed interface ServerState {
    data object Stopped : ServerState
    data object Running : ServerState
    data class Error(val message: String) : ServerState
}

enum class CharProperty { READ, WRITE, NOTIFY }

data class ServerCharConfig(
    val uuid: String = "",
    val properties: Set<CharProperty> = emptySet(),
    val initialValue: String = "",
)

data class ServerServiceConfig(
    val uuid: String = "",
    val characteristics: List<ServerCharConfig> = emptyList(),
)

data class GattServerUiState(
    val state: ServerState = ServerState.Stopped,
    val services: List<ServerServiceConfig> = listOf(
        ServerServiceConfig(characteristics = listOf(ServerCharConfig())),
    ),
    val connectedClients: Int = 0,
    val error: String? = null,
)
