package link.pip.player

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import java.net.Inet4Address

class MainActivity : ComponentActivity() {
    private var overlayAllowed by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        overlayAllowed = Settings.canDrawOverlays(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlayerState.closeRequested = false
        setContent {
            LaunchedEffect(PlayerState.closeRequested) {
                if (PlayerState.closeRequested) finishAndRemoveTask()
            }
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFA5D6FF))) {
                val actionFocus = remember { FocusRequester() }
                Row(
                    Modifier.fillMaxSize().background(Color(0xFF10151E)).padding(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    Column(Modifier.width(230.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text("PiP / DEV", color = Color(0xFFA5D6FF), fontSize = 16.sp)
                        Text("Keep it\nin view.", color = Color.White, fontSize = 38.sp, lineHeight = 42.sp)
                        Text("Google TV player", color = Color(0xFFBAC5D5), fontSize = 16.sp)
                        Text(if (PlayerState.running) "PLAYER ACTIVE" else "PLAYER STOPPED", color = Color(0xFFA5D6FF))
                        Text("HTTP control", color = Color.White)
                        Text(controlAddresses(), color = Color(0xFFBAC5D5), fontSize = 14.sp)
                        Text("Press Home after starting playback.", color = Color(0xFFBAC5D5), fontSize = 14.sp)
                    }
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Stream URL", color = Color.White, fontSize = 20.sp)
                        BasicTextField(
                            value = PlayerState.url,
                            onValueChange = { PlayerState.url = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(Color(0xFFA5D6FF)),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
                                if (event.key == Key.DirectionDown) {
                                    if (event.type == KeyEventType.KeyDown) actionFocus.requestFocus()
                                    true
                                } else {
                                    false
                                }
                            }
                                .border(1.dp, Color(0xFFA5D6FF), RoundedCornerShape(8.dp)).padding(16.dp),
                            decorationBox = { input ->
                                if (PlayerState.url.isEmpty()) Text("http://", color = Color(0xFF8290A4))
                                input()
                            }
                        )
                        if (!overlayAllowed) {
                            Button(modifier = Modifier.focusRequester(actionFocus), onClick = {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            }) { Text("Allow display over other apps") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Button(modifier = if (overlayAllowed) Modifier.focusRequester(actionFocus) else Modifier, enabled = overlayAllowed, onClick = {
                                startForegroundService(Intent(this@MainActivity, PlayerService::class.java)
                                    .setAction(if (PlayerState.url.isEmpty()) "start" else "stream").putExtra("url", PlayerState.url))
                            }) { Text("Start player") }
                            Button(enabled = PlayerState.running, onClick = {
                                stopService(Intent(this@MainActivity, PlayerService::class.java))
                            }) { Text("Stop player") }
                        }
                        Text("Window position", color = Color.White, fontSize = 20.sp)
                        Position.entries.chunked(3).forEach { positions ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                positions.forEach { position ->
                                    Button(enabled = PlayerState.running, onClick = {
                                        PlayerState.position = position
                                        command("layout")
                                    }) { Text(position.label, fontSize = 13.sp) }
                                }
                            }
                        }
                        Text(PlayerState.position.label, color = Color(0xFFBAC5D5), fontSize = 14.sp)
                        Controls("Size", PlayerState.size, 15, 80) { value ->
                            PlayerState.size = value
                            command("layout")
                        }
                        Controls("Volume", PlayerState.volume, 0, 100) { value ->
                            PlayerState.volume = value
                            command("volume")
                        }
                    }
                }
            }
        }
    }

    private fun command(action: String) {
        startService(Intent(this, PlayerService::class.java).setAction(action))
    }

    private fun controlAddresses(): String {
        val connectivity = getSystemService(ConnectivityManager::class.java)
        return connectivity.getLinkProperties(connectivity.activeNetwork)!!.linkAddresses
            .filter { it.address is Inet4Address }
            .joinToString("\n") { "http://${it.address.hostAddress}:8080" }
    }
}

@Composable
private fun Controls(label: String, value: Int, minimum: Int, maximum: Int, change: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("$label $value%", Modifier.width(140.dp).padding(top = 10.dp), color = Color.White)
        Button(enabled = PlayerState.running && value > minimum, onClick = { change((value - 5).coerceAtLeast(minimum)) }) { Text("−") }
        Button(enabled = PlayerState.running && value < maximum, onClick = { change((value + 5).coerceAtMost(maximum)) }) { Text("+") }
    }
}
