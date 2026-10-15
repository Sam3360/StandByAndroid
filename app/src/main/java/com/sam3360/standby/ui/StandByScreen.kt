package com.sam3360.standby.ui

import android.app.Activity
import android.net.Uri
import android.os.BatteryManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sam3360.standby.model.StandByUiState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StandByScreen(
    uiState: StandByUiState,
    onToggleSettings: () -> Unit,
    onToggleNightMode: () -> Unit,
    onTogglePowerSave: () -> Unit,
    onClockStyleChange: (String) -> Unit,
    onToggle24Hour: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onPhotoPick: (List<Uri>) -> Unit,
    onNextPhoto: () -> Unit,
    onPreviousPhoto: () -> Unit,
    onSetupChargingMode: () -> Unit,
    onUpdatePhotoInterval: (Int) -> Unit,
    onToggleKeepScreenOn: () -> Unit
) {
    val accent = if (uiState.nightMode) Color(0xFFFF6B72) else Color(0xFFB9A7FF)
    val background = if (uiState.nightMode) Color(0xFF10090B) else Color(0xFF08080B)
    val textColor = if (uiState.nightMode) Color(0xFFFFE8E8) else Color(0xFFF4F1FA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(background, if (uiState.nightMode) Color(0xFF190B0D) else Color(0xFF111018))
                )
            )
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val landscape = maxWidth > maxHeight
            val availableHeight = maxHeight
            if (landscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 34.dp, vertical = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(38.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.94f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeaderActions(accent, textColor, onToggleNightMode, onToggleSettings)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ClockFace(
                                uiState,
                                accent,
                                textColor,
                                (availableHeight.value * 0.29f).coerceIn(54f, 104f).sp,
                                compact = true
                            )
                            Text(
                                text = formatDate(uiState.currentDate),
                                color = textColor.copy(alpha = 0.62f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        BatterySummary(uiState, accent, textColor)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1.06f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PhotoSlideshowCard(
                            uiState,
                            accent,
                            textColor,
                            onPhotoPick,
                            onNextPhoto,
                            onPreviousPhoto,
                            height = availableHeight * 0.53f
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoCard(
                                title = "Charging setup",
                                subtitle = "Screen saver",
                                icon = Icons.Default.BatteryChargingFull,
                                accent = accent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = onSetupChargingMode)
                            )
                            InfoCard(
                                title = "Up next",
                                subtitle = uiState.upcomingEvents.firstOrNull()?.title ?: "No upcoming events",
                                icon = Icons.Default.Event,
                                accent = accent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    HeaderActions(accent, textColor, onToggleNightMode, onToggleSettings)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ClockFace(uiState, accent, textColor, 78.sp)
                        Text(
                            text = formatDate(uiState.currentDate),
                            color = textColor.copy(alpha = 0.62f),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoCard(
                            title = "Battery",
                            subtitle = "${uiState.batteryLevel}% · ${chargingStateLabel(uiState.chargingState)}",
                            icon = Icons.Default.BatteryChargingFull,
                            accent = accent,
                            modifier = Modifier.weight(1f)
                        )
                        InfoCard(
                            title = "Charging setup",
                            subtitle = "Screen saver",
                            icon = Icons.Default.Timer,
                            accent = accent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onSetupChargingMode)
                        )
                    }
                    if (uiState.upcomingEvents.isNotEmpty()) {
                        EventCard(uiState.upcomingEvents, accent, textColor)
                    } else {
                        EventCardEmpty { onRequestCalendarPermission() }
                    }
                    PhotoSlideshowCard(
                        uiState,
                        accent,
                        textColor,
                        onPhotoPick,
                        onNextPhoto,
                        onPreviousPhoto
                    )
                }
            }

            if (uiState.showSettings) {
                SettingsSheet(
                    uiState = uiState,
                    onDismiss = onToggleSettings,
                    onToggle24Hour = onToggle24Hour,
                    onToggleNightMode = onToggleNightMode,
                    onTogglePowerSave = onTogglePowerSave,
                    onClockStyleChange = onClockStyleChange,
                    onSetupChargingMode = onSetupChargingMode,
                    onUpdatePhotoInterval = onUpdatePhotoInterval,
                    onToggleKeepScreenOn = onToggleKeepScreenOn,
                    onRequestCalendarPermission = onRequestCalendarPermission
                )
            }
        }
    }
}

@Composable
private fun HeaderActions(
    accent: Color,
    textColor: Color,
    onToggleNightMode: () -> Unit,
    onToggleSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("STANDBY", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("A calmer bedside", color = textColor.copy(alpha = 0.55f), fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButtonWithLabel(icon = Icons.Default.NightsStay, label = "Night", onClick = onToggleNightMode)
            IconButtonWithLabel(icon = Icons.Default.Settings, label = "Settings", onClick = onToggleSettings)
        }
    }
}

@Composable
private fun ClockFace(
    uiState: StandByUiState,
    accent: Color,
    textColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    compact: Boolean = false
) {
    when (uiState.settings.clockStyle) {
        "analog" -> AnalogClockCard(uiState, accent, compact)
        "split" -> SplitFlapCard(uiState, accent, compact)
        else -> Text(
            text = formatTime(uiState.currentTime, uiState.settings.use24Hour),
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Light,
            letterSpacing = (-3).sp,
            lineHeight = fontSize
        )
    }
}

@Composable
private fun BatterySummary(uiState: StandByUiState, accent: Color, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Text(
            "${uiState.batteryLevel}%  ${chargingStateLabel(uiState.chargingState)}",
            color = textColor.copy(alpha = 0.72f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun DigitalClockCard(uiState: StandByUiState, accent: Color, textColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C1622).copy(alpha = 0.78f))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatTime(uiState.currentTime, uiState.settings.use24Hour),
                color = accent,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatDate(uiState.currentDate),
                color = textColor.copy(alpha = 0.75f),
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun SplitFlapCard(uiState: StandByUiState, accent: Color, compact: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 130.dp else 180.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101720))
                .padding(if (compact) 12.dp else 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (compact) 44.dp else 56.dp, if (compact) 72.dp else 90.dp)
                            .background(Color(0xFF1E2B38))
                            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (index) {
                                0 -> formatTime(uiState.currentTime, uiState.settings.use24Hour).substringBefore(":").padStart(2, '0').take(2)
                                1 -> formatTime(uiState.currentTime, uiState.settings.use24Hour).substringAfter(":").substringBefore(" ").padStart(2, '0')
                                else -> if (index == 2) "S" else " "
                            },
                            color = accent,
                            fontSize = if (compact) 22.sp else 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalogClockCard(uiState: StandByUiState, accent: Color, compact: Boolean = false) {
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 172.dp else 230.dp)
                .padding(if (compact) 8.dp else 18.dp),
            contentAlignment = Alignment.Center
        ) {
            val seconds = uiState.currentTime.second.toFloat()
            val minutes = uiState.currentTime.minute.toFloat() + seconds / 60f
            val hours = (uiState.currentTime.hour % 12).toFloat() + minutes / 60f
            Canvas(modifier = Modifier.size(if (compact) 150.dp else 180.dp)) {
                drawCircle(color = Color(0xFF111C29), radius = size.minDimension / 2f, style = Stroke(width = 2.dp.toPx()))
                for (i in 0..11) {
                    val angle = Math.toRadians((i * 30.0) - 90.0)
                    val x = center.x + (cos(angle) * 60f).toFloat()
                    val y = center.y + (sin(angle) * 60f).toFloat()
                    drawLine(
                        color = accent,
                        start = Offset(
                            center.x + (cos(angle) * 70f).toFloat(),
                            center.y + (sin(angle) * 70f).toFloat()
                        ),
                        end = Offset(x, y),
                        strokeWidth = 2f
                    )
                }
                val hourAngle = Math.toRadians((hours * 30.0) - 90.0)
                val minuteAngle = Math.toRadians((minutes * 6.0) - 90.0)
                val secondAngle = Math.toRadians((seconds * 6.0) - 90.0)

                drawLine(
                    color = accent,
                    start = Offset(center.x, center.y),
                    end = Offset(
                        center.x + (cos(hourAngle) * 40f).toFloat(),
                        center.y + (sin(hourAngle) * 40f).toFloat()
                    ),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFB8D9FF),
                    start = Offset(center.x, center.y),
                    end = Offset(
                        center.x + (cos(minuteAngle) * 60f).toFloat(),
                        center.y + (sin(minuteAngle) * 60f).toFloat()
                    ),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFEF5350),
                    start = Offset(center.x, center.y),
                    end = Offset(
                        center.x + (cos(secondAngle) * 70f).toFloat(),
                        center.y + (sin(secondAngle) * 70f).toFloat()
                    ),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = accent, radius = 8f)
            }
        }
    }
}

@Composable
private fun EventCard(events: List<com.sam3360.standby.model.CalendarEvent>, accent: Color, textColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C1420))
                .padding(18.dp)
        ) {
            Text(text = "Upcoming", color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            events.take(3).forEach { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = accent)
                    Column {
                        Text(event.title, color = textColor, fontWeight = FontWeight.SemiBold)
                        val time = java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM • HH:mm")
                            .format(java.time.Instant.ofEpochMilli(event.startTimeMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                        Text(time, color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                        event.location?.let {
                            Text(it, color = textColor.copy(alpha = 0.65f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCardEmpty(onRequestCalendarPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C1420))
                .padding(18.dp)
        ) {
            Text("Upcoming events", color = Color(0xFF7EC8FF), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("No calendar access yet. Enable event access to show upcoming items.", color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRequestCalendarPermission) {
                Text("Enable calendar access")
            }
        }
    }
}

@Composable
private fun PhotoSlideshowCard(
    uiState: StandByUiState,
    accent: Color,
    textColor: Color,
    onPhotoPick: (List<Uri>) -> Unit,
    onNextPhoto: () -> Unit,
    onPreviousPhoto: () -> Unit,
    height: androidx.compose.ui.unit.Dp = 220.dp
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (uris.isNotEmpty()) onPhotoPick(uris) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Box(modifier = Modifier.height(height).fillMaxWidth().background(Color(0xFF0B1220))) {
            val currentPhoto = uiState.selectedPhotos.getOrNull(uiState.settings.photoIndex)
            if (currentPhoto != null) {
                AsyncImage(
                    model = currentPhoto,
                    contentDescription = "StandBy photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No photos selected", color = textColor)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { photoPicker.launch("image/*") }) {
                        Text("Select photos")
                    }
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { onPreviousPhoto() }) { Text("Prev") }
                OutlinedButton(onClick = { onNextPhoto() }) { Text("Next") }
                OutlinedButton(onClick = { photoPicker.launch("image/*") }) { Text("Add") }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1520))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text(title, color = accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsSheet(
    uiState: StandByUiState,
    onDismiss: () -> Unit,
    onToggle24Hour: () -> Unit,
    onToggleNightMode: () -> Unit,
    onTogglePowerSave: () -> Unit,
    onClockStyleChange: (String) -> Unit,
    onSetupChargingMode: () -> Unit,
    onUpdatePhotoInterval: (Int) -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onRequestCalendarPermission: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("StandBy settings", style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("24-hour clock")
                Switch(checked = uiState.settings.use24Hour, onCheckedChange = { onToggle24Hour() })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Night mode")
                Switch(checked = uiState.nightMode, onCheckedChange = { onToggleNightMode() })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Power save")
                Switch(checked = uiState.settings.powerSaveMode, onCheckedChange = { onTogglePowerSave() })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Keep screen on")
                Switch(checked = uiState.settings.keepScreenOn, onCheckedChange = { onToggleKeepScreenOn() })
            }
            Text(
                "To start StandBy while charging, enable it as your Android screen saver and choose the charging trigger."
            )
            Button(onClick = onSetupChargingMode) { Text("Set up charging screen") }
            Text("Clock style")
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("digital", "analog", "split").forEach { style ->
                    Button(onClick = { onClockStyleChange(style) }) {
                        Text(style.replaceFirstChar { it.uppercase() })
                    }
                }
            }
            Text("Photo interval: ${uiState.settings.photoIntervalSeconds}s")
            Slider(
                value = uiState.settings.photoIntervalSeconds.toFloat(),
                onValueChange = { onUpdatePhotoInterval(it.toInt()) },
                valueRange = 4f..20f,
                steps = 15
            )
            Button(onClick = onRequestCalendarPermission) { Text("Grant calendar access") }
        }
    }
}

@Composable
private fun IconButtonWithLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(shape = CircleShape, color = Color(0xFF0F1B28), modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = Color.White)
            }
        }
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

private fun formatTime(time: java.time.LocalDateTime, use24Hour: Boolean): String {
    val pattern = if (use24Hour) "HH:mm" else "hh:mm a"
    return time.format(DateTimeFormatter.ofPattern(pattern))
}

private fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
}

private fun chargingStateLabel(state: Int): String {
    return when (state) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        else -> "Unknown"
    }
}
