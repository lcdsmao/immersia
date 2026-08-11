# Immersia

Immersia is a helper app for Samsung Galaxy Z Fold devices. It uses a black split-screen companion pane to keep fullscreen video away from the inner display camera hole.

## Requirements

- Samsung Galaxy Z Fold device with a compatible One UI version
- Expanded inner display, fully unfolded
- A video app and Immersia already placed in split-screen mode
- Immersia accessibility service enabled

## Usage

1. Open the inner display completely.
2. Enable **Immersia** in **Settings > Accessibility**.
3. Start a video app and Immersia in split-screen mode.
4. Keep Immersia visible in the split pair. It automatically enters immersive mode when the display is flat, landscape, and the split environment is ready.

Immersia will:

- Wait for and verify a landscape display layout.
- Convert left/right split-screen to top/bottom when needed.
- Place Immersia over the camera side.
- Resize the Immersia pane to approximately 32% of the display.
- Show a plain black companion surface.

Double-tap the black surface to pause immersive mode. The controls return temporarily and immersive mode resumes automatically after three seconds. Use **Exit Immersia** to leave the helper app.

## Build

```shell
./gradlew :app:assembleDebug
```

Install the generated APK from `app/build/outputs/apk/debug/`.

## Limitations

Immersia relies on Samsung accessibility UI controls because normal third-party apps cannot directly control another app's split-screen stage or divider ratio. Accessibility labels and Samsung window behavior may change between One UI versions.
