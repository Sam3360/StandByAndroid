package com.sam3360.standby

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.os.BatteryManager
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.View
import android.graphics.Canvas
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import coil.ImageLoader
import coil.request.ImageRequest
import com.sam3360.standby.model.StandByUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.format.DateTimeFormatter

class StandByDreamService : DreamService(), ViewModelStoreOwner {
    private val dreamViewModelStore = ViewModelStore()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val imageLoaderDelegate = lazy { ImageLoader(this) }
    private val imageLoader by imageLoaderDelegate
    private lateinit var viewModel: StandByViewModel
    private lateinit var clockView: TextView
    private lateinit var analogClockView: DreamAnalogClockView
    private lateinit var dateView: TextView
    private lateinit var batteryView: TextView
    private lateinit var photoView: ImageView
    private lateinit var emptyPhotoView: TextView
    private var displayJob: Job? = null
    private var displayedPhoto: android.net.Uri? = null
    private var batteryReceiverRegistered = false

    override val viewModelStore: ViewModelStore
        get() = dreamViewModelStore

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateBattery(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(StandByViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return StandByViewModel(applicationContext) as T
                }
                throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
            }
        })[StandByViewModel::class.java]

        isInteractive = true
        isFullscreen = true
        isScreenBright = false
        setContentView(createDreamView())
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        if (displayJob?.isActive != true) {
            displayJob = serviceScope.launch {
                viewModel.uiState.collect(::renderState)
            }
        }
        if (!batteryReceiverRegistered) {
            val stickyBatteryIntent = ContextCompat.registerReceiver(
                this,
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            batteryReceiverRegistered = true
            stickyBatteryIntent?.let(::updateBattery)
        }
        viewModel.loadCalendarEvents(this)
    }

    override fun onDreamingStopped() {
        unregisterBatteryReceiver()
        displayJob?.cancel()
        displayJob = null
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        unregisterBatteryReceiver()
        serviceScope.cancel()
        if (imageLoaderDelegate.isInitialized()) {
            imageLoader.shutdown()
        }
        dreamViewModelStore.clear()
        super.onDestroy()
    }

    private fun createDreamView(): View {
        val landscape = resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(7, 8, 11))
            contentDescription = "StandBy screen saver. Tap to exit."
            isClickable = true
            isFocusable = true
            setOnClickListener { wakeUp() }
        }
        val content = LinearLayout(this).apply {
            orientation = if (landscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(if (landscape) 42 else 24), dp(24), dp(if (landscape) 42 else 24), dp(24))
        }

        val clockPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (landscape) Gravity.CENTER_VERTICAL or Gravity.START else Gravity.CENTER
        }
        val accent = Color.rgb(213, 201, 255)
        clockPanel.addView(
            textView("STANDBY", 12f, accent, bold = true).apply { letterSpacing = 0.24f },
            layoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        )
        clockView = textView("", if (landscape) 82f else 68f, Color.rgb(244, 241, 250))
        analogClockView = DreamAnalogClockView(this).apply {
            visibility = View.GONE
        }
        val clockDisplay = FrameLayout(this).apply {
            addView(
                clockView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL or if (landscape) Gravity.START else Gravity.CENTER_HORIZONTAL
                )
            )
            addView(
                analogClockView,
                FrameLayout.LayoutParams(dp(if (landscape) 150 else 176), dp(if (landscape) 150 else 176))
            )
        }
        clockPanel.addView(
            clockDisplay,
            layoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(if (landscape) 160 else 184),
                top = if (landscape) 18 else 10
            )
        )
        dateView = textView("", 16f, Color.rgb(174, 172, 184))
        clockPanel.addView(
            dateView,
            layoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                top = 8
            )
        )
        batteryView = textView("", 14f, accent)
        clockPanel.addView(
            batteryView,
            layoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                top = 18
            )
        )

        val photoPanel = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.rgb(16, 17, 22))
                cornerRadius = dp(28).toFloat()
            }
            clipToOutline = true
        }
        photoView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        emptyPhotoView = textView("A calmer bedside", 16f, accent)
        photoPanel.addView(
            photoView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        photoPanel.addView(
            emptyPhotoView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        if (landscape) {
            content.addView(clockPanel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.1f))
            content.addView(
                photoPanel,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.9f).apply {
                    marginStart = dp(36)
                }
            )
        } else {
            content.addView(clockPanel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ))
            content.addView(
                photoPanel,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(170))
            )
        }
        root.addView(
            content,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        return root
    }

    private fun renderState(state: StandByUiState) {
        val accent = if (state.nightMode) Color.rgb(255, 154, 139) else Color.rgb(201, 185, 255)
        clockView.text = when (state.settings.clockStyle) {
            "split" -> state.currentTime.format(DateTimeFormatter.ofPattern("HH  mm"))
            else -> state.currentTime.format(
                DateTimeFormatter.ofPattern(if (state.settings.use24Hour) "HH:mm" else "h:mm a")
            )
        }
        clockView.typeface = if (state.settings.clockStyle == "split") {
            android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        } else {
            android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
        }
        clockView.visibility = if (state.settings.clockStyle == "analog") View.GONE else View.VISIBLE
        analogClockView.visibility = if (state.settings.clockStyle == "analog") View.VISIBLE else View.GONE
        analogClockView.setClock(state.currentTime, accent)
        dateView.text = state.currentDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
        batteryView.text = getString(
            R.string.battery_summary,
            state.batteryLevel,
            chargingStateLabel(state.chargingState)
        )
        batteryView.setTextColor(accent)

        val photo = state.selectedPhotos.getOrNull(state.settings.photoIndex)
        if (photo != displayedPhoto) {
            displayedPhoto = photo
            photoView.setImageDrawable(null)
            photoView.visibility = if (photo == null) View.GONE else View.VISIBLE
            emptyPhotoView.visibility = if (photo == null) View.VISIBLE else View.GONE
            photo?.let {
                imageLoader.enqueue(
                    ImageRequest.Builder(this)
                        .data(it)
                        .placeholder(ColorDrawable(Color.rgb(16, 17, 22)))
                        .error(ColorDrawable(Color.rgb(16, 17, 22)))
                        .target(photoView)
                        .build()
                )
            }
        }
    }

    private fun updateBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        viewModel.updateBattery(level, scale, status)
    }

    private class DreamAnalogClockView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var time = java.time.LocalDateTime.now()
        private var accent = Color.rgb(201, 185, 255)

        fun setClock(time: java.time.LocalDateTime, accent: Int) {
            this.time = time
            this.accent = accent
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = minOf(width, height) * 0.46f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = minOf(width, height) * 0.012f
            paint.color = Color.rgb(48, 47, 57)
            canvas.drawCircle(cx, cy, radius, paint)

            for (index in 0 until 60) {
                val angle = Math.toRadians(index * 6.0 - 90.0)
                val major = index % 5 == 0
                val innerRadius = radius * if (major) 0.86f else 0.93f
                paint.color = if (major) accent else Color.rgb(100, 98, 110)
                paint.strokeWidth = minOf(width, height) * if (major) 0.014f else 0.006f
                canvas.drawLine(
                    cx + (kotlin.math.cos(angle) * innerRadius).toFloat(),
                    cy + (kotlin.math.sin(angle) * innerRadius).toFloat(),
                    cx + (kotlin.math.cos(angle) * radius).toFloat(),
                    cy + (kotlin.math.sin(angle) * radius).toFloat(),
                    paint
                )
            }

            val minuteValue = time.minute + time.second / 60f
            drawHand(
                canvas,
                cx,
                cy,
                radius * 0.52f,
                time.hour % 12 / 12f + minuteValue / 720f,
                accent,
                radius * 0.055f
            )
            drawHand(canvas, cx, cy, radius * 0.74f, minuteValue / 60f, Color.WHITE, radius * 0.035f)
            paint.style = Paint.Style.FILL
            paint.color = accent
            canvas.drawCircle(cx, cy, radius * 0.06f, paint)
        }

        private fun drawHand(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            length: Float,
            fraction: Float,
            color: Int,
            strokeWidth: Float
        ) {
            val angle = Math.toRadians(fraction * 360.0 - 90.0)
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = strokeWidth
            paint.color = color
            canvas.drawLine(
                cx,
                cy,
                cx + (kotlin.math.cos(angle) * length).toFloat(),
                cy + (kotlin.math.sin(angle) * length).toFloat(),
                paint
            )
        }
    }

    private fun unregisterBatteryReceiver() {
        if (batteryReceiverRegistered) {
            unregisterReceiver(batteryReceiver)
            batteryReceiverRegistered = false
        }
    }

    private fun textView(text: String, sizeSp: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            gravity = if (resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
            ) Gravity.START else Gravity.CENTER
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun layoutParams(width: Int, height: Int, top: Int = 0) =
        LinearLayout.LayoutParams(width, height).apply {
            if (top > 0) topMargin = dp(top)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun chargingStateLabel(state: Int): String = when (state) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        else -> "Unknown"
    }
}
