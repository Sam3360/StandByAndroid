package com.sam3360.standby

import android.os.BatteryManager
import com.sam3360.standby.util.StandByLogic
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class StandByLogicTest {
    @Test
    fun `time formatting uses 24 hour by default`() {
        val time = LocalDateTime.of(2024, 1, 2, 14, 7, 3)
        assertEquals("14:07", StandByLogic.formatTime(time, true))
    }

    @Test
    fun `time formatting uses AMPM when requested`() {
        val time = LocalDateTime.of(2024, 1, 2, 14, 7, 3)
        assertEquals("02:07 PM", StandByLogic.formatTime(time, false))
    }

    @Test
    fun `battery percentage is calculated correctly`() {
        assertEquals(50, StandByLogic.batteryPercent(50, 100))
        assertEquals(25, StandByLogic.batteryPercent(1, 4))
    }

    @Test
    fun `charging labels are interpreted correctly`() {
        assertEquals("Charging", StandByLogic.chargingStateLabel(BatteryManager.BATTERY_STATUS_CHARGING))
        assertEquals("Full", StandByLogic.chargingStateLabel(BatteryManager.BATTERY_STATUS_FULL))
        assertEquals("Discharging", StandByLogic.chargingStateLabel(BatteryManager.BATTERY_STATUS_DISCHARGING))
    }

    @Test
    fun `slideshow index wraps correctly`() {
        assertEquals(1, StandByLogic.nextIndex(0, 3))
        assertEquals(2, StandByLogic.nextIndex(1, 3))
        assertEquals(2, StandByLogic.previousIndex(0, 3))
    }

    @Test
    fun `date formatting is locale friendly`() {
        val date = LocalDate.of(2024, 1, 2)
        assertEquals("Tuesday, 2 January", StandByLogic.formatDate(date))
    }
}
