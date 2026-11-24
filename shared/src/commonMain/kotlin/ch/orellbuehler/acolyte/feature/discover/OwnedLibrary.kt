package ch.orellbuehler.acolyte.feature.discover

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.DiscoverEndpoints
import ch.orellbuehler.acolyte.api.valueOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class OwnedLibrary(
    private val api: Api,
    private val who: () -> String? = { null },
    private val clock: TimeSource = TimeSource.Monotonic,
) {
    private val mutable = MutableStateFlow(OwnedIndex.empty)
    private val lock = Mutex()
    private var loadedAt: TimeMark? = null
    private var loadedFor: String? = null

    val index: StateFlow<OwnedIndex> = mutable.asStateFlow()

    suspend fun get(): OwnedIndex =
        lock.withLock {
            val key = who()
            if (key != loadedFor) {
                loadedFor = key
                loadedAt = null
                mutable.value = OwnedIndex.empty
            }
            val at = loadedAt
            if (at == null || at.elapsedNow().inWholeSeconds >= KEEP_SECONDS) {
                api.result(DiscoverEndpoints.owned).valueOrNull()?.let {
                    mutable.value = OwnedIndex(it.items)
                    loadedAt = clock.markNow()
                }
            }
            mutable.value
        }

    suspend fun refresh(): OwnedIndex {
        lock.withLock { loadedAt = null }
        return get()
    }

    private companion object {
        const val KEEP_SECONDS = 60
    }
}
