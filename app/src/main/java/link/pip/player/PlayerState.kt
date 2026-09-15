package link.pip.player

import android.view.Gravity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Position(val label: String, val gravity: Int) {
    TOP_LEFT("Top left", Gravity.TOP or Gravity.LEFT),
    TOP_CENTER("Top center", Gravity.TOP or Gravity.CENTER_HORIZONTAL),
    TOP_RIGHT("Top right", Gravity.TOP or Gravity.RIGHT),
    BOTTOM_LEFT("Bottom left", Gravity.BOTTOM or Gravity.LEFT),
    BOTTOM_CENTER("Bottom center", Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL),
    BOTTOM_RIGHT("Bottom right", Gravity.BOTTOM or Gravity.RIGHT)
}

object PlayerState {
    var running by mutableStateOf(false)
    var fullscreen by mutableStateOf(false)
    var closeRequested by mutableStateOf(false)
    var url by mutableStateOf("")
    var size by mutableIntStateOf(30)
    var volume by mutableIntStateOf(50)
    var position by mutableStateOf(Position.BOTTOM_RIGHT)
}
