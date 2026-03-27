package com.atruedev.bletoolkit.navigation

sealed interface Tab {
    data object Scanner : Tab
    data object Peripheral : Tab
}
