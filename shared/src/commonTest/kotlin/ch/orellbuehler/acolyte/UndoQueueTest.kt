package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.feature.requests.UNDO_MILLIS
import ch.orellbuehler.acolyte.feature.requests.UndoQueue
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UndoQueueTest {
    @Test
    fun sendsOnlyAfterTheWindow() =
        runTest {
            val sent = mutableListOf<Int>()
            val queue = UndoQueue<Int>(backgroundScope)
            queue.hold(1) { sent += 1 }
            advanceTimeBy(UNDO_MILLIS - 1)
            runCurrent()
            assertTrue(sent.isEmpty())
            assertTrue(queue.isHeld(1))
            advanceTimeBy(2)
            runCurrent()
            assertEquals(listOf(1), sent)
            assertFalse(queue.isHeld(1))
        }

    @Test
    fun undoDropsTheDecision() =
        runTest {
            val sent = mutableListOf<Int>()
            val undone = mutableListOf<Int>()
            val queue = UndoQueue<Int>(backgroundScope)
            queue.hold(1, onUndone = { undone += 1 }) { sent += 1 }
            queue.hold(2) { sent += 2 }
            advanceTimeBy(1_000)
            assertTrue(queue.undo(1))
            queue.idle()
            assertEquals(listOf(2), sent)
            assertEquals(listOf(1), undone)
            assertFalse(queue.undo(1))
        }

    @Test
    fun flushSendsAtOnce() =
        runTest {
            val sent = mutableListOf<Int>()
            val queue = UndoQueue<Int>(backgroundScope)
            queue.hold(1) { sent += 1 }
            queue.hold(2) { sent += 2 }
            queue.flush()
            runCurrent()
            assertEquals(listOf(1, 2), sent)
            assertEquals(0L, testScheduler.currentTime)
        }

    @Test
    fun holdingAgainReplacesTheFirst() =
        runTest {
            val sent = mutableListOf<String>()
            val queue = UndoQueue<Int>(backgroundScope)
            queue.hold(1) { sent += "approve" }
            queue.hold(1) { sent += "decline" }
            queue.idle()
            assertEquals(listOf("decline"), sent)
        }
}
