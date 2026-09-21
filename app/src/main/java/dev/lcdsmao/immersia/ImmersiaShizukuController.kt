package dev.lcdsmao.immersia

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

class ImmersiaShizukuController(
    private val componentName: ComponentName,
) {
    private var started = false
    private var permissionRequestPending = false
    private var permissionListenerRegistered = false
    private var serviceBound = false
    private var service: IImmersiaShizukuService? = null
    private var previousValue: Boolean? = null
    private var previousAccessibilityServices: String? = null
    private var previousAccessibilityEnabled: String? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(componentName)
        .daemon(false)
        .processNameSuffix("multi-window")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != REQUEST_CODE) return@OnRequestPermissionResultListener
            permissionRequestPending = false
            if (grantResult == PackageManager.PERMISSION_GRANTED && started) {
                bindUserService()
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val connectedService = IImmersiaShizukuService.Stub.asInterface(binder)
            service = connectedService
            if (!started) {
                unbindUserService()
                return
            }

            previousValue = connectedService.isSplitImmersiveModeEnabled()
            previousAccessibilityServices = connectedService
                .getSecureSetting(ENABLED_ACCESSIBILITY_SERVICES)
                .takeUnless { it == NULL_SETTING_VALUE }
            previousAccessibilityEnabled = connectedService
                .getSecureSetting(ACCESSIBILITY_ENABLED)
                .takeUnless { it == NULL_SETTING_VALUE }
            connectedService.setSplitImmersiveMode(true)
            connectedService.putSecureSetting(
                ENABLED_ACCESSIBILITY_SERVICES,
                addAccessibilityService(
                    previousAccessibilityServices,
                    IMMERSIA_ACCESSIBILITY_SERVICE,
                ),
            )
            connectedService.putSecureSetting(ACCESSIBILITY_ENABLED, "1")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }

    fun onStart() {
        started = true
        if (!Shizuku.pingBinder()) return

        if (!permissionListenerRegistered) {
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            permissionListenerRegistered = true
        }
        if (serviceBound || permissionRequestPending) return

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindUserService()
        } else {
            permissionRequestPending = true
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    fun onStop() {
        started = false
        permissionRequestPending = false
        previousValue?.let { previous ->
            checkNotNull(service).setSplitImmersiveMode(previous)
        }
        val previousServices = previousAccessibilityServices
        val previousAccessibility = previousAccessibilityEnabled
        if (previousServices == null) {
            checkNotNull(service).deleteSecureSetting(ENABLED_ACCESSIBILITY_SERVICES)
        } else {
            checkNotNull(service).putSecureSetting(
                ENABLED_ACCESSIBILITY_SERVICES,
                previousServices,
            )
        }
        if (previousAccessibility == null) {
            checkNotNull(service).deleteSecureSetting(ACCESSIBILITY_ENABLED)
        } else {
            checkNotNull(service).putSecureSetting(
                ACCESSIBILITY_ENABLED,
                previousAccessibility,
            )
        }
        previousValue = null
        previousAccessibilityServices = null
        previousAccessibilityEnabled = null
        service = null
        if (serviceBound) {
            unbindUserService()
        }
        if (permissionListenerRegistered) {
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
            permissionListenerRegistered = false
        }
    }

    private fun bindUserService() {
        serviceBound = true
        Shizuku.bindUserService(userServiceArgs, serviceConnection)
    }

    private fun unbindUserService() {
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        serviceBound = false
    }

    private fun addAccessibilityService(
        existingServices: String?,
        service: String,
    ): String = existingServices
        .orEmpty()
        .split(':')
        .filter(String::isNotEmpty)
        .let { services ->
            if (service in services) services else services + service
        }
        .joinToString(":")

    companion object {
        private const val REQUEST_CODE = 1001
        private const val NULL_SETTING_VALUE = "null"
        private const val ENABLED_ACCESSIBILITY_SERVICES =
            "enabled_accessibility_services"
        private const val ACCESSIBILITY_ENABLED = "accessibility_enabled"
        private const val IMMERSIA_ACCESSIBILITY_SERVICE =
            "dev.lcdsmao.immersia/dev.lcdsmao.immersia.ImmersiaAccessibilityService"
    }
}
