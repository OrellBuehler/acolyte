package ch.orellbuehler.acolyte.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.orellbuehler.acolyte.AcolyteSdk
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.discover.CardKind
import ch.orellbuehler.acolyte.feature.home.ArrivedTitle
import ch.orellbuehler.acolyte.feature.home.ComingUp
import ch.orellbuehler.acolyte.feature.home.GreetingTime
import ch.orellbuehler.acolyte.feature.home.HomeBoard
import ch.orellbuehler.acolyte.feature.home.HomePicture
import ch.orellbuehler.acolyte.feature.home.HomeState
import ch.orellbuehler.acolyte.feature.home.HomeTile
import ch.orellbuehler.acolyte.feature.home.JoinedWelcome
import ch.orellbuehler.acolyte.feature.home.PictureLabel
import ch.orellbuehler.acolyte.feature.home.UpWhen
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.discover.CardEvents
import ch.orellbuehler.acolyte.ui.discover.CardHost
import ch.orellbuehler.acolyte.ui.discover.CardRowView
import ch.orellbuehler.acolyte.ui.discover.DiscoverNav
import ch.orellbuehler.acolyte.ui.discover.serverWords
import ch.orellbuehler.acolyte.ui.discover.shortDate
import ch.orellbuehler.acolyte.ui.theme.Radius
import ch.orellbuehler.acolyte.ui.theme.Spacing
import ch.orellbuehler.acolyte.ui.theme.acolyte
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.TimeZone

class HomeViewModel(
    sdk: AcolyteSdk,
) : ViewModel() {
    private val presenter = sdk.home(TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000)

    val state: StateFlow<HomeState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun show(board: HomeBoard) = presenter.show(board)

    fun dismissWelcome() {
        viewModelScope.launch { presenter.dismissWelcome() }
    }

    suspend fun watch() = presenter.watch()
}

@Composable
fun HomeScreen(
    nav: DiscoverNav,
    onBack: (() -> Unit)?,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel { HomeViewModel(sdk) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() } }
    ScreenScaffold(
        title = stringResource(R.string.section_home),
        onBack = onBack,
        actions = {
            IconButton(onClick = { nav.openSection(Section.SEARCH) }) {
                Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.section_search))
            }
            IconButton(onClick = { nav.openSection(Section.EXPLORE) }) {
                Icon(Icons.Rounded.Explore, contentDescription = stringResource(R.string.section_explore))
            }
        },
    ) { padding ->
        CardHost(nav, false, viewModel::refresh, Modifier.padding(padding)) { events ->
            StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                LazyColumn(contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                    item(key = "greeting") { Greeting(state) }
                    if (state.system) {
                        item(key = "board") {
                            SingleChoiceSegmentedButtonRow(
                                Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
                            ) {
                                HomeBoard.entries.forEachIndexed { index, board ->
                                    SegmentedButton(
                                        selected = state.board == board,
                                        onClick = { viewModel.show(board) },
                                        shape = SegmentedButtonDefaults.itemShape(index, HomeBoard.entries.size),
                                        label = {
                                            Text(
                                                stringResource(
                                                    if (board ==
                                                        HomeBoard.HOME
                                                    ) {
                                                        R.string.home_board_home
                                                    } else {
                                                        R.string.home_board_system
                                                    },
                                                ),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                    state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
                    if (state.board == HomeBoard.SYSTEM && state.systemBoard != null) {
                        systemBoard(state, nav)
                    } else {
                        homeBoard(state, nav, events, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting(state: HomeState) {
    Column(Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small)) {
        val word =
            stringResource(
                when (state.greeting) {
                    GreetingTime.MORNING -> R.string.home_good_morning
                    GreetingTime.AFTERNOON -> R.string.home_good_afternoon
                    GreetingTime.EVENING -> R.string.home_good_evening
                },
            )
        Text(
            if (state.name.isEmpty()) word else stringResource(R.string.home_greeting_name, word, state.name),
            style = MaterialTheme.typography.headlineMedium,
        )
        newsText(state)?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

private fun LazyListScope.homeBoard(
    state: HomeState,
    nav: DiscoverNav,
    events: CardEvents,
    viewModel: HomeViewModel,
) {
    if (state.finishSetup) {
        item(key = "finish") {
            LinkCard(
                title = stringResource(R.string.home_finish_setup),
                body = stringResource(R.string.home_finish_setup_body),
                onClick = { nav.openSection(Section.SETTINGS) },
            )
        }
    }
    state.joined?.let { joined -> item(key = "joined") { JoinedWelcomeCard(joined, viewModel::dismissWelcome, nav) } }
    if (state.welcome) item(key = "welcome") { WelcomeCard(state, nav) }
    item(key = "tiles") { TileGrid(state, nav) }
    state.arrived?.let { row ->
        item(key = "arrived") {
            CardRowView(
                row,
                events,
                title = arrivedTitle(state),
                onSeeAll = { row.cards.firstOrNull()?.let { nav.openSection(kindSection(it.kind)) } },
            )
        }
    }
    if (state.comingUp.isNotEmpty()) {
        item(key = "coming") {
            SectionHeader(
                stringResource(R.string.home_coming_up),
                actionLabel = stringResource(R.string.section_calendar),
                onAction = if (state.system) ({ nav.openSection(Section.CALENDAR) }) else null,
            )
        }
        items(state.comingUp, key = { "up-" + it.key }) { up -> ComingUpLine(up, nav) }
    }
    items(state.borrowed, key = { "borrowed-" + it.id }) { row ->
        CardRowView(row, events, onSeeAll = { nav.openSection(Section.EXPLORE) })
    }
}

@Composable
private fun arrivedTitle(state: HomeState): String =
    stringResource(
        when (state.arrivedTitle) {
            ArrivedTitle.NEW_TODAY -> R.string.home_new_today
            ArrivedTitle.NEW_THIS_WEEK -> R.string.home_new_this_week
            ArrivedTitle.JUST_ARRIVED -> R.string.home_just_arrived
            ArrivedTitle.RECENTLY_ADDED -> R.string.home_recently_added
        },
    )

fun kindSection(kind: CardKind): Section =
    when (kind) {
        CardKind.MOVIE -> Section.MOVIES
        CardKind.SHOW -> Section.SHOWS
        CardKind.ARTIST -> Section.MUSIC
    }

@Composable
private fun LinkCard(
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JoinedWelcomeCard(
    joined: JoinedWelcome,
    onDismiss: () -> Unit,
    nav: DiscoverNav,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(
                if (joined.name.isEmpty()) {
                    stringResource(
                        R.string.home_welcome_title,
                    )
                } else {
                    stringResource(R.string.home_joined_title, joined.name)
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(R.string.home_joined_find), style = MaterialTheme.typography.bodyMedium)
            if (joined.canAsk) {
                Text(stringResource(R.string.home_joined_ask), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    joined.kinds.forEach { kind ->
                        StatusChip(kindsText(kind), Tone.IDLE)
                    }
                }
                Text(stringResource(R.string.home_joined_follow), style = MaterialTheme.typography.bodyMedium)
                if (joined.noLimit || joined.limits.isEmpty()) {
                    Text(stringResource(R.string.home_joined_no_limit), style = MaterialTheme.typography.bodySmall)
                } else {
                    joined.limits.forEach { limit ->
                        Text(
                            if (limit.noLimit) {
                                stringResource(R.string.home_limit_none, kindsText(limit.kind))
                            } else {
                                stringResource(R.string.home_limit_line, kindsText(limit.kind), limit.perWeek, limit.left)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Button(onClick = onDismiss) { Text(stringResource(R.string.home_joined_got_it)) }
                OutlinedButton(onClick = { nav.openSection(Section.EXPLORE) }) { Text(stringResource(R.string.home_joined_open_explore)) }
            }
        }
    }
}

@Composable
private fun kindsText(kind: CardKind): String =
    stringResource(
        when (kind) {
            CardKind.MOVIE -> R.string.section_movies
            CardKind.SHOW -> R.string.section_shows
            CardKind.ARTIST -> R.string.section_music
        },
    )

@Composable
private fun WelcomeCard(
    state: HomeState,
    nav: DiscoverNav,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            if (state.owner && state.steps.isNotEmpty()) {
                Text(
                    stringResource(R.string.home_setup_done, state.stepsDone, state.steps.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.home_welcome_title), style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(progress = { state.stepsDone / state.steps.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                state.steps.forEachIndexed { index, step ->
                    TextButton(onClick = { nav.openSection(step.target) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                "${index + 1}. ${stepTitle(step.kind)}" + if (step.done) " ✓" else "",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                stepDetail(step.kind),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                state.nextStep?.let { next ->
                    Button(onClick = { nav.openSection(next.target) }) { Text(stepAction(next.kind)) }
                }
            } else {
                Text(stringResource(R.string.home_welcome_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.home_welcome_body), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { nav.openSection(Section.EXPLORE) }) { Text(stringResource(R.string.home_welcome_start)) }
            }
        }
    }
}

@Composable
private fun TileGrid(
    state: HomeState,
    nav: DiscoverNav,
) {
    Column(
        Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        state.pictures.firstOrNull()?.let { PictureTile(it, nav, Modifier.fillMaxWidth().aspectRatio(16f / 9f)) }
        val stats = state.tiles
        val rest = state.pictures.drop(1)
        stats.chunked(2).forEachIndexed { index, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                pair.forEach { tile -> StatTile(tile, state, nav, Modifier.weight(1f)) }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
            rest.getOrNull(index)?.let { PictureTile(it, nav, Modifier.fillMaxWidth().aspectRatio(2f)) }
        }
        rest.drop(stats.chunked(2).size).forEach { PictureTile(it, nav, Modifier.fillMaxWidth().aspectRatio(2f)) }
    }
}

@Composable
private fun PictureTile(
    picture: HomePicture,
    nav: DiscoverNav,
    modifier: Modifier,
) {
    val card = picture.card
    val colors = MaterialTheme.acolyte
    Card(onClick = { nav.open(card) }, modifier = modifier, shape = RoundedCornerShape(Radius.Panel)) {
        Box(Modifier.fillMaxSize().background(colors.placeholder)) {
            ServerImage(
                source = card.backdrop ?: card.poster,
                contentDescription = null,
                size = ImageSize.HERO,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to colors.artScrimStrong)))
            Text(
                pictureLabel(picture),
                color = colors.onArt,
                style = MaterialTheme.typography.labelMedium,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.Medium)
                        .background(colors.artScrim, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(Spacing.Large)) {
                if (card.logo != null) {
                    ServerImage(
                        source = card.logo,
                        contentDescription = card.name,
                        size = ImageSize.POSTER,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(0.5f).height(44.dp),
                    )
                } else {
                    Text(
                        card.name,
                        color = colors.onArt,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (card.subtitle.isNotEmpty()) {
                    Text(serverWords(card.subtitle), color = colors.onArt.copy(alpha = 0.84f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun pictureLabel(picture: HomePicture): String =
    when (picture.label) {
        PictureLabel.NEW_SEASON -> stringResource(R.string.home_pill_new_season)
        PictureLabel.COMING_SOON -> stringResource(R.string.home_pill_coming_soon)
        PictureLabel.FOLLOWING -> stringResource(R.string.home_pill_following)
        PictureLabel.ARRIVED ->
            stringResource(
                when (picture.arrived) {
                    ArrivedTitle.NEW_TODAY -> R.string.home_new_today
                    ArrivedTitle.NEW_THIS_WEEK -> R.string.home_new_this_week
                    ArrivedTitle.JUST_ARRIVED -> R.string.home_just_arrived
                    ArrivedTitle.RECENTLY_ADDED -> R.string.home_recently_added
                },
            )
    }

@Composable
private fun StatTile(
    tile: HomeTile,
    state: HomeState,
    nav: DiscoverNav,
    modifier: Modifier,
) {
    val content = tileContent(tile, state)
    val target = tile.target
    Card(
        onClick = { if (target != null) nav.openSection(target) },
        enabled = target != null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.heightIn(min = 132.dp),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Icon(
                    content.icon,
                    contentDescription = null,
                    tint = content.tint ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(content.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                content.big,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = content.tint ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                content.small,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (content.posters.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                    content.posters.forEach { poster ->
                        ServerImage(
                            source = poster,
                            contentDescription = null,
                            size = ImageSize.THUMB,
                            modifier = Modifier.width(28.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(4.dp)),
                        )
                    }
                }
            }
        }
    }
}

class TileContent(
    val icon: ImageVector,
    val label: String,
    val big: String,
    val small: String,
    val tint: Color? = null,
    val posters: List<String> = emptyList(),
)

@Composable
private fun ComingUpLine(
    up: ComingUp,
    nav: DiscoverNav,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = up.tmdb.isNotEmpty()) { nav.openTitle(up.kind, up.tmdb) }
            .padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        ServerImage(
            source = up.image,
            contentDescription = null,
            size = ImageSize.BACKDROP,
            placeholderName = up.name,
            modifier = Modifier.width(112.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)),
        )
        Column(Modifier.weight(1f)) {
            Text(up.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (up.sub.isNotEmpty()) {
                Text(up.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        val soon = up.whenKind == UpWhen.OUT_NOW || up.whenKind == UpWhen.TODAY
        Text(
            when (up.whenKind) {
                UpWhen.OUT_NOW -> stringResource(R.string.home_out_now)
                UpWhen.TODAY -> stringResource(R.string.discover_day_today)
                UpWhen.DATE -> runCatching { shortDate(LocalDate.parse(up.date)) }.getOrDefault(up.date)
                UpWhen.TEXT -> up.text
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (soon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
