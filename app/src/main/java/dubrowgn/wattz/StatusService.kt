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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


class StatusService : Service() {
    private lateinit var battery: Battery
    private lateinit var noteBuilder: Notification.Builder
    private lateinit var noteMgr: NotificationManager
    private val task = PeriodicTask({ update() }, intervalMs)

    private fun debug(msg: String) {
        Log.d(this::class.java.name, msg)
    }

    private fun writePluggedInAt(dt: ZonedDateTime?) {
        getSharedPreferences(prefsName, MODE_MULTI_PROCESS)
            .edit()
            .putString("pluggedInAt", dt?.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
            .commit()
    }

    inner class PowerConnectionReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> writePluggedInAt(ZonedDateTime.now())
                Intent.ACTION_POWER_DISCONNECTED -> writePluggedInAt(null)
            }
        }
    }

    inner class ScreenReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                task.stop()
            } else if (intent.action == Intent.ACTION_SCREEN_ON) {
                task.start()
            }
        }
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

        filter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(PowerConnectionReceiver(), filter)
        writePluggedInAt(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        debug("onStartCommand()")

        super.onStartCommand(intent, flags, startId)

        init()
        task.start()

        startForeground(noteId, noteBuilder.build())

        return START_STICKY
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



    private fun update() {
        debug("update()")

        val txtWatts = fmt(battery.watts)
        val txtAvgWatts = fmt(battery.calculateAvgWatts())

        noteBuilder
            .setContentTitle("Battery Draw: $txtWatts W\nAvg. Draw: $txtAvgWatts")
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
