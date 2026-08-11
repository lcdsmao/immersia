package dev.lcdsmao.immersia

data class ImmersiaUiState(
    val status: ImmersiveStatus = ImmersiveStatus.ACCESSIBILITY_DISABLED,
    val message: String = "Immersia uses an accessibility service to operate Samsung's split-screen controls.",
)
