package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.platformEngine
import ch.orellbuehler.acolyte.feature.health.HealthPresenter
import ch.orellbuehler.acolyte.image.Images
import ch.orellbuehler.acolyte.session.SecureStore
import ch.orellbuehler.acolyte.session.SessionManager
import ch.orellbuehler.acolyte.session.SignInFlow
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AcolyteSdk internal constructor(
    store: SecureStore,
    appVersion: String,
    engineFactory: () -> HttpClientEngine,
    scope: CoroutineScope,
) {
    constructor(store: SecureStore, appVersion: String) : this(
        store,
        appVersion,
        ::platformEngine,
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    val session: SessionManager = SessionManager(store, engineFactory, appVersion, scope)

    val images: Images = Images { session.client }

    fun signIn(): SignInFlow = SignInFlow(session)

    fun health(): HealthPresenter = HealthPresenter(session.api)
}
