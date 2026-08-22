package dev.lcdsmao.immersia

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    Box(modifier = Modifier.fillMaxSize()) {
        when (state.mode) {
            ImmersiveMode.Empty -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            )

            ImmersiveMode.UnifiedRemote -> KeyboardMouseSurface(
                state = state,
                onUiEvent = onUiEvent,
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
private fun KeyboardMouseSurface(
    state: ImmersiaUiState.Immersive,
    onUiEvent: (ImmersiaUiEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 8.dp),
        ) {
            val mouseWidth = (maxWidth * 0.32f).coerceIn(84.dp, 160.dp)
            val keyboardGap = (maxWidth * 0.07f).coerceIn(10.dp, 22.dp)
            val keyGap = 2.dp
            val maxKeyCount = 7
            val keyWidth = (
                    maxWidth - mouseWidth - keyboardGap * 2 - keyGap * (maxKeyCount * 2 - 2)
                    ) / (maxKeyCount * 2)

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = state.message,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 8.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KeyboardHalf(
                        rows = KeyboardKey.leftHalf,
                        keyWidth = keyWidth,
                        keyGap = keyGap,
                        horizontalAlignment = Alignment.Start,
                        heldModifiers = state.heldModifiers,
                        onKey = { onUiEvent(ImmersiaUiEvent.OnKeyboardKey(it)) },
                        modifier = Modifier.weight(1f),
                    )
                    MouseSurface(
                        onMove = { x, y -> onUiEvent(ImmersiaUiEvent.OnMouseMove(x, y)) },
                        onButton = { onUiEvent(ImmersiaUiEvent.OnMouseClick(it)) },
                        modifier = Modifier
                            .width(mouseWidth)
                            .fillMaxHeight(),
                    )
                    KeyboardHalf(
                        rows = KeyboardKey.rightHalf,
                        keyWidth = keyWidth,
                        keyGap = keyGap,
                        horizontalAlignment = Alignment.End,
                        heldModifiers = state.heldModifiers,
                        onKey = { onUiEvent(ImmersiaUiEvent.OnKeyboardKey(it)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardHalf(
    rows: List<List<KeyboardKey>>,
    keyWidth: Dp,
    keyGap: Dp,
    horizontalAlignment: Alignment.Horizontal,
    heldModifiers: Set<KeyboardKey>,
    onKey: (KeyboardKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(keyGap),
        horizontalAlignment = horizontalAlignment,
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(keyGap),
            ) {
                row.forEach { key ->
                    KeyboardKeyButton(
                        key = key,
                        active = key in heldModifiers,
                        onClick = { onKey(key) },
                        modifier = Modifier.width(keyWidth),
                    )
                }
            }
        }
    }
}

@Composable
private fun MouseSurface(
    onMove: (Int, Int) -> Unit,
    onButton: (MouseButton) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onMove(dragAmount.x.toInt(), dragAmount.y.toInt())
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        onButton(MouseButton.LEFT)
                    }
                },
        ) {}
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MouseButtonButton(MouseButton.LEFT, onButton, Modifier.weight(1f))
            MouseButtonButton(MouseButton.RIGHT, onButton, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MouseButtonButton(
    button: MouseButton,
    onClick: (MouseButton) -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = { onClick(button) },
        modifier = modifier.height(28.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(button.label, fontSize = 7.sp, maxLines = 1)
    }
}

@Composable
private fun KeyboardKeyButton(
    key: KeyboardKey,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        enabled = key != KeyboardKey.PLACEHOLDER,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Text(text = key.label, fontSize = 7.sp, maxLines = 1)
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
