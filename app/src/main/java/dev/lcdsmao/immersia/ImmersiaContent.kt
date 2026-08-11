package dev.lcdsmao.immersia

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ImmersiaContent(
    state: ImmersiaUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onExit: () -> Unit,
    onPause: () -> Unit,
) {
    if (state.status == ImmersiveStatus.IMMERSIVE) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onPause() })
                },
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "IMMERSIA",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = when (state.status) {
                    ImmersiveStatus.ACCESSIBILITY_DISABLED -> "Accessibility access required"
                    ImmersiveStatus.IDLE -> "Ready for immersive video"
                    ImmersiveStatus.IMMERSIVE_PAUSE -> "Immersive mode paused"
                    ImmersiveStatus.IMMERSIVE -> ""
                },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            if (state.message.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(Modifier.height(28.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (state.status) {
                            ImmersiveStatus.ACCESSIBILITY_DISABLED -> "1"
                            ImmersiveStatus.IDLE -> "2"
                            ImmersiveStatus.IMMERSIVE_PAUSE -> "P"
                            ImmersiveStatus.IMMERSIVE -> ""
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = when (state.status) {
                            ImmersiveStatus.ACCESSIBILITY_DISABLED -> "Accessibility disabled"
                            ImmersiveStatus.IDLE -> "Idle"
                            ImmersiveStatus.IMMERSIVE_PAUSE -> "Immersive pause"
                            ImmersiveStatus.IMMERSIVE -> ""
                        },
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            when (state.status) {
                ImmersiveStatus.ACCESSIBILITY_DISABLED -> Button(onClick = onOpenAccessibilitySettings) {
                    Text("Open accessibility settings")
                }
                ImmersiveStatus.IMMERSIVE_PAUSE -> Button(onClick = onExit) {
                    Text("Exit Immersia")
                }
                ImmersiveStatus.IDLE,
                ImmersiveStatus.IMMERSIVE,
                    -> Unit
            }
        }
    }
}
