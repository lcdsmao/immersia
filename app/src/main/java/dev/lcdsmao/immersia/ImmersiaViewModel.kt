package dev.lcdsmao.immersia

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ImmersiaViewModel : ViewModel(),
    ImmersiaAccessibilityService.Listener {

    private data class Environment(
        val accessibilityEnabled: Boolean = false,
        val serviceReady: Boolean = false,
        val fullyUnfolded: Boolean = true,
        val landscapeReady: Boolean = false,
        val inSplitMode: Boolean = false,
    )

    var uiState by mutableStateOf(ImmersiaUiState())
        private set

    private var environment = Environment()

    private var autoResumeJob: Job? = null

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
        val environmentMessage = when {
            !environment.accessibilityEnabled -> ACCESSIBILITY_MESSAGE
            !environment.fullyUnfolded -> UNFOLDED_MESSAGE
            !environment.serviceReady -> SERVICE_MESSAGE
            !environment.landscapeReady -> LANDSCAPE_MESSAGE
            !environment.inSplitMode -> SPLIT_MESSAGE
            else -> ""
        }
        when {
            !accessibilityEnabled -> {
                updateUiState {
                    copy(
                        status = ImmersiveStatus.ACCESSIBILITY_DISABLED,
                        message = environmentMessage,
                        canStartImmersive = false,
                    )
                }
            }
            !isEnvironmentReady() -> {
                updateUiState {
                    copy(
                        status = ImmersiveStatus.IDLE,
                        message = environmentMessage,
                        canStartImmersive = false,
                    )
                }
            }
            else -> {
                updateUiState {
                    copy(
                        message = environmentMessage,
                        canStartImmersive = true,
                    )
                }
            }
        }
    }

    fun startImmersive(isFlatPosture: Boolean = environment.fullyUnfolded) {
        updateEnvironment(fullyUnfolded = isFlatPosture)
        if (prepareImmersiveOperation()) {
            ImmersiaAccessibilityService.instance?.beginImmersive()
        }
    }

    private fun prepareImmersiveOperation(): Boolean {
        if (!isEnvironmentReady()) return false
        setMessage("Adjusting the split screen...")
        return true
    }

    override fun onImmersiveSucceeded() {
        updateUiState {
            copy(
                status = ImmersiveStatus.IMMERSIVE,
                message = "",
                canStartImmersive = false,
            )
        }
    }

    override fun onImmersiveFailed(reason: String) {
        setMessage(reason)
        updateEnvironment()
    }

    override fun onEnvironmentChanged() {
        updateEnvironment()
    }

    fun pauseImmersive() {
        if (uiState.status != ImmersiveStatus.IMMERSIVE) return
        updateUiState {
            copy(
                status = ImmersiveStatus.IMMERSIVE_PAUSE,
                message = PAUSE_MESSAGE,
                canStartImmersive = false,
            )
        }
        autoResumeJob?.cancel()
        autoResumeJob = viewModelScope.launch {
            delay(IMMERSIVE_PAUSE_TIMEOUT)
            startImmersive()
        }
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

    private fun setMessage(message: String) {
        updateUiState { copy(message = message) }
    }

    override fun onCleared() {
        if (ImmersiaAccessibilityService.listener == this) {
            ImmersiaAccessibilityService.listener = null
        }
    }

    companion object {
        const val ACCESSIBILITY_MESSAGE =
            "Immersia uses an accessibility service to operate Samsung's split-screen controls."
        const val UNFOLDED_MESSAGE = "Open the inner display completely before starting Immersia."
        const val SERVICE_MESSAGE =
            "The accessibility service is still starting. Try again in a moment."
        const val LANDSCAPE_MESSAGE = "Rotate the device to landscape before starting Immersia."
        const val SPLIT_MESSAGE = "Create a split screen with your video app and Immersia first."
        const val PAUSE_MESSAGE = "The controls will return automatically in three seconds."
        val IMMERSIVE_PAUSE_TIMEOUT = 3.seconds
    }
}
