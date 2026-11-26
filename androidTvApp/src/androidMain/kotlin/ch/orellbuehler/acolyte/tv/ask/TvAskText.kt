package ch.orellbuehler.acolyte.tv.ask

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.feature.requests.AskFailure
import ch.orellbuehler.acolyte.feature.requests.AskOutcome
import ch.orellbuehler.acolyte.feature.requests.FOUR_K_LINE
import ch.orellbuehler.acolyte.feature.requests.WaitTime
import ch.orellbuehler.acolyte.feature.requests.askFailure
import ch.orellbuehler.acolyte.feature.requests.waitTime
import ch.orellbuehler.acolyte.tv.R
import ch.orellbuehler.acolyte.ui.components.errorText

@Composable
fun tvOutcomeTitle(
    outcome: AskOutcome,
    name: String,
    fourK: Boolean,
): String =
    when (outcome) {
        AskOutcome.APPROVED -> stringResource(R.string.tv_requests_outcome_approved, name)
        AskOutcome.JOINED, AskOutcome.ALREADY -> stringResource(R.string.tv_requests_outcome_already, name)
        AskOutcome.TURNED_DOWN -> stringResource(R.string.tv_requests_outcome_turned_down, name)
        AskOutcome.REQUESTED ->
            stringResource(if (fourK) R.string.tv_requests_outcome_requested_four_k else R.string.tv_requests_outcome_requested, name)
    }

@Composable
fun tvOutcomeMessage(outcome: AskOutcome): String =
    stringResource(
        when (outcome) {
            AskOutcome.APPROVED -> R.string.tv_requests_outcome_searching
            AskOutcome.JOINED, AskOutcome.ALREADY -> R.string.tv_requests_outcome_on_it
            AskOutcome.TURNED_DOWN -> R.string.tv_requests_outcome_on_services
            AskOutcome.REQUESTED -> R.string.tv_requests_outcome_waiting
        },
    )

@Composable
fun tvQualityName(
    line: String,
    name: String,
): String =
    name.ifBlank {
        stringResource(if (line == FOUR_K_LINE) R.string.tv_requests_quality_four_k else R.string.tv_requests_quality_standard)
    }

@Composable
fun tvSeasonsText(seasons: List<Int>): String {
    if (seasons.isEmpty()) return stringResource(R.string.tv_requests_seasons_whole)
    val regular = seasons.filter { it > 0 }.sorted()
    val parts = mutableListOf<String>()
    if (0 in seasons) parts += stringResource(R.string.tv_requests_season_specials)
    if (regular.size == 1) parts += stringResource(R.string.tv_requests_season_one, regular[0])
    if (regular.size > 1) parts += stringResource(R.string.tv_requests_season_many, regular.joinToString(", "))
    return parts.joinToString(", ")
}

@Composable
fun tvAskSeasonName(number: Int): String =
    if (number == 0) stringResource(R.string.tv_requests_season_specials) else stringResource(R.string.tv_requests_season_one, number)

@Composable
private fun waitText(wait: WaitTime?): String {
    if (wait == null) return stringResource(R.string.tv_requests_wait_few_days)
    if (wait.underMinute) return stringResource(R.string.tv_requests_wait_minute)
    val days = pluralStringResource(R.plurals.tv_requests_days, wait.days, wait.days)
    val hours = pluralStringResource(R.plurals.tv_requests_hours, wait.hours, wait.hours)
    val minutes = pluralStringResource(R.plurals.tv_requests_minutes, wait.minutes, wait.minutes)
    return when {
        wait.days > 0 && wait.hours > 0 -> stringResource(R.string.tv_requests_wait_and, days, hours)
        wait.days > 0 -> days
        wait.hours > 0 && wait.minutes > 0 -> stringResource(R.string.tv_requests_wait_and, hours, minutes)
        wait.hours > 0 -> hours
        else -> minutes
    }
}

@Composable
fun tvAskErrorText(error: ApiError): String =
    when (askFailure(error)) {
        AskFailure.WEEKLY_LIMIT -> {
            val seconds = error.retryAfterSeconds
            if (seconds != null) {
                stringResource(R.string.tv_requests_error_limit_wait, waitText(waitTime(seconds * 1000L)))
            } else {
                stringResource(R.string.tv_requests_error_limit)
            }
        }
        AskFailure.NEVER_ADD -> stringResource(R.string.tv_requests_error_never_add)
        AskFailure.IN_LIBRARY -> stringResource(R.string.tv_requests_error_in_library)
        AskFailure.AGE_LIMIT -> stringResource(R.string.tv_requests_error_age)
        AskFailure.NO_MUSIC -> stringResource(R.string.tv_requests_error_no_music)
        AskFailure.ALREADY_DECIDED -> stringResource(R.string.tv_requests_error_decided)
        AskFailure.ASKED_AGAIN -> stringResource(R.string.tv_requests_error_asked_again)
        AskFailure.NOT_INCLUDED -> stringResource(R.string.tv_requests_error_not_included)
        AskFailure.OTHER -> if (error.message.isNotBlank() && error.status in 400..499) error.message else errorText(error)
    }
