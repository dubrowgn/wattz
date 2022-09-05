package dubrowgn.wattz

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

const val NoteChannelId = "wattz.status"
const val NoteId = 1
const val intervalMs = 1_250L

class MainActivity : Activity() {
    private lateinit var battery: Battery
    private val task = PeriodicTask({ update() }, intervalMs)
    private lateinit var txtDetails: TextView

    private fun debug(msg: String) {
        Log.d(this::class.java.name, msg)
    }

    private fun serviceRunning(): Boolean {
        val serviceName = StatusService::class.java.name
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (info in activityManager.getRunningServices(100)) {
            if (info.service.className == serviceName) {
                return true
            }
        }

        return false
    }

    private fun init() {
        battery = Battery(this)

        if (!serviceRunning()) {
            startForegroundService(Intent(this, StatusService::class.java))
        }

        txtDetails = TextView(this)
        txtDetails.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        txtDetails.gravity = Gravity.CENTER

        setContentView(txtDetails)
    }

    private fun update() {
        debug("update()")

        val txtWatts = fmt(battery.watts)

        @SuppressLint("SetTextI18n")
        txtDetails.text =
            "Charging: ${battery.charging}\n" +
            "Level: ${fmt(battery.levelAmpHours)}Ah\n" +
            "Current: ${fmt(battery.amps)}A\n" +
            "Temperature: ${fmt(battery.celsius)}°C\n" +
            "Volts: ${fmt(battery.volts)}V\n" +
            "Power: ${txtWatts}W\n" +
            "Time to Full: ${fmtSeconds(battery.secondsUntilCharged)}\n"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        debug("onCreate()")

        super.onCreate(savedInstanceState)

        init()
    }

    override fun onDestroy() {
        debug("onDestroy()")

        super.onDestroy()
    }

    override fun onPause() {
        debug("onPause()")

        task.stop()
        super.onPause()
    }

    override fun onResume() {
        debug("onResume()")

        super.onResume()
        task.start()
    }
}
