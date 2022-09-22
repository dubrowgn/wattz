package dubrowgn.wattz

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val NoteChannelId = "wattz.status"
const val NoteId = 1
const val intervalMs = 1_000L

class MainActivity : Activity() {
    private lateinit var battery: Battery
    private val task = PeriodicTask({ update() }, intervalMs)
    private lateinit var txtDetails: TextView
    private var pluggedInAt: LocalDateTime? = null

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

    private var state:Boolean = false
    private fun update() {
        debug("update()")

        if (battery.charging){
            if(!state) {
                pluggedInAt = LocalDateTime.now()
                state = true
            }
        }
        if (!battery.charging){
            pluggedInAt = null
            state = false
        }

        debug("state: $state")


        var chargeStr = ""
        if (battery.charging) {
            val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            chargeStr =
                "Charging Since: ${pluggedInAt!!.format(dateFmt)}\n" +
                "Time to Full: ${fmtSeconds(battery.secondsUntilCharged)}\n"
        }

        @SuppressLint("SetTextI18n")
        txtDetails.text =
            "Charging: ${battery.charging}\n" +
            "Level: ${fmt(battery.levelAmpHours)}Ah\n" +
            "Current: ${fmt(battery.amps)}A\n" +
            "Temperature: ${fmt(battery.celsius)}°C\n" +
            "Volts: ${fmt(battery.volts)}V\n" +
            "Power: ${fmt(battery.watts)}W\n" +
            chargeStr
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
