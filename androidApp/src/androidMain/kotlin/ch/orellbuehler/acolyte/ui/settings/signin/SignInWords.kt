package ch.orellbuehler.acolyte.ui.settings.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInFixKind
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInJoin
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInLineProblem
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInNotice
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInNoticeKind
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInPerson
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInPresenter
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInRole
import ch.orellbuehler.acolyte.feature.settings.signin.SettingsSignInWayKind
import ch.orellbuehler.acolyte.ui.components.errorText
import ch.orellbuehler.acolyte.ui.library.PresenterViewModel

internal typealias SignInModel = PresenterViewModel<SettingsSignInPresenter>

private val DETAIL_STATUSES = setOf(400, 409, 422, 502, 503)

@Composable
internal fun failureText(error: ApiError): String {
    val base =
        if (error.code == ApiErrorCode.FORBIDDEN) stringResource(R.string.settings_signin_sign_in_to_change) else errorText(error)
    val detail = error.message.takeIf { error.status in DETAIL_STATUSES && it.isNotBlank() }
    return if (detail == null) base else "$base\n$detail"
}

@Composable
internal fun noticeMessage(notice: SettingsSignInNotice): String {
    val error = notice.error
    return when (notice.kind) {
        SettingsSignInNoticeKind.MIN_SAVED ->
            stringResource(R.string.settings_signin_min_saved) + "\n" + stringResource(R.string.settings_signin_characters, notice.count)
        SettingsSignInNoticeKind.TWO_FACTOR_REQUIRED -> stringResource(R.string.settings_signin_tf_required)
        SettingsSignInNoticeKind.TWO_FACTOR_OPTIONAL -> stringResource(R.string.settings_signin_tf_optional)
        SettingsSignInNoticeKind.FORGOT_SHOWN -> stringResource(R.string.settings_signin_forgot_shown)
        SettingsSignInNoticeKind.FORGOT_HIDDEN -> stringResource(R.string.settings_signin_forgot_hidden)
        SettingsSignInNoticeKind.WAY_SAVED -> stringResource(R.string.settings_signin_way_saved, notice.name)
        SettingsSignInNoticeKind.WAY_UNREACHABLE ->
            stringResource(R.string.settings_signin_way_unreachable, notice.name) + "\n" +
                stringResource(R.string.settings_signin_way_not_working)
        SettingsSignInNoticeKind.PLEX_LINKED -> stringResource(R.string.settings_signin_plex_linked, notice.name)
        SettingsSignInNoticeKind.PLEX_UNLINKED -> stringResource(R.string.settings_signin_plex_unlinked)
        SettingsSignInNoticeKind.IMPORTED ->
            pluralStringResource(R.plurals.settings_signin_made, notice.count, notice.count) + "\n" + notice.name
        SettingsSignInNoticeKind.IMPORT_NONE -> stringResource(R.string.settings_signin_nobody_new)
        SettingsSignInNoticeKind.SSO_SAVED -> {
            val gone = mutableListOf<String>()
            if (notice.count > 0) gone += pluralStringResource(R.plurals.settings_signin_sso_links_removed, notice.count, notice.count)
            if (notice.count2 > 0) gone += stringResource(R.string.settings_signin_sso_signed_out, notice.count2)
            val saved = stringResource(R.string.settings_signin_sso_saved)
            if (gone.isEmpty()) saved else saved + "\n" + gone.joinToString(", ")
        }
        SettingsSignInNoticeKind.LOCAL_ON -> stringResource(R.string.settings_signin_skips)
        SettingsSignInNoticeKind.LOCAL_OFF -> stringResource(R.string.settings_signin_needed_everywhere)
        SettingsSignInNoticeKind.LOCAL_SAVED -> stringResource(R.string.settings_signin_local_saved)
        SettingsSignInNoticeKind.HOSTS_SAVED -> stringResource(R.string.settings_signin_hosts_saved)
        SettingsSignInNoticeKind.UNLINK_FAILED ->
            stringResource(R.string.settings_signin_unlink_failed) + (error?.let { "\n" + failureText(it) }.orEmpty())
        SettingsSignInNoticeKind.FAILED ->
            stringResource(R.string.settings_signin_couldnt_save) + (error?.let { "\n" + failureText(it) }.orEmpty())
    }
}

@Composable
internal fun fixText(problem: SettingsSignInLineProblem): String =
    when (problem.kind) {
        SettingsSignInFixKind.PART -> stringResource(R.string.settings_signin_fix_part)
        SettingsSignInFixKind.MISSING -> stringResource(R.string.settings_signin_fix_missing, problem.fix)
        SettingsSignInFixKind.LETTER -> stringResource(R.string.settings_signin_fix_letter, problem.letter, problem.fix)
        SettingsSignInFixKind.EXTRA -> stringResource(R.string.settings_signin_fix_extra)
        SettingsSignInFixKind.BITS -> stringResource(R.string.settings_signin_fix_bits)
        SettingsSignInFixKind.WIDE -> stringResource(R.string.settings_signin_fix_wide)
        SettingsSignInFixKind.BRIDGE -> stringResource(R.string.settings_signin_fix_bridge)
        SettingsSignInFixKind.NOT_ADDRESS -> stringResource(R.string.settings_signin_fix_not)
    }

@Composable
internal fun roleLine(person: SettingsSignInPerson): String =
    when {
        person.kidLimit.isNotEmpty() -> stringResource(R.string.settings_signin_kid_line, person.kidLimit)
        person.role == SettingsSignInRole.OWNER -> stringResource(R.string.settings_signin_owner_line)
        person.role == SettingsSignInRole.ADMIN -> stringResource(R.string.settings_signin_admin_line)
        else -> stringResource(R.string.settings_signin_member_line)
    }

@Composable
internal fun joinOptions(kind: SettingsSignInWayKind): List<Pair<SettingsSignInJoin, String>> =
    listOf(
        SettingsSignInJoin.NOBODY to stringResource(R.string.settings_signin_join_nobody),
        SettingsSignInJoin.LIST to stringResource(R.string.settings_signin_join_list),
        SettingsSignInJoin.EVERYONE to
            if (kind == SettingsSignInWayKind.PLEX) {
                stringResource(R.string.settings_signin_join_everyone_plex)
            } else {
                stringResource(R.string.settings_signin_join_everyone, kind.app)
            },
    )
