package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.DownloadsEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.DownloadRow
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

enum class DownloadsBadgeKind {
    NONE,
    ACTIVE,
    DECIDE,
}

data class DownloadsBadge(
    val active: Int = 0,
    val percent: Int = 0,
    val decide: Int = 0,
) {
    val kind: DownloadsBadgeKind
        get() =
            when {
                active > 0 -> DownloadsBadgeKind.ACTIVE
                decide > 0 -> DownloadsBadgeKind.DECIDE
                else -> DownloadsBadgeKind.NONE
            }
}

class DownloadBadges internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val clock: Clock = Clock.System,
) {
    private val mutableState = MutableStateFlow(DownloadsBadge())
    private var lastActive = Instant.DISTANT_PAST

    val state: StateFlow<DownloadsBadge> = mutableState.asStateFlow()

    private val manager: Boolean get() = account()?.can(Need.MANAGE_LIBRARY) == true

    private val decidesOn: Boolean get() = account()?.isOn(Part.MEDIA) == true

    internal fun rows(rows: List<DownloadRow>) {
        val (count, percent) = DownloadRules.summarise(rows)
        lastActive = clock.now()
        mutableState.update { it.copy(active = count, percent = percent) }
    }

    internal fun decisions(count: Int) {
        mutableState.update { it.copy(decide = count) }
    }

    suspend fun refresh() {
        if (!manager) {
            mutableState.value = DownloadsBadge()
            return
        }
        refreshActive()
        refreshDecisions()
    }

    private suspend fun refreshActive() {
        api.result(DownloadsEndpoints.view, "limit" to 100).onSuccess { rows(it.downloads) }
    }

    private suspend fun refreshDecisions() {
        if (!decidesOn) return
        api.result(DownloadsEndpoints.decisions).onSuccess { decisions(it.items.size) }
    }

    suspend fun watch() {
        if (!manager) return
        coroutineScope {
            val poke = Channel<Unit>(Channel.CONFLATED)
            launch {
                api.live.collect { event ->
                    when (event.topic) {
                        "download", "slot", "import", LiveEvent.RESYNC -> poke.trySend(Unit)
                    }
                    if (event.topic == "slot" || event.topic == LiveEvent.RESYNC) refreshDecisions()
                }
            }
            launch {
                for (x in poke) {
                    val wait = 10.seconds - (clock.now() - lastActive)
                    if (wait.isPositive()) delay(wait)
                    refreshActive()
                }
            }
            launch {
                while (true) {
                    refreshActive()
                    delay(15.seconds)
                }
            }
            launch {
                while (true) {
                    refreshDecisions()
                    delay(60.seconds)
                }
            }
        }
    }
}
