package dubrowgn.wattz

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView


const val settingsName = "settings"
const val settingsUpdateInd = "$namespace.settings-update-ind"

class SettingsActivity : Activity() {
    private lateinit var battery: Battery
    private val task = PeriodicTask({ update() }, intervalMs)

    private lateinit var charging: TextView
    private lateinit var currentScalar: RadioGroup
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var invertCurrent: Switch
    private lateinit var power: TextView

    private fun debug(msg: String) {
        Log.d(this::class.java.name, msg)
    }

    private fun loadPrefs() {
        val settings = getSharedPreferences(settingsName, MODE_PRIVATE)
        currentScalar.check(
            when (settings.getFloat("currentScalar", -1f)) {
                1000f -> R.id.currentScalar1000
                .001f -> R.id.currentScalar0_001
                else -> R.id.currentScalar1
            }
        )
        invertCurrent.isChecked = settings.getBoolean("invertCurrent", false)
    }

    @SuppressLint("ApplySharedPref")
    private fun onChange() {
        update()

        getSharedPreferences(settingsName, MODE_PRIVATE)
            .edit()
            .putBoolean("invertCurrent", battery.invertCurrent)
            .putFloat("currentScalar", battery.currentScalar.toFloat())
            .commit()

        sendBroadcast(Intent().setPackage(packageName).setAction(settingsUpdateInd))
    }

    @SuppressLint("SetTextI18n")
    private fun update() {
        battery.currentScalar = when(currentScalar.checkedRadioButtonId) {
            R.id.currentScalar1000 -> 1000.0
            R.id.currentScalar0_001 -> 0.001
            else -> 1.0
        }
        battery.invertCurrent = invertCurrent.isChecked

        val snapshot = battery.snapshot()
        charging.text = getString(if (snapshot.charging) R.string.yes else R.string.no)
        power.text = "${fmt(snapshot.watts)}W"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        debug("onCreate()")

        super.onCreate(savedInstanceState)

        battery = Battery(this)

        setContentView(R.layout.activity_settings)

        charging = findViewById(R.id.charging)
        currentScalar = findViewById(R.id.currentScalar)
        invertCurrent = findViewById(R.id.invertCurrent)
        power = findViewById(R.id.power)

        loadPrefs()

        currentScalar.setOnCheckedChangeListener { _, _ -> onChange() }
        invertCurrent.setOnCheckedChangeListener { _, _ -> onChange() }
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

    override fun onDestroy() {
        debug("onDestroy()")

        super.onDestroy()
    }

    fun onBack(view: View) {
        finish()
    }
}
