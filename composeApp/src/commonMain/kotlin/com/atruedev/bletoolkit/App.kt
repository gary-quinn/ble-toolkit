package com.atruedev.bletoolkit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atruedev.bletoolkit.detail.DeviceDetailScreen
import com.atruedev.bletoolkit.detail.DeviceDetailViewModel
import com.atruedev.bletoolkit.navigation.Screen
import com.atruedev.bletoolkit.scanner.ScannerScreen
import com.atruedev.bletoolkit.scanner.ScannerViewModel
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Scanner) }

        val scannerViewModel = viewModel { ScannerViewModel() }

        when (val screen = currentScreen) {
            is Screen.Scanner -> {
                ScannerScreen(
                    viewModel = scannerViewModel,
                    onDeviceSelected = { advertisement ->
                        scannerViewModel.stopScan()
                        currentScreen = Screen.DeviceDetail(advertisement)
                    },
                )
            }
            is Screen.DeviceDetail -> {
                val detailViewModel = remember(screen.advertisement.identifier) {
                    DeviceDetailViewModel(screen.advertisement)
                }
                DisposableEffect(screen.advertisement.identifier) {
                    onDispose { detailViewModel.close() }
                }
                DeviceDetailScreen(
                    viewModel = detailViewModel,
                    onBack = {
                        currentScreen = Screen.Scanner
                    },
                )
            }
        }
    }
}
