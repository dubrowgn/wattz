package dubrowgn.wattz

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.Icon
import android.os.IBinder


class StatusService : Service() {
    private lateinit var battery: Battery

    private lateinit var noteBuilder: Notification.Builder
    private lateinit var noteMgr: NotificationManager

    private val task = PeriodicTask({ update() }, intervalMs)

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
                NoteChannelId,
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

        noteBuilder = Notification.Builder(this, NoteChannelId)
            .setContentTitle("Battery Draw: - W")
            .setSmallIcon(renderIcon("-", "W"))
            .setContentIntent(noteIntent)
            .setOnlyAlertOnce(true)

        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(ScreenReceiver(), filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        trace()

        init()
        task.start()

        startForeground(NoteId, noteBuilder.build())

        return super.onStartCommand(intent, flags, startId)
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
        trace()

        val txtWatts = fmt(battery.watts)

        noteBuilder
            .setContentTitle("Battery Draw: $txtWatts W")
            .setSmallIcon(renderIcon(txtWatts, "w"))
        noteMgr.notify(NoteId, noteBuilder.build())
    }
}
