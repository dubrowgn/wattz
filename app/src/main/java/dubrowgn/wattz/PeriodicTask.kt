package dubrowgn.wattz

import android.os.Handler

class PeriodicTask(callback: () -> Unit, intervalMs: Long) {
    private val ticker = Handler()
    private val callback = callback
    private val runnable = Runnable { start() }
    private val intervalMs = intervalMs

    fun stop() {
        ticker.removeCallbacks(runnable)
    }

    private fun tick() {
        callback()
        ticker.postDelayed(runnable, intervalMs)
    }

    fun start() {
        stop()
        tick()
    }
}