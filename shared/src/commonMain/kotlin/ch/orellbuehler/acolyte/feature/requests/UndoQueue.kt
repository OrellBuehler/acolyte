package ch.orellbuehler.acolyte.feature.requests

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class UndoQueue<K>(
    private val scope: CoroutineScope,
    private val windowMillis: Long = UNDO_MILLIS,
) {
    private val gates = MutableStateFlow<Map<K, CompletableDeferred<Boolean>>>(emptyMap())

    private val jobs = MutableStateFlow<Set<Job>>(emptySet())

    fun hold(
        key: K,
        onUndone: suspend () -> Unit = {},
        send: suspend () -> Unit,
    ) {
        val gate = CompletableDeferred<Boolean>()
        var replaced: CompletableDeferred<Boolean>? = null
        gates.update {
            replaced = it[key]
            it + (key to gate)
        }
        replaced?.complete(false)
        val job =
            scope.launch(start = CoroutineStart.LAZY) {
                val go = withTimeoutOrNull(windowMillis) { gate.await() } ?: true
                var mine = false
                gates.update {
                    mine = it[key] === gate
                    if (mine) it - key else it
                }
                if (!mine) return@launch
                if (go) send() else onUndone()
            }
        jobs.update { it + job }
        job.invokeOnCompletion { jobs.update { it - job } }
        job.start()
    }

    suspend fun idle() {
        while (true) {
            val running = jobs.value
            if (running.isEmpty()) return
            running.forEach { it.join() }
        }
    }

    fun isHeld(key: K): Boolean = key in gates.value

    fun undo(key: K): Boolean {
        val gate = gates.value[key] ?: return false
        return gate.complete(false)
    }

    fun flush() {
        gates.value.values.forEach { it.complete(true) }
    }
}
