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
import androidx.compose.runtime.snapshots.SnapshotStateSet
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
    ImmersiaTheme {
        KeyboardImeContent(
            pressedKeys = keyState.pressedKeys,
            onKeyPress = { keyState.press(it) },
            onKeyRelease = { keyState.release(it) },
        )
    }
}

@Composable
private fun KeyboardImeContent(
    pressedKeys: SnapshotStateSet<Int>,
    onKeyPress: (Int) -> Unit,
    onKeyRelease: (Int) -> Unit,
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
                    pressedKeys = pressedKeys,
                    onKeyPress = onKeyPress,
                    onKeyRelease = onKeyRelease,
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
                    pressedKeys = pressedKeys,
                    onKeyPress = onKeyPress,
                    onKeyRelease = onKeyRelease,
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
    pressedKeys: SnapshotStateSet<Int>,
    onKeyPress: (Int) -> Unit,
    onKeyRelease: (Int) -> Unit,
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
                        active = key.keyCode in pressedKeys,
                        onPress = { onKeyPress(key.keyCode) },
                        onRelease = { onKeyRelease(key.keyCode) },
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

@Preview(widthDp = 800, heightDp = 400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun KeyboardImeContentPreview() {
    ImmersiaTheme {
        val set = remember { mutableStateSetOf<Int>() }
        KeyboardImeContent(
            pressedKeys = set,
            onKeyPress = { set.add(it) },
            onKeyRelease = { set.remove(it) },
        )
    }
}
