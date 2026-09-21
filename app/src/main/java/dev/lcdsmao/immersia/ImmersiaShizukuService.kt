package dev.lcdsmao.immersia

class ImmersiaShizukuService : IImmersiaShizukuService.Stub() {
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

    override fun getSecureSetting(key: String): String =
        runSettingsCommand("get", "secure", key)

    override fun putSecureSetting(key: String, value: String) {
        runSettingsCommand("put", "secure", key, value)
    }

    override fun deleteSecureSetting(key: String) {
        runSettingsCommand("delete", "secure", key)
    }

    private fun runSettingsCommand(vararg arguments: String): String {
        val process = ProcessBuilder(
            listOf("/system/bin/settings") + arguments.toList(),
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        check(process.waitFor() == 0) { output }
        return output
    }

    companion object {
        private const val MULTI_WINDOW_MANAGER_CLASS =
            "com.samsung.android.multiwindow.MultiWindowManager"
    }
}
