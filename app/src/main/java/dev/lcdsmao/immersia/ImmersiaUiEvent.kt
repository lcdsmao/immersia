package dev.lcdsmao.immersia

sealed interface ImmersiaUiEvent {
    data class OnPostureChange(val isFlatPosture: Boolean) : ImmersiaUiEvent

    data class OnConfigurationChanged(
        val isLandscape: Boolean,
        val isInMultiWindowMode: Boolean,
    ) : ImmersiaUiEvent

    data object OnResume : ImmersiaUiEvent

    data object OnPause : ImmersiaUiEvent

    data class OnImmersiveModeChange(val mode: ImmersiveMode) : ImmersiaUiEvent
}
