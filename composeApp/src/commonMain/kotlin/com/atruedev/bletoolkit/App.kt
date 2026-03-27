package com.atruedev.bletoolkit

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atruedev.bletoolkit.detail.DeviceDetailScreen
import com.atruedev.bletoolkit.detail.DeviceDetailViewModel
import com.atruedev.bletoolkit.navigation.BottomNavBar
import com.atruedev.bletoolkit.navigation.Screen
import com.atruedev.bletoolkit.navigation.Tab
import com.atruedev.bletoolkit.peripheral.PeripheralScreen
import com.atruedev.bletoolkit.scanner.ScannerScreen
import com.atruedev.bletoolkit.scanner.ScannerViewModel
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun App() {
    MaterialTheme {
        var currentTab by remember { mutableStateOf<Tab>(Tab.Scanner) }
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Scanner) }

        val showBottomNav = currentScreen is Screen.Scanner

        Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    BottomNavBar(
                        currentTab = currentTab,
                        onTabSelected = { tab ->
                            currentTab = tab
                            currentScreen = Screen.Scanner
                        },
                    )
                }
            },
        ) { paddingValues ->
            when (currentTab) {
                Tab.Scanner -> ScannerTabContent(
                    currentScreen = currentScreen,
                    onScreenChange = { currentScreen = it },
                    modifier = Modifier.padding(paddingValues),
                )
                Tab.Peripheral -> PeripheralScreen()
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ScannerTabContent(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scannerViewModel = viewModel { ScannerViewModel() }

    when (val screen = currentScreen) {
        is Screen.Scanner -> {
            ScannerScreen(
                viewModel = scannerViewModel,
                onDeviceSelected = { advertisement ->
                    scannerViewModel.stopScan()
                    onScreenChange(Screen.DeviceDetail(advertisement))
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
                onBack = { onScreenChange(Screen.Scanner) },
            )
        }
    }
}
