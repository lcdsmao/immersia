package dev.lcdsmao.immersia

import android.app.Application
import android.view.Display
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi

@OptIn(ExperimentalComposeUiApi::class)
class ImmersiaApplication : Application(),
    ImmersiaAccessibilityInteractor.Holder {
    override fun onCreate() {
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true
        super.onCreate()
    }

    override val interactor = ImmersiaAccessibilityInteractorImpl()

    override fun bindService(
        accessibilityService: ImmersiaAccessibilityService?,
    ) = interactor.bindService(accessibilityService)

    override fun bindDisplay(display: Display?) = interactor.bindDisplay(display)
}
