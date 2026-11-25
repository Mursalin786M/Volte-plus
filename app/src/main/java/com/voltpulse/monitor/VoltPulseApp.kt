package com.voltpulse.monitor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.voltpulse.monitor.worker.BatteryMonitorWorker
import java.util.concurrent.TimeUnit

class VoltPulseApp : Application() {

    companion object {
        const val ALERT_CHANNEL_ID = "voltpulse_battery_alerts"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleBatteryWorker()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Battery & Screen Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for full charge, low battery, overheating, and screen time limits."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleBatteryWorker() {
        val workRequest = PeriodicWorkRequestBuilder<BatteryMonitorWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "VoltPulseBatteryMonitor",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
