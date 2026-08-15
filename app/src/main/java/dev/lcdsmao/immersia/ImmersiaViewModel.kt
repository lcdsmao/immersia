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

class ImmersiaViewModel : ViewModel(),
    ImmersiaAccessibilityService.Listener {

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

    fun startImmersive(isFlatPosture: Boolean = environment.fullyUnfolded) {
        immersiveJob?.cancel()
        immersiveJob = viewModelScope.launch {
            while (isActive) {
                updateEnvironment(fullyUnfolded = isFlatPosture)
                if (prepareImmersiveOperation()) {
                    ImmersiaAccessibilityService.instance?.beginImmersive()
                }
                delay(IMMERSIVE_CHECK_DELAY)
            }
        }
    }

    private fun prepareImmersiveOperation(): Boolean {
        if (!isEnvironmentReady()) return false
        trySetPreparationMessage("Adjusting the split screen...")
        return true
    }

    override fun onImmersiveSucceeded() {
        immersiveJob?.cancel()
        immersiveJob = null
        updateUiState {
            ImmersiaUiState.Immersive(mode = ImmersiveMode.Empty)
        }
    }

    override fun onImmersiveFailed(reason: String) {
        trySetPreparationMessage(reason)
        startImmersive()
    }

    override fun onEnvironmentChanged() {
        startImmersive()
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

    override fun onCleared() {
        if (ImmersiaAccessibilityService.listener == this) {
            ImmersiaAccessibilityService.listener = null
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
