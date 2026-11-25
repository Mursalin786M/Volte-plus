package com.voltpulse.monitor.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.abs

class BatteryRepository(private val context: Context) {

    fun getLiveBatteryStatus(customCapacityMah: Int = 5000): BatteryLiveStatus {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, intentFilter)
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val levelPercent = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val chargeSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast/Wall"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "On Battery"
        }

        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthText = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Good"
        }

        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperatureC = tempTenths / 10f

        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltageV = voltageMv / 1000f

        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        // Current in microamperes -> convert to mA
        val rawCurrentUa = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = if (abs(rawCurrentUa) > 10_000) rawCurrentUa / 1000 else rawCurrentUa
        val powerWatts = abs(currentMa) * voltageV / 1000f

        val detectedCapacity = readSystemBatteryCapacityMah() ?: customCapacityMah
        val avgDrawMa = if (!isCharging && abs(currentMa) > 50) abs(currentMa) else 420
        val remainingMah = (detectedCapacity * (levelPercent / 100f))
        val estimatedRemainingHours = if (avgDrawMa > 0) remainingMah / avgDrawMa else 0f

        return BatteryLiveStatus(
            levelPercent = levelPercent,
            isCharging = isCharging,
            chargeSource = chargeSource,
            statusText = statusText,
            healthText = healthText,
            temperatureC = temperatureC,
            voltageV = voltageV,
            currentMa = currentMa,
            powerWatts = powerWatts,
            technology = technology,
            designCapacityMah = detectedCapacity,
            estimatedRemainingHours = estimatedRemainingHours
        )
    }

    @SuppressLint("PrivateApi")
    fun readSystemBatteryCapacityMah(): Int? {
        return try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val powerProfile = constructor.newInstance(context)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = (method.invoke(powerProfile) as? Double)?.toInt()
            if (capacity != null && capacity > 500) capacity else null
        } catch (_: Exception) {
            null
        }
    }
}
