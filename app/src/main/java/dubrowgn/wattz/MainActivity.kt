package dubrowgn.wattz

import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView

const val namespace = "dubrowgn.wattz"
const val batteryDataReq = "$namespace.battery-data-req"
const val batteryDataResp = "$namespace.battery-data-resp"
const val intervalMs = 1_250L
const val noteChannelId = "$namespace.status"
const val noteId = 1

class MainActivity : Activity() {
    private val batteryReceiver = BatteryDataReceiver()

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

    inner class BatteryDataReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            debug("BatteryDataReceiver.onReceive()")

            if (intent == null)
                return

            val ind = getString(R.string.indeterminate)

            charging.text = intent.getStringExtra("charging") ?: ind
            chargingSince.text = intent.getStringExtra("chargingSince") ?: ind
            current.text = intent.getStringExtra("current") ?: ind
            energy.text = intent.getStringExtra("energy") ?: ind
            power.text = intent.getStringExtra("power") ?: ind
            temperature.text = intent.getStringExtra("temperature") ?: ind
            timeToFullCharge.text = intent.getStringExtra("timeToFullCharge") ?: ind
            voltage.text = intent.getStringExtra("voltage") ?: ind
        }
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
        if (!serviceRunning()) {
            startForegroundService(Intent(this, StatusService::class.java))
        }

        initUi()
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

        unregisterReceiver(batteryReceiver)

        super.onPause()
    }

    override fun onResume() {
        debug("onResume()")

        super.onResume()

        registerReceiver(batteryReceiver, IntentFilter(batteryDataResp))
        sendBroadcast(Intent().setPackage(packageName).setAction(batteryDataReq))
    }

    fun onSettings(view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
