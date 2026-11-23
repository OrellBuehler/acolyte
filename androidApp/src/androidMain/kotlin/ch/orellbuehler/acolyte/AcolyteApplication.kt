package ch.orellbuehler.acolyte

import android.app.Application
import ch.orellbuehler.acolyte.session.AndroidSecureStore
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

class AcolyteApplication :
    Application(),
    SingletonImageLoader.Factory {
    val sdk: AcolyteSdk by lazy { AcolyteSdk(AndroidSecureStore(this), BuildConfig.VERSION_NAME) }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val url = request.url.toString()
                    val headers = sdk.images.headers(url)
                    val response =
                        chain.proceed(
                            request
                                .newBuilder()
                                .apply {
                                    headers.forEach { (name, value) ->
                                        header(name, value)
                                    }
                                }.build(),
                        )
                    val cookies = response.headers("Set-Cookie")
                    if (cookies.isNotEmpty()) runBlocking { sdk.images.storeCookies(url, cookies) }
                    response
                }.build()
        return ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .crossfade(true)
            .build()
    }
}
