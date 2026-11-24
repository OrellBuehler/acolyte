package ch.orellbuehler.acolyte.feature.requests

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.RequestsEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.model.ApproveBody
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.api.model.PartEpisode
import ch.orellbuehler.acolyte.api.model.PartSong
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.api.model.QualityChoice
import ch.orellbuehler.acolyte.api.model.StoredParts
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EpisodeTick(
    val number: Int,
    val name: String,
    val ticked: Boolean,
    val asked: Boolean,
)

data class SeasonPickRow(
    val number: Int,
    val all: Int,
    val asked: Int,
    val kept: Int,
    val tick: Tick,
    val expanded: Boolean,
    val loading: Boolean,
    val failed: Boolean,
    val episodes: List<EpisodeTick>,
)

data class SongTick(
    val key: String,
    val name: String,
    val disc: Int,
    val number: Int,
    val ticked: Boolean,
)

data class AlbumPickRow(
    val mbid: String,
    val name: String,
    val year: Int,
    val kind: AlbumKind,
    val editions: Int,
    val tick: Tick,
    val expanded: Boolean,
    val songs: List<SongTick>,
    val kept: Int,
)

data class AskReviewState(
    val status: LoadStatus = LoadStatus(),
    val ask: Ask? = null,
    val seasons: List<SeasonPickRow> = emptyList(),
    val albums: List<AlbumPickRow> = emptyList(),
    val pills: List<QualityPill> = emptyList(),
    val selected: String = "",
    val changed: Boolean = false,
    val empty: Boolean = false,
) {
    val canApprove: Boolean get() = ask?.isWaiting == true && !empty

    val showsQuality: Boolean get() = pills.size > 1

    val selectedPill: QualityPill? get() = pills.firstOrNull { it.key == selected }

    val extraOnly: Boolean get() = selectedPill?.extra == true && ask?.askedLine != selectedPill?.line

    val albumCounts: Map<AlbumKind, Int>
        get() = AlbumKind.entries.associateWith { kind -> albums.count { it.kind == kind } }.filterValues { it > 0 }

    val keptSeasons: Int get() = seasons.count { it.tick != Tick.NONE }

    val keptEpisodes: Int get() = seasons.sumOf { it.kept }

    val keptAlbums: Int get() = albums.count { it.tick != Tick.NONE }

    val allSongs: Int get() = albums.sumOf { it.songs.size }

    val keptSongs: Int get() = albums.sumOf { it.kept }
}

class AskReviewPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val askId: Long,
) {
    private data class Inner(
        val ask: Ask? = null,
        val episodes: Map<Int, List<PartEpisode>> = emptyMap(),
        val loading: Set<Int> = emptySet(),
        val failed: Set<Int> = emptySet(),
        val seasonTicks: Map<Int, List<Int>> = emptyMap(),
        val groups: List<AlbumGroup> = emptyList(),
        val songs: Map<String, List<PartSong>> = emptyMap(),
        val albumOff: Set<String> = emptySet(),
        val songTicks: Map<String, List<String>> = emptyMap(),
        val expandedSeasons: Set<Int> = emptySet(),
        val expandedAlbums: Set<String> = emptySet(),
        val lines: List<QualityChoice> = emptyList(),
        val rules: List<PickRule> = emptyList(),
        val selected: String = "",
    )

    private val inner = MutableStateFlow(Inner())
    private val mutableState = MutableStateFlow(AskReviewState())
    private val lock = Mutex()

    val state: StateFlow<AskReviewState> = mutableState.asStateFlow()

    suspend fun load() {
        lock.withLock {
            if (mutableState.value.status.phase == Phase.READY) return
            mutableState.update { it.copy(status = it.status.started()) }
            val me = account()
            val ask =
                when (val result = api.result(RequestsEndpoints.ask, "id" to askId)) {
                    is ApiResult.Ok -> result.value
                    is ApiResult.Failed -> {
                        mutableState.update { it.copy(status = it.status.failed(result.error)) }
                        return
                    }
                }
            coroutineScope {
                val parts =
                    async {
                        if (ask.kind ==
                            "movie"
                        ) {
                            StoredParts()
                        } else {
                            api.result(RequestsEndpoints.parts, "id" to askId).valueOrNull()
                        }
                    }
                val artist =
                    async {
                        if (!ask.isArtist || ask.artistMbid.isEmpty()) {
                            null
                        } else {
                            api.result(LibraryEndpoints.lookupArtists, "term" to "mbid:${ask.artistMbid}", "albums" to "true").valueOrNull()
                        }
                    }
                val limit =
                    async {
                        if (!ask.isArtist &&
                            me?.can(Need.ASK) == true
                        ) {
                            api.result(RequestsEndpoints.limit).valueOrNull()
                        } else {
                            null
                        }
                    }
                val rules =
                    async {
                        if (!ask.isArtist &&
                            me?.can(Need.MANAGE_LIBRARY) == true
                        ) {
                            api.result(LibraryEndpoints.pickRules).valueOrNull()
                        } else {
                            null
                        }
                    }
                val stored = parts.await() ?: StoredParts()
                val known =
                    artist
                        .await()
                        ?.artists
                        ?.firstOrNull()
                        ?.askAlbums
                        .orEmpty()
                val lines =
                    limit
                        .await()
                        ?.qualities
                        ?.let { if (ask.kind == "show") it.show else it.movie }
                        .orEmpty()
                val episodes = stored.seasons.mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()
                val groups = if (ask.isArtist) albumGroups(askAlbums(ask, known, stored.albums.keys)) else emptyList()
                val songs =
                    groups.associate { g ->
                        g.mbid to
                            (g.ids.firstNotNullOfOrNull { id -> stored.albums[id]?.takeIf { it.isNotEmpty() } } ?: emptyList())
                    }
                inner.value =
                    Inner(
                        ask = ask,
                        episodes = episodes,
                        groups = groups,
                        songs = songs,
                        lines = lines,
                        rules = rules.await()?.pickRules.orEmpty(),
                        selected = ask.askedLine,
                    )
                publish(LoadStatus.ready)
            }
        }
    }

    fun toggleSeason(number: Int) =
        change { s ->
            val row = rows(s).firstOrNull { it.number == number } ?: return@change s
            val pick = seasonPick(row, s.seasonTicks[number])
            val next = if (pick.tick == Tick.ALL) emptyList() else row.episodes.ifEmpty { pick.asked }
            if (pick.tick != Tick.ALL && row.episodes.isEmpty()) {
                s.copy(seasonTicks = s.seasonTicks - number)
            } else {
                s.copy(seasonTicks = s.seasonTicks + (number to next))
            }
        }

    fun toggleEpisode(
        number: Int,
        episode: Int,
    ) = change { s ->
        val row = rows(s).firstOrNull { it.number == number } ?: return@change s
        val ticked = seasonPick(row, s.seasonTicks[number]).ticked
        val next = if (episode in ticked) ticked - episode else (ticked + episode).sorted()
        s.copy(seasonTicks = s.seasonTicks + (number to next))
    }

    suspend fun expandSeason(number: Int) {
        val s = inner.value
        if (number in s.expandedSeasons) {
            change { it.copy(expandedSeasons = it.expandedSeasons - number) }
            return
        }
        change { it.copy(expandedSeasons = it.expandedSeasons + number) }
        val tmdb = s.ask?.tmdbId.orEmpty()
        if (s.episodes[number]?.isNotEmpty() == true || tmdb.isEmpty() || number in s.loading) return
        change { it.copy(loading = it.loading + number, failed = it.failed - number) }
        when (val result = api.result(TitleEndpoints.season, "tmdb_id" to tmdb, "number" to number)) {
            is ApiResult.Ok ->
                change {
                    it.copy(
                        loading = it.loading - number,
                        episodes = it.episodes + (number to result.value.episodes.map { e -> PartEpisode(e.number, e.name) }),
                    )
                }
            is ApiResult.Failed -> change { it.copy(loading = it.loading - number, failed = it.failed + number) }
        }
    }

    fun toggleAlbum(mbid: String) =
        change { s ->
            val off = mbid in s.albumOff || s.songTicks[mbid]?.isEmpty() == true
            if (off) s.copy(albumOff = s.albumOff - mbid, songTicks = s.songTicks - mbid) else s.copy(albumOff = s.albumOff + mbid)
        }

    fun toggleSong(
        mbid: String,
        key: String,
    ) = change { s ->
        val all = s.songs[mbid].orEmpty().map { it.key }
        val kept = if (mbid in s.albumOff) emptyList() else s.songTicks[mbid] ?: all
        val next = if (key in kept) kept - key else all.filter { it in kept || it == key }
        s.copy(albumOff = s.albumOff - mbid, songTicks = s.songTicks + (mbid to next))
    }

    fun expandAlbum(mbid: String) =
        change { s ->
            s.copy(expandedAlbums = if (mbid in s.expandedAlbums) s.expandedAlbums - mbid else s.expandedAlbums + mbid)
        }

    fun select(key: String) = change { it.copy(selected = key) }

    internal val approveBody: ApproveBody
        get() {
            val s = inner.value
            val ask = s.ask ?: return ApproveBody()
            val pill = mutableState.value.selectedPill
            val choice =
                if (ask.isArtist) {
                    albumsChoice(
                        albumPicks(s),
                    )
                } else {
                    seasonsChoice(rows(s).map { seasonPick(it, s.seasonTicks[it.number]) })
                }
            return ApproveBody(
                pickRulesId = pill?.rulesId ?: 0,
                quality = pill?.line?.takeIf { it != ask.askedLine },
                seasons = choice.seasons,
                episodes = choice.episodes,
                albums = choice.albums,
                songs = choice.songs,
            )
        }

    private fun rows(s: Inner): List<SeasonRow> {
        val ask = s.ask ?: return emptyList()
        if (ask.kind != "show") return emptyList()
        return askSeasonRows(ask, s.episodes.mapValues { (_, list) -> list.map { it.number } })
    }

    private fun albumPicks(s: Inner): List<AlbumPick> =
        s.groups.map { g ->
            val all = s.songs[g.mbid].orEmpty().map { it.key }
            AlbumPick(g, all, g.mbid !in s.albumOff, s.songTicks[g.mbid] ?: all)
        }

    private fun change(transform: (Inner) -> Inner) {
        inner.update(transform)
        publish(mutableState.value.status)
    }

    private fun publish(status: LoadStatus) {
        val s = inner.value
        val ask = s.ask
        val seasonPicks = rows(s).map { seasonPick(it, s.seasonTicks[it.number]) }
        val seasons =
            seasonPicks.map { p ->
                val names = s.episodes[p.number].orEmpty().associate { it.number to it.name }
                SeasonPickRow(
                    number = p.number,
                    all = p.all.size,
                    asked = p.asked.size,
                    kept = p.ticked.size,
                    tick = p.tick,
                    expanded = p.number in s.expandedSeasons,
                    loading = p.number in s.loading,
                    failed = p.number in s.failed,
                    episodes = p.all.map { n -> EpisodeTick(n, names[n].orEmpty(), n in p.ticked, n in p.asked) },
                )
            }
        val albumPicks = albumPicks(s)
        val albums =
            albumPicks.map { p ->
                val songs = s.songs[p.group.mbid].orEmpty()
                AlbumPickRow(
                    mbid = p.group.mbid,
                    name = p.group.name,
                    year = p.group.year,
                    kind = p.group.kind,
                    editions = p.group.editions.size,
                    tick = p.tick,
                    expanded = p.group.mbid in s.expandedAlbums,
                    songs = songs.map { SongTick(it.key, it.name, it.disc, it.number, p.ticked && it.key in p.keptSongs) },
                    kept = if (p.ticked) p.keptSongs.size else 0,
                )
            }
        val choice =
            when {
                ask == null -> PartsChoice()
                ask.isArtist -> albumsChoice(albumPicks)
                else -> seasonsChoice(seasonPicks)
            }
        val pills = if (ask == null || ask.isArtist) emptyList() else ownerRow(s.lines, s.rules, ask)
        mutableState.value =
            AskReviewState(
                status = status,
                ask = ask,
                seasons = seasons,
                albums = albums,
                pills = pills,
                selected = s.selected,
                changed = choice.changed,
                empty = choice.empty,
            )
    }
}
