package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.MusicEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.RequestsEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.model.LookupArtist
import ch.orellbuehler.acolyte.api.model.MusicAlbumAnswer
import ch.orellbuehler.acolyte.api.model.MusicPageTrack
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

enum class MusicHave {
    UNKNOWN,
    IN_LIBRARY,
    DOWNLOADING,
    MISSING,
}

data class MusicTrackRow(
    val disc: Int,
    val number: Int,
    val name: String,
    val length: String,
    val explicit: Boolean,
    val have: MusicHave,
)

data class MusicEditionChip(
    val mbid: String,
    val label: String,
    val tracks: Int,
)

enum class MusicAlbumCta {
    NONE,
    OPEN_LIBRARY,
    MANAGE_LIBRARY,
    ADD,
    ASK,
    ASKED,
    OPEN_ARTIST,
}

data class MusicAlbumState(
    val status: LoadStatus = LoadStatus(),
    val mbid: String = "",
    val artistMbid: String = "",
    val name: String = "",
    val artistName: String = "",
    val kind: MusicAlbumKind = MusicAlbumKind.ALBUM,
    val year: String = "",
    val date: String = "",
    val image: String = "",
    val tint: Long = 0,
    val explicit: Boolean = false,
    val disambiguation: String = "",
    val label: String = "",
    val songs: Int = 0,
    val minutes: Int = 0,
    val tracks: List<MusicTrackRow> = emptyList(),
    val discs: Int = 1,
    val tracksWaiting: Boolean = true,
    val gap: String = "",
    val editions: List<MusicEditionChip> = emptyList(),
    val edition: String = "",
    val titleId: Long = 0,
    val partId: Long = 0,
    val inLibraryAlbum: Boolean = false,
    val mark: MusicMark = MusicMark.NONE,
    val have: Int = 0,
    val missing: Int = 0,
    val moreBy: List<MusicAlbum> = emptyList(),
    val near: List<MusicAlbum> = emptyList(),
    val similar: List<MusicArtistCard> = emptyList(),
    val followKnown: Boolean = false,
    val following: Boolean = false,
    val lookupLoaded: Boolean = false,
    val asked: Boolean = false,
    val canAdd: Boolean = false,
    val canAsk: Boolean = false,
    val busy: Boolean = false,
    val note: MusicNote? = null,
) {
    val cta: MusicAlbumCta
        get() =
            when {
                titleId > 0 -> if (inLibraryAlbum) MusicAlbumCta.OPEN_LIBRARY else MusicAlbumCta.MANAGE_LIBRARY
                asked -> MusicAlbumCta.ASKED
                canAdd -> MusicAlbumCta.ADD
                canAsk -> MusicAlbumCta.ASK
                else -> MusicAlbumCta.OPEN_ARTIST
            }

    val ctaReady: Boolean get() = titleId > 0 || lookupLoaded || cta == MusicAlbumCta.OPEN_ARTIST

    val problem: MusicProblem get() = if (status.phase == Phase.FAILED) musicProblem(status.error) else MusicProblem.NONE

    val showCounts: Boolean get() = partId > 0 && tracks.any { it.have != MusicHave.UNKNOWN }
}

class MusicAlbumPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val mbid: String,
    artist: String,
) {
    private val mutableState = MutableStateFlow(MusicAlbumState(mbid = mbid, artistMbid = artist))
    private val notes = MusicNotes()
    private var answer = MusicAlbumAnswer()
    private var lookup: LookupArtist? = null
    private var similar = emptyList<ch.orellbuehler.acolyte.api.model.MusicPageArtist>()
    private var near = emptyList<ch.orellbuehler.acolyte.api.model.MusicPageAlbum>()
    private var askedHere = false

    val state: StateFlow<MusicAlbumState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        mutableState.update { it.copy(status = it.status.started()) }
        if (!fetch(quiet = false)) return
        loadSide()
        var tries = 0
        while (answer.tracks.isEmpty() && tries < (if (answer.gap != null) GAP_TRIES else PAUSES.size)) {
            delay(if (answer.gap != null) GAP_MILLIS else PAUSES[tries])
            tries++
            if (!fetch(quiet = true)) break
        }
        mutableState.update { it.copy(tracksWaiting = false) }
    }

    private suspend fun loadSide() {
        val artist = mutableState.value.artistMbid
        if (artist.isEmpty()) return
        coroutineScope {
            val a = async { api.result(MusicEndpoints.artist, "mbid" to artist, "part" to "similar", "n" to SIMILAR).valueOrNull() }
            val n = async { api.result(MusicEndpoints.album, "mbid" to mbid, "part" to "near", "artist" to artist).valueOrNull() }
            val l = async { api.result(LibraryEndpoints.lookupArtists, "term" to "mbid:$artist").valueOrNull() }
            similar = a.await()?.similar.orEmpty()
            near = n.await()?.near.orEmpty()
            lookup = l.await()?.artists?.firstOrNull()
            publish(lookupDone = lookup != null)
        }
        loadFollow()
    }

    suspend fun reloadQuiet() {
        if (mutableState.value.status.phase != Phase.READY) return
        val artist = mutableState.value.artistMbid
        if (artist.isEmpty()) return
        api.result(LibraryEndpoints.lookupArtists, "term" to "mbid:$artist").valueOrNull()?.artists?.firstOrNull()?.let {
            lookup = it
            publish(lookupDone = true)
        }
    }

    private suspend fun fetch(quiet: Boolean): Boolean {
        val artist = mutableState.value.artistMbid
        val result =
            if (artist.isNotEmpty()) {
                api.result(MusicEndpoints.album, "mbid" to mbid, "artist" to artist, "similar" to 0)
            } else {
                api.result(MusicEndpoints.album, "mbid" to mbid, "similar" to 0)
            }
        return when (result) {
            is ApiResult.Ok -> {
                answer = result.value
                if (artist.isEmpty() && answer.artist != null) mutableState.update { it.copy(artistMbid = answer.artist?.mbid.orEmpty()) }
                publish()
                mutableState.update { it.copy(status = LoadStatus.ready) }
                true
            }
            is ApiResult.Failed -> {
                if (!quiet) mutableState.update { it.copy(status = it.status.failed(result.error)) }
                false
            }
        }
    }

    private suspend fun loadFollow() {
        val id = titleId()
        if (id <= 0 || mutableState.value.followKnown) return
        val follow = api.result(TitleEndpoints.followTitle, "id" to id).valueOrNull() ?: return
        mutableState.update { it.copy(followKnown = true, following = follow.following) }
    }

    private fun titleId(): Long = answer.titleId.takeIf { it > 0 } ?: lookup?.titleId ?: 0L

    private fun publish(lookupDone: Boolean = false) {
        val me = account()
        val a = answer
        val edition = mutableState.value.edition
        val list = a.editions.firstOrNull { it.mbid == edition }?.list ?: a.tracks
        val rows = list.map { it.toRow() }
        val asked = askedHere || lookup?.ask?.let { it.albums.isEmpty() || it.albums.any { x -> x.mbid == mbid } } == true
        val have = rows.count { it.have == MusicHave.IN_LIBRARY }
        val missing = rows.count { it.have == MusicHave.MISSING || it.have == MusicHave.DOWNLOADING }
        val picture = a.pictures["album:$mbid"]
        mutableState.update { s ->
            s.copy(
                name = a.name.ifEmpty { s.name },
                artistName =
                    a.artist
                        ?.name
                        .orEmpty()
                        .ifEmpty { s.artistName },
                kind = MusicRules.albumKind(a.type, a.secondaryTypes),
                year = MusicRules.year(a.releaseDate),
                date = a.releaseDate,
                image = a.image.ifEmpty { s.image },
                tint = picture?.color?.let { MusicRules.tint(it) } ?: s.tint,
                explicit = a.explicit,
                disambiguation = a.disambiguation,
                label = a.label,
                songs = rows.size,
                minutes = MusicRules.minutes(list.sumOf { it.lengthMs }.takeIf { it > 0 } ?: a.lengthMs),
                tracks = rows,
                discs =
                    list
                        .map { it.disc }
                        .distinct()
                        .size
                        .coerceAtLeast(1),
                gap =
                    a.gap
                        ?.message
                        .orEmpty()
                        .ifEmpty { if (a.gap != null) "gap" else "" },
                editions = a.editions.map { MusicEditionChip(it.mbid, it.label, it.tracks) },
                titleId = titleId(),
                partId = a.partId,
                inLibraryAlbum = a.state.isNotEmpty() && a.state != "not_included",
                mark = MusicRules.pill(a.state, asked),
                have = have,
                missing = missing,
                moreBy = a.moreBy.filter { it.image.isNotEmpty() }.map { it.toAlbum(emptySet(), false) },
                near = near.filter { it.image.isNotEmpty() }.map { it.toAlbum(emptySet(), false) },
                similar = similar.map { it.toCard() },
                lookupLoaded = s.lookupLoaded || lookupDone,
                asked = asked,
                canAdd = me?.allows(Need.MANAGE_LIBRARY, Part.MUSIC) == true,
                canAsk = me?.allows(Need.ASK, Part.WISHLIST) == true && me.isOn(Part.MUSIC),
            )
        }
    }

    fun pickEdition(id: String) {
        mutableState.update { it.copy(edition = if (it.edition == id) "" else id) }
        publish()
    }

    fun stillReading() = mutableState.update { it.copy(note = notes.next(MusicNoteKind.STILL_READING)) }

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

    suspend fun askWithEdition() {
        val s = mutableState.value
        if (s.edition.isEmpty() || s.busy) return
        mutableState.update { it.copy(busy = true) }
        val body =
            NewAsk(id = "mbid:${s.artistMbid}", kind = "artist", albums = listOf(mbid), editions = mapOf(mbid to s.edition), follow = true)
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
                askedHere = true
                mutableState.update { it.copy(busy = false, note = notes.next(kind, text = it.name)) }
                publish()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, note = notes.failed(result.error)) }
        }
    }

    private companion object {
        val PAUSES = listOf(1_000L, 1_500L, 2_000L, 2_000L, 2_500L, 3_000L, 4_000L, 5_000L, 6_000L, 8_000L, 8_000L, 8_000L, 8_000L)
        const val GAP_TRIES = 90
        const val GAP_MILLIS = 2_000L
        const val SIMILAR = 14
    }
}

internal fun MusicPageTrack.toRow(): MusicTrackRow =
    MusicTrackRow(
        disc = disc,
        number = number,
        name = name,
        length = MusicRules.lengthText(lengthMs),
        explicit = explicit,
        have =
            when (have) {
                "in_library" -> MusicHave.IN_LIBRARY
                "downloading" -> MusicHave.DOWNLOADING
                "missing" -> MusicHave.MISSING
                else -> MusicHave.UNKNOWN
            },
    )
