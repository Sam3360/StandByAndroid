package com.sam3360.standby

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sam3360.standby.data.SettingsRepository
import com.sam3360.standby.ui.StandByScreen
import com.sam3360.standby.ui.theme.StandByTheme

class MainActivity : ComponentActivity() {
    private val viewModel by lazy { StandByViewModel(applicationContext, this) }

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCalendarPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            DisposableEffect(state.settings.keepScreenOn) {
                if (state.settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            StandByTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StandByScreen(
                        uiState = state,
                        onToggleSettings = { viewModel.toggleSettings() },
                        onToggleNightMode = { viewModel.toggleNightMode() },
                        onTogglePowerSave = { viewModel.togglePowerSave() },
                        onClockStyleChange = { viewModel.updateClockStyle(it) },
                        onToggle24Hour = { viewModel.update24Hour(!state.settings.use24Hour) },
                        onRequestCalendarPermission = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.READ_CALENDAR
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                viewModel.loadCalendarEvents(this)
                            } else {
                                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }
                        },
                        onPhotoPick = { uris -> viewModel.setPhotoSelection(uris) },
                        onNextPhoto = { viewModel.nextPhoto() },
                        onPreviousPhoto = { viewModel.previousPhoto() },
                        onToggleAuto = { viewModel.toggleAutoActivate() },
                        onUpdatePhotoInterval = { viewModel.updatePhotoInterval(it) },
                        onToggleKeepScreenOn = { viewModel.setKeepScreenOn(!state.settings.keepScreenOn) }
                    )
                }
            }
        }

        registerBatteryReceiver()
        viewModel.loadCalendarEvents(this)
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                viewModel.updateBattery(level, scale, status)
            }
        }, filter)
    }
}
