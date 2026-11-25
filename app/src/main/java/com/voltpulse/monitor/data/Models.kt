package com.voltpulse.monitor.data

import android.graphics.drawable.Drawable

data class BatteryLiveStatus(
    val levelPercent: Int = 0,
    val isCharging: Boolean = false,
    val chargeSource: String = "Battery",
    val statusText: String = "Discharging",
    val healthText: String = "Good",
    val temperatureC: Float = 0f,
    val voltageV: Float = 0f,
    val currentMa: Int = 0,
    val powerWatts: Float = 0f,
    val technology: String = "Li-ion",
    val designCapacityMah: Int = 5000,
    val estimatedRemainingHours: Float = 0f
)

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val foregroundTimeMs: Long,
    val launchCount: Int,
    val lastTimeUsedMs: Long,
    val estimatedBatteryPercent: Float,
    val estimatedBatteryMah: Float,
    val shareOfScreenTime: Float
)

data class BatterySnapshot(
    val timestampMs: Long,
    val levelPercent: Int,
    val temperatureC: Float,
    val isCharging: Boolean,
    val screenOnMinutes: Long
)

data class DailyScreenSummary(
    val dayLabel: String,
    val totalMinutes: Long
)

data class AlertSettings(
    val fullChargeAlertEnabled: Boolean = true,
    val fullChargeThreshold: Int = 90,
    val lowBatteryAlertEnabled: Boolean = true,
    val lowBatteryThreshold: Int = 20,
    val overheatAlertEnabled: Boolean = true,
    val overheatThresholdC: Float = 42.0f,
    val dailyScreenLimitMinutes: Int = 300,
    val customCapacityMah: Int = 5000,
    val darkThemeMode: Int = 0 // 0 = System, 1 = Light, 2 = Dark
)
