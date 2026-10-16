package com.sam3360.standby.ui

import android.net.Uri
import android.os.BatteryManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sam3360.standby.model.CalendarEvent
import com.sam3360.standby.model.StandByUiState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val ScreenBackground = Color(0xFF08090D)
private val CardBackground = Color(0xFF15161D)
private val MutedText = Color(0xFFA5A4AF)
private val Lavender = Color(0xFFC9B9FF)
private val NightAccent = Color(0xFFFF9A8B)

@Composable
fun StandByScreen(
    uiState: StandByUiState,
    onToggleSettings: () -> Unit,
    onToggleNightMode: () -> Unit,
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
    val accent = if (uiState.nightMode) NightAccent else Lavender
    val textColor = Color(0xFFF3F1F7)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (uiState.nightMode) {
                        listOf(Color(0xFF1A1012), Color(0xFF100B0D), ScreenBackground)
                    } else {
                        listOf(Color(0xFF111118), ScreenBackground, ScreenBackground)
                    }
                )
            )
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val landscape = maxWidth > maxHeight
            val availableHeight = maxHeight.value
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (landscape) 28.dp else 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(if (landscape) 14.dp else 18.dp)
            ) {
                Header(
                    accent = accent,
                    onNightMode = onToggleNightMode,
                    onSettings = onToggleSettings
                )

                if (landscape) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(26.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.94f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            ClockPanel(uiState, textColor, availableHeight, landscape = true)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BatteryTile(
                                    level = uiState.batteryLevel,
                                    state = uiState.chargingState,
                                    accent = accent,
                                    modifier = Modifier.weight(1f)
                                )
                                ChargingSetupTile(
                                    accent = accent,
                                    modifier = Modifier.weight(1f),
                                    onClick = onSetupChargingMode
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1.06f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PhotoCard(
                                uiState = uiState,
                                accent = accent,
                                onPhotoPick = onPhotoPick,
                                onNextPhoto = onNextPhoto,
                                onPreviousPhoto = onPreviousPhoto,
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.upcomingEvents.isNotEmpty()) {
                                UpcomingEventTile(uiState.upcomingEvents.first(), accent)
                            } else {
                                CalendarHintTile(accent, onRequestCalendarPermission)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        ClockPanel(uiState, textColor, availableHeight, landscape = false)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BatteryTile(
                                level = uiState.batteryLevel,
                                state = uiState.chargingState,
                                accent = accent,
                                modifier = Modifier.weight(1f)
                            )
                            ChargingSetupTile(
                                accent = accent,
                                modifier = Modifier.weight(1f),
                                onClick = onSetupChargingMode
                            )
                        }
                        uiState.upcomingEvents.firstOrNull()?.let {
                            UpcomingEventTile(it, accent)
                        }
                        PhotoCard(
                            uiState = uiState,
                            accent = accent,
                            onPhotoPick = onPhotoPick,
                            onNextPhoto = onNextPhoto,
                            onPreviousPhoto = onPreviousPhoto,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.55f)
                        )
                        if (uiState.upcomingEvents.isEmpty()) {
                            CalendarHintTile(accent, onRequestCalendarPermission)
                        }
                    }
                }
            }
        }

        if (uiState.showSettings) {
            SettingsSheet(
                uiState = uiState,
                onDismiss = onToggleSettings,
                onToggle24Hour = onToggle24Hour,
                onToggleNightMode = onToggleNightMode,
                onClockStyleChange = onClockStyleChange,
                onSetupChargingMode = onSetupChargingMode,
                onUpdatePhotoInterval = onUpdatePhotoInterval,
                onToggleKeepScreenOn = onToggleKeepScreenOn,
                onRequestCalendarPermission = onRequestCalendarPermission
            )
        }
    }
}

@Composable
private fun Header(accent: Color, onNightMode: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "standby",
                color = Color(0xFFF5F2FA),
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )
            Text("Your bedside, at a glance", color = MutedText, fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNightMode) {
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = "Toggle night mode",
                    tint = accent
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open settings",
                    tint = Color(0xFFE1DFE8)
                )
            }
        }
    }
}

@Composable
private fun ClockPanel(
    uiState: StandByUiState,
    textColor: Color,
    availableHeight: Float,
    landscape: Boolean
) {
    val accent = if (uiState.nightMode) NightAccent else Lavender
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (landscape) 4.dp else 10.dp),
        horizontalAlignment = if (landscape) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (uiState.settings.clockStyle) {
            "analog" -> AnalogClock(
                time = uiState.currentTime,
                accent = accent,
                modifier = Modifier
                    .size(if (landscape) (availableHeight * 0.34f).coerceIn(120f, 190f).dp else 190.dp)
                    .align(if (landscape) Alignment.Start else Alignment.CenterHorizontally)
            )
            "split" -> SplitClock(
                uiState.currentTime,
                uiState.settings.use24Hour,
                textColor,
                accent
            )
            else -> {
                val clockSize = if (landscape) {
                    (availableHeight * 0.32f).coerceIn(54f, 104f).sp
                } else {
                    82.sp
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (landscape) Arrangement.Start else Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formatTime(uiState.currentTime, uiState.settings.use24Hour),
                        color = textColor,
                        fontSize = clockSize,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-3).sp,
                        lineHeight = clockSize,
                        maxLines = 1
                    )
                    if (!uiState.settings.use24Hour) {
                        Text(
                            text = if (uiState.currentTime.hour < 12) "AM" else "PM",
                            modifier = Modifier.padding(start = 8.dp, bottom = 11.dp),
                            color = MutedText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        Text(
            text = formatDate(uiState.currentDate),
            color = MutedText,
            fontSize = if (landscape) 16.sp else 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SplitClock(time: LocalDateTime, use24Hour: Boolean, textColor: Color, accent: Color) {
    val hour = if (use24Hour) time.hour else time.hour % 12.let { if (it == 0) 12 else it }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SplitDigitPair(hour.toString().padStart(2, '0'), textColor)
        Text(":", color = accent, fontSize = 40.sp, fontWeight = FontWeight.Light)
        SplitDigitPair(time.minute.toString().padStart(2, '0'), textColor)
        if (!use24Hour) {
            Text(if (time.hour < 12) "AM" else "PM", color = MutedText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SplitDigitPair(value: String, textColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        value.forEach { digit ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF20212A)
            ) {
                Text(
                    text = digit.toString(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = textColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun AnalogClock(time: LocalDateTime, accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.46f
        val tickInner = radius * 0.88f
        drawCircle(
            color = Color(0xFF292833),
            radius = radius,
            style = Stroke(width = size.minDimension * 0.012f)
        )
        for (index in 0 until 60) {
            val angle = Math.toRadians(index * 6.0 - 90.0)
            val major = index % 5 == 0
            val inner = if (major) tickInner else radius * 0.94f
            drawLine(
                color = if (major) accent.copy(alpha = 0.85f) else Color(0xFF666471),
                start = Offset(
                    center.x + (cos(angle) * inner).toFloat(),
                    center.y + (sin(angle) * inner).toFloat()
                ),
                end = Offset(
                    center.x + (cos(angle) * radius).toFloat(),
                    center.y + (sin(angle) * radius).toFloat()
                ),
                strokeWidth = if (major) size.minDimension * 0.014f else size.minDimension * 0.006f,
                cap = StrokeCap.Round
            )
        }
        val minuteValue = time.minute + time.second / 60f
        val hourValue = time.hour % 12 + minuteValue / 60f
        drawClockHand(hourValue / 12f, radius * 0.52f, size.minDimension * 0.035f, accent)
        drawClockHand(minuteValue / 60f, radius * 0.75f, size.minDimension * 0.022f, Color(0xFFE8E4F1))
        drawCircle(color = accent, radius = size.minDimension * 0.035f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClockHand(
    fraction: Float,
    handLength: Float,
    strokeWidth: Float,
    color: Color
) {
    val angle = Math.toRadians(fraction * 360.0 - 90.0)
    drawLine(
        color = color,
        start = center,
        end = Offset(
            center.x + (cos(angle) * handLength).toFloat(),
            center.y + (sin(angle) * handLength).toFloat()
        ),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

@Composable
private fun BatteryTile(
    level: Int,
    state: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = CardBackground) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = accent)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("$level%", color = Color(0xFFF3F1F7), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(chargingStateLabel(state), color = MutedText, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ChargingSetupTile(
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = CardBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = accent)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Charging mode", color = Color(0xFFF3F1F7), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Set up screen saver", color = MutedText, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PhotoCard(
    uiState: StandByUiState,
    accent: Color,
    onPhotoPick: (List<Uri>) -> Unit,
    onNextPhoto: () -> Unit,
    onPreviousPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onPhotoPick(uris)
    }
    val currentPhoto = uiState.selectedPhotos.getOrNull(uiState.settings.photoIndex)
    val surfaceShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .clip(surfaceShape)
            .background(Color(0xFF171720))
    ) {
        if (currentPhoto != null) {
            AsyncImage(
                model = currentPhoto,
                contentDescription = "Selected bedside photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.30f),
                                0.38f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.62f)
                            )
                        )
                    }
            )
            Text(
                text = "YOUR PHOTOS",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PhotoControl("Previous photo", Icons.AutoMirrored.Filled.NavigateBefore, onPreviousPhoto)
                PhotoControl("Next photo", Icons.AutoMirrored.Filled.NavigateNext, onNextPhoto)
                PhotoControl("Add photos", Icons.Default.Add) {
                    photoPicker.launch(arrayOf("image/*"))
                }
                Text(
                    text = "${uiState.settings.photoIndex + 1} / ${uiState.selectedPhotos.size}",
                    modifier = Modifier.padding(start = 4.dp),
                    color = Color.White.copy(alpha = 0.84f),
                    fontSize = 12.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { photoPicker.launch(arrayOf("image/*")) }
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(shape = CircleShape, color = Color(0xFF24232E)) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.padding(13.dp).size(25.dp),
                        tint = accent
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Make it yours", color = Color(0xFFF3F1F7), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("Choose photos for your bedside display", color = MutedText, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { photoPicker.launch(arrayOf("image/*")) }) {
                    Text("Choose photos")
                }
            }
        }
    }
}

@Composable
private fun PhotoControl(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.36f)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp)) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun UpcomingEventTile(event: CalendarEvent, accent: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = CardBackground) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Event, contentDescription = null, tint = accent)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("UP NEXT", color = accent, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
                Text(event.title, color = Color(0xFFF3F1F7), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatEventTime(event.startTimeMillis), color = MutedText, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CalendarHintTile(accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = CardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Event, contentDescription = null, tint = accent)
            Column {
                Text("Add your calendar", color = Color(0xFFF3F1F7), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Show your next event here", color = MutedText, fontSize = 11.sp)
            }
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
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFF3F1F7))
            Text("Make your bedside display feel right.", color = MutedText, fontSize = 14.sp)
            SettingsSwitchRow("24-hour time", uiState.settings.use24Hour, onToggle24Hour)
            SettingsSwitchRow("Night colors", uiState.nightMode, onToggleNightMode)
            SettingsSwitchRow("Keep screen on in app", uiState.settings.keepScreenOn, onToggleKeepScreenOn)

            Text("Clock style", color = Color(0xFFF3F1F7), fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("digital", "analog", "split").forEach { style ->
                    FilterChip(
                        selected = uiState.settings.clockStyle == style,
                        onClick = { onClockStyleChange(style) },
                        label = { Text(style.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Photo interval", color = Color(0xFFF3F1F7), fontWeight = FontWeight.Medium)
                Text("Change photos every ${uiState.settings.photoIntervalSeconds} seconds", color = MutedText, fontSize = 12.sp)
                Slider(
                    value = uiState.settings.photoIntervalSeconds.toFloat(),
                    onValueChange = { onUpdatePhotoInterval(it.toInt()) },
                    valueRange = 4f..20f,
                    steps = 15
                )
            }

            Button(onClick = onSetupChargingMode, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Set up charging screen")
            }
            Button(onClick = onRequestCalendarPermission, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Event, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Enable calendar events")
            }
            Text(
                "Android starts the screen saver when your phone is idle; it may not appear as soon as you plug in.",
                color = MutedText,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFE9E7EF), fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = { onCheckedChange() })
    }
}

private fun formatTime(time: LocalDateTime, use24Hour: Boolean): String {
    val pattern = if (use24Hour) "HH:mm" else "h:mm"
    return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

private fun formatEventTime(timeMillis: Long): String =
    java.time.Instant.ofEpochMilli(timeMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm", Locale.getDefault()))

private fun chargingStateLabel(state: Int): String = when (state) {
    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
    BatteryManager.BATTERY_STATUS_FULL -> "Fully charged"
    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Not charging"
    else -> "Battery status unavailable"
}
