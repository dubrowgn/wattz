package dubrowgn.wattz


class BatterySnapshot(
    // configuration
    private val currentScalar: Double,
    private val invertCurrent: Boolean,

    // data
    val chargeTimeRemainingRaw: Long,
    val currentRaw: Long?,
    val energyRaw: Long?,
    val isChargingRaw: Boolean,
    val plugType: PlugType?,
    val tempRaw: Long?,
    val voltsRaw: Long?,
) {
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

    val microamps : Double? get() {
        val sign = if (invertCurrent) 1.0 else -1.0
        return currentRaw?.times(currentScalar)?.times(sign)
    }
    val milliamps : Double? get() = microamps?.div(1_000.0)
    val amps : Double? get() = fromMicros(microamps)

    val millivolts : Double? get() = voltsRaw?.toDouble()
    val volts : Double? get() = fromMillis(millivolts)

    val milliwatts : Double? get() = milliamps.times(volts)
    val watts : Double? get() = amps.times(volts)

    val energyAmpHours : Double? get() = fromMicros(energyRaw?.toDouble())
    val energyWattHours : Double? get() = volts?.times(energyAmpHours)

    val celsius : Double? get() = tempRaw?.toDouble()?.div(10.0)

    // Some devices always report false for isCharging, so fall back to battery current detection
    val charging : Boolean get() {
        if (isChargingRaw)
            return true

        val ma = milliamps
        return ma != null && ma < 1.0
    }

    val secondsUntilCharged: Double? get() {
        // Some devices incorrectly report "0 seconds to full" when not charging,
        // so ensure we are actually charging first.
        if (!charging)
            return null

        val ms = chargeTimeRemainingRaw
        if (ms == -1L)
            return null

        return fromMillis(ms.toDouble())
    }
}
