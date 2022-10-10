package dubrowgn.wattz

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class Battery(ctx: Context) {
    private val mgr = ctx.getSystemService(Activity.BATTERY_SERVICE) as BatteryManager
    private val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    // configuration
    var currentScalar = 1.0
    var invertCurrent = false

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

    fun snapshot(): BatterySnapshot {
        return BatterySnapshot(
            chargeTimeRemainingRaw = mgr.computeChargeTimeRemaining(),
            currentRaw = prop(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
            currentScalar = currentScalar,
            energyRaw = prop(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
            invertCurrent = invertCurrent,
            isChargingRaw = mgr.isCharging,
            tempRaw = prop(BatteryManager.EXTRA_TEMPERATURE),
            voltsRaw = prop(BatteryManager.EXTRA_VOLTAGE),
        )
    }
}
