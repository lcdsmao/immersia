package dev.lcdsmao.immersia

import kotlinx.coroutines.flow.Flow

interface ImmersiaAccessibilityInteractor {
    val eventFlow: Flow<Event>

    fun isAccessibilityEnabled(): Boolean

    fun isInSplitMode(): Boolean

    fun isLandscapeDisplay(): Boolean

    fun beginImmersive()

    fun sendKeyboardStroke(
        key: KeyboardKey,
        modifiers: Set<KeyboardKey>,
    ): UnifiedRemoteClient.Result

    fun sendMouseMove(deltaX: Int, deltaY: Int): UnifiedRemoteClient.Result

    fun clickMouse(button: MouseButton): UnifiedRemoteClient.Result

    sealed interface Event {
        object ImmersiveSucceeded : Event

        data class ImmersiveFailed(val reason: String) : Event

        object EnvironmentChanged : Event
    }
}
