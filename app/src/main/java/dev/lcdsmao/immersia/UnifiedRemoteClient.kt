package dev.lcdsmao.immersia

import android.content.Context
import android.content.Intent
import android.net.Uri

class UnifiedRemoteClient(private val context: Context) {
    data class Result(val success: Boolean, val message: String)

    fun sendStroke(key: KeyboardKey, modifiers: Set<KeyboardKey>): Result {
        val packageName = findPackage() ?: return Result(
            success = false,
            message = "Unified Remote is not installed.",
        )
        val keys = modifiers
            .filter(KeyboardKey::isModifier)
            .sortedBy(KeyboardKey::ordinal)
            .map(KeyboardKey::remoteName) + key.remoteName
        val uri = buildUri("Core.Keyboard", "stroke", keys)
        context.sendBroadcast(
            Intent(ACTION_URI_SEND)
                .setPackage(packageName)
                .putExtra(EXTRA_URI, uri),
        )
        return Result(
            success = true,
            message = "Unified Remote sent ${keys.joinToString("+")}.",
        )
    }

    fun sendMouseMove(deltaX: Int, deltaY: Int): Result = sendAction(
        remote = "Core.Input",
        action = "MoveBy",
        extras = listOf(deltaX.toString(), deltaY.toString()),
        message = "Unified Remote moved the mouse.",
    )

    fun clickMouse(button: MouseButton): Result = sendAction(
        remote = "Core.Input",
        action = "Click",
        extras = listOf(button.remoteName),
        message = "Unified Remote clicked ${button.label}.",
    )

    private fun findPackage(): String? = PACKAGE_NAMES.firstOrNull { packageName ->
        runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
        }.isSuccess
    }

    private fun buildUri(remote: String, action: String, keys: List<String>): String = buildString {
        append("ur://intent/remote:")
        append(remote)
        append("/action:")
        append(action)
        keys.forEach { key ->
            append("/extra:")
            append(Uri.encode(key))
        }
    }

    private fun sendAction(
        remote: String,
        action: String,
        extras: List<String>,
        message: String,
    ): Result {
        val packageName = findPackage() ?: return Result(
            success = false,
            message = "Unified Remote is not installed.",
        )
        context.sendBroadcast(
            Intent(ACTION_URI_SEND)
                .setPackage(packageName)
                .putExtra(EXTRA_URI, buildUri(remote, action, extras)),
        )
        return Result(success = true, message = message)
    }

    companion object {
        private const val ACTION_URI_SEND = "com.unifiedremote.ACTION_URI_SEND"
        private const val EXTRA_URI = "com.unifiedremote.EXTRA_URI"
        private val PACKAGE_NAMES = listOf(
            "com.Relmtech.RemotePaid",
            "com.Relmtech.Remote",
        )
    }
}
