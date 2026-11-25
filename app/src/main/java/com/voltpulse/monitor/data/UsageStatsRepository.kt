package com.voltpulse.monitor.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UsageStatsRepository(private val context: Context) {

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getAppUsageForRange(
        daysBack: Int,
        capacityMah: Int
    ): List<AppUsageItem> {
        if (!hasUsageStatsPermission()) return emptyList()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager

        val cal = Calendar.getInstance()
        val endTime = System.currentTimeMillis()
        if (daysBack == 0) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -daysBack)
        }
        val startTime = cal.timeInMillis

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            ?: return emptyList()

        // Aggregate launch counts via UsageEvents
        val launchCounts = mutableMapOf<String, Int>()
        val events = usm.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                launchCounts[event.packageName] = (launchCounts[event.packageName] ?: 0) + 1
            }
        }

        val aggregated = mutableMapOf<String, Long>()
        val lastUsedMap = mutableMapOf<String, Long>()

        for (usage in stats) {
            if (usage.totalTimeInForeground > 0) {
                aggregated[usage.packageName] =
                    (aggregated[usage.packageName] ?: 0L) + usage.totalTimeInForeground
                val prevLast = lastUsedMap[usage.packageName] ?: 0L
                if (usage.lastTimeUsed > prevLast) {
                    lastUsedMap[usage.packageName] = usage.lastTimeUsed
                }
            }
        }

        val totalScreenTimeMs = aggregated.values.sum().coerceAtLeast(1L)

        // Typical active screen+SoC drain ~ 550 mA average across apps
        val avgActiveDrainMa = 550f

        return aggregated.entries
            .filter { it.value >= 10_000L } // >= 10 seconds
            .mapNotNull { (pkg, timeMs) ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    val hours = timeMs / 3_600_000f
                    val estMah = hours * avgActiveDrainMa
                    val estPct = if (capacityMah > 0) (estMah / capacityMah) * 100f else 0f
                    val share = timeMs.toFloat() / totalScreenTimeMs.toFloat()

                    AppUsageItem(
                        packageName = pkg,
                        appName = appName,
                        icon = icon,
                        foregroundTimeMs = timeMs,
                        launchCount = launchCounts[pkg] ?: 1,
                        lastTimeUsedMs = lastUsedMap[pkg] ?: 0L,
                        estimatedBatteryPercent = estPct,
                        estimatedBatteryMah = estMah,
                        shareOfScreenTime = share
                    )
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedByDescending { it.foregroundTimeMs }
    }

    fun getLast7DaysSummary(): List<DailyScreenSummary> {
        if (!hasUsageStatsPermission()) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val dayFormat = SimpleDateFormat("EEE", Locale.ENGLISH)
        val result = mutableListOf<DailyScreenSummary>()

        for (i in 6 downTo 0) {
            val startCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = startCal.timeInMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startCal.timeInMillis,
                minOf(endCal.timeInMillis, System.currentTimeMillis())
            ) ?: emptyList()

            val totalMin = stats.sumOf { it.totalTimeInForeground } / 60_000L
            result.add(DailyScreenSummary(dayFormat.format(startCal.time), totalMin))
        }
        return result
    }
}
