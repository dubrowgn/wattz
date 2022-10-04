package dubrowgn.wattz

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.abs

class Battery(ctx: Context) {
    private var ampScalar = 1.0
    private val mgr = ctx.getSystemService(Activity.BATTERY_SERVICE) as BatteryManager
    private val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    // configuration
    var invertCurrent = false

    // Current is defined as uA in
    // https://developer.android.com/reference/android/os/BatteryManager#BATTERY_PROPERTY_CURRENT_NOW,
    // but some devices report other units. Dynamically detect the correct units by assuming the
    // value is in A, and dividing by orders of 1000 until a reasonable value is found.
    // See: https://github.com/dubrowgn/wattz/issues/5
    private fun tuneAmpScalar(amps: Double) {
        while (abs(amps * ampScalar) > 100.0) {
            ampScalar /= 1_000.0
        }
    }

    private fun prop(id: Int): Long? {
        val v = mgr.getLongProperty(id)
        if (v == Long.MIN_VALUE)
            return null

        return v
    }

    private fun prop(id: String): Long? {
        if (intent?.hasExtra(id) != true)
            return null

        val v = intent.getIntExtra(id, Int.MIN_VALUE)
        if (v == Int.MIN_VALUE)
            return null

        return v.toLong()
    }

    private fun propd(id: Int): Double? = prop(id)?.toDouble()
    private fun propd(id: String): Double? = prop(id)?.toDouble()

    private fun Double?.times(v: Double?) : Double? {
        if (v == null)
            return null

        return this?.times(v)
    }

    private fun fromMicros(v: Double?) : Double? {
        return v?.div(1_000_000.0)
    }

    private fun fromMillis(v: Double?) : Double? {
        return v?.div(1_000.0)
    }

    private fun toMillis(v: Double?) : Double? {
        return v?.times(1_000.0)
    }

    val currentRaw : Long? get() = prop(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

    val milliamps : Double? get() = toMillis(amps)
    val amps : Double? get() {
        val sign = if (invertCurrent) 1.0 else -1.0
        val amps = sign.times(currentRaw?.toDouble())
            ?: return null

        tuneAmpScalar(amps)

        return amps * ampScalar
    }

    val millivolts : Double? get() = propd(BatteryManager.EXTRA_VOLTAGE)
    val volts : Double? get() = fromMillis(millivolts)

    val milliwatts : Double? get() = milliamps.times(volts)
    val watts : Double? get() = amps.times(volts)

    val energyAmpHours : Double? get() = fromMicros(propd(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))
    val energyWattHours : Double? get() = volts?.times(energyAmpHours)

    val celsius : Double? get() = propd(BatteryManager.EXTRA_TEMPERATURE)?.div(10.0)

    // Some devices always report false for isCharging, so fall back to battery current detection
    val charging : Boolean get() {
        if (mgr.isCharging)
            return true

        val ma = milliamps
        return ma != null && ma < 1.0
    }

    val secondsUntilCharged: Double? get() {
        // Some devices incorrectly report "0 seconds to full" when not charging,
        // so ensure we are actually charging first.
        if (!charging)
            return null

        val secs = mgr.computeChargeTimeRemaining()
        if (secs == -1L)
            return null

        return fromMillis(secs.toDouble())
    }
}
