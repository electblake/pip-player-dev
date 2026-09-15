package link.pip.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import io.ktor.http.ContentType
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerService : Service() {
    private enum class Action { FULLSCREEN, PIP, CLOSE_APP }
    private lateinit var player: ExoPlayer
    private lateinit var window: PlayerView
    private lateinit var manager: WindowManager
    private lateinit var layout: WindowManager.LayoutParams
    private val server = embeddedServer(CIO, host = "0.0.0.0", port = 8080) {
        routing {
            get("/config") {
                val state = withContext(Dispatchers.Main) {
                    JSONObject()
                        .put("streamUrl", PlayerState.url)
                        .put("size", PlayerState.size)
                        .put("volume", PlayerState.volume)
                        .put("position", PlayerState.position.name.lowercase().replace('_', '-'))
                        .toString()
                }
                call.respondText(state, ContentType.Application.Json)
            }
            post("/config") {
                val parameters = call.receiveParameters()
                val url = parameters.getAll("streamUrl")!!.single()
                val position = Position.valueOf(parameters.getAll("position")!!.single().uppercase().replace('-', '_'))
                val size = parameters.getAll("size")!!.single().toInt()
                val volume = parameters.getAll("volume")!!.single().toInt()
                withContext(Dispatchers.Main) {
                    PlayerState.position = position
                    PlayerState.size = size
                    PlayerState.volume = volume
                    player.volume = volume / 100f
                    updateWindow()
                    PlayerState.url = url
                    if (url.isEmpty()) {
                        player.clearMediaItems()
                        window.visibility = View.GONE
                    } else if (url != player.currentMediaItem?.localConfiguration?.uri?.toString()) {
                        play(url)
                    }
                }
                call.respondText("OK")
            }
            post("/action") {
                val action = Action.valueOf(call.receiveParameters().getAll("action")!!.single().uppercase())
                call.respondText("OK")
                withContext(Dispatchers.Main) {
                    when (action) {
                        Action.FULLSCREEN -> {
                            PlayerState.fullscreen = true
                            updateWindow()
                        }
                        Action.PIP -> {
                            PlayerState.fullscreen = false
                            updateWindow()
                        }
                        Action.CLOSE_APP -> {
                            PlayerState.closeRequested = true
                            stopSelf()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel("playback", "PiP playback", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, "playback")
            .setContentTitle("PiP Player Dev")
            .setContentText("Player active · HTTP port 8080")
            .setSmallIcon(link.pip.player.R.drawable.icon)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(0)))
            .build()
        player.volume = PlayerState.volume / 100f
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { throw error }
        })
        window = PlayerView(this).apply {
            player = this@PlayerService.player
            useController = false
            keepScreenOn = true
            visibility = View.GONE
        }
        manager = getSystemService(WindowManager::class.java)
        layout = WindowManager.LayoutParams(
            0, 0, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        )
        setLayout()
        manager.addView(window, layout)
        server.start(wait = false)
        PlayerState.running = true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            "stream" -> play(intent.getStringExtra("url")!!)
            "layout" -> updateWindow()
            "volume" -> player.volume = PlayerState.volume / 100f
        }
        return START_NOT_STICKY
    }

    private fun play(url: String) {
        PlayerState.url = url
        window.visibility = View.VISIBLE
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    private fun setLayout() {
        val width = resources.displayMetrics.widthPixels
        layout.width = if (PlayerState.fullscreen) width else width * PlayerState.size / 100
        layout.height = if (PlayerState.fullscreen) resources.displayMetrics.heightPixels else layout.width * 9 / 16
        layout.gravity = PlayerState.position.gravity
    }

    private fun updateWindow() {
        setLayout()
        manager.updateViewLayout(window, layout)
    }

    override fun onDestroy() {
        server.stop(0, 0)
        manager.removeView(window)
        player.release()
        PlayerState.running = false
        PlayerState.fullscreen = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null
}
