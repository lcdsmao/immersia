package dev.lcdsmao.immersia

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.mediaQuery
import dev.lcdsmao.immersia.ui.theme.ImmersiaTheme

class MainActivity : ComponentActivity(),
    ImmersiaAccessibilityService.DisplayProvider {
    private val viewModel by viewModels<ImmersiaViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ImmersiaAccessibilityService.listener = viewModel
        ImmersiaAccessibilityService.displayProvider = this

        setContent {
            @OptIn(ExperimentalComposeUiApi::class, ExperimentalMediaQueryApi::class)
            val isFlatPosture = mediaQuery {
                windowPosture == UiMediaScope.Posture.Flat
            }
            LaunchedEffect(isFlatPosture) {
                viewModel.startImmersive(isFlatPosture)
            }
            ImmersiaTheme {
                ImmersiaContent(
                    state = viewModel.uiState,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onStartImmersive = viewModel::startImmersive,
                    onPause = viewModel::pauseImmersive,
                    onExit = { finishAndRemoveTask() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startImmersive()
    }

    override fun onDestroy() {
        if (ImmersiaAccessibilityService.displayProvider === this) {
            ImmersiaAccessibilityService.displayProvider = null
        }
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.startImmersive()
    }

    override fun display(): Display = display
}
