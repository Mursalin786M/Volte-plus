package com.voltpulse.monitor.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.voltpulse.monitor.VoltPulseApp
import com.voltpulse.monitor.data.BatteryRepository
import com.voltpulse.monitor.data.BatterySnapshot
import com.voltpulse.monitor.data.PreferencesStore
import com.voltpulse.monitor.data.UsageStatsRepository

class BatteryMonitorWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = PreferencesStore(applicationContext)
        val settings = prefs.getAlertSettings()
        val batteryRepo = BatteryRepository(applicationContext)
        val usageRepo = UsageStatsRepository(applicationContext)

        val live = batteryRepo.getLiveBatteryStatus(settings.customCapacityMah)
        val screenMin = if (usageRepo.hasUsageStatsPermission()) {
            usageRepo.getAppUsageForRange(0, settings.customCapacityMah)
                .sumOf { it.foregroundTimeMs } / 60_000L
        } else 0L

        prefs.addSnapshot(
            BatterySnapshot(
                timestampMs = System.currentTimeMillis(),
                levelPercent = live.levelPercent,
                temperatureC = live.temperatureC,
                isCharging = live.isCharging,
                screenOnMinutes = screenMin
            )
        )

        val now = System.currentTimeMillis()
        val cooldownMs = 30 * 60 * 1000L

        if (settings.fullChargeAlertEnabled && live.isCharging &&
            live.levelPercent >= settings.fullChargeThreshold &&
            now - prefs.getLastAlertTime("full") > cooldownMs
        ) {
            sendAlert(
                1001,
                "Charge Target Reached (${live.levelPercent}%)",
                "Unplug your charger to preserve long-term battery health."
            )
            prefs.setLastAlertTime("full", now)
        }

        if (settings.lowBatteryAlertEnabled && !live.isCharging &&
            live.levelPercent <= settings.lowBatteryThreshold &&
            now - prefs.getLastAlertTime("low") > cooldownMs
        ) {
            sendAlert(
                1002,
                "Low Battery (${live.levelPercent}%)",
                "Battery is below ${settings.lowBatteryThreshold}%. Consider enabling Power Saver."
            )
            prefs.setLastAlertTime("low", now)
        }

        if (settings.overheatAlertEnabled &&
            live.temperatureC >= settings.overheatThresholdC &&
            now - prefs.getLastAlertTime("heat") > cooldownMs
        ) {
            sendAlert(
                1003,
                "Battery Overheating (${live.temperatureC}°C)",
                "Close heavy apps or pause charging until temperature drops."
            )
            prefs.setLastAlertTime("heat", now)
        }

        return Result.success()
    }

    private fun sendAlert(id: Int, title: String, message: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, VoltPulseApp.ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }
}
