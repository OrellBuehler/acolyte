package ch.orellbuehler.acolyte.ui.title

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.title.TrailerSource
import ch.orellbuehler.acolyte.ui.theme.Palette
import ch.orellbuehler.acolyte.ui.theme.acolyte

@OptIn(UnstableApi::class)
@Composable
fun TrailerPlayer(
    source: TrailerSource,
    viewModel: TitleViewModel,
    links: TitleNav,
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
                    ?.let { viewModel.mediaUrl(it) }
                    ?.let { it to true },
            )
        }
    val fallBack = {
        if (source.youTubeUrl.isNotBlank()) links.url(source.youTubeUrl)
        onDismiss()
    }
    if (streams.isEmpty()) {
        LaunchedEffect(source) { fallBack() }
        return
    }
    var index by remember(source) { mutableIntStateOf(0) }
    val player =
        remember(source) {
            val http = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
            val signed =
                ResolvingDataSource.Factory(http) { spec ->
                    val headers = viewModel.mediaHeaders(spec.uri.toString())
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
    LaunchedEffect(index) {
        val stream = streams.getOrNull(index)
        if (stream == null) {
            fallBack()
            return@LaunchedEffect
        }
        val item = MediaItem.Builder().setUri(stream.first)
        if (stream.second) item.setMimeType(MimeTypes.APPLICATION_M3U8)
        player.setMediaItem(item.build())
        player.prepare()
        player.playWhenReady = true
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Palette.Ink)) {
            AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.title_close), tint = MaterialTheme.acolyte.onArt)
            }
            if (source.youTubeUrl.isNotBlank()) {
                TextButton(
                    onClick = {
                        links.url(source.youTubeUrl)
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                ) { Text(stringResource(R.string.title_watch_on_youtube), color = MaterialTheme.acolyte.onArt) }
            }
        }
    }
}
