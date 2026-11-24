package ch.orellbuehler.acolyte.ui.settingsb

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.move.MoveCheckNote
import ch.orellbuehler.acolyte.feature.move.MoveCheckStep
import ch.orellbuehler.acolyte.feature.move.MoveCountKind
import ch.orellbuehler.acolyte.feature.move.MoveCountPart
import ch.orellbuehler.acolyte.feature.move.MoveGate
import ch.orellbuehler.acolyte.feature.move.MoveGateReason
import ch.orellbuehler.acolyte.feature.move.MoveGroup
import ch.orellbuehler.acolyte.feature.move.MovePathProblem
import ch.orellbuehler.acolyte.feature.move.MoveProblem
import ch.orellbuehler.acolyte.feature.move.MoveReachFailure
import ch.orellbuehler.acolyte.feature.move.MoveStateWord
import ch.orellbuehler.acolyte.feature.move.MoveSummaryPart
import ch.orellbuehler.acolyte.feature.move.MoveTone
import ch.orellbuehler.acolyte.feature.move.MoveWord
import ch.orellbuehler.acolyte.ui.components.errorText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun moveWordText(
    word: MoveWord,
    raw: String = "",
): String =
    when (word) {
        MoveWord.WILL_BE_ADDED -> stringResource(R.string.acq_mv_w_will_be_added)
        MoveWord.ADDED -> stringResource(R.string.acq_mv_w_added)
        MoveWord.WILL_USE_THE_ONE -> stringResource(R.string.acq_mv_w_will_use)
        MoveWord.USING_THE_ONE -> stringResource(R.string.acq_mv_w_using)
        MoveWord.WILL_BE_UPDATED -> stringResource(R.string.acq_mv_w_will_update)
        MoveWord.UPDATED -> stringResource(R.string.acq_mv_w_updated)
        MoveWord.MATCHES_ONE_HERE -> stringResource(R.string.acq_mv_w_matches)
        MoveWord.MATCHED -> stringResource(R.string.acq_mv_w_matched)
        MoveWord.ALREADY_HERE -> stringResource(R.string.acq_mv_w_already)
        MoveWord.LEFT_OUT -> stringResource(R.string.acq_mv_w_left_out)
        MoveWord.NO_FOLDER_HERE -> stringResource(R.string.acq_mv_w_no_folder)
        MoveWord.WONT_WORK -> stringResource(R.string.acq_mv_w_wont_work)
        MoveWord.FAILED -> stringResource(R.string.acq_mv_w_failed)
        MoveWord.REMOVED -> stringResource(R.string.acq_mv_w_removed)
        MoveWord.KEPT -> stringResource(R.string.acq_mv_w_kept)
        MoveWord.NOT_REMOVED -> stringResource(R.string.acq_mv_w_not_removed)
        MoveWord.OTHER -> raw
    }

@Composable
fun moveSummaryWord(
    word: MoveWord,
    raw: String,
): String =
    when (word) {
        MoveWord.WILL_BE_ADDED -> stringResource(R.string.acq_mv_s_will_be_added)
        MoveWord.ADDED -> stringResource(R.string.acq_mv_s_added)
        MoveWord.WILL_USE_THE_ONE -> stringResource(R.string.acq_mv_s_will_use)
        MoveWord.USING_THE_ONE -> stringResource(R.string.acq_mv_s_using)
        MoveWord.WILL_BE_UPDATED -> stringResource(R.string.acq_mv_s_will_update)
        MoveWord.UPDATED -> stringResource(R.string.acq_mv_s_updated)
        MoveWord.MATCHES_ONE_HERE -> stringResource(R.string.acq_mv_s_matches)
        MoveWord.MATCHED -> stringResource(R.string.acq_mv_s_matched)
        MoveWord.ALREADY_HERE -> stringResource(R.string.acq_mv_s_already)
        MoveWord.LEFT_OUT -> stringResource(R.string.acq_mv_s_left_out)
        MoveWord.NO_FOLDER_HERE -> stringResource(R.string.acq_mv_s_no_folder)
        MoveWord.WONT_WORK -> stringResource(R.string.acq_mv_s_wont_work)
        MoveWord.FAILED -> stringResource(R.string.acq_mv_s_failed)
        MoveWord.REMOVED -> stringResource(R.string.acq_mv_s_removed)
        MoveWord.KEPT -> stringResource(R.string.acq_mv_s_kept)
        MoveWord.NOT_REMOVED -> stringResource(R.string.acq_mv_s_not_removed)
        MoveWord.OTHER -> raw
    }

@Composable
fun moveSummaryText(parts: List<MoveSummaryPart>): String {
    val texts = mutableListOf<String>()
    for (it in parts) texts += "${it.n} ${moveSummaryWord(it.word, it.raw)}"
    return texts.joinToString(" · ")
}

@Composable
fun moveCountText(part: MoveCountPart): String {
    val plural =
        when (part.kind) {
            MoveCountKind.MOVIE -> R.plurals.acq_mv_c_movie
            MoveCountKind.SHOW -> R.plurals.acq_mv_c_show
            MoveCountKind.ARTIST -> R.plurals.acq_mv_c_artist
            MoveCountKind.TITLE -> R.plurals.acq_mv_c_title
            MoveCountKind.FOLDER -> R.plurals.acq_mv_c_folder
            MoveCountKind.PICK_RULES -> R.plurals.acq_mv_c_pick_rules
            MoveCountKind.TRAIT -> R.plurals.acq_mv_c_trait
            MoveCountKind.WAIT_RULE -> R.plurals.acq_mv_c_wait_rule
            MoveCountKind.SOURCE -> R.plurals.acq_mv_c_source
            MoveCountKind.DOWNLOADER -> R.plurals.acq_mv_c_downloader
            MoveCountKind.OUTSIDE_LIST -> R.plurals.acq_mv_c_outside_list
            MoveCountKind.ALERT -> R.plurals.acq_mv_c_alert
            MoveCountKind.LINKED_APP -> R.plurals.acq_mv_c_linked_app
            MoveCountKind.PERSON -> R.plurals.acq_mv_c_person
            MoveCountKind.REQUEST -> R.plurals.acq_mv_c_request
            MoveCountKind.TAG -> R.plurals.acq_mv_c_tag
            MoveCountKind.SUBTITLE_LANGUAGE -> R.plurals.acq_mv_c_subtitle_language
            MoveCountKind.SUBTITLE_SET -> R.plurals.acq_mv_c_subtitle_set
            MoveCountKind.SUBTITLE_SITE -> R.plurals.acq_mv_c_subtitle_site
            MoveCountKind.KEY_TO_RETYPE -> R.plurals.acq_mv_c_key_again
        }
    return pluralStringResource(plural, part.n, part.n)
}

@Composable
fun moveCountsText(parts: List<MoveCountPart>): String {
    val texts = mutableListOf<String>()
    for (part in parts) texts += moveCountText(part)
    return texts.joinToString(" · ")
}

@Composable
fun moveGroupName(
    group: MoveGroup,
    raw: String,
): String =
    when (group) {
        MoveGroup.FOLDERS -> stringResource(R.string.acq_mv_g_folders)
        MoveGroup.PICK_RULES -> stringResource(R.string.acq_mv_g_pick_rules)
        MoveGroup.SOURCES -> stringResource(R.string.acq_mv_g_sources)
        MoveGroup.TITLES -> stringResource(R.string.acq_mv_g_titles)
        MoveGroup.NEVER_ADD -> stringResource(R.string.acq_mv_g_never_add)
        MoveGroup.OUTSIDE_LISTS -> stringResource(R.string.acq_mv_g_outside_lists)
        MoveGroup.DOWNLOADERS -> stringResource(R.string.acq_mv_g_downloaders)
        MoveGroup.TURNED_DOWN -> stringResource(R.string.acq_mv_g_turned_down)
        MoveGroup.NAMING -> stringResource(R.string.acq_mv_g_naming)
        MoveGroup.SUBTITLES -> stringResource(R.string.acq_mv_g_subtitles)
        MoveGroup.PEOPLE -> stringResource(R.string.acq_mv_g_people)
        MoveGroup.WISHLIST -> stringResource(R.string.acq_mv_g_wishlist)
        MoveGroup.ALERTS -> stringResource(R.string.acq_mv_g_alerts)
        MoveGroup.OTHER -> stringResource(R.string.acq_mv_g_other)
        MoveGroup.UNKNOWN -> raw
    }

@Composable
fun moveAlsoName(group: MoveGroup): String =
    when (group) {
        MoveGroup.FOLDERS -> stringResource(R.string.acq_mv_also_folders)
        MoveGroup.PICK_RULES -> stringResource(R.string.acq_mv_also_pick_rules)
        MoveGroup.SOURCES -> stringResource(R.string.acq_mv_also_sources)
        MoveGroup.NEVER_ADD -> stringResource(R.string.acq_mv_also_never_add)
        MoveGroup.OUTSIDE_LISTS -> stringResource(R.string.acq_mv_also_outside_lists)
        MoveGroup.DOWNLOADERS -> stringResource(R.string.acq_mv_also_downloaders)
        MoveGroup.TURNED_DOWN -> stringResource(R.string.acq_mv_also_turned_down)
        MoveGroup.NAMING -> stringResource(R.string.acq_mv_also_naming)
        MoveGroup.SUBTITLES -> stringResource(R.string.acq_mv_also_subtitles)
        MoveGroup.ALERTS -> stringResource(R.string.acq_mv_also_alerts)
        else -> stringResource(R.string.acq_mv_also_other)
    }

@Composable
fun moveAndList(items: List<String>): String =
    when {
        items.isEmpty() -> ""
        items.size == 1 -> items[0]
        else -> stringResource(R.string.acq_mv_and_list, items.dropLast(1).joinToString(", "), items.last())
    }

@Composable
fun moveStateText(word: MoveStateWord): String =
    when (word) {
        MoveStateWord.WAITING -> stringResource(R.string.acq_mv_state_waiting)
        MoveStateWord.IMPORTING -> stringResource(R.string.acq_mv_state_importing)
        MoveStateWord.IMPORTED -> stringResource(R.string.acq_mv_state_imported)
        MoveStateWord.STOPPED -> stringResource(R.string.acq_mv_state_stopped)
        MoveStateWord.UNDONE -> stringResource(R.string.acq_mv_state_undone)
        MoveStateWord.UNDOING -> stringResource(R.string.acq_mv_state_undoing)
        MoveStateWord.UNDO_FAILED -> stringResource(R.string.acq_mv_state_undo_failed)
    }

fun moveTone(tone: MoveTone): Tone =
    when (tone) {
        MoveTone.OK -> Tone.OK
        MoveTone.WARN -> Tone.WAIT
        MoveTone.BAD -> Tone.FAIL
        MoveTone.QUIET -> Tone.IDLE
    }

@Composable
fun moveCheckText(note: MoveCheckNote): String =
    if (note.counting) {
        buildString {
            append(stringResource(R.string.acq_mv_check_of, note.done, note.total))
            if (note.now.isNotEmpty()) append(" · ${note.now}")
            if (note.issues > 0) append(" · ${stringResource(R.string.acq_mv_check_issues, note.issues)}")
        }
    } else {
        when (note.step) {
            MoveCheckStep.TITLES -> stringResource(R.string.acq_mv_check_titles)
            MoveCheckStep.FOLDERS -> stringResource(R.string.acq_mv_check_folders)
            MoveCheckStep.READING -> stringResource(R.string.acq_mv_check_reading)
        }
    }

@Composable
fun moveGateText(
    gate: MoveGate,
    note: MoveCheckNote?,
): String =
    when (gate.reason) {
        MoveGateReason.NONE -> ""
        MoveGateReason.TURN_ON_READ -> stringResource(R.string.acq_mv_gate_turn_on_read)
        MoveGateReason.UPDATING -> stringResource(R.string.acq_mv_gate_updating)
        MoveGateReason.PROGRESS -> note?.let { moveCheckText(it) } ?: stringResource(R.string.acq_mv_gate_updating)
        MoveGateReason.FIX_FOLDER -> stringResource(R.string.acq_mv_gate_fix_folder)
        MoveGateReason.FIX_RULE -> stringResource(R.string.acq_mv_gate_fix_rule)
        MoveGateReason.NOTHING_NEW -> stringResource(R.string.acq_mv_gate_nothing_new)
        MoveGateReason.TURN_ON_GROUP -> stringResource(R.string.acq_mv_gate_turn_on_group)
        MoveGateReason.SAFE_MODE -> stringResource(R.string.acq_mv_gate_safe_mode)
        MoveGateReason.TEST_FAILED -> stringResource(R.string.acq_mv_gate_test_failed)
        MoveGateReason.CHANGED_SINCE_TEST -> stringResource(R.string.acq_mv_gate_changed)
        MoveGateReason.TRYING -> stringResource(R.string.acq_mv_gate_trying)
        MoveGateReason.ONE_NEEDS_CHOICE -> stringResource(R.string.acq_mv_gate_one_choice)
        MoveGateReason.MANY_NEED_CHOICE -> stringResource(R.string.acq_mv_gate_many_choice, gate.n)
    }

@Composable
fun movePathProblemText(problem: MovePathProblem): String =
    when (problem) {
        MovePathProblem.NONE -> ""
        MovePathProblem.FILL_BOTH -> stringResource(R.string.acq_mv_p_fill_both)
        MovePathProblem.FULL_PATH -> stringResource(R.string.acq_mv_p_full_path)
        MovePathProblem.DOTS -> stringResource(R.string.acq_mv_p_dots)
        MovePathProblem.TYPE_PATH -> stringResource(R.string.acq_mv_p_type_path)
    }

@Composable
fun moveProblemText(problem: MoveProblem): String {
    val error = problem.error
    if (error.code == ApiErrorCode.CONFLICT && error.message.contains("safe mode", ignoreCase = true)) {
        return stringResource(R.string.acq_mv_safe_mode)
    }
    return when (problem.reach) {
        MoveReachFailure.TRY_HTTP -> stringResource(R.string.acq_mv_reach_try_http)
        MoveReachFailure.CANT_REACH -> stringResource(R.string.acq_mv_reach_cant)
        MoveReachFailure.TIMEOUT -> stringResource(R.string.acq_mv_reach_timeout)
        MoveReachFailure.TOO_BIG -> stringResource(R.string.acq_mv_reach_too_big)
        MoveReachFailure.REFUSED_KEY -> stringResource(R.string.acq_mv_reach_refused)
        MoveReachFailure.WRONG_APP -> stringResource(R.string.acq_mv_reach_wrong_app)
        MoveReachFailure.UNSUPPORTED -> stringResource(R.string.acq_mv_reach_unsupported)
        MoveReachFailure.NOT_OWNER -> stringResource(R.string.acq_mv_reach_owner)
        MoveReachFailure.SAFE_MODE -> stringResource(R.string.acq_mv_safe_mode)
        MoveReachFailure.OTHER ->
            if (error.code == ApiErrorCode.BAD_REQUEST && error.message.isNotEmpty()) error.message else errorText(error)
    }
}

fun moveDate(iso: String): String =
    try {
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(iso))
    } catch (e: Exception) {
        iso
    }

fun moveShortDate(iso: String): String =
    try {
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(iso))
    } catch (e: Exception) {
        iso
    }
