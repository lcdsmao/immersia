package dev.lcdsmao.immersia

import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class ImmersiaInputMethodService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private lateinit var keyState: KeyboardKeyState

    companion object {
        @Volatile
        var instance: ImmersiaInputMethodService? = null
            private set
    }


    override val lifecycle = LifecycleRegistry(this)

    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        instance = this
        keyState = KeyboardKeyState { event ->
            currentInputConnection?.sendKeyEvent(event) ?: false
        }
        setCandidatesViewShown(false)
    }

    override fun onUnbindInput() {
        keyState.releaseAll()
        KeyboardImeState.setReady(false)
        super.onUnbindInput()
    }

    override fun onCreateInputView(): View = KeyboardImeView(this).also {
        it.keyState = keyState
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
    }

    override fun onConfigureWindow(
        win: Window,
        isFullscreen: Boolean,
        isCandidatesOnly: Boolean,
    ) {
        super.onConfigureWindow(win, false, isCandidatesOnly)
        win.isNavigationBarContrastEnforced = false
        win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        applyOverlayBounds(win)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onComputeInsets(outInsets: Insets) {
        outInsets.contentTopInsets = 0
        outInsets.visibleTopInsets = 0
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_FRAME
        outInsets.touchableRegion.setEmpty()
    }

    override fun onBindInput() {
        super.onBindInput()
        KeyboardImeState.setReady(false)
    }

    override fun onStartInput(
        editorInfo: EditorInfo?,
        restarting: Boolean,
    ) {
        keyState.releaseAll()
        super.onStartInput(editorInfo, restarting)
        KeyboardImeState.setReady(true)
    }

    override fun onStartInputView(
        editorInfo: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInputView(editorInfo, restarting)
        setCandidatesViewShown(false)
        applyOverlayBounds()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        keyState.releaseAll()
        KeyboardImeState.setReady(false)
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        keyState.releaseAll()
        KeyboardImeState.setReady(false)
        super.onFinishInput()
    }

    override fun onWindowHidden() {
        keyState.releaseAll()
        KeyboardImeState.setReady(false)
        super.onWindowHidden()
    }

    override fun onDestroy() {
        keyState.releaseAll()
        KeyboardImeState.setReady(false)
        if (instance === this) instance = null
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    fun applyOverlayBounds() {
        window?.window?.let(::applyOverlayBounds)
    }

    fun releaseAllPressedKeys() {
        keyState.releaseAll()
    }

    private fun applyOverlayBounds(window: Window) {
        val bounds = KeyboardImeState.overlayBounds ?: return
        if (bounds.isEmpty) return

        val attributes = window.attributes
        attributes.gravity = Gravity.TOP or Gravity.START
        attributes.x = bounds.left
        attributes.y = bounds.top
        attributes.width = bounds.width()
        attributes.height = bounds.height()
        attributes.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        window.attributes = attributes
        window.setLayout(bounds.width(), bounds.height())
    }
}
