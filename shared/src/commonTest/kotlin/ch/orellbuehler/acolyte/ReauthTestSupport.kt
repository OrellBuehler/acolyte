package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.feature.account.ReauthGate
import ch.orellbuehler.acolyte.session.Account

internal fun testGate(
    account: Account?,
    reauth: suspend (String, String?) -> ApiError? = { _, _ -> null },
) = ReauthGate(reauth, { account })
