package dubrowgn.wattz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    private fun debug(msg: String) {
        Log.d(this::class.java.name, msg)
    }
    private fun error(msg: String) {
        Log.e(this::class.java.name, msg)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        debug("onReceive()")

        if (context == null) {
            error("context is null")
            return
        }

        if (intent == null) {
            error("intent is null")
            return
        }

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            error("received non-boot action '${intent.action}'")
            return
        }

        debug("starting status service...")
        context.startForegroundService(Intent(context, StatusService::class.java))
    }
}