package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.AuthState
import ch.orellbuehler.acolyte.api.model.Me
import ch.orellbuehler.acolyte.api.model.PasswordSignIn
import ch.orellbuehler.acolyte.api.model.Reauth
import ch.orellbuehler.acolyte.api.model.SignInAnswer
import ch.orellbuehler.acolyte.api.model.TwoFactorSignIn

internal data object AuthEndpoints : EndpointGroup() {
    val state = get("/api/v1/auth/state", AuthState.serializer(), Need.PUBLIC)
    val login = post("/api/v1/auth/login", PasswordSignIn.serializer(), SignInAnswer.serializer(), Need.PUBLIC)
    val mediaServerLogin =
        post("/api/v1/auth/media-server/{kind}", PasswordSignIn.serializer(), SignInAnswer.serializer(), Need.PUBLIC)
    val twoFactor = post("/api/v1/auth/login/two-factor", TwoFactorSignIn.serializer(), SignInAnswer.serializer(), Need.PUBLIC)
    val logout = post("/api/v1/auth/logout", NoContent, Need.SELF)
    val me = get("/api/v1/auth/me", Me.serializer(), Need.SELF)
    val reauth = post("/api/v1/auth/reauth", Reauth.serializer(), SignInAnswer.serializer(), Need.SELF)
}
