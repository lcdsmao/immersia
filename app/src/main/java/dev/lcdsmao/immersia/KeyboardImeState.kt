package dev.lcdsmao.immersia

import android.graphics.Rect

object KeyboardImeState {
    @Volatile
    var mode: ImmersiveMode = ImmersiveMode.Keyboard
        private set

    @Volatile
    var overlayBounds: Rect? = null
        private set

    @Volatile
    var ready: Boolean = false
        private set

    fun setOverlayBounds(bounds: Rect) {
        overlayBounds = Rect(bounds)
        ImmersiaInputMethodService.instance?.applyOverlayBounds()
    }

    fun setMode(mode: ImmersiveMode) {
        this.mode = mode
        ImmersiaInputMethodService.instance?.setMode(mode)
    }

    fun reset() {
        ImmersiaInputMethodService.instance?.releaseAllPressedKeys()
        mode = ImmersiveMode.Keyboard
        overlayBounds = null
        ready = false
    }

    fun setReady(value: Boolean) {
        ready = value
    }
}
