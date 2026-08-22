package dev.lcdsmao.immersia

import android.content.Context
import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lcdsmao.immersia.ui.theme.ImmersiaTheme

internal class KeyboardImeView(context: Context) : AbstractComposeView(context) {
    lateinit var keyState: KeyboardKeyState

    var mode: ImmersiveMode by mutableStateOf(ImmersiveMode.Keyboard)

    @Composable
    override fun Content() {
        ImmersiaTheme {
            when (mode) {
                ImmersiveMode.Keyboard -> KeyboardImeContent(keyState)
                ImmersiveMode.Gamepad -> GamepadImeContent(keyState)
                ImmersiveMode.Default -> Unit
            }
        }
    }
}

@Composable
private fun KeyboardImeContent(
    keyState: KeyboardKeyState,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 8.dp),
        ) {
            val keyboardGap = (maxWidth * 0.07f).coerceIn(10.dp, 22.dp)
            val centerGap = (maxWidth * 0.32f).coerceIn(84.dp, 160.dp)
            val keyGap = 2.dp
            val maxKeyCount = 7
            val keyWidth =
                (maxWidth - centerGap - keyboardGap * 2 - keyGap * (maxKeyCount * 2 - 2)) /
                        (maxKeyCount * 2)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KeyboardImeHalf(
                    rows = KeyboardKey.leftHalf,
                    keyWidth = keyWidth,
                    keyGap = keyGap,
                    horizontalAlignment = Alignment.Start,
                    keyState = keyState,
                    modifier = Modifier.weight(1f),
                )
                Spacer(
                    modifier = Modifier
                        .width(centerGap)
                        .fillMaxHeight(),
                )
                KeyboardImeHalf(
                    rows = KeyboardKey.rightHalf,
                    keyWidth = keyWidth,
                    keyGap = keyGap,
                    horizontalAlignment = Alignment.End,
                    keyState = keyState,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun KeyboardImeHalf(
    rows: List<List<KeyboardKey>>,
    keyWidth: Dp,
    keyGap: Dp,
    horizontalAlignment: Alignment.Horizontal,
    keyState: KeyboardKeyState,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(keyGap),
        horizontalAlignment = horizontalAlignment,
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(keyGap)) {
                row.forEach { key ->
                    ImeKey(
                        label = key.label,
                        keyCode = key.keyCode,
                        active = key.keyCode in keyState.pressedKeys,
                        onPress = { keyState.press(key.keyCode) },
                        onRelease = { keyState.release(key.keyCode) },
                        modifier = Modifier
                            .width(keyWidth)
                            .height(32.dp)
                            .padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImeKey(
    label: String,
    keyCode: Int,
    active: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = when {
                    keyCode == KeyEvent.KEYCODE_UNKNOWN -> MaterialTheme.colorScheme.surface
                    active -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(6.dp),
            )
            .pointerInput(keyCode) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        try {
                            tryAwaitRelease()
                        } finally {
                            onRelease()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GamepadImeContent(keyState: KeyboardKeyState) {
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 8.dp),
        ) {
            val keyGap = 12.dp
            val buttonSize = (maxHeight * 0.25f).coerceIn(42.dp, 88.dp)
            val bottomPadding = 48.dp
            val availableHeight = (maxHeight - bottomPadding).coerceAtLeast(0.dp)
            val shoulderHeight = (availableHeight * 0.16f).coerceIn(24.dp, 34.dp)
            val buttonHeight = (
                    (availableHeight - keyGap * 4) / 3
                    ).coerceIn(20.dp, 56.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GamepadDpad(
                    keyState = keyState,
                    buttonSize = buttonSize,
                    buttonHeight = buttonHeight,
                    gap = keyGap,
                    modifier = Modifier,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        GamepadImeButton(
                            key = GamepadKey.SELECT,
                            keyState = keyState,
                            modifier = Modifier
                                .width(buttonSize * 1.6f)
                                .height(shoulderHeight),
                        )
                        GamepadImeButton(
                            key = GamepadKey.START,
                            keyState = keyState,
                            modifier = Modifier
                                .width(buttonSize * 1.6f)
                                .height(shoulderHeight),
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        GamepadImeButton(
                            key = GamepadKey.L1,
                            keyState = keyState,
                            modifier = Modifier
                                .width(buttonSize * 1.5f)
                                .height(shoulderHeight),
                        )
                        GamepadImeButton(
                            key = GamepadKey.R1,
                            keyState = keyState,
                            modifier = Modifier
                                .width(buttonSize * 1.5f)
                                .height(shoulderHeight),
                        )
                    }

                    Spacer(modifier = Modifier.height(keyGap))

                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        GamepadImeButton(
                            key = GamepadKey.L2,
                            keyState = keyState,
                            modifier = Modifier
                                .width(buttonSize * 1.5f)
                                .height(shoulderHeight),
                        )
                        GamepadImeButton(
                            key = GamepadKey.R2,
                            keyState = keyState,
                            modifier = Modifier
                                .width(buttonSize * 1.5f)
                                .height(shoulderHeight),
                        )
                    }
                }

                GamepadFaceButtons(
                    keyState = keyState,
                    buttonSize = buttonSize,
                    buttonHeight = buttonHeight,
                    gap = keyGap,
                    modifier = Modifier,
                )
            }
        }
    }
}

@Composable
private fun GamepadDpad(
    keyState: KeyboardKeyState,
    buttonSize: Dp,
    buttonHeight: Dp,
    gap: Dp,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        GamepadImeButton(
            GamepadKey.DPAD_UP,
            keyState,
            Modifier
                .width(buttonSize)
                .height(buttonHeight),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap * 2)) {
            GamepadImeButton(
                GamepadKey.DPAD_LEFT,
                keyState,
                Modifier
                    .width(buttonSize)
                    .height(buttonHeight),
            )
            GamepadImeButton(
                GamepadKey.DPAD_RIGHT,
                keyState,
                Modifier
                    .width(buttonSize)
                    .height(buttonHeight),
            )
        }
        GamepadImeButton(
            GamepadKey.DPAD_DOWN,
            keyState,
            Modifier
                .width(buttonSize)
                .height(buttonHeight),
        )
    }
}

@Composable
private fun GamepadFaceButtons(
    keyState: KeyboardKeyState,
    buttonSize: Dp,
    buttonHeight: Dp,
    gap: Dp,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        GamepadImeButton(
            GamepadKey.Y,
            keyState,
            Modifier
                .width(buttonSize)
                .height(buttonHeight),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap * 2)) {
            GamepadImeButton(
                GamepadKey.X,
                keyState,
                Modifier
                    .width(buttonSize)
                    .height(buttonHeight),
            )
            GamepadImeButton(
                GamepadKey.B,
                keyState,
                Modifier
                    .width(buttonSize)
                    .height(buttonHeight),
            )
        }
        GamepadImeButton(
            GamepadKey.A,
            keyState,
            Modifier
                .width(buttonSize)
                .height(buttonHeight),
        )
    }
}

@Composable
private fun GamepadImeButton(
    key: GamepadKey,
    keyState: KeyboardKeyState,
    modifier: Modifier,
) {
    ImeKey(
        label = key.label,
        keyCode = key.keyCode,
        active = key.keyCode in keyState.pressedKeys,
        onPress = { keyState.press(key.keyCode) },
        onRelease = { keyState.release(key.keyCode) },
        modifier = modifier,
    )
}

@Preview(widthDp = 800, heightDp = 250, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun KeyboardImeContentPreview() {
    ImmersiaTheme {
        val set = remember { mutableStateSetOf<Int>() }
        KeyboardImeContent(
            keyState = KeyboardKeyState { set.add(it.keyCode) },
        )
    }
}

@Preview(widthDp = 800, heightDp = 250, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GamepadImeContentPreview() {
    ImmersiaTheme {
        val set = remember { mutableStateSetOf<Int>() }
        GamepadImeContent(
            keyState = KeyboardKeyState { set.add(it.keyCode) },
        )
    }
}
