package app.tah.shell

import android.app.Application
import app.tah.shell.data.AppContainer

class TahApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.ensureChannel()
        container.notifySeededNeedsYou()
    }
}
