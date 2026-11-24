package ch.orellbuehler.acolyte.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.library.ActiveChip
import ch.orellbuehler.acolyte.feature.library.ActiveFilter
import ch.orellbuehler.acolyte.feature.library.FactKind
import ch.orellbuehler.acolyte.feature.library.LibState
import ch.orellbuehler.acolyte.feature.library.LibStateKey
import ch.orellbuehler.acolyte.feature.library.LibraryFact
import ch.orellbuehler.acolyte.feature.library.LibraryKind
import ch.orellbuehler.acolyte.feature.library.LibrarySort

@Composable
fun stateLabel(state: LibState): String =
    when (state.key) {
        LibStateKey.FAILED, LibStateKey.NEEDS_DECISION ->
            if (state.key == LibStateKey.FAILED && state.count > 0) {
                stringResource(R.string.library_state_failed_n, state.count)
            } else {
                val parts = mutableListOf<String>()
                if (state.fileNotFound) parts += stringResource(R.string.library_state_file_not_found)
                parts += state.reasons
                val none =
                    stringResource(
                        if (state.key == LibStateKey.FAILED) R.string.library_state_failed_none else R.string.library_state_decision_none,
                    )
                if (state.noReason || parts.isEmpty()) parts += none
                parts.joinToString("; ")
            }
        LibStateKey.STALLED -> stringResource(R.string.library_state_stalled, state.percent.coerceAtLeast(0))
        LibStateKey.MISSING_FILE -> stringResource(R.string.library_state_missing_file)
        LibStateKey.MISSING ->
            if (state.count >
                0
            ) {
                stringResource(R.string.library_state_missing_n, state.count)
            } else {
                stringResource(R.string.library_state_missing)
            }
        LibStateKey.BETTER_WANTED ->
            when {
                state.count > 0 -> stringResource(R.string.library_state_better_n, state.count)
                state.quality.isNotEmpty() -> stringResource(R.string.library_state_better_q, state.quality)
                else -> stringResource(R.string.library_state_better)
            }
        LibStateKey.PAUSED -> stringResource(R.string.library_state_paused, state.percent.coerceAtLeast(0))
        LibStateKey.DOWNLOADING ->
            when {
                state.importing -> stringResource(R.string.library_state_importing)
                state.count > 0 -> stringResource(R.string.library_state_downloading_n, state.count)
                else -> stringResource(R.string.library_state_downloading, state.percent.coerceAtLeast(0))
            }
        LibStateKey.QUEUED -> stringResource(R.string.library_state_queued)
        LibStateKey.APPROVED -> stringResource(R.string.library_state_searching)
        LibStateKey.WAITING_APPROVAL -> stringResource(R.string.library_state_waiting)
        LibStateKey.NOT_OUT_YET ->
            if (state.date.isNotEmpty()) {
                stringResource(R.string.library_state_upcoming_on, LibraryFormat.day(state.date))
            } else {
                stringResource(R.string.library_state_upcoming)
            }
        LibStateKey.NOT_KEPT ->
            if (state.quality.isNotEmpty()) {
                stringResource(R.string.library_state_not_kept_q, state.quality)
            } else {
                stringResource(R.string.library_state_not_kept)
            }
        LibStateKey.SPECIALS_ONLY -> stringResource(R.string.library_state_specials, state.count)
        LibStateKey.DECLINED -> stringResource(R.string.library_state_declined)
        LibStateKey.IN_LIBRARY ->
            if (state.quality.isNotEmpty()) {
                stringResource(R.string.library_state_in_library_q, state.quality)
            } else {
                stringResource(R.string.library_state_in_library)
            }
        LibStateKey.UP_TO_DATE -> stringResource(R.string.library_state_complete)
        LibStateKey.ENDED -> stringResource(R.string.library_state_ended)
    }

@Composable
fun factText(fact: LibraryFact): String {
    val day = LibraryFormat.day(fact.date)
    return when (fact.kind) {
        FactKind.RELEASED -> stringResource(R.string.library_fact_released, day)
        FactKind.HOME_RELEASE -> stringResource(R.string.library_fact_home, day)
        FactKind.IN_CINEMAS -> stringResource(R.string.library_fact_in_cinemas, day)
        FactKind.CINEMAS -> stringResource(R.string.library_fact_cinemas, day)
        FactKind.RELEASE_DATE -> stringResource(R.string.library_fact_release, day)
        FactKind.NOT_KNOWN -> stringResource(R.string.library_fact_not_known)
        FactKind.EPISODES -> stringResource(R.string.library_fact_episodes, fact.number.toInt(), fact.total.toInt())
        FactKind.TRACKS -> stringResource(R.string.library_fact_tracks, fact.number.toInt(), fact.total.toInt())
        FactKind.NEXT_EPISODE -> stringResource(R.string.library_fact_next, day)
        FactKind.NEXT_RELEASE -> stringResource(R.string.library_fact_next_release, day)
        FactKind.ENDED -> stringResource(R.string.library_fact_ended)
        FactKind.LAST_AIRED -> stringResource(R.string.library_fact_last, day)
        FactKind.ADDED -> stringResource(R.string.library_fact_added, day)
        FactKind.SIZE -> LibraryFormat.bytes(fact.number)
        FactKind.RUNTIME -> stringResource(R.string.library_fact_runtime, (fact.number / 60).toInt(), (fact.number % 60).toInt())
        FactKind.SCORE -> stringResource(R.string.library_fact_score, LibraryFormat.score(fact.score))
        FactKind.YEAR -> fact.number.toString()
        FactKind.NETWORK, FactKind.QUALITY -> fact.text
    }
}

@Composable
fun sortLabel(
    sort: LibrarySort,
    kind: LibraryKind,
): String =
    stringResource(
        when (sort) {
            LibrarySort.STATUS -> R.string.library_sort_status
            LibrarySort.TITLE -> R.string.library_sort_name
            LibrarySort.YEAR -> R.string.library_sort_year
            LibrarySort.SCORE -> R.string.library_sort_score
            LibrarySort.OUT -> R.string.library_sort_release
            LibrarySort.ADDED -> R.string.library_sort_added
            LibrarySort.SIZE -> R.string.library_sort_size
            LibrarySort.RUNTIME -> R.string.library_sort_runtime
            LibrarySort.RULES -> R.string.library_sort_rules
            LibrarySort.NEXT -> if (kind == LibraryKind.ARTIST) R.string.library_sort_next_release else R.string.library_sort_next
            LibrarySort.LAST -> R.string.library_sort_last
            LibrarySort.PROGRESS -> R.string.library_sort_progress
            LibrarySort.NETWORK -> R.string.library_sort_network
        },
    )

@Composable
fun statusFilterLabel(id: String): String =
    stringResource(
        when (id) {
            "missing" -> R.string.library_filter_missing
            "not_out_yet" -> R.string.library_filter_upcoming
            "better" -> R.string.library_filter_better
            "have" -> R.string.library_filter_have
            else -> R.string.library_filter_all
        },
    )

@Composable
fun chipLabel(chip: ActiveChip): String =
    when (chip.filter) {
        ActiveFilter.STATUS -> statusFilterLabel(chip.id)
        ActiveFilter.KEPT -> stringResource(if (chip.id == "on") R.string.library_chip_kept else R.string.library_chip_not_kept)
        ActiveFilter.ASKER ->
            if (chip.id ==
                "me"
            ) {
                stringResource(R.string.library_chip_asked_me)
            } else {
                stringResource(R.string.library_chip_asked_by, chip.label)
            }
        ActiveFilter.SERIES -> stringResource(R.string.library_chip_series, chip.label)
        ActiveFilter.CODEC -> codecLabel(chip.id)
        ActiveFilter.SUBS -> stringResource(R.string.library_chip_subs)
        ActiveFilter.WATCHED -> stringResource(R.string.library_chip_watched)
        ActiveFilter.SEARCH -> stringResource(R.string.library_chip_search, chip.label)
        ActiveFilter.GROUPED -> stringResource(R.string.library_chip_grouped)
    }

@Composable
fun countWord(
    kind: LibraryKind,
    count: Int,
): String =
    when (kind) {
        LibraryKind.SHOW -> pluralStringResource(R.plurals.library_word_show, count, count)
        else -> pluralStringResource(R.plurals.library_word_movie, count, count)
    }
