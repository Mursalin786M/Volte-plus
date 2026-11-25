package com.voltpulse.monitor.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesStore(context: Context) {

    private val prefs = context.getSharedPreferences("voltpulse_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAlertSettings(): AlertSettings {
        val json = prefs.getString("alert_settings", null) ?: return AlertSettings()
        return try {
            gson.fromJson(json, AlertSettings::class.java) ?: AlertSettings()
        } catch (_: Exception) {
            AlertSettings()
        }
    }

    fun saveAlertSettings(settings: AlertSettings) {
        prefs.edit().putString("alert_settings", gson.toJson(settings)).apply()
    }

    fun getSnapshots(): List<BatterySnapshot> {
        val json = prefs.getString("battery_snapshots", null) ?: return emptyList()
        val type = object : TypeToken<List<BatterySnapshot>>() {}.type
        return try {
            gson.fromJson<List<BatterySnapshot>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addSnapshot(snapshot: BatterySnapshot) {
        val current = getSnapshots().toMutableList()
        current.add(snapshot)
        // Keep last 96 snapshots (~24-48 hours)
        val trimmed = if (current.size > 96) current.takeLast(96) else current
        prefs.edit().putString("battery_snapshots", gson.toJson(trimmed)).apply()
    }

    fun getLastAlertTime(key: String): Long = prefs.getLong("last_alert_$key", 0L)

    fun setLastAlertTime(key: String, timeMs: Long) {
        prefs.edit().putLong("last_alert_$key", timeMs).apply()
    }
}
