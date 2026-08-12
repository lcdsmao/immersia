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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.mediaQuery
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import dev.lcdsmao.immersia.ui.theme.ImmersiaTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<ImmersiaViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ImmersiaAccessibilityService.listener = viewModel
        window.insetsController?.apply {
            hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            @OptIn(ExperimentalComposeUiApi::class, ExperimentalMediaQueryApi::class)
            val isFlatPosture = mediaQuery {
                windowPosture == UiMediaScope.Posture.Flat
            }
            val orientation = LocalConfiguration.current.orientation
            LifecycleResumeEffect(isFlatPosture, orientation) {
                viewModel.startImmersive(isFlatPosture)
                onPauseOrDispose {
                    viewModel.pauseImmersive()
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
                    onPause = viewModel::pauseImmersive,
                    onExit = { finishAndRemoveTask() },
                )
            }
        }
    }
}
