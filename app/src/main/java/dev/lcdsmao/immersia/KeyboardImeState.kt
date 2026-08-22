package dev.lcdsmao.immersia

import android.graphics.Rect

object KeyboardImeState {
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

    fun reset() {
        ImmersiaInputMethodService.instance?.releaseAllPressedKeys()
        overlayBounds = null
        ready = false
    }

    fun setReady(value: Boolean) {
        ready = value
    }
}
