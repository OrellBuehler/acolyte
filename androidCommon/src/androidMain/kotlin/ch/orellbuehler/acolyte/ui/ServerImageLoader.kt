package ch.orellbuehler.acolyte.ui

import ch.orellbuehler.acolyte.AcolyteSdk
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

object ServerImageLoader {
    fun create(
        context: PlatformContext,
        sdk: AcolyteSdk,
    ): ImageLoader {
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
