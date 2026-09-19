package dev.lcdsmao.immersia

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

class SamsungSplitImmersiveController(
    private val componentName: ComponentName,
) {
    private var started = false
    private var permissionRequestPending = false
    private var permissionListenerRegistered = false
    private var serviceBound = false
    private var service: ISamsungMultiWindowService? = null
    private var previousValue: Boolean? = null

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
            val connectedService = ISamsungMultiWindowService.Stub.asInterface(binder)
            service = connectedService
            if (!started) {
                unbindUserService()
                return
            }

            previousValue = connectedService.isSplitImmersiveModeEnabled()
            connectedService.setSplitImmersiveMode(true)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }

    fun onStart() {
        started = true
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
        previousValue = null
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

    companion object {
        private const val REQUEST_CODE = 1001
    }
}
