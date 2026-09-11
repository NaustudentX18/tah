package app.tah.shell

import android.app.Application
import app.tah.shell.data.AppContainer
import app.tah.shell.data.ProviderKind

class TahApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.ensureChannel()
        // Returning users who already configured a provider skip SCR-ONBOARD.
        if (!container.settings.current.onboardingComplete) {
            if (container.providers.current.kind != ProviderKind.Demo) {
                container.settings.setOnboardingComplete(true)
            }
        }
        container.notifySeededNeedsYou()
    }
}
