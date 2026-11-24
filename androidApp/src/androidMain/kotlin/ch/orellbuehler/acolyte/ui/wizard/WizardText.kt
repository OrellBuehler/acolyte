package ch.orellbuehler.acolyte.ui.wizard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.ui.components.errorText

enum class WizardProblem {
    WRONG_LOGIN,
    NOT_ADMIN,
    NEEDS_HTTPS,
    BAD_ADDRESS,
    UNREACHABLE,
    NOT_THAT_SERVER,
    ALREADY_LINKED,
    NONE,
}

fun wizardProblem(error: ApiError): WizardProblem {
    val text = error.message.lowercase()
    return when (error.code) {
        ApiErrorCode.UNAUTHORIZED -> WizardProblem.WRONG_LOGIN
        ApiErrorCode.FORBIDDEN -> if ("admin" in text) WizardProblem.NOT_ADMIN else WizardProblem.NONE
        ApiErrorCode.BAD_REQUEST -> if ("https" in text) WizardProblem.NEEDS_HTTPS else WizardProblem.BAD_ADDRESS
        ApiErrorCode.CONFLICT -> WizardProblem.ALREADY_LINKED
        ApiErrorCode.SERVER ->
            when {
                "could not reach" in text -> WizardProblem.UNREACHABLE
                "did not answer like" in text -> WizardProblem.NOT_THAT_SERVER
                else -> WizardProblem.NONE
            }
        else -> WizardProblem.NONE
    }
}

@Composable
fun wizardErrorText(error: ApiError): String =
    when (wizardProblem(error)) {
        WizardProblem.WRONG_LOGIN -> stringResource(R.string.wizard_error_wrong_login)
        WizardProblem.NOT_ADMIN -> stringResource(R.string.wizard_error_not_admin)
        WizardProblem.NEEDS_HTTPS -> stringResource(R.string.wizard_error_needs_https)
        WizardProblem.BAD_ADDRESS -> stringResource(R.string.wizard_error_bad_address)
        WizardProblem.UNREACHABLE -> stringResource(R.string.wizard_error_unreachable)
        WizardProblem.NOT_THAT_SERVER -> stringResource(R.string.wizard_error_not_that_server)
        WizardProblem.ALREADY_LINKED -> stringResource(R.string.wizard_error_already_linked)
        WizardProblem.NONE ->
            if (error.code == ApiErrorCode.FORBIDDEN && error.message.isNotBlank()) error.message else errorText(error)
    }
