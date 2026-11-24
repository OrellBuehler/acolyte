package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.ui.account.followDisplayName
import ch.orellbuehler.acolyte.ui.account.passwordMismatch
import ch.orellbuehler.acolyte.ui.account.passwordReady
import ch.orellbuehler.acolyte.ui.account.passwordTooShort
import ch.orellbuehler.acolyte.ui.account.sampleSize
import ch.orellbuehler.acolyte.ui.account.squareCrop
import ch.orellbuehler.acolyte.ui.wizard.WizardProblem
import ch.orellbuehler.acolyte.ui.wizard.wizardProblem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountUiRulesTest {
    @Test
    fun cropIsCenteredAndSquare() {
        val wide = squareCrop(4000, 3000)
        assertEquals(3000, wide.side)
        assertEquals(500, wide.left)
        assertEquals(0, wide.top)
        val tall = squareCrop(600, 1000)
        assertEquals(600, tall.side)
        assertEquals(0, tall.left)
        assertEquals(200, tall.top)
    }

    @Test
    fun sampleSizeKeepsAtLeastTheTarget() {
        assertEquals(1, sampleSize(600, 512))
        assertEquals(1, sampleSize(1000, 512))
        assertEquals(2, sampleSize(1024, 512))
        assertEquals(4, sampleSize(3000, 512))
    }

    @Test
    fun passwordRules() {
        assertFalse(passwordTooShort("", 8))
        assertTrue(passwordTooShort("abc", 8))
        assertFalse(passwordTooShort("abcdefgh", 8))
        assertTrue(passwordMismatch("abcdefgh", "abcdefg"))
        assertFalse(passwordMismatch("abcdefgh", ""))
        assertTrue(passwordReady("abcdefgh", "abcdefgh", 8))
        assertFalse(passwordReady("abcdefgh", "abcdefgx", 8))
        assertFalse(passwordReady("abc", "abc", 8))
    }

    @Test
    fun displayNameFollowsUsernameUntilEdited() {
        assertEquals("anna", followDisplayName("anna", "", false))
        assertEquals("Anna B", followDisplayName("anna", "Anna B", true))
    }

    @Test
    fun wizardErrorsAreSortedByCodeAndText() {
        fun problem(
            code: ApiErrorCode,
            message: String,
        ) = wizardProblem(ApiError(code, 0, message))
        assertEquals(WizardProblem.NOT_ADMIN, problem(ApiErrorCode.FORBIDDEN, "Use an admin account on that server."))
        assertEquals(WizardProblem.NONE, problem(ApiErrorCode.FORBIDDEN, "Your account is turned off"))
        assertEquals(WizardProblem.NEEDS_HTTPS, problem(ApiErrorCode.BAD_REQUEST, "an outside Jellyfin address needs https"))
        assertEquals(WizardProblem.BAD_ADDRESS, problem(ApiErrorCode.BAD_REQUEST, "give the Jellyfin address"))
        assertEquals(WizardProblem.UNREACHABLE, problem(ApiErrorCode.SERVER, "could not reach x"))
        assertEquals(WizardProblem.NOT_THAT_SERVER, problem(ApiErrorCode.SERVER, "that address did not answer like Jellyfin"))
        assertEquals(WizardProblem.ALREADY_LINKED, problem(ApiErrorCode.CONFLICT, "linked"))
        assertEquals(WizardProblem.WRONG_LOGIN, problem(ApiErrorCode.UNAUTHORIZED, ""))
    }
}
