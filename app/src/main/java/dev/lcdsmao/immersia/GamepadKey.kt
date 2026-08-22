package dev.lcdsmao.immersia

import android.view.KeyEvent

enum class GamepadKey(
    val label: String,
    val keyCode: Int,
) {
    DPAD_UP("Up", KeyEvent.KEYCODE_DPAD_UP),
    DPAD_DOWN("Down", KeyEvent.KEYCODE_DPAD_DOWN),
    DPAD_LEFT("Left", KeyEvent.KEYCODE_DPAD_LEFT),
    DPAD_RIGHT("Right", KeyEvent.KEYCODE_DPAD_RIGHT),
    A("A", KeyEvent.KEYCODE_BUTTON_A),
    B("B", KeyEvent.KEYCODE_BUTTON_B),
    X("X", KeyEvent.KEYCODE_BUTTON_X),
    Y("Y", KeyEvent.KEYCODE_BUTTON_Y),
    L1("L1", KeyEvent.KEYCODE_BUTTON_L1),
    R1("R1", KeyEvent.KEYCODE_BUTTON_R1),
    L2("L2", KeyEvent.KEYCODE_BUTTON_L2),
    R2("R2", KeyEvent.KEYCODE_BUTTON_R2),
    START("Start", KeyEvent.KEYCODE_BUTTON_START),
    SELECT("Select", KeyEvent.KEYCODE_BUTTON_SELECT),
}
