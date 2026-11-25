package com.voltpulse.monitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voltpulse.monitor.data.AlertSettings
import java.util.Locale

@Composable
fun DashboardScreen(
    state: MainUiState,
    onGrantUsageClick: () -> Unit
) {
    val battery = state.battery
    val totalScreenMs = state.appUsageList.sumOf { it.foregroundTimeMs }
    val totalLaunches = state.appUsageList.sumOf { it.launchCount }
    val topApp = state.appUsageList.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!state.hasUsagePermission) {
            item { PermissionBanner(onGrantClick = onGrantUsageClick) }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (battery.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${battery.levelPercent}% • ${battery.statusText}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${battery.chargeSource} • Health: ${battery.healthText}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        if (!battery.isCharging) {
                            Text(
                                text = String.format(Locale.ENGLISH, "Est. Remaining: %.1f hrs", battery.estimatedRemainingHours),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    label = "Temperature",
                    value = String.format(Locale.ENGLISH, "%.1f °C", battery.temperatureC),
                    subValue = if (battery.temperatureC >= 40f) "Warm" else "Normal",
                    accentColor = if (battery.temperatureC >= 40f) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Power / Current",
                    value = String.format(Locale.ENGLISH, "%.1f W", battery.powerWatts),
                    subValue = "${battery.currentMa} mA @ ${String.format(Locale.ENGLISH, "%.2fV", battery.voltageV)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    label = "Screen Time Today",
                    value = formatDuration(totalScreenMs),
                    subValue = "$totalLaunches app opens",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Battery Capacity",
                    value = "${battery.designCapacityMah} mAh",
                    subValue = battery.technology,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (topApp != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconImage(drawable = topApp.icon, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Top Battery & Screen App", style = MaterialTheme.typography.labelMedium)
                            Text(topApp.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${formatDuration(topApp.foregroundTimeMs)} • ~${String.format(Locale.ENGLISH, "%.1f", topApp.estimatedBatteryPercent)}% drain",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Smart Battery Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val tip = when {
                        battery.temperatureC >= 40f -> "Phone is warm (${battery.temperatureC}°C). Reduce brightness or pause heavy apps."
                        battery.isCharging && battery.levelPercent >= 85 -> "Unplugging around 85–90% helps extend battery cycle life."
                        totalScreenMs > state.settings.dailyScreenLimitMinutes * 60_000L -> "You have exceeded your daily screen time goal."
                        else -> "Battery health and temperature are operating in the optimal range."
                    }
                    Text(tip, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun ScreenTimeScreen(
    state: MainUiState,
    onRangeChange: (Int) -> Unit,
    onGrantUsageClick: () -> Unit
) {
    val totalMs = state.appUsageList.sumOf { it.foregroundTimeMs }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!state.hasUsagePermission) {
            item { PermissionBanner(onGrantClick = onGrantUsageClick) }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedRangeDays == 0,
                    onClick = { onRangeChange(0) },
                    label = { Text("Today") }
                )
                FilterChip(
                    selected = state.selectedRangeDays == 7,
                    onClick = { onRangeChange(7) },
                    label = { Text("Last 7 Days") }
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Screen Time", style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatDuration(totalMs),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.weeklySummary.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val maxMin = state.weeklySummary.maxOf { it.totalMinutes }.coerceAtLeast(1L)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(90.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            state.weeklySummary.forEach { day ->
                                val ratio = (day.totalMinutes.toFloat() / maxMin.toFloat()).coerceIn(0.08f, 1f)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height((60 * ratio).dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(day.dayLabel, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        items(state.appUsageList) { app ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIconImage(drawable = app.icon, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${app.launchCount} launches", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(formatDuration(app.foregroundTimeMs), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { app.shareOfScreenTime.coerceIn(0.02f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryDrainScreen(
    state: MainUiState,
    onGrantUsageClick: () -> Unit
) {
    val totalEstDrainPct = state.appUsageList.sumOf { it.estimatedBatteryPercent.toDouble() }.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!state.hasUsagePermission) {
            item { PermissionBanner(onGrantClick = onGrantUsageClick) }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Battery Drain Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        String.format(
                            Locale.ENGLISH,
                            "Estimated active app drain: %.1f%% (based on Screen Time, SoC load & %d mAh capacity).",
                            totalEstDrainPct,
                            state.battery.designCapacityMah
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        items(state.appUsageList) { app ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconImage(drawable = app.icon, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Active: ${formatDuration(app.foregroundTimeMs)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            String.format(Locale.ENGLISH, "~%.1f%%", app.estimatedBatteryPercent),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            String.format(Locale.ENGLISH, "%.0f mAh", app.estimatedBatteryMah),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        if (state.snapshots.isNotEmpty()) {
            item {
                Text(
                    "Recent Background Battery Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(state.snapshots.reversed().take(12)) { snap ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${snap.levelPercent}% • ${if (snap.isCharging) "Charging" else "Battery"}")
                        Text(String.format(Locale.ENGLISH, "%.1f°C • %dm screen", snap.temperatureC, snap.screenOnMinutes))
                    }
                }
            }
        }
    }
}

@Composable
fun AlertsSettingsScreen(
    settings: AlertSettings,
    onSave: (AlertSettings) -> Unit
) {
    var fullEnabled by remember(settings) { mutableStateOf(settings.fullChargeAlertEnabled) }
    var fullThreshold by remember(settings) { mutableIntStateOf(settings.fullChargeThreshold) }
    var lowEnabled by remember(settings) { mutableStateOf(settings.lowBatteryAlertEnabled) }
    var lowThreshold by remember(settings) { mutableIntStateOf(settings.lowBatteryThreshold) }
    var heatEnabled by remember(settings) { mutableStateOf(settings.overheatAlertEnabled) }
    var heatThreshold by remember(settings) { mutableFloatStateOf(settings.overheatThresholdC) }
    var capacityText by remember(settings) { mutableStateOf(settings.customCapacityMah.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Full Charge Alarm", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Switch(checked = fullEnabled, onCheckedChange = { fullEnabled = it })
                    }
                    Text("Notify at $fullThreshold%")
                    Slider(
                        value = fullThreshold.toFloat(),
                        onValueChange = { fullThreshold = it.toInt() },
                        valueRange = 75f..100f
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Low Battery Alert", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Switch(checked = lowEnabled, onCheckedChange = { lowEnabled = it })
                    }
                    Text("Notify at $lowThreshold%")
                    Slider(
                        value = lowThreshold.toFloat(),
                        onValueChange = { lowThreshold = it.toInt() },
                        valueRange = 10f..35f
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Overheat Warning", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Switch(checked = heatEnabled, onCheckedChange = { heatEnabled = it })
                    }
                    Text(String.format(Locale.ENGLISH, "Alert above %.0f °C", heatThreshold))
                    Slider(
                        value = heatThreshold,
                        onValueChange = { heatThreshold = it },
                        valueRange = 38f..48f
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = capacityText,
                onValueChange = { capacityText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Battery Capacity (mAh)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = {
                    onSave(
                        settings.copy(
                            fullChargeAlertEnabled = fullEnabled,
                            fullChargeThreshold = fullThreshold,
                            lowBatteryAlertEnabled = lowEnabled,
                            lowBatteryThreshold = lowThreshold,
                            overheatAlertEnabled = heatEnabled,
                            overheatThresholdC = heatThreshold,
                            customCapacityMah = capacityText.toIntOrNull() ?: 5000
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}
