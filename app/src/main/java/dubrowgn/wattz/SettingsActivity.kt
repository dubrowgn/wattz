package dubrowgn.wattz

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.TextView


const val settingsName = "settings"
const val settingsChannel = "dubrowgn.wattz.settings"

class SettingsActivity : Activity() {
    private lateinit var battery: Battery
    private val task = PeriodicTask({ update() }, intervalMs)

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var invertCurrent: Switch
    private lateinit var invertCurrentPreview: TextView

    private fun debug(msg: String) {
        Log.d(this::class.java.name, msg)
    }

    private fun loadPrefs() {
        val settings = getSharedPreferences(settingsName, MODE_PRIVATE)
        invertCurrent.isChecked = settings.getBoolean("invertCurrent", false)
    }

    @SuppressLint("ApplySharedPref")
    private fun onChange() {
        update()

        getSharedPreferences(settingsName, MODE_PRIVATE)
            .edit()
            .putBoolean("invertCurrent", invertCurrent.isChecked)
            .commit()

        sendBroadcast(Intent().setAction(settingsChannel))
    }

    private fun update() {
        val current = battery.currentRaw
        val sign = if(invertCurrent.isChecked) -1L else 1L
        invertCurrentPreview.text = "${current?.times(sign)}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        debug("onCreate()")

        super.onCreate(savedInstanceState)

        battery = Battery(this)

        setContentView(R.layout.activity_settings)

        invertCurrent = findViewById(R.id.invertCurrent)
        invertCurrent.setOnCheckedChangeListener { _, _ -> onChange() }
        invertCurrentPreview = findViewById(R.id.invertCurrentPreview)

        loadPrefs()
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
