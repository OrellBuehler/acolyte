package ch.orellbuehler.acolyte.ui.requests

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.common.R
import ch.orellbuehler.acolyte.feature.requests.AlbumKind
import ch.orellbuehler.acolyte.feature.requests.ApproverTab
import ch.orellbuehler.acolyte.feature.requests.AskFailure
import ch.orellbuehler.acolyte.feature.requests.FOUR_K_LINE
import ch.orellbuehler.acolyte.feature.requests.KindFilter
import ch.orellbuehler.acolyte.feature.requests.LimitKind
import ch.orellbuehler.acolyte.feature.requests.LimitLine
import ch.orellbuehler.acolyte.feature.requests.MemberTab
import ch.orellbuehler.acolyte.feature.requests.ReportKind
import ch.orellbuehler.acolyte.feature.requests.RequestsNote
import ch.orellbuehler.acolyte.feature.requests.RequestsNoteKind
import ch.orellbuehler.acolyte.feature.requests.RequestsState
import ch.orellbuehler.acolyte.feature.requests.Stage
import ch.orellbuehler.acolyte.feature.requests.StepKind
import ch.orellbuehler.acolyte.feature.requests.WaitTime
import ch.orellbuehler.acolyte.feature.requests.askFailure
import ch.orellbuehler.acolyte.feature.requests.askerNames
import ch.orellbuehler.acolyte.feature.requests.deciderName
import ch.orellbuehler.acolyte.feature.requests.fromWatchlistCount
import ch.orellbuehler.acolyte.feature.requests.isArtist
import ch.orellbuehler.acolyte.feature.requests.isFailed
import ch.orellbuehler.acolyte.feature.requests.qualityTagLine
import ch.orellbuehler.acolyte.feature.requests.stepKind
import ch.orellbuehler.acolyte.feature.requests.waitTime
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.components.relativeTime

@Composable
fun memberTabLabel(tab: MemberTab): String =
    stringResource(
        when (tab) {
            MemberTab.WAITING -> R.string.requests_tab_waiting
            MemberTab.APPROVED -> R.string.requests_tab_approved
            MemberTab.DECLINED -> R.string.requests_tab_declined
            MemberTab.READY -> R.string.requests_tab_ready
            MemberTab.FAILED -> R.string.requests_tab_failed
        },
    )

@Composable
fun approverTabLabel(tab: ApproverTab): String =
    stringResource(
        when (tab) {
            ApproverTab.WAITING -> R.string.requests_tab_waiting
            ApproverTab.PROCESSING -> R.string.requests_tab_processing
            ApproverTab.DONE -> R.string.requests_tab_done
        },
    )

@Composable
fun stageLabel(stage: Stage): String =
    stringResource(
        when (stage) {
            Stage.FAILED -> R.string.requests_stage_failed
            Stage.WAITING -> R.string.requests_stage_waiting
            Stage.UPCOMING -> R.string.requests_stage_upcoming
            Stage.SEARCHING -> R.string.requests_stage_searching
            Stage.DOWNLOADING -> R.string.requests_stage_downloading
            Stage.IMPORTING -> R.string.requests_stage_importing
            Stage.NEEDS_DECISION -> R.string.requests_stage_needs_decision
            Stage.LOOKING -> R.string.requests_stage_looking
            Stage.PROCESSING -> R.string.requests_stage_processing
            Stage.READY -> R.string.requests_stage_ready
            Stage.DECLINED -> R.string.requests_stage_declined
        },
    )

@Composable
fun kindFilterLabel(kind: KindFilter): String =
    stringResource(
        when (kind) {
            KindFilter.ALL -> R.string.requests_kind_all
            KindFilter.MOVIE -> R.string.requests_kind_movies
            KindFilter.SHOW -> R.string.requests_kind_shows
            KindFilter.ARTIST -> R.string.requests_kind_music
        },
    )

@Composable
fun kindWord(kind: String): String =
    stringResource(
        when (kind) {
            "show" -> R.string.requests_kind_show
            "artist" -> R.string.requests_kind_artist
            else -> R.string.requests_kind_movie
        },
    )

@Composable
fun qualityName(
    line: String,
    name: String,
): String =
    name.ifBlank {
        if (line ==
            FOUR_K_LINE
        ) {
            stringResource(R.string.requests_quality_four_k)
        } else {
            stringResource(R.string.requests_quality_standard)
        }
    }

@Composable
fun waitText(wait: WaitTime?): String {
    if (wait == null) return stringResource(R.string.requests_wait_few_days)
    if (wait.underMinute) return stringResource(R.string.requests_wait_minute)
    val days = pluralStringResource(R.plurals.requests_days, wait.days, wait.days)
    val hours = pluralStringResource(R.plurals.requests_hours, wait.hours, wait.hours)
    val minutes = pluralStringResource(R.plurals.requests_minutes, wait.minutes, wait.minutes)
    return when {
        wait.days > 0 && wait.hours > 0 -> stringResource(R.string.requests_wait_and, days, hours)
        wait.days > 0 -> days
        wait.hours > 0 && wait.minutes > 0 -> stringResource(R.string.requests_wait_and, hours, minutes)
        wait.hours > 0 -> hours
        else -> minutes
    }
}

@Composable
fun limitText(line: LimitLine): String {
    val plural =
        when (line.kind) {
            LimitKind.MOVIES -> R.plurals.requests_movie_asks
            LimitKind.SHOWS -> R.plurals.requests_show_asks
            LimitKind.MUSIC -> R.plurals.requests_music_asks
        }
    return if (line.left > 0) {
        stringResource(R.string.requests_limit_left, pluralStringResource(plural, line.left, line.left), line.perWeek)
    } else {
        stringResource(R.string.requests_limit_used, pluralStringResource(plural, line.perWeek, line.perWeek), waitText(line.wait))
    }
}

@Composable
fun namesText(names: List<String>): String =
    when (names.size) {
        0 -> ""
        1 -> names[0]
        else -> stringResource(R.string.requests_names_join, names.dropLast(1).joinToString(", "), names.last())
    }

@Composable
fun seasonsText(seasons: List<Int>): String {
    if (seasons.isEmpty()) return stringResource(R.string.requests_seasons_whole)
    val regular = seasons.filter { it > 0 }.sorted()
    val parts = mutableListOf<String>()
    if (0 in seasons) parts += stringResource(R.string.requests_season_specials)
    if (regular.size == 1) parts += stringResource(R.string.requests_season_one, regular[0])
    if (regular.size > 1) parts += stringResource(R.string.requests_season_many, regular.joinToString(", "))
    return parts.joinToString(", ")
}

@Composable
fun seasonName(number: Int): String =
    if (number == 0) stringResource(R.string.requests_season_specials) else stringResource(R.string.requests_season_one, number)

@Composable
fun albumKindText(
    kind: AlbumKind,
    count: Int,
): String =
    pluralStringResource(
        when (kind) {
            AlbumKind.ALBUMS -> R.plurals.requests_albums
            AlbumKind.SINGLES -> R.plurals.requests_singles
            AlbumKind.LIVE -> R.plurals.requests_live_albums
            AlbumKind.COMPILATIONS -> R.plurals.requests_compilations
            AlbumKind.OTHER -> R.plurals.requests_other_albums
        },
        count,
        count,
    )

@Composable
fun albumKindWord(kind: AlbumKind): String =
    stringResource(
        when (kind) {
            AlbumKind.ALBUMS -> R.string.requests_album_kind_album
            AlbumKind.SINGLES -> R.string.requests_album_kind_single
            AlbumKind.LIVE -> R.string.requests_album_kind_live
            AlbumKind.COMPILATIONS -> R.string.requests_album_kind_compilation
            AlbumKind.OTHER -> R.string.requests_album_kind_other
        },
    )

@Composable
fun askedWhat(ask: Ask): String =
    when (ask.kind) {
        "show" ->
            when {
                ask.futureOnly -> stringResource(R.string.requests_seasons_future)
                ask.seasons.isEmpty() && ask.episodes.isEmpty() -> stringResource(R.string.requests_seasons_whole)
                else -> ask.seasonWords.ifBlank { seasonsText(ask.seasons) }
            }
        "artist" -> ask.albumWords.ifBlank { stringResource(R.string.requests_albums_every) }
        else -> ""
    }

@Composable
fun factsLine(ask: Ask): String {
    val tag = ask.qualityTagLine?.let { qualityName(it, ask.qualityName) }
    return listOfNotNull(
        tag,
        kindWord(ask.kind),
        ask.year.takeIf { it > 0 }?.toString(),
        askedWhat(ask).ifBlank { null },
    ).joinToString(" · ")
}

@Composable
fun memberStatus(ask: Ask): String =
    when {
        ask.state == "asked" -> stringResource(R.string.requests_status_requested)
        ask.state == "turned_down" -> stringResource(R.string.requests_status_declined)
        ask.state == "available" ->
            stringResource(
                if (ask.isArtist) R.string.requests_status_ready_listen else R.string.requests_status_ready_watch,
            )
        ask.isFailed -> stringResource(R.string.requests_status_failed)
        else -> stringResource(R.string.requests_status_approved)
    }

@Composable
fun stepText(ask: Ask): String? =
    when (ask.stepKind) {
        StepKind.SEARCHING -> stringResource(R.string.requests_step_searching)
        StepKind.DOWNLOADING -> stringResource(R.string.requests_step_downloading)
        StepKind.IMPORTING -> stringResource(R.string.requests_step_importing)
        StepKind.WAITING_ON_ADMIN -> stringResource(R.string.requests_step_admin)
        StepKind.NOT_OUT -> stringResource(R.string.requests_step_not_out)
        StepKind.WAITING ->
            ask.step
                ?.searchedAt
                ?.let { relativeTime(it) }
                ?.let { stringResource(R.string.requests_step_nothing_found, it) } ?: stringResource(R.string.requests_step_not_looked)
        StepKind.NONE -> null
    }

@Composable
fun failedText(ask: Ask): String = ask.problem.ifBlank { stringResource(R.string.requests_failed_not_added) }

@Composable
fun keepsText(ask: Ask): String =
    stringResource(
        when (ask.kind) {
            "show" -> R.string.requests_keeps_show
            "artist" -> R.string.requests_keeps_artist
            else -> R.string.requests_keeps_movie
        },
    )

@Composable
fun askErrorText(error: ApiError): String {
    val failure = askFailure(error)
    return when (failure) {
        AskFailure.WEEKLY_LIMIT -> {
            val seconds = error.retryAfterSeconds
            if (seconds != null) {
                stringResource(R.string.requests_error_limit_wait, waitText(waitTime(seconds * 1000L)))
            } else {
                stringResource(R.string.requests_error_limit)
            }
        }
        AskFailure.NEVER_ADD -> stringResource(R.string.requests_error_never_add)
        AskFailure.IN_LIBRARY -> stringResource(R.string.requests_error_in_library)
        AskFailure.AGE_LIMIT -> stringResource(R.string.requests_error_age)
        AskFailure.NO_MUSIC -> stringResource(R.string.requests_error_no_music)
        AskFailure.ALREADY_DECIDED -> stringResource(R.string.requests_error_decided)
        AskFailure.ASKED_AGAIN -> stringResource(R.string.requests_error_asked_again)
        AskFailure.NOT_INCLUDED -> stringResource(R.string.requests_error_not_included)
        AskFailure.OTHER -> if (error.message.isNotBlank() && error.status in 400..499) error.message else errorText(error)
    }
}

@Composable
fun reportKindLabel(kind: ReportKind): String =
    stringResource(
        when (kind) {
            ReportKind.PICTURE -> R.string.requests_report_kind_picture
            ReportKind.SOUND -> R.string.requests_report_kind_sound
            ReportKind.SUBTITLES -> R.string.requests_report_kind_subtitles
            ReportKind.WONT_PLAY -> R.string.requests_report_kind_wont_play
            ReportKind.OTHER -> R.string.requests_report_kind_other
        },
    )

@Composable
fun reportKindHint(kind: ReportKind): String =
    stringResource(
        when (kind) {
            ReportKind.PICTURE -> R.string.requests_report_hint_picture
            ReportKind.SOUND -> R.string.requests_report_hint_sound
            ReportKind.SUBTITLES -> R.string.requests_report_hint_subtitles
            ReportKind.WONT_PLAY -> R.string.requests_report_hint_wont_play
            ReportKind.OTHER -> R.string.requests_report_hint_other
        },
    )

@Composable
fun regionName(code: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return runCatching {
        java.util.Locale
            .Builder()
            .setRegion(code)
            .build()
            .getDisplayCountry(locale)
    }.getOrNull()?.ifBlank { null } ?: code
}

@Composable
fun titleWithYear(ask: Ask?): String {
    ask ?: return ""
    return if (ask.year > 0) "${ask.name} (${ask.year})" else ask.name
}

@Composable
fun requestsNoteText(note: RequestsNote): String {
    val error = note.error?.let { askErrorText(it) }.orEmpty()
    return when (note.kind) {
        RequestsNoteKind.APPROVED -> stringResource(R.string.requests_note_approved, note.name)
        RequestsNoteKind.DECLINED -> stringResource(R.string.requests_note_declined, note.name)
        RequestsNoteKind.APPROVED_MANY -> pluralStringResource(R.plurals.requests_note_approved_many, note.count, note.count)
        RequestsNoteKind.APPROVE_FAILED -> stringResource(R.string.requests_note_approve_failed, note.name, error)
        RequestsNoteKind.DECLINE_FAILED -> stringResource(R.string.requests_note_decline_failed, note.name, error)
        RequestsNoteKind.ALREADY_DECIDED -> {
            val head =
                stringResource(
                    when (note.state) {
                        "turned_down" -> R.string.requests_note_already_declined
                        "approved", "available" -> R.string.requests_note_already_approved
                        else -> R.string.requests_note_already_answered
                    },
                    note.name,
                )
            if (note.by.isNotBlank()) stringResource(R.string.requests_note_by, head, note.by) else head
        }
        RequestsNoteKind.NOTICE -> note.text
        RequestsNoteKind.REMOVED -> pluralStringResource(R.plurals.requests_note_removed, note.count, note.count)
        RequestsNoteKind.NOTHING_TO_REMOVE -> stringResource(R.string.requests_note_nothing_removed)
        RequestsNoteKind.REMOVE_FAILED -> stringResource(R.string.requests_note_remove_failed, error)
        RequestsNoteKind.TAKEN_BACK -> stringResource(R.string.requests_note_taken_back, note.name)
        RequestsNoteKind.TAKE_BACK_FAILED -> stringResource(R.string.requests_note_remove_failed, error)
    }
}

val RequestsNote.failed: Boolean
    get() =
        when (kind) {
            RequestsNoteKind.APPROVE_FAILED,
            RequestsNoteKind.DECLINE_FAILED,
            RequestsNoteKind.REMOVE_FAILED,
            RequestsNoteKind.TAKE_BACK_FAILED,
            -> true
            else -> false
        }

enum class AskLineKind {
    MUTED,
    NOTE,
    FAIL,
}

data class AskLine(
    val text: String,
    val kind: AskLineKind = AskLineKind.MUTED,
)

@Composable
fun askLines(
    ask: Ask,
    approver: Boolean,
    myId: Long,
): List<AskLine> {
    val lines = mutableListOf<AskLine>()

    fun muted(text: String) {
        lines += AskLine(text)
    }
    val ago = relativeTime(ask.askedAt).orEmpty()
    val askedBy = stringResource(R.string.requests_asked_by, namesText(ask.askerNames))
    muted(if (ago.isNotEmpty()) "$askedBy · $ago" else askedBy)
    if (ask.viaPlugin.isNotBlank()) muted(stringResource(R.string.requests_via, ask.viaPlugin))
    val watch = ask.people.filter { it.fromWatchlist }
    if (watch.isNotEmpty()) {
        muted(
            when {
                ask.fromWatchlistCount > 1 -> stringResource(R.string.requests_watchlist_many, ask.fromWatchlistCount)
                watch[0].id == myId -> stringResource(R.string.requests_watchlist_yours)
                else -> stringResource(R.string.requests_watchlist_of, watch[0].name)
            },
        )
    }
    val decider = ask.deciderName
    val decided = relativeTime(ask.decidedAt)
    when {
        ask.autoApproved && ask.state != "asked" -> muted(stringResource(R.string.requests_auto_approved))
        ask.state == "turned_down" && decider.isNotBlank() -> muted(stringResource(R.string.requests_declined_by, decider))
        ask.state != "asked" && ask.state != "turned_down" && decider.isNotBlank() ->
            muted(stringResource(R.string.requests_approved_by, decider))
        decided != null && ask.state == "turned_down" -> muted(stringResource(R.string.requests_declined_at, decided))
    }
    if (ask.note.isNotBlank()) lines += AskLine("\u201c${ask.note}\u201d", AskLineKind.NOTE)
    if (approver && ask.alsoDeclined.isNotEmpty()) {
        muted(stringResource(R.string.requests_declined_before, namesText(ask.alsoDeclined.map { it.displayName.ifBlank { it.name } })))
    }
    when {
        ask.isFailed -> lines += AskLine(failedText(ask), AskLineKind.FAIL)
        ask.state == "approved" -> {
            muted(stepText(ask) ?: keepsText(ask))
            if (approver && ask.step?.text?.isNotBlank() == true) muted(ask.step?.text.orEmpty())
        }
    }
    return lines
}

data class CalmWords(
    val title: String,
    val message: String?,
)

@Composable
fun calmWords(state: RequestsState): CalmWords {
    val filtered = state.approver && state.filtersOn > 0
    return when {
        filtered ->
            CalmWords(
                state.stage?.let { stringResource(R.string.requests_filtered_stage, stageLabel(it)) }
                    ?: stringResource(R.string.requests_filtered_kind, kindFilterLabel(state.kind)),
                stringResource(R.string.requests_filtered_hidden),
            )
        state.approver ->
            when (state.approverTab) {
                ApproverTab.WAITING ->
                    CalmWords(stringResource(R.string.requests_calm_caught_up), stringResource(R.string.requests_calm_waiting))
                ApproverTab.PROCESSING ->
                    CalmWords(stringResource(R.string.requests_calm_processing_title), stringResource(R.string.requests_calm_processing))
                ApproverTab.DONE ->
                    CalmWords(stringResource(R.string.requests_calm_done_title), stringResource(R.string.requests_calm_done))
            }
        else ->
            CalmWords(
                when (state.memberTab) {
                    MemberTab.WAITING -> stringResource(R.string.requests_calm_caught_up)
                    MemberTab.FAILED -> stringResource(R.string.requests_calm_nothing_failed)
                    else -> stringResource(R.string.requests_calm_nothing_yet, memberTabLabel(state.memberTab))
                },
                null,
            )
    }
}
