package com.voltpulse.monitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.voltpulse.monitor.ui.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            MaterialTheme {
                VoltPulseMainScaffold(
                    viewModel = viewModel,
                    onGrantUsageClick = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAll()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoltPulseMainScaffold(
    viewModel: MainViewModel,
    onGrantUsageClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VoltPulse • Battery & Screen Time") }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Overview") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    label = { Text("Screen Time") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.BatteryChargingFull, contentDescription = null) },
                    label = { Text("Drain") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    label = { Text("Alerts") }
                )
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> DashboardScreen(state = state, onGrantUsageClick = onGrantUsageClick)
                1 -> ScreenTimeScreen(
                    state = state,
                    onRangeChange = { viewModel.setUsageRange(it) },
                    onGrantUsageClick = onGrantUsageClick
                )
                2 -> BatteryDrainScreen(state = state, onGrantUsageClick = onGrantUsageClick)
                3 -> AlertsSettingsScreen(
                    settings = state.settings,
                    onSave = { viewModel.updateSettings(it) }
                )
            }
        }
    }
}
