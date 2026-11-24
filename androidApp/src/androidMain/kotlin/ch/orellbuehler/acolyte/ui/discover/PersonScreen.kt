package ch.orellbuehler.acolyte.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import ch.orellbuehler.acolyte.feature.discover.DiscoverCard
import ch.orellbuehler.acolyte.feature.discover.FilmRole
import ch.orellbuehler.acolyte.feature.discover.FilmSort
import ch.orellbuehler.acolyte.feature.discover.PersonState
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.ui.LocalSdk
import ch.orellbuehler.acolyte.ui.components.CardShelf
import ch.orellbuehler.acolyte.ui.components.ErrorBanner
import ch.orellbuehler.acolyte.ui.components.MessageView
import ch.orellbuehler.acolyte.ui.components.NoticeBanner
import ch.orellbuehler.acolyte.ui.components.PersonCircle
import ch.orellbuehler.acolyte.ui.components.RankedPoster
import ch.orellbuehler.acolyte.ui.components.ScreenScaffold
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusContent
import ch.orellbuehler.acolyte.ui.theme.Spacing
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class PersonViewModel(
    sdk: AcolyteSdk,
    id: String,
) : ViewModel() {
    private val presenter = sdk.person(id)

    val state: StateFlow<PersonState> = presenter.state

    init {
        viewModelScope.launch { presenter.load() }
    }

    fun refresh() {
        viewModelScope.launch { presenter.refresh() }
    }

    fun select(
        role: FilmRole = state.value.role,
        sort: FilmSort = state.value.sort,
    ) {
        viewModelScope.launch { presenter.select(role, sort) }
    }

    suspend fun refreshOwned() = presenter.refreshOwned()
}

@Composable
fun PersonScreen(
    id: String,
    nav: DiscoverNav,
    canHide: Boolean,
    onBack: () -> Unit,
) {
    val sdk = LocalSdk.current
    val viewModel = viewModel(key = "person-$id") { PersonViewModel(sdk, id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.refreshOwned() } }
    var about by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()

    ScreenScaffold(title = state.name, onBack = onBack) { padding ->
        CardHost(nav, canHide, viewModel::refresh, Modifier.padding(padding)) { events ->
            StatusContent(status = state.status, isEmpty = false, onRefresh = viewModel::refresh) {
                LazyColumn(state = list, contentPadding = PaddingValues(bottom = Spacing.XLarge)) {
                    state.status.error?.let { item(key = "error") { ErrorBanner(it) } }
                    item(key = "head") {
                        PersonHead(
                            state,
                            onFilmography = {
                                scope.launch {
                                    list.animateScrollToItem(
                                        FILMOGRAPHY_INDEX.coerceAtMost(
                                            list.layoutInfo.totalItemsCount - 1,
                                        ),
                                    )
                                }
                            },
                            onAbout = { about = true },
                        )
                    }
                    state.limit?.let { limit ->
                        item(key = "limit") { NoticeBanner(stringResource(R.string.discover_person_limit, limit.used, limit.left)) }
                    }
                    if (state.knownFor.isNotEmpty()) {
                        item(key = "known") {
                            CardShelf(title = stringResource(R.string.discover_person_known_for)) {
                                itemsIndexed(state.knownFor, key = { _, card -> card.key }) { index, card ->
                                    RankedPoster(index + 1) { TitlePosterCard(card, events) }
                                }
                            }
                        }
                    }
                    shelf("coming", R.string.discover_person_coming_soon, state.comingSoon, events)
                    shelf("library", R.string.discover_search_in_library, state.inLibrary, events)
                    shelf("asked", R.string.discover_person_asked, state.asked, events)
                    item(key = "filmography") { FilmographyBar(state, viewModel) }
                    if (state.decades.isEmpty() && !state.reloading) {
                        item(key = "nofilms") {
                            MessageView(icon = Icons.Rounded.Movie, title = stringResource(R.string.discover_person_no_movies))
                        }
                    }
                    items(state.decades, key = { "decade-" + it.year }) { decade ->
                        CardShelf(
                            title =
                                if (decade.year == 0) {
                                    stringResource(R.string.discover_person_coming_soon)
                                } else {
                                    stringResource(R.string.discover_person_decade, decade.year)
                                },
                        ) {
                            items(decade.cards, key = { it.key }) { card -> TitleWideCard(card, events) }
                        }
                    }
                    if (state.about.biography.isNotEmpty()) {
                        item(key = "about") { AboutCard(state, onOpen = { about = true }) }
                    }
                    if (state.oftenWith.size >= OFTEN_MIN) {
                        item(key = "often") {
                            CardShelf(title = stringResource(R.string.discover_person_often)) {
                                items(
                                    state.oftenWith,
                                    key = { it.id },
                                ) { person -> PersonCircle(person, onClick = { nav.openPerson(person.id) }) }
                            }
                        }
                    }
                    if (state.attribution.isNotEmpty()) {
                        item(key = "attribution") {
                            Text(
                                state.attribution,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(Spacing.Screen),
                            )
                        }
                    }
                }
            }
        }
    }
    if (about) AboutSheet(state) { about = false }
}

private const val FILMOGRAPHY_INDEX = 6
private const val OFTEN_MIN = 4

private fun LazyListScope.shelf(
    key: String,
    title: Int,
    cards: List<DiscoverCard>,
    events: CardEvents,
) {
    if (cards.isEmpty()) return
    item(key = key) {
        CardShelf(title = stringResource(title)) {
            items(cards, key = { it.key }) { card -> TitleWideCard(card, events) }
        }
    }
}

@Composable
private fun roleLine(state: PersonState): String {
    val roles =
        state.roles.map { role ->
            when (role) {
                "Actor" -> stringResource(R.string.discover_role_actor)
                "Director" -> stringResource(R.string.discover_role_director)
                else -> role
            }
        }
    val since = if (state.firstYear > 0) stringResource(R.string.discover_person_since, state.firstYear) else ""
    return (roles + since).filter { it.isNotEmpty() }.joinToString(" · ")
}

@Composable
private fun PersonHead(
    state: PersonState,
    onFilmography: () -> Unit,
    onAbout: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(Spacing.Screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        ServerImage(
            source = state.photo,
            contentDescription = null,
            size = ImageSize.POSTER,
            placeholderName = state.name,
            modifier = Modifier.size(128.dp).clip(CircleShape),
        )
        Text(state.name, style = MaterialTheme.typography.headlineMedium)
        Text(roleLine(state), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Button(onClick = onFilmography) { Text(stringResource(R.string.discover_person_filmography)) }
            if (state.about.biography.isNotEmpty() || state.about.birthday.isNotEmpty()) {
                FilledTonalButton(onClick = onAbout) { Text(stringResource(R.string.discover_person_about)) }
            }
        }
    }
}

@Composable
private fun FilmographyBar(
    state: PersonState,
    viewModel: PersonViewModel,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        SectionHeader(stringResource(R.string.discover_person_filmography))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.Screen),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                FilmRole.entries.forEachIndexed { index, role ->
                    SegmentedButton(
                        selected = role == state.role,
                        onClick = { viewModel.select(role = role) },
                        shape = SegmentedButtonDefaults.itemShape(index, FilmRole.entries.size),
                        label = { Text(roleText(role), maxLines = 1) },
                    )
                }
            }
            Column {
                OutlinedButton(onClick = { open = true }, contentPadding = PaddingValues(horizontal = Spacing.Medium)) {
                    Text(sortText(state.sort), maxLines = 1)
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = stringResource(R.string.discover_person_sort))
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    FilmSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sortText(sort)) },
                            onClick = {
                                open = false
                                viewModel.select(sort = sort)
                            },
                        )
                    }
                }
            }
        }
        if (state.reloading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(Spacing.Screen))
    }
}

@Composable
private fun roleText(role: FilmRole): String =
    stringResource(
        when (role) {
            FilmRole.ALL -> R.string.discover_type_all
            FilmRole.ACTOR -> R.string.discover_role_actor
            FilmRole.DIRECTOR -> R.string.discover_role_director
        },
    )

@Composable
private fun sortText(sort: FilmSort): String =
    stringResource(
        when (sort) {
            FilmSort.OLDEST -> R.string.discover_sort_oldest
            FilmSort.NEWEST -> R.string.discover_sort_newest
            FilmSort.POPULAR -> R.string.discover_sort_popular
        },
    )

@Composable
private fun AboutCard(
    state: PersonState,
    onOpen: () -> Unit,
) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(Spacing.Screen),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(stringResource(R.string.discover_person_about), style = MaterialTheme.typography.titleLarge)
            if (state.films > 0) {
                Text(
                    pluralStringResource(R.plurals.discover_person_films, state.films, state.films),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(state.about.biography, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.discover_person_more_about, state.name.substringBefore(' ')),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSheet(
    state: PersonState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.Screen)
                .padding(bottom = Spacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(stringResource(R.string.discover_person_about_name, state.name), style = MaterialTheme.typography.titleLarge)
            val born = date(state.about.birthday)
            val died = date(state.about.deathday)
            if (born != null) {
                val end = died ?: LocalDate.now()
                val age = Period.between(born, end).years
                Fact(
                    stringResource(R.string.discover_person_born),
                    format(born) + " · " +
                        if (died == null) {
                            stringResource(R.string.discover_person_age, age)
                        } else {
                            stringResource(R.string.discover_person_aged, age)
                        },
                )
            }
            if (state.about.birthplace.isNotEmpty()) Fact(stringResource(R.string.discover_person_from), state.about.birthplace)
            if (died != null) Fact(stringResource(R.string.discover_person_died), format(died))
            if (state.about.department.isNotEmpty()) Fact(stringResource(R.string.discover_person_known_for), state.about.department)
            if (state.about.biography.isNotEmpty()) {
                Text(stringResource(R.string.discover_person_story), style = MaterialTheme.typography.titleMedium)
                state.about.biography.split("\n").filter { it.isNotBlank() }.forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun Fact(
    label: String,
    value: String,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun date(text: String): LocalDate? = runCatching { LocalDate.parse(text.take(10)) }.getOrNull()

private fun format(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
