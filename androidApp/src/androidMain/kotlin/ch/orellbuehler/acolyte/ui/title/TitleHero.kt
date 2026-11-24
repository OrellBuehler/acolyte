package ch.orellbuehler.acolyte.ui.title

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.title.AskOffer
import ch.orellbuehler.acolyte.feature.title.LibraryKind
import ch.orellbuehler.acolyte.feature.title.ReleaseScope
import ch.orellbuehler.acolyte.feature.title.ReleaseScopeKind
import ch.orellbuehler.acolyte.feature.title.TitleAction
import ch.orellbuehler.acolyte.feature.title.TitleLogic
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.feature.title.TitleTab
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TitleHero(
    state: TitleState,
    viewModel: TitleViewModel,
    links: TitleNav,
) {
    val background = MaterialTheme.colorScheme.background
    Column {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 11f)) {
            ServerImage(
                source = state.backdrop.ifBlank { state.poster },
                contentDescription = null,
                size = ImageSize.HERO,
                placeholderName = state.name,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 11f),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 11f)
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.acolyte.artScrim.copy(alpha = 0.35f),
                            0.45f to Color.Transparent,
                            1f to background,
                        ),
                    ),
            )
            Column(
                Modifier.align(Alignment.BottomStart).padding(horizontal = Spacing.Screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                if (state.logo.isNotBlank()) {
                    ServerImage(
                        source = state.logo,
                        contentDescription = state.name,
                        size = ImageSize.POSTER_LARGE,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.heightIn(max = 88.dp).fillMaxWidth(0.7f).height(88.dp),
                    )
                } else {
                    Text(state.name, style = MaterialTheme.typography.headlineMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Column(
            Modifier.padding(horizontal = Spacing.Screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            if (state.tagline.isNotBlank()) {
                Text(
                    state.tagline,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MetaLine(state)
            pillText(state.pill, state.askIsMine)?.let { text ->
                StatusChip(text, if (state.pill == ch.orellbuehler.acolyte.feature.title.HeroPill.APPROVED) Tone.OK else Tone.IDLE)
            }
            OwnedByline(state)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                modifier = Modifier.padding(top = Spacing.XSmall),
            ) { HeroActions(state, viewModel, links) }
        }
    }
}

@Composable
private fun MetaLine(state: TitleState) {
    val parts = mutableListOf<String>()
    if (state.year > 0) parts += state.year.toString()
    if (state.isShow && state.seasonCount > 0) {
        parts +=
            androidx.compose.ui.res
                .pluralStringResource(R.plurals.title_seasons_count, state.seasonCount, state.seasonCount)
    } else {
        runtimeText(state.runtime).takeIf { it.isNotBlank() }?.let { parts += it }
    }
    if (state.certification.isNotBlank()) parts += state.certification
    parts += state.genres.take(if (state.isShow) 2 else 3)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        if (parts.isNotEmpty()) {
            Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val scores = state.factScores
        if (scores.isNotEmpty() || state.factTmdb > 0 || state.awardBadges.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                if (state.factTmdb > 0) ScoreText("TMDB", TitleLogic.oneDecimal(state.factTmdb))
                scores.forEach { ScoreText(scoreName(it.source), TitleLogic.scoreText(it)) }
                state.awardBadges.forEach { badge ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.acolyte.wait,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(badge, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreText(
    name: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun scoreName(source: String): String =
    when (source) {
        "imdb" -> "IMDb"
        "rt_critics" -> stringResource(R.string.title_score_rt_critics)
        "rt_audience" -> stringResource(R.string.title_score_rt_audience)
        "metacritic" -> "Metacritic"
        "letterboxd" -> "Letterboxd"
        "trakt" -> "Trakt"
        else -> source
    }

@Composable
private fun OwnedByline(state: TitleState) {
    val owned = state.owned ?: return
    val askers =
        owned.asks
            .flatMap { ask ->
                ask.people.ifEmpty { listOf(ask.askedBy) }
            }.map { it.shown }
            .filter { it.isNotBlank() }
            .distinct()
    if (askers.isNotEmpty()) {
        Text(
            stringResource(R.string.title_asked_by, askers.take(3).joinToString(", ")),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val outcome = state.outcome
    if (outcome != null && outcome.empty) {
        val whys = outcome.sources.map { outcomeWhy(it.name, it.why) }
        val text =
            when {
                outcome.refused -> stringResource(R.string.title_outcome_refused)
                outcome.sources.isNotEmpty() ->
                    stringResource(R.string.title_outcome_nothing, whys.joinToString(", ")) +
                        if (outcome.note.isNotBlank()) " · ${outcome.note}" else ""
                else -> stringResource(R.string.title_outcome_none)
            }
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun outcomeWhy(
    name: String,
    why: String,
): String =
    when (why) {
        "rate_limited" -> stringResource(R.string.title_outcome_rate_limited, name)
        "down" -> stringResource(R.string.title_outcome_down, name)
        "blocked" -> stringResource(R.string.title_outcome_blocked, name)
        "login_refused" -> stringResource(R.string.title_outcome_login_refused, name)
        "broken_definition" -> stringResource(R.string.title_outcome_broken, name)
        "bot_check" -> stringResource(R.string.title_outcome_bot_check, name)
        "site_ban" -> stringResource(R.string.title_outcome_site_ban, name)
        "paused" -> stringResource(R.string.title_outcome_paused, name)
        else -> if (why.isBlank()) name else "$name ($why)"
    }

@Composable
private fun HeroActions(
    state: TitleState,
    viewModel: TitleViewModel,
    links: TitleNav,
) {
    val status = state.libraryStatus
    val presenter = viewModel.presenter
    if (status != null) {
        val icon =
            when (status.kind) {
                LibraryKind.HAVE -> Icons.Rounded.Check
                LibraryKind.FAILED -> Icons.Rounded.Refresh
                LibraryKind.DOWNLOADING -> Icons.Rounded.Download
                LibraryKind.MISSING -> Icons.Rounded.Search
            }
        Button(
            onClick = {
                if (state.primaryRuns) viewModel.launch { presenter.searchNow() } else presenter.selectTab(TitleTab.LIBRARY)
            },
            enabled = state.busy != "search",
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                when {
                    state.primaryRuns && status.kind == LibraryKind.FAILED -> stringResource(R.string.title_retry)
                    state.primaryRuns -> stringResource(R.string.title_search_now)
                    else -> libraryLabel(status)
                },
                modifier = Modifier.padding(start = Spacing.XSmall),
            )
        }
    }
    if (state.canAdd) {
        Button(onClick = { viewModel.openAdd() }) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.title_add_to_library), modifier = Modifier.padding(start = Spacing.XSmall))
        }
    }
    if (state.canCheckReleases) {
        OutlinedButton(onClick = { viewModel.openCheck() }) {
            Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.title_check_releases), modifier = Modifier.padding(start = Spacing.XSmall))
        }
    }
    state.askOffer?.let { offer ->
        val label =
            when (offer) {
                AskOffer.ASK, AskOffer.SEASONS -> stringResource(R.string.title_request)
                AskOffer.ASK_TOO -> stringResource(R.string.title_request_too)
                AskOffer.ASKED -> stringResource(R.string.title_requested)
            }
        Button(onClick = { links.ask(state.kind, state.tmdb) }, enabled = offer != AskOffer.ASKED) { Text(label) }
    }
    state.fourKOffer?.let { offer ->
        OutlinedButton(onClick = { links.ask(state.kind, state.tmdb, fourK = true) }, enabled = offer != AskOffer.ASKED) {
            Text(if (offer == AskOffer.ASKED) stringResource(R.string.title_requested_4k) else stringResource(R.string.title_request_4k))
        }
    }
    if (state.trailer != null) {
        FilledTonalButton(onClick = viewModel::playTrailer, enabled = !viewModel.trailerLoading) {
            if (viewModel.trailerLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(stringResource(R.string.title_trailer), modifier = Modifier.padding(start = Spacing.XSmall))
        }
    }
    if (state.access.manage && status != null && status.kind != LibraryKind.MISSING) {
        state.watchLinks.forEach { link ->
            val kinds = state.watchLinks.count { it.kind == link.kind }
            OutlinedButton(onClick = { links.url(link.url) }) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.title_open_in, if (kinds > 1) link.server else watchKind(link.kind)),
                    modifier = Modifier.padding(start = Spacing.XSmall),
                )
            }
        }
    }
}

private fun watchKind(kind: String): String =
    when (kind) {
        "plex" -> "Plex"
        "jellyfin" -> "Jellyfin"
        "emby" -> "Emby"
        else -> kind
    }

@Composable
fun TitleTopActions(
    state: TitleState,
    viewModel: TitleViewModel,
    links: TitleNav,
) {
    if (state.access.followOn && state.hasTmdb) {
        IconButton(onClick = { viewModel.launch { viewModel.presenter.toggleFollow() } }, enabled = !state.followBusy) {
            Icon(
                if (state.following) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                contentDescription = stringResource(if (state.following) R.string.title_following else R.string.title_follow),
                tint = if (state.following) MaterialTheme.acolyte.accentText else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    val groups = state.actionGroups
    if (groups.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.title_more_actions))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            groups.forEachIndexed { index, group ->
                if (index > 0) HorizontalDivider()
                group.forEach { action ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                actionText(action, state.isShow),
                                color = if (action == TitleAction.DELETE) MaterialTheme.acolyte.failText else Color.Unspecified,
                            )
                        },
                        onClick = {
                            open = false
                            runAction(action, state, viewModel, links)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun actionText(
    action: TitleAction,
    show: Boolean,
): String =
    stringResource(
        when (action) {
            TitleAction.SEARCH -> R.string.title_search_now
            TitleAction.CHOOSE_RELEASE -> R.string.title_choose_release
            TitleAction.RENAME -> R.string.title_rename_files
            TitleAction.ADD_VERSION -> R.string.title_add_version
            TitleAction.SETTINGS -> R.string.title_settings
            TitleAction.KEEP -> R.string.title_keep_up_to_date
            TitleAction.STOP_KEEPING -> R.string.title_stop_keeping
            TitleAction.REFRESH -> R.string.title_refresh_details
            TitleAction.HISTORY -> R.string.title_all_history
            TitleAction.DELETE -> if (show) R.string.title_delete_show else R.string.title_delete_movie
            TitleAction.REPORT -> R.string.title_report
            TitleAction.HIDE -> R.string.title_hide
        },
    )

private fun runAction(
    action: TitleAction,
    state: TitleState,
    viewModel: TitleViewModel,
    links: TitleNav,
) {
    val presenter = viewModel.presenter
    when (action) {
        TitleAction.SEARCH -> viewModel.launch { presenter.searchNow() }
        TitleAction.CHOOSE_RELEASE -> viewModel.openReleases(ReleaseScope(ReleaseScopeKind.WHOLE))
        TitleAction.RENAME -> viewModel.openRename()
        TitleAction.ADD_VERSION -> {
            presenter.selectTab(TitleTab.LIBRARY)
            viewModel.launch { presenter.loadChoices() }
            viewModel.open(TitleSheet.VersionForm(null))
        }
        TitleAction.SETTINGS -> presenter.selectTab(TitleTab.LIBRARY)
        TitleAction.KEEP, TitleAction.STOP_KEEPING -> viewModel.launch { presenter.toggleKeep() }
        TitleAction.REFRESH -> viewModel.launch { presenter.refreshDetails() }
        TitleAction.HISTORY -> viewModel.open(TitleSheet.History)
        TitleAction.DELETE -> viewModel.openRemove()
        TitleAction.REPORT -> links.report(state.ownedId, state.name)
        TitleAction.HIDE -> {
            viewModel.launch { presenter.loadUsers() }
            viewModel.open(TitleSheet.Hide)
        }
    }
}

@Composable
fun SmallPoster(
    image: String,
    name: String,
    width: androidx.compose.ui.unit.Dp,
) {
    ServerImage(
        source = image,
        contentDescription = null,
        size = ImageSize.POSTER,
        placeholderName = name,
        modifier = Modifier.width(width).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)),
    )
}
