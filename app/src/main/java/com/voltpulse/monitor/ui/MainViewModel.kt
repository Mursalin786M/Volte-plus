package com.voltpulse.monitor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltpulse.monitor.data.AlertSettings
import com.voltpulse.monitor.data.AppUsageItem
import com.voltpulse.monitor.data.BatteryLiveStatus
import com.voltpulse.monitor.data.BatteryRepository
import com.voltpulse.monitor.data.BatterySnapshot
import com.voltpulse.monitor.data.DailyScreenSummary
import com.voltpulse.monitor.data.PreferencesStore
import com.voltpulse.monitor.data.UsageStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val battery: BatteryLiveStatus = BatteryLiveStatus(),
    val hasUsagePermission: Boolean = false,
    val selectedRangeDays: Int = 0, // 0 = Today, 7 = 7 Days
    val appUsageList: List<AppUsageItem> = emptyList(),
    val weeklySummary: List<DailyScreenSummary> = emptyList(),
    val snapshots: List<BatterySnapshot> = emptyList(),
    val settings: AlertSettings = AlertSettings(),
    val isLoading: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val batteryRepo = BatteryRepository(application)
    private val usageRepo = UsageStatsRepository(application)
    private val prefs = PreferencesStore(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
        startLiveBatteryPolling()
    }

    private fun startLiveBatteryPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val settings = prefs.getAlertSettings()
                val live = batteryRepo.getLiveBatteryStatus(settings.customCapacityMah)
                _uiState.value = _uiState.value.copy(battery = live)
                delay(3000L)
            }
        }
    }

    fun setUsageRange(days: Int) {
        _uiState.value = _uiState.value.copy(selectedRangeDays = days)
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = prefs.getAlertSettings()
            val live = batteryRepo.getLiveBatteryStatus(settings.customCapacityMah)
            val hasPerm = usageRepo.hasUsageStatsPermission()
            val apps = if (hasPerm) {
                usageRepo.getAppUsageForRange(_uiState.value.selectedRangeDays, settings.customCapacityMah)
            } else emptyList()
            val weekly = if (hasPerm) usageRepo.getLast7DaysSummary() else emptyList()
            val snaps = prefs.getSnapshots()

            _uiState.value = _uiState.value.copy(
                battery = live,
                hasUsagePermission = hasPerm,
                appUsageList = apps,
                weeklySummary = weekly,
                snapshots = snaps,
                settings = settings,
                isLoading = false
            )
        }
    }

    fun updateSettings(newSettings: AlertSettings) {
        prefs.saveAlertSettings(newSettings)
        _uiState.value = _uiState.value.copy(settings = newSettings)
        refreshAll()
    }
}
