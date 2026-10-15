package com.sam3360.standby

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.View
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
import java.time.format.DateTimeFormatter

class StandByDreamService : DreamService(), ViewModelStoreOwner {
    private val dreamViewModelStore = ViewModelStore()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val imageLoaderDelegate = lazy { ImageLoader(this) }
    private val imageLoader by imageLoaderDelegate
    private lateinit var viewModel: StandByViewModel
    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private lateinit var batteryView: TextView
    private lateinit var photoView: ImageView
    private lateinit var emptyPhotoView: TextView
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
        setContentView(createDreamView())
        serviceScope.launch {
            viewModel.uiState.collect(::renderState)
        }
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        val stickyBatteryIntent = ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        batteryReceiverRegistered = true
        stickyBatteryIntent?.let(::updateBattery)
        viewModel.loadCalendarEvents(this)
    }

    override fun onDreamingStopped() {
        unregisterBatteryReceiver()
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
        clockPanel.addView(
            clockView,
            layoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
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
        val accent = if (state.nightMode) Color.rgb(255, 119, 125) else Color.rgb(213, 201, 255)
        clockView.text = state.currentTime.format(
            DateTimeFormatter.ofPattern(if (state.settings.use24Hour) "HH:mm" else "hh:mm a")
        )
        dateView.text = state.currentDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
        batteryView.text = "${state.batteryLevel}%  ${chargingStateLabel(state.chargingState)}"
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
