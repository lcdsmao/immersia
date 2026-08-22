package dev.lcdsmao.immersia

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.mediaQuery
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.lcdsmao.immersia.ui.theme.ImmersiaTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<ImmersiaViewModel> {
        viewModelFactory {
            initializer { ImmersiaViewModel(immersiveInteractor = { ImmersiaAccessibilityService.instance }) }
        }
    }

    @OptIn(
        ExperimentalComposeUiApi::class,
        ExperimentalMediaQueryApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.insetsController?.apply {
            hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val isFlatPosture = mediaQuery {
                windowPosture == UiMediaScope.Posture.Flat
            }
            LaunchedEffect(isFlatPosture) {
                viewModel.onUiEvent(ImmersiaUiEvent.OnPostureChange(isFlatPosture))
            }

            LifecycleResumeEffect( Unit) {
                viewModel.onUiEvent(ImmersiaUiEvent.OnResume)
                onPauseOrDispose {
                    viewModel.onUiEvent(ImmersiaUiEvent.OnPause)
                }
            }

            LifecycleStartEffect(Unit) {
                val displayProvider = ImmersiaAccessibilityService.DisplayProvider { display }
                ImmersiaAccessibilityService.displayProvider = displayProvider
                onStopOrDispose {
                    if (ImmersiaAccessibilityService.displayProvider == displayProvider) {
                        ImmersiaAccessibilityService.displayProvider = null
                    }
                }
            }

            ImmersiaTheme {
                ImmersiaContent(
                    state = viewModel.uiState,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onUiEvent = viewModel::onUiEvent,
                )
            }
        }
    }
}
