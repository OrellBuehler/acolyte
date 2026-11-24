package ch.orellbuehler.acolyte.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.session.SignInError
import ch.orellbuehler.acolyte.session.SignInErrorCode

@StringRes
fun ApiErrorCode.messageRes(): Int =
    when (this) {
        ApiErrorCode.OFFLINE -> R.string.error_offline
        ApiErrorCode.TIMEOUT -> R.string.error_timeout
        ApiErrorCode.TLS -> R.string.error_tls
        ApiErrorCode.UNAUTHORIZED -> R.string.error_unauthorized
        ApiErrorCode.FORBIDDEN -> R.string.error_forbidden
        ApiErrorCode.NOT_FOUND -> R.string.error_not_found
        ApiErrorCode.BAD_REQUEST -> R.string.error_bad_request
        ApiErrorCode.CONFLICT -> R.string.error_conflict
        ApiErrorCode.PART_OFF -> R.string.error_part_off
        ApiErrorCode.HANDS_OFF -> R.string.error_hands_off
        ApiErrorCode.REAUTH -> R.string.error_reauth
        ApiErrorCode.RATE_LIMITED -> R.string.error_rate_limited
        ApiErrorCode.SERVER -> R.string.error_server
        ApiErrorCode.INVALID_RESPONSE -> R.string.error_invalid_response
        ApiErrorCode.UNKNOWN -> R.string.error_unknown
    }

@StringRes
fun SignInErrorCode.messageRes(): Int =
    when (this) {
        SignInErrorCode.INVALID_ADDRESS -> R.string.signin_error_invalid_address
        SignInErrorCode.UNREACHABLE -> R.string.signin_error_unreachable
        SignInErrorCode.TLS -> R.string.signin_error_tls
        SignInErrorCode.NOT_HOLYARRS -> R.string.signin_error_not_holyarrs
        SignInErrorCode.NEEDS_SETUP -> R.string.signin_error_needs_setup
        SignInErrorCode.HOST_REFUSED -> R.string.signin_error_host_refused
        SignInErrorCode.WRONG_CREDENTIALS -> R.string.signin_error_wrong_credentials
        SignInErrorCode.WRONG_CODE -> R.string.signin_error_wrong_code
        SignInErrorCode.SIGN_IN_ENDED -> R.string.signin_error_sign_in_ended
        SignInErrorCode.KEY_REJECTED -> R.string.signin_error_key_rejected
        SignInErrorCode.NOT_ALLOWED -> R.string.signin_error_not_allowed
        SignInErrorCode.WAY_OFF -> R.string.signin_error_way_off
        SignInErrorCode.RATE_LIMITED -> R.string.error_rate_limited
        SignInErrorCode.INVALID_NAME -> R.string.signin_error_invalid_name
        SignInErrorCode.PASSWORD_SHORT -> R.string.signin_error_password_short
        SignInErrorCode.NAME_TAKEN -> R.string.signin_error_name_taken
        SignInErrorCode.OWNER_EXISTS -> R.string.signin_error_owner_exists
        SignInErrorCode.PLEX_TIMEOUT -> R.string.signin_error_plex_timeout
        SignInErrorCode.RESET_CODE_WRONG -> R.string.signin_error_reset_code_wrong
        SignInErrorCode.INVITE_GONE -> R.string.signin_error_invite_gone
        SignInErrorCode.LINKED -> R.string.signin_error_linked
        SignInErrorCode.SERVER -> R.string.signin_error_server
    }

@Composable
fun errorText(error: ApiError): String = stringResource(error.code.messageRes())

@Composable
fun signInErrorText(error: SignInError): String {
    val seconds = error.retryAfterSeconds
    if (error.code == SignInErrorCode.RATE_LIMITED && seconds != null) return stringResource(R.string.signin_error_rate_limited, seconds)
    return stringResource(error.code.messageRes())
}
