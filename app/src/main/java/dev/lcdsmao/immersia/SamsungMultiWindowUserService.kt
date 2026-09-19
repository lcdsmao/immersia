package dev.lcdsmao.immersia

class SamsungMultiWindowUserService : ISamsungMultiWindowService.Stub() {
    private val managerType = Class.forName(MULTI_WINDOW_MANAGER_CLASS)
    private val manager = managerType
        .getMethod("getInstance")
        .invoke(null)
    private val isSplitImmersiveModeEnabled = managerType
        .getMethod("isSplitImmersiveModeEnabled")
    private val setSplitImmersiveMode = managerType.getMethod(
        "setSplitImmersiveMode",
        Boolean::class.javaPrimitiveType,
    )

    override fun isSplitImmersiveModeEnabled(): Boolean =
        isSplitImmersiveModeEnabled.invoke(manager) as Boolean

    override fun setSplitImmersiveMode(enabled: Boolean) {
        setSplitImmersiveMode.invoke(manager, enabled)
    }

    companion object {
        private const val MULTI_WINDOW_MANAGER_CLASS =
            "com.samsung.android.multiwindow.MultiWindowManager"
    }
}
