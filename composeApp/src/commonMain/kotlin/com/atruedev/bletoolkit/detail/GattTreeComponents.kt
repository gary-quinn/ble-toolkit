package com.atruedev.bletoolkit.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun ServiceList(
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
                if (service.isExpanded) "▾" else "▸",
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
            CharacteristicHeader(characteristic)

            AnimatedVisibility(
                visible = characteristic.isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                CharacteristicExpandedContent(
                    characteristic = characteristic,
                    onRead = onRead,
                    onWrite = onWrite,
                    onToggleNotify = onToggleNotify,
                    onFormatChange = onFormatChange,
                    onDismissError = onDismissError,
                )
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CharacteristicHeader(characteristic: CharacteristicUiModel) {
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
}

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CharacteristicExpandedContent(
    characteristic: CharacteristicUiModel,
    onRead: () -> Unit,
    onWrite: () -> Unit,
    onToggleNotify: () -> Unit,
    onFormatChange: (DisplayFormat) -> Unit,
    onDismissError: () -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        OperationButtons(
            properties = characteristic.properties,
            isNotifying = characteristic.isNotifying,
            onRead = onRead,
            onWrite = onWrite,
            onToggleNotify = onToggleNotify,
        )

        if (characteristic.lastReadValue != null || characteristic.notificationValues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FormatSelector(currentFormat = characteristic.displayFormat, onFormatChange = onFormatChange)
        }

        if (characteristic.lastReadValue != null) {
            Spacer(modifier = Modifier.height(4.dp))
            ValueDisplay(value = characteristic.lastReadValue, format = characteristic.displayFormat)
        }

        if (characteristic.notificationValues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            NotificationLog(values = characteristic.notificationValues, format = characteristic.displayFormat)
        }

        if (characteristic.error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            InlineError(message = characteristic.error, onDismiss = onDismissError)
        }

        if (characteristic.descriptors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            DescriptorList(descriptors = characteristic.descriptors)
        }
    }
}

@Composable
private fun OperationButtons(
    properties: com.atruedev.kmpble.gatt.Characteristic.Properties,
    isNotifying: Boolean,
    onRead: () -> Unit,
    onWrite: () -> Unit,
    onToggleNotify: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (properties.read) {
            OutlinedButton(onClick = onRead) { Text("Read", style = MaterialTheme.typography.labelSmall) }
        }
        if (properties.write || properties.writeWithoutResponse) {
            OutlinedButton(onClick = onWrite) { Text("Write", style = MaterialTheme.typography.labelSmall) }
        }
        if (properties.notify || properties.indicate) {
            OutlinedButton(onClick = onToggleNotify) {
                Text(
                    if (isNotifying) "Unsubscribe" else "Subscribe",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ValueDisplay(value: ByteArray, format: DisplayFormat) {
    Text(
        text = "Value: ${ValueFormatter.format(value, format)}",
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
    )
}

@Composable
private fun NotificationLog(values: List<TimestampedValue>, format: DisplayFormat) {
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
        values.forEach { tv ->
            val elapsed = Clock.System.now() - tv.timestamp
            val timeStr = when {
                elapsed.inWholeSeconds < 60 -> "${elapsed.inWholeSeconds}s"
                else -> "${elapsed.inWholeMinutes}m"
            }
            Text(
                "[$timeStr] ${ValueFormatter.format(tv.value, format)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}

@Composable
private fun InlineError(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) { Text("Dismiss") }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun DescriptorList(descriptors: List<DescriptorUiModel>) {
    Text("Descriptors:", style = MaterialTheme.typography.labelSmall)
    descriptors.forEach { desc ->
        Text(
            "${desc.displayName} (${desc.uuid.toString().take(8)})",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PropertyChips(properties: com.atruedev.kmpble.gatt.Characteristic.Properties) {
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
internal fun FormatSelector(
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
