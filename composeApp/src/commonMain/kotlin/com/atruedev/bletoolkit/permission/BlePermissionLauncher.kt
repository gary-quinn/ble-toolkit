package com.atruedev.bletoolkit.permission

import androidx.compose.runtime.Composable

@Composable
expect fun rememberBlePermissionLauncher(onResult: (PermissionResult) -> Unit): () -> Unit
