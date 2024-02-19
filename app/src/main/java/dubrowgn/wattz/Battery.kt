package dubrowgn.wattz

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

enum class PlugType {
    Ac,
    Dock,
    Unknown,
    Usb,
    Wireless;

    companion object {
        fun fromRaw(plugType: Int?): PlugType? {
            return when (plugType) {
                null, 0 -> null
                BatteryManager.BATTERY_PLUGGED_AC -> Ac
                BatteryManager.BATTERY_PLUGGED_DOCK -> Dock
                BatteryManager.BATTERY_PLUGGED_USB -> Usb
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> Wireless
                else -> Unknown
            }
        }
    }
}

class Battery(private val ctx: Context) {
    private val mgr = ctx.getSystemService(Activity.BATTERY_SERVICE) as BatteryManager

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
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
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
            plugType = PlugType.fromRaw(prop(BatteryManager.EXTRA_PLUGGED)?.toInt()),
            tempRaw = prop(BatteryManager.EXTRA_TEMPERATURE),
            voltsRaw = prop(BatteryManager.EXTRA_VOLTAGE),
        )
    }
}
