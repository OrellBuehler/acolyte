package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.MusicEndpoints
import ch.orellbuehler.acolyte.api.MusicPaths
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.RequestsEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.model.LookupArtist
import ch.orellbuehler.acolyte.api.model.MusicAlbumFollowBody
import ch.orellbuehler.acolyte.api.model.MusicArtistAnswer
import ch.orellbuehler.acolyte.api.model.MusicPageAlbum
import ch.orellbuehler.acolyte.api.model.NewAsk
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MusicArtistState(
    val status: LoadStatus = LoadStatus(),
    val mbid: String = "",
    val name: String = "",
    val image: String = "",
    val wide: String = "",
    val tint: Long = 0,
    val kind: MusicArtistKind = MusicArtistKind.ARTIST,
    val country: String = "",
    val disambiguation: String = "",
    val ended: Boolean = false,
    val genres: List<String> = emptyList(),
    val listeners: Long = 0,
    val rank: Int = 0,
    val change: Long = 0,
    val plays: Long = 0,
    val titleId: Long = 0,
    val followKnown: Boolean = false,
    val following: Boolean = false,
    val bio: MusicBio? = null,
    val top: List<MusicSong> = emptyList(),
    val essential: List<MusicAlbum> = emptyList(),
    val shelves: List<MusicShelf> = emptyList(),
    val storeAlbums: List<MusicStoreItem> = emptyList(),
    val coming: List<MusicAlbum> = emptyList(),
    val videos: List<MusicVideo> = emptyList(),
    val appearsOn: List<MusicAlbum> = emptyList(),
    val related: List<MusicArtistCard> = emptyList(),
    val similar: List<MusicArtistCard> = emptyList(),
    val similarLoading: Boolean = true,
    val albumCount: Int = 0,
    val singleCount: Int = 0,
    val activeSince: String = "",
    val activeUntil: String = "",
    val gap: String = "",
    val incomplete: Boolean = false,
    val lookupLoaded: Boolean = false,
    val asked: Boolean = false,
    val askedWhole: Boolean = false,
    val neverAdd: Boolean = false,
    val canAdd: Boolean = false,
    val canAsk: Boolean = false,
    val askAll: MusicAskAll? = null,
    val asking: Boolean = false,
    val note: MusicNote? = null,
) {
    val main: MusicMain
        get() =
            when {
                titleId > 0 -> MusicMain.OPEN_LIBRARY
                !canAdd && !canAsk -> MusicMain.NONE
                asked -> MusicMain.ASKED
                canAdd -> MusicMain.ADD
                else -> MusicMain.ASK
            }

    val mainReady: Boolean get() = titleId > 0 || lookupLoaded

    val problem: MusicProblem get() = if (status.phase == Phase.FAILED) musicProblem(status.error) else MusicProblem.NONE

    val hasDiscography: Boolean get() = shelves.isNotEmpty() || coming.isNotEmpty() || storeAlbums.isNotEmpty()
}

class MusicArtistPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val mbid: String,
    private val today: () -> String,
) {
    private val mutableState = MutableStateFlow(MusicArtistState(mbid = mbid, wide = MusicPaths.wide(mbid)))
    private val notes = MusicNotes()
    private var answer = MusicArtistAnswer()
    private var similar: List<ch.orellbuehler.acolyte.api.model.MusicPageArtist> = emptyList()
    private var essential: List<String> = emptyList()
    private var appears: List<MusicPageAlbum> = emptyList()
    private var lookup: LookupArtist? = null
    private var asked: Set<String> = emptySet()
    private var wholeAsked = false

    val state: StateFlow<MusicArtistState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase == Phase.READY) return
        refresh()
    }

    suspend fun refresh() {
        mutableState.update { it.copy(status = it.status.started()) }
        if (!fetch(quiet = false)) return
        loadAll()
        var tries = 0
        while (answer.incomplete && tries < REASKS.size) {
            delay(REASKS[tries++])
            if (fetch(quiet = true) && !answer.incomplete) loadAll()
        }
    }

    private suspend fun loadAll() {
        coroutineScope {
            val side = async { loadSide() }
            val lookupJob = async { loadLookup() }
            side.await()
            lookupJob.await()
        }
        loadFollow()
    }

    suspend fun reloadQuiet() {
        if (mutableState.value.status.phase != Phase.READY) return
        loadLookup()
        val id = answer.titleId
        if (id <= 0 && lookup?.titleId.orZero() > 0) {
            fetch(quiet = true)
            loadFollow()
        }
    }

    private suspend fun fetch(quiet: Boolean): Boolean {
        when (val result = api.result(MusicEndpoints.artist, "mbid" to mbid)) {
            is ApiResult.Ok -> {
                answer = result.value.let { fresh -> if (fresh.videos.isEmpty()) fresh.copy(videos = answer.videos) else fresh }
                publish()
                mutableState.update { it.copy(status = LoadStatus.ready) }
                return true
            }
            is ApiResult.Failed -> {
                if (!quiet && result.error.status == SERVER_RETRY && mutableState.value.status.phase != Phase.READY) {
                    delay(RETRY_MILLIS)
                    return fetch(quiet = true)
                }
                if (!quiet) mutableState.update { it.copy(status = it.status.failed(result.error)) }
                return false
            }
        }
    }

    private suspend fun loadSide() =
        coroutineScope {
            val sim = async { loadSimilar() }
            val more = async { api.result(MusicEndpoints.artist, "mbid" to mbid, "part" to "more").valueOrNull() }
            val videos = async { api.result(MusicEndpoints.artist, "mbid" to mbid, "part" to "videos").valueOrNull() }
            val moreAnswer = more.await()
            essential = moreAnswer?.essential.orEmpty()
            appears = moreAnswer?.appearsOn.orEmpty()
            answer = answer.copy(videos = videos.await()?.videos.orEmpty())
            similar = sim.await()
            publish(similarDone = true)
        }

    private suspend fun loadSimilar(): List<ch.orellbuehler.acolyte.api.model.MusicPageArtist> {
        var tries = 0
        while (true) {
            val r =
                api.result(MusicEndpoints.artist, "mbid" to mbid, "part" to "similar", "n" to SIMILAR).valueOrNull() ?: return emptyList()
            if (r.similar.isNotEmpty() || !r.incomplete || tries >= REASKS.size) return r.similar
            delay(REASKS[tries++])
        }
    }

    private suspend fun loadLookup() {
        val result = api.result(LibraryEndpoints.lookupArtists, "term" to "mbid:$mbid")
        val found =
            (result as? ApiResult.Ok)?.value?.artists?.firstOrNull { it.mbid == mbid || it.mbid.isEmpty() }
                ?: (result as? ApiResult.Ok)?.value?.artists?.firstOrNull()
        if (found != null) {
            lookup = found
            asked =
                found.ask
                    ?.albums
                    ?.map { it.mbid }
                    ?.toSet()
                    .orEmpty()
            wholeAsked = found.ask != null && found.ask.albums.isEmpty()
            publish(lookupDone = true)
        }
    }

    private suspend fun loadFollow() {
        val id = titleId()
        if (id <= 0 || mutableState.value.followKnown) return
        val follow = api.result(TitleEndpoints.followTitle, "id" to id).valueOrNull() ?: return
        mutableState.update { it.copy(followKnown = true, following = follow.following) }
    }

    private fun titleId(): Long = answer.titleId.takeIf { it > 0 } ?: lookup?.titleId.orZero()

    private fun Long?.orZero(): Long = this ?: 0L

    private fun publish(
        similarDone: Boolean = false,
        lookupDone: Boolean = false,
    ) {
        val me = account()
        val a = answer
        val day = today()
        val rules = access(me)
        val pairs = a.albums.map { it to it.toAlbum(asked, wholeAsked) }
        val coming = pairs.filter { (w, u) -> w.upcoming || MusicRules.isFuture(u.date, day) }.map { it.second }.sortedBy { it.date }
        val released = pairs.filter { (_, u) -> coming.none { it.mbid == u.mbid } }
        val shelves =
            MusicShelfKind.entries.mapNotNull { kind ->
                val albums = released.filter { MusicRules.shelfKind(it.first.shelf) == kind }.map { it.second }
                if (albums.isEmpty()) null else MusicShelf(kind, albums.sortedByDescending { it.date })
            }
        val all = pairs.map { it.second }
        val essentialAlbums =
            essential
                .mapNotNull { id ->
                    all.firstOrNull { it.mbid == id }
                }.filter { it.image.isNotEmpty() }
                .take(ESSENTIAL)
        val dates = released.mapNotNull { it.second.year.toIntOrNull() }
        val pop = a.popularity
        val picture = a.pictures["wide:$mbid"] ?: a.pictures["artist:$mbid"]
        val missing = a.missing
        val inLibrary = titleId() > 0
        val askAll =
            if (rules.ask && !rules.manage && !inLibrary && missing.count > 0 && !wholeAsked && missing.albums.isNotEmpty()) {
                MusicAskAll(missing.count, MusicRules.sizeIsGb(missing.sizeBytes), MusicRules.sizeValue(missing.sizeBytes))
            } else {
                null
            }
        mutableState.update { s ->
            s.copy(
                name = a.name.ifEmpty { s.name },
                image = a.image.ifEmpty { s.image },
                tint = picture?.color?.let { MusicRules.tint(it) } ?: s.tint,
                kind = kindOf(a.type),
                country = a.country,
                disambiguation = a.disambiguation,
                ended = a.status == "ended",
                genres = a.genres,
                listeners = pop?.monthlyListeners ?: 0,
                rank = pop?.worldRank ?: 0,
                change = pop?.listenersChangeToday ?: 0,
                plays = pop?.totalPlays ?: 0,
                titleId = titleId(),
                bio =
                    a.bio?.takeIf { it.extract.isNotEmpty() }?.let {
                        MusicBio(
                            it.extract,
                            it.url,
                            it.license,
                            it.licenseUrl,
                            it.attribution,
                        )
                    },
                top = a.top.take(TOP).map { it.toSong() },
                essential = essentialAlbums,
                shelves = shelves,
                storeAlbums = a.storeAlbums.map { it.toItem() },
                coming = coming,
                videos = a.videos.map { it.toVideo(a.name) },
                appearsOn = appears.filter { it.image.isNotEmpty() }.map { it.toAlbum(emptySet(), false) },
                related = a.related.map { it.toCard() },
                similar = similar.map { it.toCard() },
                similarLoading = if (similarDone) false else s.similarLoading,
                albumCount = released.count { it.first.shelf == "albums" },
                singleCount = released.count { it.first.shelf == "singles" },
                activeSince = dates.minOrNull()?.toString().orEmpty(),
                activeUntil = if (a.status == "ended") dates.maxOrNull()?.toString().orEmpty() else "",
                gap =
                    a.gap
                        ?.message
                        .orEmpty()
                        .ifEmpty { if (a.gap != null) "gap" else "" },
                incomplete = a.incomplete,
                lookupLoaded = s.lookupLoaded || lookupDone,
                asked = lookup?.ask != null,
                askedWhole = wholeAsked,
                neverAdd = lookup?.neverAdd == true,
                canAdd = rules.manage,
                canAsk = rules.ask,
                askAll = askAll,
            )
        }
    }

    suspend fun toggleFollow() {
        val s = mutableState.value
        val id = titleId()
        if (id <= 0 || !s.followKnown) return
        val on = !s.following
        mutableState.update { it.copy(following = on) }
        val result = if (on) api.result(TitleEndpoints.followTitleOn, "id" to id) else api.result(TitleEndpoints.followTitleOff, "id" to id)
        when (result) {
            is ApiResult.Ok -> mutableState.update { it.copy(following = result.value.following) }
            is ApiResult.Failed ->
                mutableState.update {
                    it.copy(following = !on, note = notes.next(MusicNoteKind.FOLLOW_FAILED, text = result.error.message))
                }
        }
    }

    suspend fun toggleAlbumFollow(albumMbid: String) {
        val s = mutableState.value
        val album = s.coming.firstOrNull { it.mbid == albumMbid } ?: return
        val on = !album.following
        setFollowing(albumMbid, on)
        val body = MusicAlbumFollowBody(album.name, s.name, album.date)
        val result =
            if (on) {
                api.result(MusicEndpoints.followAlbum, body, "mbid" to albumMbid)
            } else {
                api.result(MusicEndpoints.unfollowAlbum, "mbid" to albumMbid)
            }
        if (result is ApiResult.Failed) {
            setFollowing(albumMbid, !on)
            mutableState.update { it.copy(note = notes.next(MusicNoteKind.FOLLOW_FAILED, text = result.error.message)) }
        } else {
            answer = answer.copy(albums = answer.albums.map { if (it.mbid == albumMbid) it.copy(following = on) else it })
        }
    }

    private fun setFollowing(
        id: String,
        on: Boolean,
    ) = mutableState.update { s -> s.copy(coming = s.coming.map { if (it.mbid == id) it.copy(following = on) else it }) }

    fun stillReading() = mutableState.update { it.copy(note = notes.next(MusicNoteKind.STILL_READING)) }

    suspend fun askAll() {
        val s = mutableState.value
        val all = s.askAll ?: return
        if (s.asking) return
        mutableState.update { it.copy(asking = true) }
        val body = NewAsk(id = "mbid:$mbid", kind = "artist", albums = answer.missing.albums, follow = true)
        when (val result = api.result(RequestsEndpoints.make, body)) {
            is ApiResult.Ok -> {
                val ask = result.value
                val kind =
                    when {
                        ask.ask.state == "approved" -> MusicNoteKind.ASKED_APPROVED
                        ask.joined -> MusicNoteKind.ASKED_JOINED
                        ask.already -> MusicNoteKind.ASKED_ALREADY
                        else -> MusicNoteKind.ASKED_REQUESTED
                    }
                asked = asked + answer.missing.albums
                mutableState.update { it.copy(asking = false, note = notes.next(kind, text = s.name, count = all.count)) }
                loadLookup()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(asking = false, note = notes.failed(result.error)) }
        }
    }

    private class Access(
        val manage: Boolean,
        val ask: Boolean,
    )

    private fun access(me: Account?): Access =
        Access(
            manage = me?.allows(Need.MANAGE_LIBRARY, Part.MUSIC) == true,
            ask = me?.allows(Need.ASK, Part.WISHLIST) == true && me.isOn(Part.MUSIC),
        )

    private companion object {
        val REASKS = listOf(2_500L, 3_500L, 5_000L, 7_000L)
        const val RETRY_MILLIS = 1_500L
        const val SERVER_RETRY = 502
        const val SIMILAR = 14
        const val TOP = 10
        const val ESSENTIAL = 6
    }
}

internal fun MusicPageAlbum.toAlbum(
    asked: Set<String>,
    whole: Boolean,
): MusicAlbum {
    val isAsked = whole || mbid in asked
    return MusicAlbum(
        mbid = mbid,
        name = name,
        kind = MusicRules.albumKind(type, secondaryTypes),
        year = MusicRules.year(releaseDate),
        date = releaseDate,
        image = image,
        explicit = explicit,
        upcoming = upcoming,
        mark = MusicRules.pill(state, isAsked),
        following = following,
        partId = partId,
        artistMbid = artistMbid,
        artistName = artistName,
        asked = isAsked,
    )
}

internal fun ch.orellbuehler.acolyte.api.model.MusicPageSong.toSong(): MusicSong =
    MusicSong(
        title = title,
        album = album,
        albumMbid = albumMbid,
        year = "",
        length = MusicRules.lengthText(lengthMs),
        image = image,
        explicit = explicit,
    )

internal fun ch.orellbuehler.acolyte.api.model.MusicStoreAlbum.toItem(): MusicStoreItem =
    MusicStoreItem(
        title = title,
        year = MusicRules.year(releaseDate),
        kind =
            when (type.lowercase()) {
                "single" -> MusicAlbumKind.SINGLE
                "ep" -> MusicAlbumKind.EP
                "compilation" -> MusicAlbumKind.COMPILATION
                else -> MusicAlbumKind.ALBUM
            },
        tracks = trackCount,
        image = image,
        explicit = explicit,
    )

internal fun ch.orellbuehler.acolyte.api.model.MusicVideoItem.toVideo(artist: String): MusicVideo {
    val clean = title.removePrefix("$artist - ").removePrefix("$artist – ").trim()
    return MusicVideo(
        id = id,
        title = clean.ifEmpty { title },
        length = MusicRules.lengthText((duration * 1000).toLong()),
        thumb = "https://i.ytimg.com/vi/$id/mqdefault.jpg",
        url = "https://www.youtube.com/watch?v=$id",
    )
}

internal fun ch.orellbuehler.acolyte.api.model.MusicPageArtist.toCard(): MusicArtistCard = MusicArtistCard(mbid, name, image, note)

internal fun kindOf(type: String): MusicArtistKind =
    when (type.lowercase()) {
        "person" -> MusicArtistKind.SOLO
        "group", "orchestra", "choir" -> MusicArtistKind.GROUP
        else -> MusicArtistKind.ARTIST
    }
