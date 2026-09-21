package dev.lcdsmao.immersia

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.derivedMediaQuery
import androidx.compose.ui.mediaQuery
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.lcdsmao.immersia.ui.theme.ImmersiaTheme

class MainActivity : ComponentActivity() {

    private val shizukuController by lazy {
        ImmersiaShizukuController(
            ComponentName(this, ImmersiaShizukuService::class.java),
        )
    }

    private val accessibilityInteractorHolder
        get() = applicationContext as ImmersiaAccessibilityInteractor.Holder

    private val viewModel by viewModels<ImmersiaViewModel> {
        viewModelFactory {
            initializer { ImmersiaViewModel(immersiveInteractor = accessibilityInteractorHolder.interactor) }
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
            LifecycleResumeEffect(Unit) {
                viewModel.onUiEvent(ImmersiaUiEvent.OnResume)
                onPauseOrDispose {
                    viewModel.onUiEvent(ImmersiaUiEvent.OnPause)
                }
            }

            val isFlatPosture = mediaQuery { windowPosture == UiMediaScope.Posture.Flat }
            val isLargeWidth by derivedMediaQuery { windowWidth >= 600.dp }
            val isLargeHeight by derivedMediaQuery { windowHeight >= 480.dp }
            val configuration = LocalConfiguration.current
            LaunchedEffect(isFlatPosture, isLargeWidth, isLargeHeight, configuration) {
                viewModel.onUiEvent(
                    ImmersiaUiEvent.OnConfigurationChanged(
                        isLandscape = display?.rotation == Surface.ROTATION_90 || display?.rotation == Surface.ROTATION_270,
                        isInMultiWindowMode = isInMultiWindowMode,
                        isFlatPosture = isFlatPosture,
                        isLargeWidth = isLargeWidth,
                        isLargeHeight = isLargeHeight,
                    )
                )
            }

            LifecycleStartEffect(Unit) {
                shizukuController.onStart()
                accessibilityInteractorHolder.bindDisplay(display)
                onStopOrDispose {
                    shizukuController.onStop()
                    accessibilityInteractorHolder.bindDisplay(null)
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
