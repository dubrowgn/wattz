package dubrowgn.wattz

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

const val intervalMs = 1_250L
const val noteChannelId = "wattz.status"
const val noteId = 1
const val prefsName = "prefs"

class MainActivity : Activity() {
    private lateinit var battery: Battery
    private val task = PeriodicTask({ update() }, intervalMs)

    private lateinit var charging: TextView
    private lateinit var chargingSince: TextView
    private lateinit var current: TextView
    private lateinit var energy: TextView
    private lateinit var power: TextView
    private lateinit var temperature: TextView
    private lateinit var timeToFullCharge: TextView
    private lateinit var voltage: TextView

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

    private fun initUi() {
        setContentView(R.layout.activity_main)

        charging = findViewById(R.id.charging)
        chargingSince = findViewById(R.id.chargingSince)
        current = findViewById(R.id.current)
        energy = findViewById(R.id.energy)
        power = findViewById(R.id.power)
        temperature = findViewById(R.id.temperature)
        timeToFullCharge = findViewById(R.id.timeToFullCharge)
        voltage = findViewById(R.id.voltage)
    }

    private fun init() {
        battery = Battery(this)

        if (!serviceRunning()) {
            startForegroundService(Intent(this, StatusService::class.java))
        }

        initUi()
    }

    @SuppressLint("SetTextI18n")
    private fun update() {
        debug("update()")

        charging.text = if (battery.charging) getString(R.string.yes) else getString(R.string.no)
        current.text = fmt(battery.amps) + "A"
        energy.text = fmt(battery.levelAmpHours) + "Ah"
        power.text = fmt(battery.watts) + "W"
        temperature.text = fmt(battery.celsius) + "°C"
        voltage.text = fmt(battery.volts) + "V"

        val pluggedInAtStr = getSharedPreferences(prefsName, MODE_MULTI_PROCESS)
            .getString("pluggedInAt", null)
        chargingSince.text = if (pluggedInAtStr != null) {
            val pluggedInAt = ZonedDateTime.parse(pluggedInAtStr)
            val localTime = LocalDateTime.ofInstant(pluggedInAt.toInstant(), pluggedInAt.zone)
            val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            localTime.format(dateFmt)
        } else {
            getString(R.string.indeterminate)
        }

        timeToFullCharge.text = if (battery.secondsUntilCharged != null) {
            fmtSeconds(battery.secondsUntilCharged)
        } else {
            getString(R.string.indeterminate)
        }
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
