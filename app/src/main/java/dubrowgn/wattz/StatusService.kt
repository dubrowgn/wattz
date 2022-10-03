package dubrowgn.wattz

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.Icon
import android.os.IBinder
import android.util.Log
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


class StatusService : Service() {
    private lateinit var battery: Battery
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private lateinit var noteBuilder: Notification.Builder
    private lateinit var noteMgr: NotificationManager
    private var pluggedInAt: ZonedDateTime? = null
    private val task = PeriodicTask({ update() }, intervalMs)

    private fun debug(msg: String) {
        Log.d(this::class.java.name, msg)
    }

    inner class PowerConnectionReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> pluggedInAt = ZonedDateTime.now()
                Intent.ACTION_POWER_DISCONNECTED -> pluggedInAt = null
            }
        }
    }

    inner class SettingsReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != settingsChannel)
                return

            loadSettings()
        }
    }

    inner class ScreenReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> task.stop()
                Intent.ACTION_SCREEN_ON -> task.start()
            }
        }
    }

    private fun loadSettings() {
        val settings = getSharedPreferences(settingsName, MODE_MULTI_PROCESS)
        battery.invertCurrent = settings.getBoolean("invertCurrent", false)
    }

    private fun init() {
        battery = Battery(applicationContext)

        noteMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        noteMgr.createNotificationChannel(
            NotificationChannel(
                noteChannelId,
                "Power Status",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Continuously displays current battery power consumption"
            }
        )

        val noteIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        noteBuilder = Notification.Builder(this, noteChannelId)
            .setContentTitle("Battery Draw: - W")
            .setSmallIcon(renderIcon("-", "W"))
            .setContentIntent(noteIntent)
            .setOnlyAlertOnce(true)

        var filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(ScreenReceiver(), filter)

        registerReceiver(SettingsReceiver(), IntentFilter(settingsChannel))

        filter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(PowerConnectionReceiver(), filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        debug("onStartCommand()")

        super.onStartCommand(intent, flags, startId)

        init()
        loadSettings()
        task.start()

        startForeground(noteId, noteBuilder.build())

        return START_STICKY;
    }

    override fun onDestroy() {
        debug("onDestroy()")

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun renderIcon(value: String, unit: String): Icon {
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)

        val textSize = 56f
        val paint = Paint()
        paint.textSize = textSize
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER

        canvas.drawText(value, bitmap.width / 2f, 48f, paint)
        canvas.drawText(unit, bitmap.width / 2f, 96f, paint)

        return Icon.createWithBitmap(bitmap)
    }

    private fun updateData() {
        val intent = Intent()
            .setAction(batteryDataChannel)
            .putExtra("charging",
                when (battery.charging) {
                    true -> getString(R.string.yes)
                    false -> getString(R.string.no)
                }
            )
            .putExtra("chargingSince",
                when (val pluggedInAt = pluggedInAt) {
                    null -> getString(R.string.indeterminate)
                    else -> LocalDateTime
                        .ofInstant(pluggedInAt.toInstant(), pluggedInAt.zone)
                        .format(dateFmt)
                }
            )
            .putExtra("current", fmt(battery.amps) + "A")
            .putExtra("energy",
                "${fmt(battery.energyWattHours)}Wh (${fmt(battery.energyAmpHours)}Ah)"
            )
            .putExtra("power", fmt(battery.watts) + "W")
            .putExtra("temperature", fmt(battery.celsius) + "°C")
            .putExtra("timeToFullCharge",
                when (val secondsUntilCharged = battery.secondsUntilCharged) {
                    null -> getString(R.string.indeterminate)
                    0.0 -> "fully charged"
                    else -> fmtSeconds(secondsUntilCharged)
                }
            )
            .putExtra("voltage", fmt(battery.volts) + "V")

        applicationContext.sendBroadcast(intent)
    }

    private fun update() {
        debug("update()")

        updateData()

        val txtWatts = fmt(battery.watts)

        noteBuilder
            .setContentTitle("Battery Draw: $txtWatts W")
            .setSmallIcon(renderIcon(txtWatts, "w"))

        noteBuilder.setContentText(
            when(battery.secondsUntilCharged) {
                null -> ""
                0.0 -> "fully charged"
                else -> "${fmtSeconds(battery.secondsUntilCharged)} until full charge"
            }
        )

        noteMgr.notify(noteId, noteBuilder.build())
    }
}
