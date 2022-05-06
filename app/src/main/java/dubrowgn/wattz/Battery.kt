package dubrowgn.wattz

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class Battery(ctx: Context) {
    private val mgr = ctx.getSystemService(Activity.BATTERY_SERVICE) as BatteryManager
    private val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

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

    val microamps : Double? get() = -1.0 * prop(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    val milliamps : Double? get() = fromMillis(microamps)
    val amps : Double? get() = fromMicros(microamps)

    val millivolts : Double? get() = prop(BatteryManager.EXTRA_VOLTAGE)
    val volts : Double? get() = fromMillis(millivolts)

    val milliwatts : Double? get() = milliamps?.times(volts)
    val watts : Double? get() = amps?.times(volts)

    val levelAmpHours : Double? get() = fromMicros(prop(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))

    val celsius : Double? get() = prop(BatteryManager.EXTRA_TEMPERATURE)?.div(10.0)

    val charging : Boolean? get() = mgr.isCharging
    val secondsUntilCharged: Double? get() {
        val secs = mgr.computeChargeTimeRemaining()
        return if(secs == -1L) null else secs.toDouble().div(1_000.0)
    }
}
