package dev.lcdsmao.immersia

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface ImmersiaAccessibilityInteractor {
    val eventFlow: Flow<Event>

    fun beginImmersive()

    fun setImeMode(mode: ImmersiveMode)

    sealed interface Event {
        object ImmersiveSucceeded : Event

        data class ImmersiveFailed(val reason: String) : Event

        data class SettingsChanged(
            val accessibilityEnabled: Boolean = false,
            val serviceReady: Boolean = false,
        ) : Event
    }

    fun interface EventEmitter {
        fun emit(event: Event)
    }

    interface Holder {
        val interactor: ImmersiaAccessibilityInteractor

        fun bindService(accessibilityService: ImmersiaAccessibilityService?): EventEmitter
    }
}

class ImmersiaAccessibilityInteractorImpl : ImmersiaAccessibilityInteractor {
    private var service: ImmersiaAccessibilityService? = null

    override val eventFlow = MutableSharedFlow<ImmersiaAccessibilityInteractor.Event>(
        replay = 1,
        extraBufferCapacity = 20,
    )

    fun bind(accessibilityService: ImmersiaAccessibilityService?) {
        service = accessibilityService
    }

    override fun beginImmersive() {
        service?.beginImmersive()
    }

    override fun setImeMode(mode: ImmersiveMode) {
        service?.setImeMode(mode)
    }
}
