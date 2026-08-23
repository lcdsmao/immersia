package dev.lcdsmao.immersia

sealed interface ImmersiaUiEvent {
    data class OnConfigurationChanged(
        val isFlatPosture: Boolean,
        val isLargeWidth: Boolean,
        val isLargeHeight: Boolean,
        val isLandscape: Boolean,
        val isInMultiWindowMode: Boolean,
    ) : ImmersiaUiEvent

    data object OnResume : ImmersiaUiEvent

    data object OnPause : ImmersiaUiEvent

    data class OnImmersiveModeChange(val mode: ImmersiveMode) : ImmersiaUiEvent
}
