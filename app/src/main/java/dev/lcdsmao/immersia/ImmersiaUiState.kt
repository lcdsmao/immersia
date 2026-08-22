package dev.lcdsmao.immersia

sealed interface ImmersiaUiState {
    data object AccessibilityDisabled : ImmersiaUiState

    data class Preparation(
        val message: String,
    ) : ImmersiaUiState

    data class Immersive(
        val mode: ImmersiveMode,
    ) : ImmersiaUiState
}
