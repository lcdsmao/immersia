package dev.lcdsmao.immersia

import kotlinx.coroutines.flow.Flow

interface ImmersiaAccessibilityInteractor {
    val eventFlow: Flow<Event>

    fun isAccessibilityEnabled(): Boolean

    fun beginImmersive()

    sealed interface Event {
        object ImmersiveSucceeded : Event

        data class ImmersiveFailed(val reason: String) : Event

        object EnvironmentChanged : Event
    }
}
