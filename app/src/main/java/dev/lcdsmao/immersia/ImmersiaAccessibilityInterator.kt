package dev.lcdsmao.immersia

import kotlinx.coroutines.flow.Flow

interface ImmersiaAccessibilityInteractor {
    val eventFlow: Flow<Event>

    fun beginImmersive()

    fun enterKeyboardMode(): KeyboardImeResult

    fun enterGamepadMode(): KeyboardImeResult

    fun exitKeyboardMode()

    data class KeyboardImeResult(
        val success: Boolean,
        val message: String,
    )

    sealed interface Event {
        object ImmersiveSucceeded : Event

        data class ImmersiveFailed(val reason: String) : Event

        data class SettingsChanged(
            val accessibilityEnabled: Boolean = false,
            val serviceReady: Boolean = false,
        ) : Event
    }
}
