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
const val settingsChannel = "dubrowgn.wattz.settings"

class SettingsActivity : Activity() {
    private lateinit var battery: Battery
    private val task = PeriodicTask({ update() }, intervalMs)

    private lateinit var currentUnits: RadioGroup
    private lateinit var currentUnitsPreview: TextView
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var invertCurrent: Switch
    private lateinit var invertCurrentPreview: TextView

    private fun debug(msg: String) {
        Log.d(this::class.java.name, msg)
    }

    private fun loadPrefs() {
        val settings = getSharedPreferences(settingsName, MODE_PRIVATE)
        currentUnits.check(
            when (settings.getFloat("currentScalar", -1f)) {
                1000f -> R.id.currentUnitsMillis
                .001f -> R.id.currentUnitsNanos
                else -> R.id.currentUnitsMicros
            }
        )
        invertCurrent.isChecked = settings.getBoolean("invertCurrent", false)
    }

    private val currentScalar: Float get() {
        return when(currentUnits.checkedRadioButtonId) {
            R.id.currentUnitsMillis -> 1000f
            R.id.currentUnitsNanos -> .001f
            else -> 1f
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun onChange() {
        update()

        getSharedPreferences(settingsName, MODE_PRIVATE)
            .edit()
            .putBoolean("invertCurrent", invertCurrent.isChecked)
            .putFloat("currentScalar", currentScalar)
            .commit()

        sendBroadcast(Intent().setAction(settingsChannel))
    }

    private fun update() {
        val current = battery.currentRaw

        currentUnitsPreview.text = "${current?.times(currentScalar)?.toLong()}"

        val sign = if(invertCurrent.isChecked) -1L else 1L
        invertCurrentPreview.text = "${current?.times(sign)}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        debug("onCreate()")

        super.onCreate(savedInstanceState)

        battery = Battery(this)

        setContentView(R.layout.activity_settings)

        currentUnits = findViewById(R.id.currentUnits)
        currentUnitsPreview = findViewById(R.id.currentUnitsPreview)
        invertCurrent = findViewById(R.id.invertCurrent)
        invertCurrentPreview = findViewById(R.id.invertCurrentPreview)

        loadPrefs()

        currentUnits.setOnCheckedChangeListener { _, _ -> onChange() }
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
