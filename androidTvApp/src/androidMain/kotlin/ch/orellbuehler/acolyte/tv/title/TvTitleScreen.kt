package ch.orellbuehler.acolyte.tv.title

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.title.AskOffer
import ch.orellbuehler.acolyte.feature.title.HeroPill
import ch.orellbuehler.acolyte.feature.title.TitleProblem
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.tv.components.TvActionButton
import ch.orellbuehler.acolyte.tv.components.TvBackdropLayer
import ch.orellbuehler.acolyte.tv.components.TvEmptyView
import ch.orellbuehler.acolyte.tv.components.TvStatusChip
import ch.orellbuehler.acolyte.tv.components.TvStatusContent
import ch.orellbuehler.acolyte.tv.components.requestFocusSafely
import ch.orellbuehler.acolyte.tv.components.tvPageInset
import ch.orellbuehler.acolyte.tv.nav.LocalTvNavigator
import ch.orellbuehler.acolyte.tv.theme.TvColors
import ch.orellbuehler.acolyte.tv.theme.TvMetrics
import ch.orellbuehler.acolyte.tv.theme.TvType
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ServerImage
import java.util.Locale

@Composable
fun TvTitleScreen(
    kind: String,
    tmdb: String,
    libraryId: Long,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "title-$kind-$tmdb-$libraryId") { TvTitleViewModel(sdk, kind, tmdb, libraryId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    var resumed by remember { mutableStateOf(false) }
    LifecycleResumeEffect(viewModel) {
        if (resumed) viewModel.refresh() else resumed = true
        onPauseOrDispose { }
    }
    TvTitleContent(
        state = state,
        trailerLoading = viewModel.trailerLoading,
        onRefresh = viewModel::refresh,
        onTrailer = viewModel::playTrailer,
        onFollow = viewModel::toggleFollow,
        episodes = if (state.isShow && !state.details?.seasons.isNullOrEmpty()) ({ EpisodesSection(state, viewModel) }) else null,
    )
    viewModel.trailer?.let { source ->
        TvTrailerPlayer(source, viewModel::mediaUrl, viewModel::mediaHeaders, viewModel::closeTrailer)
    }
}

@Composable
fun TvTitleContent(
    state: TitleState,
    trailerLoading: Boolean,
    onRefresh: () -> Unit,
    onTrailer: () -> Unit,
    onFollow: () -> Unit,
    episodes: (@Composable () -> Unit)?,
) {
    Box(Modifier.fillMaxSize()) {
        if (state.problem == TitleProblem.NONE && state.status.phase == Phase.READY) {
            TvBackdropLayer(state.backdrop.ifEmpty { state.poster })
        }
        when (state.problem) {
            TitleProblem.BLOCKED -> TvEmptyView(title = stringResource(R.string.tv_title_blocked), icon = Icons.Rounded.Block)
            TitleProblem.NOT_FOUND ->
                TvEmptyView(
                    title = stringResource(R.string.tv_title_not_found),
                    message = stringResource(R.string.tv_title_not_found_hint),
                    icon = Icons.Rounded.Search,
                )
            TitleProblem.NOT_IN_LIBRARY ->
                TvEmptyView(
                    title = stringResource(R.string.tv_title_not_in_library),
                    message = stringResource(R.string.tv_title_not_in_library_hint),
                    icon = Icons.Rounded.VideoLibrary,
                )
            TitleProblem.NONE ->
                TvStatusContent(state.status, isEmpty = false, reload = onRefresh) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = TvMetrics.marginY),
                        verticalArrangement = Arrangement.spacedBy(TvMetrics.sectionGap),
                    ) {
                        item(key = "hero") {
                            TitleHero(
                                state,
                                trailerLoading,
                                onTrailer,
                                onFollow,
                                Modifier.fillParentMaxHeight(TvMetrics.TITLE_HERO_FRACTION),
                            )
                        }
                        titleShelves(state, episodes)
                    }
                }
        }
    }
}

@Composable
private fun TitleHero(
    state: TitleState,
    trailerLoading: Boolean,
    onTrailer: () -> Unit,
    onFollow: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.tvPageInset(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
    ) {
        val logo = state.logo
        if (logo.isEmpty()) {
            Text(state.name, style = TvType.largeTitle, color = TvColors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            ServerImage(
                source = logo,
                contentDescription = state.name,
                size = ImageSize.POSTER_LARGE,
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier.size(TvMetrics.logoWidth, TvMetrics.logoHeight),
            )
        }
        TitleChips(state)
        val meta = metaLine(state)
        if (meta.isNotEmpty()) Text(meta, style = TvType.callout, color = TvColors.text, maxLines = 1)
        if (state.tagline.isNotEmpty()) {
            Text(
                state.tagline,
                style = TvType.callout.copy(fontStyle = FontStyle.Italic),
                color = TvColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = TvMetrics.readableWidth),
            )
        }
        if (state.overview.isNotEmpty()) {
            Text(
                state.overview,
                style = TvType.footnote,
                color = TvColors.text,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = TvMetrics.readableWidth),
            )
        }
        if (state.starring.isNotEmpty()) {
            Text(
                stringResource(R.string.tv_title_starring) + ": " + state.starring.joinToString(", ") { it.name },
                style = TvType.footnote,
                color = TvColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = TvMetrics.readableWidth),
            )
        }
        TitleActions(state, trailerLoading, onTrailer, onFollow, Modifier.padding(top = 4.dp, bottom = TvMetrics.focusPad))
    }
}

@Composable
private fun TitleChips(state: TitleState) {
    val status = state.libraryStatus
    val pill = tvPillText(state.pill, state.askIsMine)
    if (status == null && pill == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (status != null) TvStatusChip(tvLibraryLabel(status), status.tone)
        if (pill != null) {
            val ok = state.pill == HeroPill.APPROVED || state.pill == HeroPill.AVAILABLE
            TvStatusChip(pill, if (ok) Tone.OK else Tone.IDLE)
        }
    }
}

@Composable
private fun metaLine(state: TitleState): String {
    val parts = mutableListOf<String>()
    if (state.year > 0) parts += state.year.toString()
    if (state.isShow && state.seasonCount > 0) {
        parts += pluralText(state.seasonCount)
    } else {
        val runtime = tvRuntimeText(state.runtime)
        if (runtime.isNotEmpty()) parts += runtime
    }
    if (state.certification.isNotEmpty()) parts += state.certification
    parts += state.genres.take(3)
    if (state.factTmdb > 0) parts += "TMDB " + String.format(Locale.US, "%.1f", state.factTmdb)
    return parts.joinToString(" · ")
}

@Composable
private fun pluralText(count: Int): String =
    androidx.compose.ui.res
        .pluralStringResource(R.plurals.tv_title_seasons_count, count, count)

@Composable
private fun TitleActions(
    state: TitleState,
    trailerLoading: Boolean,
    onTrailer: () -> Unit,
    onFollow: () -> Unit,
    modifier: Modifier,
) {
    val navigator = LocalTvNavigator.current
    val first = remember { FocusRequester() }
    val buttons = mutableListOf<@Composable (Modifier) -> Unit>()
    state.askOffer?.let { offer ->
        buttons +=
            { m ->
                TvActionButton(
                    title = askText(offer),
                    icon = if (offer == AskOffer.ASKED) Icons.Rounded.Check else Icons.Rounded.Add,
                    enabled = offer != AskOffer.ASKED,
                    modifier = m,
                    onClick = { navigator.openAsk(state.kind, state.tmdb) },
                )
            }
    }
    if (state.trailer != null) {
        buttons +=
            { m ->
                TvActionButton(
                    title = stringResource(R.string.tv_title_trailer),
                    icon = Icons.Rounded.PlayCircle,
                    enabled = !trailerLoading,
                    modifier = m,
                    onClick = onTrailer,
                )
            }
    }
    state.fourKOffer?.let { offer ->
        buttons +=
            { m ->
                TvActionButton(
                    title = stringResource(if (offer == AskOffer.ASKED) R.string.tv_title_requested_4k else R.string.tv_title_request_4k),
                    enabled = offer != AskOffer.ASKED,
                    modifier = m,
                    onClick = { navigator.openAsk(state.kind, state.tmdb, fourK = true) },
                )
            }
    }
    if (state.access.followOn && state.hasTmdb) {
        buttons +=
            { m ->
                TvActionButton(
                    title = stringResource(if (state.following) R.string.tv_title_following else R.string.tv_title_follow),
                    icon = if (state.following) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                    enabled = !state.followBusy,
                    modifier = m,
                    onClick = onFollow,
                )
            }
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        buttons.forEachIndexed { index, button -> button(if (index == 0) Modifier.focusRequester(first) else Modifier) }
    }
    LaunchedEffect(Unit) { first.requestFocusSafely() }
}

@Composable
private fun askText(offer: AskOffer): String =
    stringResource(
        when (offer) {
            AskOffer.ASK, AskOffer.SEASONS -> R.string.tv_title_request
            AskOffer.ASK_TOO -> R.string.tv_title_request_too
            AskOffer.ASKED -> R.string.tv_title_requested
        },
    )
