package ch.orellbuehler.acolyte

import android.app.Application
import ch.orellbuehler.acolyte.session.AndroidSecureStore
import ch.orellbuehler.acolyte.ui.ServerImageLoader
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader

class AcolyteApplication :
    Application(),
    SingletonImageLoader.Factory {
    val sdk: AcolyteSdk by lazy { AcolyteSdk(AndroidSecureStore(this), BuildConfig.VERSION_NAME) }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ServerImageLoader.create(context, sdk)
}
