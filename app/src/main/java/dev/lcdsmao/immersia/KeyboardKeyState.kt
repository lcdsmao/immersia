package dev.lcdsmao.immersia

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.runtime.mutableStateSetOf

internal class KeyboardKeyState(
    private val dispatch: (KeyEvent) -> Boolean,
) {
    private val pressedAt = LinkedHashMap<Int, Long>()
    val pressedKeys = mutableStateSetOf<Int>()

    fun press(keyCode: Int) {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN || pressedAt.containsKey(keyCode)) return

        val downTime = SystemClock.uptimeMillis()
        if (dispatch(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0))) {
            pressedAt[keyCode] = downTime
            pressedKeys.add(keyCode)
        }
    }

    fun release(keyCode: Int) {
        val downTime = pressedAt.remove(keyCode) ?: return
        pressedKeys.remove(keyCode)
        dispatch(
            KeyEvent(
                downTime,
                SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP,
                keyCode,
                0,
            )
        )
    }

    fun releaseAll() {
        pressedAt.keys.toList().forEach(::release)
    }
}
