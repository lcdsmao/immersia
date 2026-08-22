package dev.lcdsmao.immersia

import android.content.Context
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal class KeyboardImeView(context: Context) : AbstractComposeView(context) {
    var keyState: KeyboardKeyState? by mutableStateOf(null)

    @Composable
    override fun Content() {
        keyState?.let {
            KeyboardImeContent(it)
        }
    }
}

@Composable
private fun KeyboardImeContent(keyState: KeyboardKeyState) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0C0F16),
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
                    KeyboardImeKey(
                        key = key,
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
private fun KeyboardImeKey(
    key: KeyboardKey,
    active: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = when {
                    key == KeyboardKey.PLACEHOLDER -> Color.Transparent
                    active -> MaterialTheme.colorScheme.primary
                    else -> Color(0xFF283040)
                },
                shape = RoundedCornerShape(6.dp),
            )
            .pointerInput(key) {
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
            text = key.label,
            color = Color.White,
            fontSize = 9.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
