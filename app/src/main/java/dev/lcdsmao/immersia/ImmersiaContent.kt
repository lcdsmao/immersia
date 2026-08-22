package dev.lcdsmao.immersia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun ImmersiaContent(
    state: ImmersiaUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onUiEvent: (ImmersiaUiEvent) -> Unit,
) {
    when (state) {
        ImmersiaUiState.AccessibilityDisabled -> InfoLayout(
            primaryText = "Accessibility access required",
            secondaryText = "Immersia uses an accessibility service to operate Samsung's split-screen controls.",
            statusPrefix = "1",
            statusText = "Accessibility disabled",
        ) {
            Button(onClick = onOpenAccessibilitySettings) {
                Text("Open accessibility settings")
            }
        }

        is ImmersiaUiState.Preparation -> InfoLayout(
            primaryText = "Ready for immersive video",
            secondaryText = state.message,
            statusPrefix = "2",
            statusText = "Preparing...",
        )

        is ImmersiaUiState.Immersive -> ImmersiveContent(
            state = state,
            onUiEvent = onUiEvent,
        )
    }
}

@Composable
private fun ImmersiveContent(
    state: ImmersiaUiState.Immersive,
    onUiEvent: (ImmersiaUiEvent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        var showModeName by remember { mutableStateOf(false) }

        LaunchedEffect(state) {
            showModeName = true
            delay(1.seconds)
            showModeName = false
        }

        AnimatedVisibility(
            visible = showModeName,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = state.mode.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(48.dp)
                .height(48.dp)
                .pointerInput(state.mode) {
                    detectTapGestures(onDoubleTap = {
                        onUiEvent(
                            ImmersiaUiEvent.OnImmersiveModeChange(
                                ImmersiveMode.entries[
                                    (state.mode.ordinal + 1) % ImmersiveMode.entries.size
                                ]
                            )
                        )
                    })
                }
        )
    }
}

@Composable
private fun InfoLayout(
    primaryText: String,
    secondaryText: String,
    statusPrefix: String,
    statusText: String,
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "IMMERSIA",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                primaryText,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                secondaryText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(28.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        statusPrefix,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(statusText)
                }
            }
            extraContent()
        }
    }
}
