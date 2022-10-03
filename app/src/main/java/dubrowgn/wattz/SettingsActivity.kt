package dubrowgn.wattz

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Switch


const val settingsName = "settings"
const val settingsChannel = "dubrowgn.wattz.settings"

class SettingsActivity : Activity() {
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var invertCurrent: Switch

    private fun debug(msg: String) {
        Log.d(this::class.java.name, msg)
    }

    private fun loadPrefs() {
        val settings = getSharedPreferences(settingsName, MODE_PRIVATE)
        invertCurrent.isChecked = settings.getBoolean("invertCurrent", false)
    }

    @SuppressLint("ApplySharedPref")
    private fun storePrefs() {
        getSharedPreferences(settingsName, MODE_PRIVATE)
            .edit()
            .putBoolean("invertCurrent", invertCurrent.isChecked)
            .commit()

        sendBroadcast(Intent().setAction(settingsChannel))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        debug("onCreate()")

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        invertCurrent = findViewById(R.id.invertCurrent)
        invertCurrent.setOnCheckedChangeListener { _, _ -> storePrefs() }

        loadPrefs()
    }

    override fun onDestroy() {
        debug("onDestroy()")

        super.onDestroy()
    }

    fun onBack(view: View) {
        finish()
    }
}
