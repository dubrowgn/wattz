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
    private var indicatorUnits: String? = null
    private lateinit var noteBuilder: Notification.Builder
    private lateinit var noteMgr: NotificationManager
    private var pluggedInAt: ZonedDateTime? = null
    private lateinit var snapshot: BatterySnapshot
    private val task = PeriodicTask({ update() }, intervalMs)

    private fun debug(msg: String) {
        Log.d(this::class.java.name, msg)
    }

    private inner class MsgReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                batteryDataReq -> updateData()
                settingsUpdateInd -> {
                    loadSettings()
                    update()
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    pluggedInAt = ZonedDateTime.now()
                    update()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    pluggedInAt = null
                    update()
                }
                Intent.ACTION_SCREEN_OFF -> task.stop()
                Intent.ACTION_SCREEN_ON -> task.start()
            }
        }
    }

    private fun loadSettings() {
        val settings = getSharedPreferences(settingsName, MODE_MULTI_PROCESS)
        battery.currentScalar = settings.getFloat("currentScalar", 1f).toDouble()
        battery.invertCurrent = settings.getBoolean("invertCurrent", false)
        indicatorUnits = settings.getString("indicatorUnits", null);
    }

    private fun init() {
        battery = Battery(applicationContext)
        snapshot = battery.snapshot()

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

        val ind = getString(R.string.indeterminate)
        noteBuilder = Notification.Builder(this, noteChannelId)
            .setContentTitle("Battery Draw: $ind W")
            .setSmallIcon(renderIcon(ind, "W"))
            .setContentIntent(noteIntent)
            .setOnlyAlertOnce(true)

        registerReceiver(
            MsgReceiver(),
            IntentFilter().apply {
                addAction(batteryDataReq)
                addAction(settingsUpdateInd)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        debug("onStartCommand()")

        super.onStartCommand(intent, flags, startId)

        init()
        loadSettings()
        task.start()

        try {
            startForeground(noteId, noteBuilder.build())
        } catch (e: Exception) {
            error("Failed to foreground StatusService: ${e.message}")
        }

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
        val density = resources.displayMetrics.density
        val w = (48f * density).toInt()
        val bitmap = Bitmap.createBitmap(w, w, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)

        val textSize = 28f * density
        val paint = Paint()
        paint.textSize = textSize
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER

        canvas.drawText(value, w / 2f, w / 2f, paint)
        canvas.drawText(unit, w / 2f, w.toFloat(), paint)

        return Icon.createWithBitmap(bitmap)
    }

    private fun updateData() {
        val plugType = snapshot.plugType?.name?.lowercase()
        val indeterminate = getString(R.string.indeterminate)
        val no = getString(R.string.no)
        val yes = getString(R.string.yes)

        val intent = Intent()
            .setPackage(packageName)
            .setAction(batteryDataResp)
            .putExtra("charging",
                when (snapshot.charging) {
                    true -> if (plugType == null) yes else "$yes ($plugType)"
                    false -> no
                }
            )
            .putExtra("chargingSince",
                when (val pluggedInAt = pluggedInAt) {
                    null -> indeterminate
                    else -> LocalDateTime
                        .ofInstant(pluggedInAt.toInstant(), pluggedInAt.zone)
                        .format(dateFmt)
                }
            )
            .putExtra("current", fmt(snapshot.amps) + "A")
            .putExtra("energy",
                "${fmt(snapshot.energyWattHours)}Wh (${fmt(snapshot.energyAmpHours)}Ah)"
            )
            .putExtra("power", fmt(snapshot.watts) + "W")
            .putExtra("temperature", fmt(snapshot.celsius) + "°C")
            .putExtra("timeToFullCharge",
                when (val seconds = snapshot.secondsUntilCharged) {
                    null -> indeterminate
                    0.0 -> "fully charged"
                    else -> fmtSeconds(seconds)
                }
            )
            .putExtra("voltage", fmt(snapshot.volts) + "V")

        applicationContext.sendBroadcast(intent)
    }

    private fun update() {
        debug("update()")

        snapshot = battery.snapshot()

        val txtLabel = when (indicatorUnits) {
            "A" -> getString(R.string.current)
            "Ah" -> getString(R.string.energy)
            "C" -> getString(R.string.temperature)
            "V" -> getString(R.string.voltage)
            "Wh" -> getString(R.string.energy)
            else -> getString(R.string.power)
        }
        val txtValue = fmt( when (indicatorUnits) {
            "A" -> snapshot.amps
            "Ah" -> snapshot.energyAmpHours
            "C" -> snapshot.celsius
            "V" -> snapshot.volts
            "Wh" -> snapshot.energyWattHours
            else -> snapshot.watts
        })
        val txtUnits = when (indicatorUnits) {
            "C" -> "°C"
            else -> indicatorUnits ?: "W"
        }

        noteBuilder
            .setContentTitle("${getString(R.string.battery)} ${txtLabel}: ${txtValue}${txtUnits}")
            .setSmallIcon(renderIcon(txtValue, txtUnits))

        noteBuilder.setContentText(
            when(val seconds = snapshot.secondsUntilCharged) {
                null -> ""
                0.0 -> "fully charged"
                else -> "${fmtSeconds(seconds)} until full charge"
            }
        )

        noteMgr.notify(noteId, noteBuilder.build())

        updateData()
    }
}
