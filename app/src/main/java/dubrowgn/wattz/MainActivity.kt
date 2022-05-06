package dubrowgn.wattz

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.Bundle
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
        trace()

        val txtWatts = fmt(battery.watts)

        @SuppressLint("SetTextI18n")
        txtDetails.text =
            "Charging: ${battery.charging}\n" +
            "Level: ${fmt(battery.levelAmpHours)}Ah\n" +
            "Current: ${fmt(battery.amps)}A\n" +
            "Temperature: ${fmt(battery.celsius)}°C\n" +
            "Volts: ${fmt(battery.volts)}V\n" +
            "Power: ${txtWatts}W\n" +
            "Time to Full: ${fmt(battery.secondsUntilCharged)}s\n"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        trace()

        super.onCreate(savedInstanceState)

        init()
    }

    override fun onDestroy() {
        trace()

        super.onDestroy()
    }

    override fun onPause() {
        trace()

        task.stop()
        super.onPause()
    }

    override fun onResume() {
        trace()

        super.onResume()
        task.start()
    }
}
