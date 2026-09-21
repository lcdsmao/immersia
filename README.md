# Immersia

> ⚠️ **Toy project — not production software.** This is an experimental hobby project built for personal use. It comes with no warranty, no stability guarantees, and no official support. Use at your own risk; behavior may break with One UI / Android updates.

Immersia is a helper app for Samsung Galaxy Z Fold devices. It uses a black split-screen companion pane to keep fullscreen video away from the inner display camera hole.

## Demo

https://github.com/user-attachments/assets/fe1129ec-dc34-47aa-b28e-6c0760916283

## Requirements

- Samsung Galaxy Z Fold device with a compatible One UI version
- Expanded inner display, fully unfolded
- A video app and Immersia already placed in split-screen mode
- Immersia accessibility service enabled manually, or Shizuku running with Immersia authorized

## Usage

1. Open the inner display completely.
2. Either enable **Immersia** in **Settings > Accessibility**, or start Shizuku and authorize Immersia so it can enable the service automatically.
3. Start a video app and Immersia in split-screen mode.
4. Keep Immersia visible in the split pair. It automatically enters immersive mode when the display is flat, landscape, and the split environment is ready.

When Shizuku is available and authorized, Immersia enables Samsung's **Full
screen in split screen view** mode and its accessibility service while it is
running. The previous values are restored when Immersia stops. Without
Shizuku, the manually enabled accessibility service continues to work normally.

Immersia will:

- Wait for and verify a landscape display layout.
- Convert left/right split-screen to top/bottom when needed.
- Place Immersia over the camera side.
- Resize the Immersia pane to approximately 32% of the display.
- Enable and switch to its local input method.
- Place the input-method window over Immersia's split pane.

The keyboard and gamepad are modes of a real Android `InputMethodService` with
a Compose UI. Double-tap the lower-right corner to cycle between the default,
keyboard, and gamepad modes. The adjacent app remains the input target and
controls whether the IME is shown; Immersia only supplies the IME window's size
and position. Controls are delivered to the target through
`InputConnection.sendKeyEvent` with explicit down/up events, including
held-state and cancellation handling.

The adjacent app controls when the IME is shown or hidden. Use **Exit Immersia** to leave the helper app.

## Build

```shell
./gradlew :app:assembleDebug
```

Install the generated APK from `app/build/outputs/apk/debug/`.

## Limitations

Immersia relies on Samsung accessibility UI controls because normal third-party apps cannot directly control another app's split-screen stage or divider ratio. Accessibility labels and Samsung window behavior may change between One UI versions.

The keyboard and gamepad send `KeyEvent`s through the focused adjacent app.
Gamepad mode does not create an Android gamepad `InputDevice` or provide analog
axes, and compatibility depends on the target accepting IME input events.

## License

MIT — see [LICENSE](LICENSE).
