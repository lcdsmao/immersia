package dev.lcdsmao.immersia

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.ComponentName
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import kotlin.math.abs

@SuppressLint("AccessibilityPolicy")
class ImmersiaAccessibilityService : AccessibilityService() {
    interface Listener {
        fun onImmersiveSucceeded()
        fun onImmersiveFailed(reason: String)
        fun onEnvironmentChanged()
    }

    interface DisplayProvider {
        fun display(): Display
    }

    private data class Pane(val packageName: String, val bounds: Rect)

    companion object {
        @Volatile
        var instance: ImmersiaAccessibilityService? = null
            private set

        @Volatile
        var listener: Listener? = null

        @Volatile
        var displayProvider: DisplayProvider? = null

        const val VIDEO_RATIO = 0.68f
        const val CAMERA_NATURAL_X = 0.7562f
        const val CAMERA_NATURAL_Y = 0.0252f
        const val RATIO_TOLERANCE = 0.12f
        const val BOUNDARY_TOLERANCE = 40
        const val MIN_PANE_SIZE = 300
        const val SAFE_MARGIN = 160
        const val TAP_DURATION = 80L
        const val DRAG_DURATION = 220L
        const val POPUP_DELAY = 260L
        const val ANIMATION_DELAY = 550L
        const val VERIFY_DELAY = 500L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var operationRunning = false

    override fun onServiceConnected() {
        instance = this
        listener?.onEnvironmentChanged()
    }

    override fun onDestroy() {
        operationRunning = false
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            listener?.onEnvironmentChanged()
        }
    }

    override fun onInterrupt() = Unit

    fun isInSplitMode(): Boolean = findSplitPair() != null

    fun isLandscapeDisplay(): Boolean {
        val display = displayProvider?.display() ?: return false
        return display.rotation == Surface.ROTATION_90 || display.rotation == Surface.ROTATION_270
    }

    fun beginImmersive() {
        if (operationRunning) return
        if (!isInSplitMode()) {
            fail("Create a split screen with your video app and Immersia first.")
            return
        }
        if (!isLandscapeDisplay()) {
            fail("The system has not applied a landscape display layout. Rotate the device or use the system rotation control, then try again.")
            return
        }

        operationRunning = true
        ensureTopBottomSplit()
    }

    fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val expected =
            ComponentName(this, ImmersiaAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun ensureTopBottomSplit() {
        if (!operationRunning) return
        if (isTopBottomSplit()) {
            ensureImmersiaCoversCamera()
            return
        }

        val boundary = findSplitBoundary() ?: run {
            fail("Could not find the Samsung split-screen divider.")
            return
        }
        tap(boundary.x.toFloat(), boundary.y.toFloat()) {
            handler.postDelayed({
                if (!operationRunning) return@postDelayed
                if (!clickSystemUiNode("rotating_icon", "Rotate clockwise")) {
                    fail("Samsung's split orientation control was not found.")
                    return@postDelayed
                }
                handler.postDelayed({
                    if (isTopBottomSplit()) {
                        ensureImmersiaCoversCamera()
                    } else {
                        fail("Samsung did not switch the split to top/bottom mode.")
                    }
                }, ANIMATION_DELAY)
            }, POPUP_DELAY)
        }
    }

    private fun ensureImmersiaCoversCamera() {
        if (!operationRunning) return
        val display = findSplitDisplayBounds() ?: run {
            fail("Could not read the split-screen bounds.")
            return
        }
        val camera = findCameraPoint(display)
        val panes = findSplitPair() ?: run {
            fail("Could not identify the two split-screen apps.")
            return
        }
        val ours = panes.firstOrNull { it.packageName == packageName }
        if (ours == null) {
            fail("Immersia is not one of the active split-screen apps.")
            return
        }

        if (!ours.bounds.contains(camera.x, camera.y)) {
            val boundary = findSplitBoundary() ?: run {
                fail("Could not find the Samsung split-screen divider.")
                return
            }
            tap(boundary.x.toFloat(), boundary.y.toFloat()) {
                handler.postDelayed({
                    if (!operationRunning) return@postDelayed
                    if (!clickSystemUiNode("switching_icon", "Switch window")) {
                        fail("Samsung's switch-window control was not found.")
                        return@postDelayed
                    }
                    handler.postDelayed({
                        verifyCameraPane()
                    }, ANIMATION_DELAY)
                }, POPUP_DELAY)
            }
        } else {
            resizeImmersiaPane(display, camera)
        }
    }

    private fun verifyCameraPane() {
        val display = findSplitDisplayBounds()
        val camera = display?.let(::findCameraPoint)
        val ours = findSplitPair()?.firstOrNull { it.packageName == packageName }
        if (display == null || camera == null || ours == null ||
            !ours.bounds.contains(camera.x, camera.y)
        ) {
            fail("Could not place Immersia over the inner camera area.")
            return
        }
        resizeImmersiaPane(display, camera)
    }

    private fun resizeImmersiaPane(display: Rect, camera: Point) {
        val panes = findSplitPair() ?: run {
            fail("Could not identify the split-screen apps after switching them.")
            return
        }
        val ours = panes.firstOrNull { it.packageName == packageName } ?: run {
            fail("Immersia is not one of the active split-screen apps.")
            return
        }
        if (!ours.bounds.contains(camera.x, camera.y)) {
            fail("The camera area is outside the Immersia pane.")
            return
        }

        val divider = findSplitBoundary() ?: run {
            fail("Could not find the Samsung split-screen divider.")
            return
        }
        // The camera pane is the source of truth. App bounds can still be stale for
        // one accessibility frame after Samsung switches the windows.
        val cameraIsTop = camera.y < display.centerY()
        val appRatio = if (cameraIsTop) 1f - VIDEO_RATIO else VIDEO_RATIO
        val targetY = (display.top + display.height() * appRatio)
            .toInt()
            .coerceIn(display.top + SAFE_MARGIN, display.bottom - SAFE_MARGIN)

        drag(divider.x.toFloat(), divider.y.toFloat(), divider.x.toFloat(), targetY.toFloat()) {
            handler.postDelayed({
                val finalPanes = findSplitPair()
                val finalOurs = finalPanes?.firstOrNull { it.packageName == packageName }
                val finalDisplay = findSplitDisplayBounds()
                val finalCamera = finalDisplay?.let(::findCameraPoint)
                val finalRatio = finalOurs?.let {
                    it.bounds.height().toFloat() / (finalDisplay?.height()?.toFloat() ?: 1f)
                }
                if (finalOurs != null && finalDisplay != null && finalCamera != null &&
                    finalOurs.bounds.contains(finalCamera.x, finalCamera.y) &&
                    finalRatio != null && abs(finalRatio - (1f - VIDEO_RATIO)) < RATIO_TOLERANCE
                ) {
                    operationRunning = false
                    listener?.onImmersiveSucceeded()
                } else {
                    fail("The split divider did not reach the requested immersive ratio.")
                }
            }, VERIFY_DELAY)
        }
    }

    private fun tap(x: Float, y: Float, onCompleted: () -> Unit) {
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, TAP_DURATION))
            .build()
        if (!dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) = onCompleted()

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    fail("Samsung cancelled the split-screen control gesture.")
                }
            }, null)
        ) {
            fail("Could not dispatch the split-screen control gesture.")
        }
    }

    private fun drag(startX: Float, startY: Float, endX: Float, endY: Float, onCompleted: () -> Unit) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, DRAG_DURATION))
            .build()
        if (!dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) = onCompleted()

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    fail("Samsung cancelled the split-divider gesture.")
                }
            }, null)
        ) {
            fail("Could not dispatch the split-divider gesture.")
        }
    }

    private fun clickSystemUiNode(resourcePart: String, description: String): Boolean {
        for (window in windows) {
            val root = window.root ?: continue
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                val resourceId = node.viewIdResourceName.orEmpty()
                val contentDescription = node.contentDescription?.toString().orEmpty()
                if ((resourceId.contains(resourcePart, ignoreCase = true) ||
                    contentDescription.equals(description, ignoreCase = true)) && node.isVisibleToUser
                ) {
                    if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                    node.parent?.let {
                        if (it.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                    }
                }
                for (index in 0 until node.childCount) {
                    node.getChild(index)?.let(queue::add)
                }
            }
        }
        return false
    }

    private fun findSplitPair(): List<Pane>? {
        val panes = applicationPanes()
        for (firstIndex in panes.indices) {
            for (secondIndex in firstIndex + 1 until panes.size) {
                val first = panes[firstIndex]
                val second = panes[secondIndex]
                val topBottom = abs(first.bounds.left - second.bounds.left) < BOUNDARY_TOLERANCE &&
                    abs(first.bounds.right - second.bounds.right) < BOUNDARY_TOLERANCE &&
                    (abs(first.bounds.bottom - second.bounds.top) < BOUNDARY_TOLERANCE ||
                        abs(second.bounds.bottom - first.bounds.top) < BOUNDARY_TOLERANCE)
                val leftRight = abs(first.bounds.top - second.bounds.top) < BOUNDARY_TOLERANCE &&
                    abs(first.bounds.bottom - second.bounds.bottom) < BOUNDARY_TOLERANCE &&
                    (abs(first.bounds.right - second.bounds.left) < BOUNDARY_TOLERANCE ||
                        abs(second.bounds.right - first.bounds.left) < BOUNDARY_TOLERANCE)
                if (topBottom || leftRight) return listOf(first, second)
            }
        }
        return null
    }

    private fun applicationPanes(): List<Pane> = windows.mapNotNull { window ->
        if (window.type != AccessibilityWindowInfo.TYPE_APPLICATION) return@mapNotNull null
        val root = window.root ?: return@mapNotNull null
        val rect = Rect().also(window::getBoundsInScreen)
        val appPackage = root.packageName?.toString() ?: return@mapNotNull null
        if (rect.width() < MIN_PANE_SIZE || rect.height() < MIN_PANE_SIZE) return@mapNotNull null
        Pane(appPackage, rect)
    }.distinctBy { "${it.packageName}:${it.bounds.toShortString()}" }

    private fun isTopBottomSplit(): Boolean {
        val panes = findSplitPair() ?: return false
        return abs(panes[0].bounds.left - panes[1].bounds.left) < BOUNDARY_TOLERANCE &&
            abs(panes[0].bounds.right - panes[1].bounds.right) < BOUNDARY_TOLERANCE
    }

    private fun findSplitBoundary(): Point? {
        for (window in windows) {
            if (window.type == AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER ||
                window.title?.toString().equals("Split screen divider", ignoreCase = true)
            ) {
                val rect = Rect().also(window::getBoundsInScreen)
                if (!rect.isEmpty) return Point(rect.centerX(), rect.centerY())
            }
        }

        val panes = findSplitPair() ?: return null
        val first = panes[0].bounds
        val second = panes[1].bounds
        return if (isTopBottomSplit()) {
            Point(first.centerX(), (first.bottom + second.top) / 2)
        } else {
            Point((first.right + second.left) / 2, first.centerY())
        }
    }

    private fun findSplitDisplayBounds(): Rect? {
        val panes = findSplitPair() ?: return null
        return Rect(panes[0].bounds).apply { union(panes[1].bounds) }
    }

    private fun findCameraPoint(display: Rect): Point {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val cutout = windowManager.currentWindowMetrics.windowInsets.displayCutout
        val cutoutRect = cutout?.boundingRects
            ?.filterNot(Rect::isEmpty)
            ?.maxByOrNull { it.width().toLong() * it.height().toLong() }
        if (cutoutRect != null && display.contains(cutoutRect.centerX(), cutoutRect.centerY())) {
            return Point(cutoutRect.centerX(), cutoutRect.centerY())
        }

        // Samsung exposes the Fold inner UDC geometry in natural-display coordinates
        // through diagnostics, but may hide it from ordinary app windows. Transform
        // the calibrated natural top-right position into the current display rotation.
        return transformNaturalCameraPoint(display, windowManager.defaultDisplay.rotation)
    }

    private fun transformNaturalCameraPoint(display: Rect, rotation: Int): Point {
        val naturalX = display.left + display.width() * CAMERA_NATURAL_X
        val naturalY = display.top + display.height() * CAMERA_NATURAL_Y
        val x: Float
        val y: Float
        when (rotation) {
            Surface.ROTATION_90 -> {
                x = display.left + display.width() * CAMERA_NATURAL_Y
                y = display.top + display.height() * (1f - CAMERA_NATURAL_X)
            }
            Surface.ROTATION_180 -> {
                x = display.left + display.width() * (1f - CAMERA_NATURAL_X)
                y = display.top + display.height() * (1f - CAMERA_NATURAL_Y)
            }
            Surface.ROTATION_270 -> {
                x = display.left + display.width() * (1f - CAMERA_NATURAL_Y)
                y = display.top + display.height() * CAMERA_NATURAL_X
            }
            else -> {
                x = naturalX
                y = naturalY
            }
        }
        return Point(x.toInt(), y.toInt())
    }

    private fun fail(reason: String) {
        if (!operationRunning) {
            listener?.onImmersiveFailed(reason)
            return
        }
        operationRunning = false
        handler.removeCallbacksAndMessages(null)
        listener?.onImmersiveFailed(reason)
    }

}
