package dev.lcdsmao.immersia

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ImmersiaViewModel(
    private val immersiveInteractor: () -> ImmersiaAccessibilityInteractor?,
) : ViewModel() {
    private data class Environment(
        val accessibilityEnabled: Boolean = false,
        val serviceReady: Boolean = false,
        val fullyUnfolded: Boolean = false,
        val landscapeReady: Boolean = false,
        val inSplitMode: Boolean = false,
    )

    var uiState: ImmersiaUiState by mutableStateOf(ImmersiaUiState.AccessibilityDisabled)
        private set

    private var environment = Environment()
    private var immersiveJob: Job? = null
    private var eventJob: Job? = null

    fun tryStartImmersive(isFlatPosture: Boolean = environment.fullyUnfolded) {
        val interactor = immersiveInteractor() ?: return
        immersiveJob?.cancel()
        immersiveJob = viewModelScope.launch {
            while (isActive) {
                updateEnvironment(fullyUnfolded = isFlatPosture)
                if (prepareImmersiveOperation()) interactor.beginImmersive()
                delay(IMMERSIVE_CHECK_DELAY)
            }
        }
        if (eventJob == null) {
            eventJob = viewModelScope.launch {
                interactor.eventFlow.collect { event ->
                    when (event) {
                        ImmersiaAccessibilityInteractor.Event.EnvironmentChanged -> tryStartImmersive()
                        is ImmersiaAccessibilityInteractor.Event.ImmersiveFailed -> onImmersiveFailed(
                            event.reason
                        )
                        ImmersiaAccessibilityInteractor.Event.ImmersiveSucceeded -> onImmersiveSucceeded()
                    }
                }
            }
        }
    }

    fun changeImmersiveMode(mode: ImmersiveMode) {
        updateUiState { (this as? ImmersiaUiState.Immersive)?.copy(mode = mode) ?: this }
    }

    fun onKeyboardKey(key: KeyboardKey) {
        val interactor = immersiveInteractor() ?: return
        val immersive = uiState as? ImmersiaUiState.Immersive ?: return
        if (key.isModifier) {
            val wasHeld = key in immersive.heldModifiers
            val nextModifiers =
                if (wasHeld) immersive.heldModifiers - key else immersive.heldModifiers + key
            updateUiState {
                immersive.copy(
                    heldModifiers = nextModifiers,
                    message = if (wasHeld) "Sending ${key.label}." else "${key.label} held.",
                )
            }
            if (wasHeld) interactor.sendKeyboardStroke(key, emptySet())
            return
        }

        val modifiers = immersive.heldModifiers
        updateUiState {
            immersive.copy(
                message = "Sending ${
                    modifiers.joinToString("+") { it.label }.let { prefix ->
                        if (prefix.isEmpty()) key.label else "$prefix+${key.label}"
                    }
                }.",
            )
        }
        interactor.sendKeyboardStroke(key, modifiers)
        if (modifiers.isNotEmpty()) {
            val consumed = KeyboardKey.entries.first { it in modifiers }
            updateUiState {
                (this as ImmersiaUiState.Immersive).copy(heldModifiers = modifiers - consumed)
            }
        }
    }

    fun onMouseMove(deltaX: Int, deltaY: Int) {
        val result = immersiveInteractor()?.sendMouseMove(deltaX, deltaY) ?: return
        updateImmersiveMessage(result.message)
    }

    fun onMouseButton(button: MouseButton) {
        val result = immersiveInteractor()?.clickMouse(button) ?: return
        updateImmersiveMessage(result.message)
    }

    private fun updateImmersiveMessage(message: String) {
        updateUiState {
            (this as? ImmersiaUiState.Immersive)?.copy(message = message) ?: this
        }
    }

    fun pauseImmersive() {
        if (uiState !is ImmersiaUiState.Immersive) return
        immersiveJob?.cancel()
    }

    private fun updateEnvironment(fullyUnfolded: Boolean = environment.fullyUnfolded) {
        val interactor = immersiveInteractor()
        environment = Environment(
            accessibilityEnabled = interactor?.isAccessibilityEnabled() == true,
            serviceReady = interactor != null,
            fullyUnfolded = fullyUnfolded,
            landscapeReady = interactor?.isLandscapeDisplay() == true,
            inSplitMode = interactor?.isInSplitMode() == true,
        )
        when {
            !environment.accessibilityEnabled -> updateUiState { ImmersiaUiState.AccessibilityDisabled }
            !isEnvironmentReady() -> updateUiState {
                ImmersiaUiState.Preparation(environmentMessage())
            }
        }
    }

    private fun prepareImmersiveOperation(): Boolean {
        if (!isEnvironmentReady()) return false
        updateUiState {
            (this as? ImmersiaUiState.Preparation)?.copy(message = "Adjusting the split screen...")
                ?: this
        }
        return true
    }

    private fun onImmersiveSucceeded() {
        immersiveJob?.cancel()
        immersiveJob = null
        updateUiState {
            this as? ImmersiaUiState.Immersive ?: ImmersiaUiState.Immersive(ImmersiveMode.Empty)
        }
    }

    private fun onImmersiveFailed(reason: String) {
        updateUiState { (this as? ImmersiaUiState.Preparation)?.copy(message = reason) ?: this }
        tryStartImmersive()
    }

    private fun environmentMessage() = when {
        !environment.fullyUnfolded -> UNFOLDED_MESSAGE
        !environment.serviceReady -> SERVICE_MESSAGE
        !environment.landscapeReady -> LANDSCAPE_MESSAGE
        !environment.inSplitMode -> SPLIT_MESSAGE
        else -> "Preparing..."
    }

    private fun isEnvironmentReady() =
        environment.accessibilityEnabled && environment.serviceReady &&
                environment.fullyUnfolded && environment.landscapeReady && environment.inSplitMode

    private fun updateUiState(update: ImmersiaUiState.() -> ImmersiaUiState) {
        uiState = uiState.update()
    }

    companion object {
        const val UNFOLDED_MESSAGE = "Open the inner display completely before starting Immersia."
        const val SERVICE_MESSAGE =
            "The accessibility service is still starting. Try again in a moment."
        const val LANDSCAPE_MESSAGE = "Rotate the device to landscape before starting Immersia."
        const val SPLIT_MESSAGE = "Create a split screen with your video app and Immersia first."
        val IMMERSIVE_CHECK_DELAY = 1.seconds
    }
}
