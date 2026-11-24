package ch.orellbuehler.acolyte.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.discover.BrowseLink
import ch.orellbuehler.acolyte.feature.discover.CardAction
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.ExploreType
import ch.orellbuehler.acolyte.feature.discover.HideDone
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.CardArt
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.launch

class DiscoverNav(
    val openTitle: (kind: String, tmdb: String) -> Unit,
    val ask: (kind: String, tmdb: String) -> Unit,
    val openPerson: (String) -> Unit,
    val openGenre: (ExploreType, Int) -> Unit,
    val openBrowse: (BrowseLink) -> Unit,
    val openSection: (Section) -> Unit,
    val openHidden: () -> Unit,
    val openStats: () -> Unit,
) {
    fun open(card: DiscoverCard) {
        if (card.opensTitle) openTitle(card.kind.wire, card.tmdb)
    }

    fun act(card: DiscoverCard) {
        when (card.action) {
            CardAction.ADD -> openTitle(card.kind.wire, card.tmdb)
            CardAction.ASK -> ask(card.kind.wire, card.tmdb)
            null -> Unit
        }
    }
}

class CardEvents(
    val open: (DiscoverCard) -> Unit,
    val longPress: (DiscoverCard) -> Unit,
    val act: (DiscoverCard) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardHost(
    nav: DiscoverNav,
    canHide: Boolean,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
    onOpen: (DiscoverCard) -> Unit = {},
    onAct: (DiscoverCard) -> Unit = {},
    content: @Composable (CardEvents) -> Unit,
) {
    val sdk = LocalSdk.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var sheet by remember { mutableStateOf<DiscoverCard?>(null) }
    var hiding by remember { mutableStateOf<DiscoverCard?>(null) }
    val undo = remember { sdk.hidden(-1) }
    val events =
        remember(nav) {
            CardEvents(
                open = { card ->
                    onOpen(card)
                    nav.open(card)
                },
                longPress = { sheet = it },
                act = { card ->
                    onAct(card)
                    nav.act(card)
                },
            )
        }

    fun hidden(done: HideDone) {
        hiding = null
        onChanged()
        val text =
            if (done.everyone) {
                resources.getString(R.string.discover_hide_done_everyone, done.name)
            } else {
                resources.getString(R.string.discover_hide_done_person, done.name, done.person)
            }
        scope.launch {
            val result = snackbar.showSnackbar(text, actionLabel = resources.getString(R.string.discover_hide_undo))
            if (result == SnackbarResult.ActionPerformed && undo.showAgain(done.id)) onChanged()
        }
    }

    Box(modifier.fillMaxSize()) {
        content(events)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    sheet?.let { card ->
        ModalBottomSheet(onDismissRequest = { sheet = null }) {
            CardSheetContent(
                card = card,
                canHide = canHide && card.opensTitle,
                onAct = {
                    sheet = null
                    events.act(card)
                },
                onOpen = {
                    sheet = null
                    events.open(card)
                },
                onHide = {
                    sheet = null
                    hiding = card
                },
            )
        }
    }
    hiding?.let { card ->
        HideFromBrowsingSheet(
            kind = card.kind,
            tmdb = card.tmdb,
            name = card.name,
            genres = card.genres,
            onDismiss = { hiding = null },
            onHidden = ::hidden,
        )
    }
}

@Composable
private fun CardSheetContent(
    card: DiscoverCard,
    canHide: Boolean,
    onAct: () -> Unit,
    onOpen: () -> Unit,
    onHide: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Screen)
            .padding(bottom = Spacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        val mark = card.mark
        CardArt(
            image = card.wideImage,
            name = card.name,
            logo = card.logo,
            mark = mark,
            markLabel = mark?.let { markText(it) }.orEmpty(),
            size = ImageSize.HERO,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Text(card.name, style = MaterialTheme.typography.headlineSmall)
        val facts = listOf(kindText(card.kind), if (card.year > 0) card.year.toString() else "") + card.genres.take(3).map { genreText(it) }
        Text(
            facts.filter { it.isNotEmpty() }.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        contextLine(card)?.let {
            Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        if (card.overview.isNotEmpty()) {
            Text(card.overview, style = MaterialTheme.typography.bodyMedium, maxLines = 8, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            actionText(card)?.let { label ->
                Button(onClick = onAct) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(
                        stringResource(
                            if (card.action ==
                                CardAction.ADD
                            ) {
                                R.string.discover_action_add_short
                            } else {
                                R.string.discover_action_ask_short
                            },
                        ),
                        modifier = Modifier.padding(start = Spacing.XSmall),
                    )
                }
            }
            if (card.opensTitle) {
                FilledTonalButton(onClick = onOpen) {
                    Text(stringResource(R.string.discover_more_info))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = Spacing.XSmall),
                    )
                }
            }
        }
        if (canHide) {
            TextButton(onClick = onHide) {
                Icon(Icons.Rounded.VisibilityOff, contentDescription = null)
                Text(stringResource(R.string.discover_hide_menu), modifier = Modifier.padding(start = Spacing.Small))
            }
        }
    }
}

@Composable
private fun contextLine(card: DiscoverCard): String? {
    val mark = card.mark
    return when {
        mark != null && card.mine -> stringResource(R.string.discover_context_mine)
        mark != null -> markText(mark)
        card.rank > 0 && card.streaming.isNotEmpty() -> stringResource(R.string.discover_context_rank, card.rank, card.streaming)
        card.streaming.isNotEmpty() -> stringResource(R.string.discover_on_service, card.streaming)
        card.rowType in setOf("because_have", "because_asked", "person", "household") && card.rowTitle.isNotEmpty() -> card.rowTitle
        else -> freshText(card.fresh)
    }
}
