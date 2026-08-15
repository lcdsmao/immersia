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
                if (prepareImmersiveOperation()) {
                    interactor.beginImmersive()
                }
                delay(IMMERSIVE_CHECK_DELAY)
            }
        }
        if (eventJob == null) {
            eventJob = viewModelScope.launch {
                interactor.eventFlow.collect { event ->
                    when (event) {
                        ImmersiaAccessibilityInteractor.Event.EnvironmentChanged -> onEnvironmentChanged()
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
        updateUiState {
            (this as? ImmersiaUiState.Immersive)?.copy(mode = mode) ?: this
        }
    }

    private fun updateEnvironment(
        fullyUnfolded: Boolean = environment.fullyUnfolded,
    ) {
        val service = ImmersiaAccessibilityService.instance
        val accessibilityEnabled = service?.isAccessibilityEnabled() == true
        val landscapeReady = service?.isLandscapeDisplay() == true
        val inSplitMode = service?.isInSplitMode() == true
        environment = Environment(
            accessibilityEnabled = accessibilityEnabled,
            serviceReady = service != null,
            fullyUnfolded = fullyUnfolded,
            landscapeReady = landscapeReady,
            inSplitMode = inSplitMode,
        )

        when {
            !accessibilityEnabled -> {
                updateUiState {
                    ImmersiaUiState.AccessibilityDisabled
                }
            }
            !isEnvironmentReady() -> {
                val environmentMessage = when {
                    !environment.fullyUnfolded -> UNFOLDED_MESSAGE
                    !environment.serviceReady -> SERVICE_MESSAGE
                    !environment.landscapeReady -> LANDSCAPE_MESSAGE
                    !environment.inSplitMode -> SPLIT_MESSAGE
                    else -> null
                }
                environmentMessage?.let {
                    updateUiState {
                        ImmersiaUiState.Preparation(environmentMessage)
                    }
                }
            }
        }
    }

    private fun prepareImmersiveOperation(): Boolean {
        if (!isEnvironmentReady()) return false
        trySetPreparationMessage("Adjusting the split screen...")
        return true
    }

    private fun onImmersiveSucceeded() {
        immersiveJob?.cancel()
        immersiveJob = null
        updateUiState {
            ImmersiaUiState.Immersive(mode = ImmersiveMode.Empty)
        }
    }

    private fun onImmersiveFailed(reason: String) {
        trySetPreparationMessage(reason)
        tryStartImmersive()
    }

    private fun onEnvironmentChanged() {
        tryStartImmersive()
    }

    fun pauseImmersive() {
        if (uiState !is ImmersiaUiState.Immersive) return
        immersiveJob?.cancel()
    }

    fun updateUiState(update: ImmersiaUiState.() -> ImmersiaUiState) {
        uiState = uiState.update()
    }

    private fun isEnvironmentReady(): Boolean =
        environment.accessibilityEnabled &&
                environment.serviceReady &&
                environment.fullyUnfolded &&
                environment.landscapeReady &&
                environment.inSplitMode

    private fun trySetPreparationMessage(message: String) {
        updateUiState {
            (this as? ImmersiaUiState.Preparation)?.copy(message = message) ?: this
        }
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
