package dev.lcdsmao.immersia

sealed interface ImmersiaUiEvent {
    data class OnPostureChange(val isFlatPosture: Boolean) : ImmersiaUiEvent

    data object OnResume : ImmersiaUiEvent

    data object OnPause : ImmersiaUiEvent

    data class OnImmersiveModeChange(val mode: ImmersiveMode) : ImmersiaUiEvent

    data class OnKeyboardKey(val key: KeyboardKey) : ImmersiaUiEvent

    data class OnMouseMove(val deltaX: Int, val deltaY: Int) : ImmersiaUiEvent

    data class OnMouseClick(val button: MouseButton) : ImmersiaUiEvent
}
