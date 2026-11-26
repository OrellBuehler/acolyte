package ch.orellbuehler.acolyte.tv.title

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayDisabled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.title.TrailerSource
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvActionButton
import ch.orellbuehler.acolyte.tv.components.requestFocusSafely
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvType

@OptIn(UnstableApi::class)
@Composable
fun TvTrailerPlayer(
    source: TrailerSource,
    mediaUrl: (String) -> String?,
    mediaHeaders: (String) -> Map<String, String>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val streams =
        remember(source) {
            listOfNotNull(
                source.hls.ifBlank { null }?.let { it to true },
                source.mp4.ifBlank { null }?.let { it to false },
                source.relay
                    .ifBlank { null }
                    ?.let { mediaUrl(it) }
                    ?.let { it to true },
            )
        }
    var index by remember(source) { mutableIntStateOf(0) }
    val player =
        remember(source) {
            val http = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
            val signed =
                ResolvingDataSource.Factory(http) { spec ->
                    val headers = mediaHeaders(spec.uri.toString())
                    if (headers.isEmpty()) spec else spec.withAdditionalHeaders(headers)
                }
            ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(signed)).build()
        }
    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    index++
                }
            }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    val failed = index >= streams.size
    LaunchedEffect(index, streams) {
        val stream = streams.getOrNull(index) ?: return@LaunchedEffect
        val item = MediaItem.Builder().setUri(stream.first)
        if (stream.second) item.setMimeType(MimeTypes.APPLICATION_M3U8)
        player.setMediaItem(item.build())
        player.prepare()
        player.playWhenReady = true
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(TvColors.background), contentAlignment = Alignment.Center) {
            if (failed) {
                val focus = remember { FocusRequester() }
                Column(
                    Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Rounded.PlayDisabled, contentDescription = null, tint = TvColors.textMuted, modifier = Modifier.size(48.dp))
                    Text(
                        stringResource(R.string.tv_trailer_unavailable),
                        style = TvType.callout,
                        color = TvColors.text,
                        textAlign = TextAlign.Center,
                    )
                    TvActionButton(stringResource(R.string.tv_title_close), onDismiss, Modifier.focusRequester(focus), prominent = true)
                }
                LaunchedEffect(Unit) { focus.requestFocusSafely() }
            } else {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            this.player = player
                            isFocusable = true
                            requestFocus()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
