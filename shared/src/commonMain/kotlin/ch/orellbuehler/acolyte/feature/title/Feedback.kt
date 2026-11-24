package ch.orellbuehler.acolyte.feature.title

import ch.orellbuehler.acolyte.api.ApiError

enum class FeedbackKind {
    FAILED,
    SEARCHING,
    SEARCHING_SEASON,
    SEARCHING_EPISODE,
    SEARCHING_MISSING,
    NOTHING_TO_SEARCH,
    TURN_SHOW_ON,
    REFRESHING,
    SAVED,
    ORDER_CHANGED,
    MOVING,
    MARKED_FAILED,
    VERSION_ADDED,
    VERSION_SAVED,
    VERSION_REMOVED,
    FILE_SAVED,
    FILE_ADOPTED,
    PACK_NEXT,
    HIDDEN,
    SHOWN_AGAIN,
    RESTORED,
    SENT,
    SENDING,
    NOT_SENT,
    TURNED_DOWN,
    GROUP_BLOCKED,
    SUBTITLES_SEARCHING,
    SUBTITLES_NO_FILE,
    SUBTITLES_SAVED,
    SUBTITLES_DEFAULT,
    SUBTITLE_SAVED,
    TRANSLATING,
    TIMING_FIXED,
    CLEANED,
    NOTHING_TO_CLEAN,
    SUBTITLE_TURNED_DOWN,
    MATCHED,
    UNMATCHED,
    RESCANNING,
    RESCAN_DONE,
    RENAMED,
    RENAME_CHANGED,
    RENAME_FAILED,
    DELETED,
    ADDED,
}

data class Feedback(
    val serial: Int,
    val kind: FeedbackKind,
    val text: String = "",
    val count: Int = 0,
    val error: ApiError? = null,
)

internal class FeedbackCounter {
    private var serial = 0

    fun next(
        kind: FeedbackKind,
        text: String = "",
        count: Int = 0,
        error: ApiError? = null,
    ): Feedback = Feedback(++serial, kind, text, count, error)

    fun failed(error: ApiError): Feedback = next(FeedbackKind.FAILED, error = error)
}
