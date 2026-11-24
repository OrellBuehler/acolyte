package ch.orellbuehler.acolyte.ui.title

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.TitleCard
import ch.orellbuehler.acolyte.api.model.TitlePerson
import ch.orellbuehler.acolyte.api.model.TitleVideo
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.title.FactKind
import ch.orellbuehler.acolyte.feature.title.TitleFact
import ch.orellbuehler.acolyte.feature.title.TitleLogic
import ch.orellbuehler.acolyte.feature.title.TitleState
import ch.orellbuehler.acolyte.image.ImageSize
import ch.orellbuehler.acolyte.ui.components.PosterCard
import ch.orellbuehler.acolyte.ui.components.SectionHeader
import ch.orellbuehler.acolyte.ui.components.ServerImage
import ch.orellbuehler.acolyte.ui.components.StatusChip
import ch.orellbuehler.acolyte.ui.theme.Spacing

fun LazyListScope.overviewContent(
    state: TitleState,
    viewModel: TitleViewModel,
    links: TitleNav,
) {
    item(key = "story") { Story(state, links) }
    if (state.trailers.isNotEmpty()) {
        item(key = "trailers") {
            Shelf(stringResource(R.string.title_trailers)) {
                items(state.trailers, key = { it.key }) { video -> TrailerCard(video, state, viewModel, links) }
            }
        }
    }
    if (state.isShow &&
        state.hasTmdb &&
        state.details
            ?.seasons
            .orEmpty()
            .isNotEmpty()
    ) {
        item(key = "seasons") { SeasonBrowser(viewModel, state.backdrop) }
    }
    if (state.cast.isNotEmpty()) {
        item(key = "cast") {
            Shelf(stringResource(R.string.title_cast), onSeeAll = { viewModel.open(TitleSheet.Cast) }) {
                items(
                    state.cast.take(12),
                    key = { it.name + it.role + it.character },
                ) { person -> PersonCircle(person) { links.person(person.id) } }
            }
        }
    }
    if (state.legacy && state.credits.isNotEmpty()) {
        item(key = "credits") {
            Shelf(stringResource(R.string.title_cast)) {
                items(state.credits.take(12), key = { it.person.id + it.person.name }) { credit ->
                    PersonCircle(
                        TitlePerson(
                            name = credit.person.name,
                            character = credit.characters.firstOrNull() ?: credit.roles.firstOrNull().orEmpty(),
                            profile = credit.person.profile,
                            id = credit.person.id,
                        ),
                    ) { links.person(credit.person.id) }
                }
            }
        }
    }
    if (state.hasTmdb) item(key = "reviews") { ReviewsShelf(viewModel) }
    if (state.collectionParts.isNotEmpty()) {
        item(key = "collection") {
            val requestable = state.collectionParts.count { it.titleId == 0L && !it.inLibrary && (state.access.manage || it.ask == null) }
            val canBatch = requestable >= 2 && (state.access.manage || state.access.mayAsk)
            Shelf(
                state.collectionName,
                actionLabel =
                    if (canBatch) {
                        stringResource(if (state.access.manage) R.string.title_add_all else R.string.title_request_all)
                    } else {
                        stringResource(R.string.common_see_all)
                    },
                onSeeAll = {
                    if (canBatch) {
                        viewModel.openAddAll()
                    } else {
                        links.collection(
                            state.details
                                ?.collection
                                ?.id
                                .orEmpty(),
                        )
                    }
                },
            ) {
                items(state.collectionParts, key = { "c" + it.tmdbId }) { card -> CardItem(card, state, links) }
            }
        }
    }
    cardShelf("similar", { stringResource(R.string.title_more_like_this) }, state.similar, state, viewModel, links)
    if (state.moreFromItems.isNotEmpty()) {
        cardShelf(
            "more",
            {
                if (state.moreFrom.person.isNotBlank()) {
                    stringResource(R.string.title_more_from, state.moreFrom.person)
                } else {
                    stringResource(R.string.title_more_from_them)
                }
            },
            state.moreFromItems,
            state,
            viewModel,
            links,
        )
    }
    state.leadPerson?.let { lead ->
        cardShelf("lead", { stringResource(R.string.title_more_with, lead.name) }, state.leadItems, state, viewModel, links)
    }
    item(key = "about") { AboutBand(state, links) }
}

private fun LazyListScope.cardShelf(
    key: String,
    title: @Composable () -> String,
    cards: List<TitleCard>,
    state: TitleState,
    viewModel: TitleViewModel,
    links: TitleNav,
) {
    if (cards.isEmpty()) return
    item(key = key) {
        val text = title()
        Shelf(text, onSeeAll = if (cards.size > 6) ({ viewModel.open(TitleSheet.Cards(text, cards)) }) else null) {
            items(cards.take(12), key = { key + it.tmdbId }) { card -> CardItem(card, state, links) }
        }
    }
}

@Composable
fun Shelf(
    title: String,
    actionLabel: String? = null,
    onSeeAll: (() -> Unit)? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(Modifier.padding(top = Spacing.Large)) {
        SectionHeader(title, actionLabel = actionLabel, onAction = onSeeAll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.Screen),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            content = content,
        )
    }
}

@Composable
fun CardItem(
    card: TitleCard,
    state: TitleState,
    links: TitleNav,
) {
    val subtitle =
        when {
            card.inLibrary || card.titleId > 0 ->
                stringResource(
                    if (state.access.manage) R.string.title_card_in_library else R.string.title_card_ready,
                )
            card.ask != null -> stringResource(R.string.title_card_requested)
            card.year > 0 -> card.year.toString()
            else -> null
        }
    PosterCard(title = card.name, image = card.poster, subtitle = subtitle, onClick = { links.card(card) })
}

@Composable
fun PersonCircle(
    person: TitlePerson,
    onClick: () -> Unit,
) {
    Column(
        Modifier.width(96.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = person.id.isNotBlank(), onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ServerImage(
            source = person.profile,
            contentDescription = null,
            size = ImageSize.THUMB,
            placeholderName = person.name,
            modifier = Modifier.size(84.dp).clip(CircleShape),
        )
        Text(
            person.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.XSmall),
        )
        val role = person.character.ifBlank { roleText(person.role) }
        if (role.isNotBlank()) {
            Text(
                role,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun roleText(role: String): String =
    when (role) {
        "director" -> stringResource(R.string.title_role_director)
        "creator" -> stringResource(R.string.title_role_creator)
        "actor", "" -> ""
        else -> role.replaceFirstChar { it.uppercase() }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Story(
    state: TitleState,
    links: TitleNav,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        if (state.overview.isNotBlank()) {
            Text(
                state.overview,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (expanded) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { expanded = !expanded },
            )
        }
        val d = state.details
        if (state.directors.isNotEmpty()) {
            CreditLine(stringResource(if (state.isShow) R.string.title_created_by else R.string.title_directed_by), state.directors, links)
        }
        if (state.starring.isNotEmpty()) CreditLine(stringResource(R.string.title_starring), state.starring, links)
        if (state.genres.isNotEmpty()) InfoLine(stringResource(R.string.title_genres), state.genres.take(3).joinToString(", "))
        if (d != null && d.contentNotes.isNotEmpty()) InfoLine(stringResource(R.string.title_contains), d.contentNotes.joinToString(", "))
        if (d != null && !state.isShow && d.studios.isNotEmpty()) InfoLine(stringResource(R.string.title_studio), d.studios.first())
        state.owned
            ?.show
            ?.network
            ?.takeIf { it.isNotBlank() }
            ?.let { InfoLine(stringResource(R.string.title_network), it) }
        val keywords = d?.keywords.orEmpty().take(5)
        if (keywords.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                keywords.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
            }
        }
    }
}

@Composable
private fun CreditLine(
    label: String,
    people: List<TitlePerson>,
    links: TitleNav,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Column {
            people.forEach { person ->
                Text(
                    person.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (person.id.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable(enabled = person.id.isNotBlank()) { links.person(person.id) },
                )
            }
        }
    }
}

@Composable
fun InfoLine(
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TrailerCard(
    video: TitleVideo,
    state: TitleState,
    viewModel: TitleViewModel,
    links: TitleNav,
) {
    val main = video === state.trailer
    Column(
        Modifier.width(240.dp).clip(RoundedCornerShape(12.dp)).clickable {
            if (main) viewModel.playTrailer() else links.url("https://www.youtube.com/watch?v=${video.key}")
        },
    ) {
        ServerImage(
            source = "https://i.ytimg.com/vi/${video.key}/mqdefault.jpg",
            contentDescription = null,
            placeholderName = video.name.ifBlank { state.name },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
        )
        Text(
            video.name.ifBlank { stringResource(R.string.title_trailer_of, state.name) },
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.Small),
        )
        val type = if (video.type == "Teaser") stringResource(R.string.title_teaser) else stringResource(R.string.title_trailer)
        val lang =
            video.language
                .takeIf { it.isNotBlank() && it != "en" }
                ?.let { " · " + languageName(it, locale()) }
                .orEmpty()
        Text(type + lang, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SeasonBrowser(
    viewModel: TitleViewModel,
    backdrop: String,
) {
    val presenter = viewModel.seasonBrowser
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(presenter) { presenter.load() }
    Column(Modifier.padding(top = Spacing.Large)) {
        SectionHeader(stringResource(R.string.title_seasons))
        LazyRow(contentPadding = PaddingValues(horizontal = Spacing.Screen), horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            items(state.ordered, key = { it.number }) { season ->
                FilterChip(
                    selected = season.number == state.selected,
                    onClick = { viewModel.launch { presenter.select(season.number) } },
                    label = { Text(seasonName(season.number) + if (season.number == state.newSeason) " •" else "") },
                )
            }
        }
        state.current?.let { season ->
            val parts = mutableListOf<String>()
            if (season.episodeCount >
                0
            ) {
                parts += pluralStringResource(R.plurals.title_episodes_count, season.episodeCount, season.episodeCount)
            }
            if (season.year > 0) parts += season.year.toString()
            if (parts.isNotEmpty()) {
                Text(
                    parts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.Screen, vertical = Spacing.Small),
                )
            }
        }
        when {
            state.failed ->
                Text(
                    stringResource(R.string.title_season_failed, seasonName(state.selected)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = Spacing.Screen),
                )
            state.loading && state.shown.isEmpty() -> Box(Modifier.height(120.dp))
            else ->
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.Screen),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    items(state.shown, key = { it.number }) { episode ->
                        Column(Modifier.width(240.dp)) {
                            ServerImage(
                                source = episode.still.ifBlank { backdrop },
                                contentDescription = null,
                                size = ImageSize.BACKDROP,
                                placeholderName = episode.name.ifBlank { episode.number.toString() },
                                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
                            )
                            Text(
                                "${episode.number}. " +
                                    episode.name.ifBlank { stringResource(R.string.title_episode_number, episode.number) },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = Spacing.Small),
                            )
                            val line = mutableListOf<String>()
                            runtimeText(episode.runtime).takeIf { it.isNotBlank() }?.let { line += it }
                            line +=
                                if (episode.airDay.isBlank()) {
                                    stringResource(
                                        R.string.title_no_air_date,
                                    )
                                } else {
                                    formatDate(episode.airDay, locale())
                                }
                            state.absolute(episode.number).takeIf { it > 0 }?.let { line += stringResource(R.string.title_absolute, it) }
                            Text(
                                line.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (episode.overview.isNotBlank()) {
                                Text(
                                    episode.overview,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun ReviewsShelf(viewModel: TitleViewModel) {
    val presenter = viewModel.reviews
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(presenter) { presenter.load() }
    if (!state.visible || state.status.phase == Phase.LOADING && state.reviews.isEmpty()) return
    Column(Modifier.padding(top = Spacing.Large)) {
        SectionHeader(stringResource(R.string.title_reviews))
        val data = state.data
        val summary = mutableListOf<String>()
        if (data.score > 0) summary += "★ " + TitleLogic.oneDecimal(data.score) + " /10"
        if (data.votes > 0) summary += pluralStringResource(R.plurals.title_ratings_count, data.votes, data.votes)
        if (data.watching > 0) summary += stringResource(R.string.title_watching_now, data.watching)
        Text(
            if (summary.isEmpty()) stringResource(R.string.title_no_score) else summary.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.Screen),
        )
        if (data.sources.size > 1 || state.canHelpful) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.Screen, vertical = Spacing.Small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                if (data.sources.size > 1) {
                    items(data.sources, key = { "s" + it.id }) { source ->
                        FilterChip(
                            selected = state.source == source.id,
                            onClick = { viewModel.launch { presenter.setSource(source.id) } },
                            label = { Text(source.name) },
                        )
                    }
                }
                if (state.canHelpful) {
                    item(key = "newest") {
                        FilterChip(
                            selected = state.order == "newest",
                            onClick = { viewModel.launch { presenter.setOrder("newest") } },
                            label = { Text(stringResource(R.string.title_reviews_newest)) },
                        )
                    }
                    item(key = "helpful") {
                        FilterChip(
                            selected = state.order == "helpful",
                            onClick = { viewModel.launch { presenter.setOrder("helpful") } },
                            label = { Text(stringResource(R.string.title_reviews_helpful)) },
                        )
                    }
                }
            }
        }
        if (state.reviews.isEmpty()) {
            Text(
                if (state.status.error !=
                    null
                ) {
                    stringResource(R.string.title_reviews_failed)
                } else {
                    stringResource(R.string.title_reviews_empty)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.Screen),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.Screen),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                items(state.reviews.size, key = { it }) { index ->
                    val review = state.reviews[index]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.width(300.dp).clickable { viewModel.open(TitleSheet.Review(review)) },
                    ) {
                        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
                            Text(
                                review.author.ifBlank { stringResource(R.string.title_review_someone) },
                                style = MaterialTheme.typography.titleSmall,
                            )
                            val meta = mutableListOf<String>()
                            if (review.date.isNotBlank()) meta += formatDate(review.date, locale())
                            if (review.rating > 0) meta += "★ " + TitleLogic.oneDecimal(review.rating)
                            if (meta.isNotEmpty()) {
                                Text(
                                    meta.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                if (review.spoiler) stringResource(R.string.title_review_spoiler) else review.text,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (review.likes > 0) {
                                Text(
                                    pluralStringResource(R.plurals.title_review_likes, review.likes, review.likes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (state.hasMore) {
                    item(key = "more") {
                        TextButton(onClick = { viewModel.launch { presenter.more() } }, enabled = !state.loadingMore) {
                            Text(stringResource(R.string.title_reviews_more))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutBand(
    state: TitleState,
    links: TitleNav,
) {
    val loc = locale()
    Column(
        Modifier.padding(top = Spacing.XLarge).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        SectionHeader(stringResource(R.string.title_about))
        Column(Modifier.padding(horizontal = Spacing.Screen), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            AboutCard {
                val status = state.libraryStatus
                if (status != null) {
                    StatusChip(libraryLabel(status), status.tone)
                    if (state.ownBadges.isNotEmpty()) InfoLine(stringResource(R.string.title_quality), state.ownBadges.joinToString(" · "))
                    state.mainFile?.let { InfoLine(stringResource(R.string.title_size), formatBytes(it.size, loc)) }
                    state.owned?.addedAt?.takeIf { it.isNotBlank() }?.let {
                        InfoLine(
                            stringResource(R.string.title_added),
                            formatDate(it, loc),
                        )
                    }
                    InfoLine(
                        stringResource(R.string.title_kept_up_to_date),
                        stringResource(if (state.owned?.keepUpToDate == true) R.string.title_yes else R.string.title_no),
                    )
                } else {
                    Text(state.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.genres.take(2).joinToString(", ").ifBlank {
                            stringResource(if (state.isShow) R.string.title_kind_show else R.string.title_kind_movie)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.overview.isNotBlank()) {
                        Text(
                            state.overview,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (state.scores.isNotEmpty() || state.tmdbScore > 0) {
                AboutCard {
                    Text(stringResource(R.string.title_ratings), style = MaterialTheme.typography.titleMedium)
                    if (state.tmdbScore > 0) InfoLine("TMDB", TitleLogic.oneDecimal(state.tmdbScore))
                    state.scores.forEach { InfoLine(scoreName(it.source), TitleLogic.scoreText(it)) }
                }
            }
            if (state.wiki.awards.isNotEmpty()) {
                AboutCard {
                    Text(
                        state.awardBadges.firstOrNull() ?: pluralStringResource(R.plurals.title_wins, state.wins, state.wins),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    InfoLine(stringResource(R.string.title_award_wins), state.wins.toString())
                    InfoLine(stringResource(R.string.title_award_nominations), state.nominations.toString())
                }
            }
            if (state.facts.isNotEmpty()) {
                AboutCard {
                    Text(stringResource(R.string.title_information), style = MaterialTheme.typography.titleMedium)
                    state.facts.forEach { fact -> InfoLine(factLabel(fact.kind), factValue(fact)) }
                }
            }
            if (state.links.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    state.links.forEach { link -> AssistChip(onClick = { links.url(link.url) }, label = { Text(link.name) }) }
                }
            }
        }
    }
}

@Composable
private fun AboutCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) { content() }
    }
}

@Composable
private fun factLabel(kind: FactKind): String =
    stringResource(
        when (kind) {
            FactKind.NETWORK -> R.string.title_network
            FactKind.STUDIO -> R.string.title_studio
            FactKind.RELEASED, FactKind.RELEASED_YEAR -> R.string.title_fact_released
            FactKind.AIRED -> R.string.title_fact_aired
            FactKind.RUNTIME -> R.string.title_fact_runtime
            FactKind.SEASONS -> R.string.title_seasons
            FactKind.RATED -> R.string.title_fact_rated
            FactKind.STATUS -> R.string.title_fact_status
            FactKind.ORIGIN -> R.string.title_fact_origin
            FactKind.LANGUAGE -> R.string.title_fact_language
            FactKind.SPOKEN -> R.string.title_fact_spoken
            FactKind.ORIGINAL_TITLE -> R.string.title_fact_original_title
            FactKind.ALSO_KNOWN -> R.string.title_fact_also_known
            FactKind.DIGITAL -> R.string.title_fact_digital
            FactKind.DISC -> R.string.title_fact_disc
            FactKind.NEXT_EPISODE -> R.string.title_fact_next_episode
            FactKind.BUDGET -> R.string.title_fact_budget
            FactKind.REVENUE -> R.string.title_fact_revenue
        },
    )

@Composable
private fun factValue(fact: TitleFact): String {
    val loc = locale()
    return when (fact.kind) {
        FactKind.RELEASED, FactKind.DIGITAL, FactKind.DISC -> formatDate(fact.text, loc)
        FactKind.AIRED ->
            if (fact.end.isNotBlank()) {
                "${fact.text}–${fact.end}"
            } else {
                stringResource(
                    R.string.title_fact_aired_now,
                    fact.text,
                )
            }
        FactKind.RUNTIME -> runtimeText(fact.number.toInt())
        FactKind.SEASONS ->
            pluralStringResource(R.plurals.title_seasons_count, fact.number.toInt(), fact.number.toInt()) + " · " +
                pluralStringResource(R.plurals.title_episodes_count, fact.episode, fact.episode)
        FactKind.ORIGIN -> fact.text.split(',').joinToString(", ") { runCatching { countryName(it, loc) }.getOrDefault(it) }
        FactKind.LANGUAGE -> languageName(fact.text, loc)
        FactKind.STATUS -> fact.text.lowercase().replaceFirstChar { it.uppercase() }
        FactKind.NEXT_EPISODE ->
            listOf(formatDate(fact.text, loc).takeIf { fact.text.isNotBlank() }, TitleLogic.episodeCode(fact.season, fact.episode))
                .filterNotNull()
                .joinToString(" · ")
        FactKind.BUDGET, FactKind.REVENUE -> formatMoney(fact.number, loc)
        else -> fact.text
    }
}
