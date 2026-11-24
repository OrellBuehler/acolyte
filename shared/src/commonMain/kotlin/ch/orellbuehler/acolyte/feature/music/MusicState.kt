package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode

enum class MusicNoteKind {
    FAILED,
    STILL_READING,
    FOLLOW_FAILED,
    ASKED_REQUESTED,
    ASKED_APPROVED,
    ASKED_JOINED,
    ASKED_ALREADY,
    ADDED,
    ALREADY_IN_LIBRARY,
    NOT_FOUND,
    KEEP_ALBUM_ON,
    KEEP_ALBUM_OFF,
    KEEP_ARTIST_ON,
    KEEP_ARTIST_OFF,
    SEARCHING,
    SEARCHING_ALL,
    KINDS_SAVED,
    REFRESHING,
    MOVING,
    PRESSING_CHANGED,
    PRESSING_FAILED,
    SETTINGS_SAVED,
    MATCHED,
    NOT_MATCHED,
    UNMATCHED,
}

data class MusicNote(
    val id: Int,
    val kind: MusicNoteKind,
    val text: String = "",
    val count: Int = 0,
    val error: ApiError? = null,
)

internal class MusicNotes {
    private var serial = 0

    fun next(
        kind: MusicNoteKind,
        text: String = "",
        count: Int = 0,
    ): MusicNote = MusicNote(++serial, kind, text, count)

    fun failed(error: ApiError): MusicNote = MusicNote(++serial, MusicNoteKind.FAILED, error = error)
}

enum class MusicProblem {
    NONE,
    OFFLINE,
    NOT_FOUND,
    BAD_LINK,
    FORBIDDEN,
    SLOW,
}

fun musicProblem(error: ApiError?): MusicProblem =
    when {
        error == null -> MusicProblem.NONE
        error.code == ApiErrorCode.OFFLINE || error.code == ApiErrorCode.TIMEOUT || error.code == ApiErrorCode.TLS -> MusicProblem.OFFLINE
        error.code == ApiErrorCode.NOT_FOUND -> MusicProblem.NOT_FOUND
        error.code == ApiErrorCode.BAD_REQUEST -> MusicProblem.BAD_LINK
        error.code == ApiErrorCode.FORBIDDEN || error.code == ApiErrorCode.UNAUTHORIZED -> MusicProblem.FORBIDDEN
        else -> MusicProblem.SLOW
    }

data class MusicAlbum(
    val mbid: String,
    val name: String,
    val kind: MusicAlbumKind,
    val year: String,
    val date: String,
    val image: String,
    val explicit: Boolean,
    val upcoming: Boolean,
    val mark: MusicMark,
    val following: Boolean,
    val partId: Long,
    val artistMbid: String,
    val artistName: String,
    val asked: Boolean,
)

data class MusicShelf(
    val kind: MusicShelfKind,
    val albums: List<MusicAlbum>,
)

data class MusicSong(
    val title: String,
    val album: String,
    val albumMbid: String,
    val year: String,
    val length: String,
    val image: String,
    val explicit: Boolean,
)

data class MusicStoreItem(
    val title: String,
    val year: String,
    val kind: MusicAlbumKind,
    val tracks: Int,
    val image: String,
    val explicit: Boolean,
)

data class MusicVideo(
    val id: String,
    val title: String,
    val length: String,
    val thumb: String,
    val url: String,
)

data class MusicArtistCard(
    val mbid: String,
    val name: String,
    val image: String,
    val note: String,
)

data class MusicBio(
    val text: String,
    val url: String,
    val license: String,
    val licenseUrl: String,
    val attribution: String,
)

enum class MusicArtistKind {
    SOLO,
    GROUP,
    ARTIST,
}

enum class MusicMain {
    NONE,
    OPEN_LIBRARY,
    ADD,
    ASK,
    ASKED,
}

data class MusicAskAll(
    val count: Int,
    val gb: Boolean,
    val size: String,
)
