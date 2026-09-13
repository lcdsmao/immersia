package dev.lcdsmao.immersia

import android.view.Display
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface ImmersiaAccessibilityInteractor {
    val eventFlow: Flow<Event>

    val activeDisplay: ActiveDisplay?

    fun emitEvent(event: Event)

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

    class ActiveDisplay(val rotation: () -> Int)

    interface Holder {
        val interactor: ImmersiaAccessibilityInteractor

        fun bindService(accessibilityService: ImmersiaAccessibilityService?)

        fun bindDisplay(display: Display?)
    }
}

class ImmersiaAccessibilityInteractorImpl : ImmersiaAccessibilityInteractor {
    private var service: ImmersiaAccessibilityService? = null

    override val eventFlow = MutableSharedFlow<ImmersiaAccessibilityInteractor.Event>(
        replay = 1,
        extraBufferCapacity = 20,
    )

    override var activeDisplay: ImmersiaAccessibilityInteractor.ActiveDisplay? = null

    override fun emitEvent(event: ImmersiaAccessibilityInteractor.Event) {
        eventFlow.tryEmit(event)
    }

    fun bindService(accessibilityService: ImmersiaAccessibilityService?) {
        service = accessibilityService
    }

    fun bindDisplay(display: Display?) {
        activeDisplay = display?.let {
            ImmersiaAccessibilityInteractor.ActiveDisplay { display.rotation }
        }
    }

    override fun beginImmersive() {
        service?.beginImmersive()
    }

    override fun setImeMode(mode: ImmersiveMode) {
        service?.setImeMode(mode)
    }
}
