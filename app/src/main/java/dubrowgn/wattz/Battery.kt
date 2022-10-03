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

    private fun prop(id: Int): Double {
        return mgr.getLongProperty(id).toDouble()
    }

    private fun prop(id: String): Double? {
        if (intent?.hasExtra(id) != true)
            return null

        return intent.getIntExtra(id, Int.MIN_VALUE).toDouble()
    }

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

    private fun toMillis(v: Double) : Double {
        return v.times(1_000.0)
    }

    val milliamps : Double get() = toMillis(amps)
    val amps : Double get() {
        val amps = -1.0 * prop(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        tuneAmpScalar(amps)

        return amps * ampScalar
    }

    val millivolts : Double? get() = prop(BatteryManager.EXTRA_VOLTAGE)
    val volts : Double? get() = fromMillis(millivolts)

    val milliwatts : Double? get() = milliamps.times(volts)
    val watts : Double? get() = amps.times(volts)

    val energyAmpHours : Double? get() = fromMicros(prop(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))
    val energyWattHours : Double? get() = volts?.times(energyAmpHours)

    val celsius : Double? get() = prop(BatteryManager.EXTRA_TEMPERATURE)?.div(10.0)

    // Some devices always report false for isCharging, so fall back to battery current detection
    val charging : Boolean get() = mgr.isCharging || milliamps < 1.0
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
