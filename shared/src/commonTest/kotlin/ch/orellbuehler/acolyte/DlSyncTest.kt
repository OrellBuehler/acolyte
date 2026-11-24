package ch.orellbuehler.acolyte

import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.model.DownloadRow
import ch.orellbuehler.acolyte.api.model.DownloadsView
import ch.orellbuehler.acolyte.api.model.ImportRecord
import ch.orellbuehler.acolyte.feature.downloads.DlHealth
import ch.orellbuehler.acolyte.feature.downloads.DlOutcome
import ch.orellbuehler.acolyte.feature.downloads.DlPatch
import ch.orellbuehler.acolyte.feature.downloads.DlSync
import ch.orellbuehler.acolyte.feature.downloads.DlSyncState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DlSyncTest {
    private val own = DownloadRow(id = 7, liveKey = "2:aaa", progress = 0.1, speed = 10, downloaderId = 2, state = "downloading")
    private val outside = DownloadRow(clientItem = true, liveKey = "2:bbb", name = "Other", downloaderId = 2, ratio = 1.5)
    private val sync = DlSyncState("boot-1", mapOf(2L to 5L))

    private fun patch(json: String): DlPatch = DlSync.parse(ApiJson.lenient.parseToJsonElement(json)) as DlPatch

    @Test
    fun readsThePushStateFromTheViewAnswer() {
        assertNull(DlSync.fromAnswer(DownloadsView()))
        val state = DlSync.fromAnswer(DownloadsView(revs = mapOf("2" to 5, "x" to 1), boot = "boot-1"))
        assertEquals(DlSyncState("boot-1", mapOf(2L to 5L)), state)
    }

    @Test
    fun appliesAPatchOnTheHeldRev() {
        val p =
            patch(
                """{"kind":"patch","boot":"boot-1","client":2,"fromRev":5,"toRev":6,
                "upsert":[{"key":"2:aaa","progress":0.5,"speed":2000},{"key":"2:bbb","ratio":null},
                {"key":"2:ccc","client_item":true,"name":"New torrent","downloader_id":2,"phase":"downloading"},{"key":"2:ddd","progress":0.3}],
                "remove":[],"totals":{"speed":2000,"up_speed":30}}""",
            )
        val outcome = DlSync.apply(listOf(own, outside), sync, p)
        assertIs<DlOutcome.Applied>(outcome)
        assertEquals(6L, outcome.sync.revs[2])
        assertEquals(3, outcome.rows.size)
        assertEquals(0.5, outcome.rows[0].progress)
        assertEquals(2000L, outcome.rows[0].speed)
        assertEquals("downloading", outcome.rows[0].state)
        assertNull(outcome.rows[1].ratio)
        assertEquals("New torrent", outcome.rows[2].name)
        assertEquals("2:ccc", outcome.rows[2].liveKey)
        assertEquals(30L, p.upSpeed)
    }

    @Test
    fun removesOnlyOutsideRows() {
        val p = patch("""{"kind":"patch","client":2,"fromRev":5,"toRev":6,"remove":["2:aaa","2:bbb"]}""")
        val outcome = DlSync.apply(listOf(own, outside), sync, p)
        assertIs<DlOutcome.Applied>(outcome)
        assertEquals(listOf(7L), outcome.rows.map { it.id })
    }

    @Test
    fun dropsOldPatchesAndRereadsOnAGap() {
        assertEquals(DlOutcome.Dropped, DlSync.apply(listOf(own), sync, patch("""{"kind":"patch","client":2,"fromRev":4,"toRev":5}""")))
        assertEquals(DlOutcome.Reread(2), DlSync.apply(listOf(own), sync, patch("""{"kind":"patch","client":2,"fromRev":6,"toRev":7}""")))
        assertEquals(
            DlOutcome.Reread(null),
            DlSync.apply(listOf(own), sync, patch("""{"kind":"patch","boot":"boot-2","client":2,"fromRev":5,"toRev":6}""")),
        )
        val fresh = patch("""{"kind":"patch","client":9,"fromRev":0,"toRev":1}""")
        assertIs<DlOutcome.Applied>(DlSync.apply(listOf(own), sync, fresh))
    }

    @Test
    fun readsHealthEvents() {
        val h =
            DlSync.parse(
                ApiJson.lenient.parseToJsonElement("""{"kind":"health","client":2,"health":"offline","stale":true}"""),
            ) as DlHealth
        assertEquals("offline", h.health)
        assertTrue(DlSync.wentStale(false, h))
        assertFalse(DlSync.wentStale(true, h))
        assertNull(DlSync.parse(ApiJson.lenient.parseToJsonElement("""{"kind":"other","client":2}""")))
        assertNull(DlSync.parse(ApiJson.lenient.parseToJsonElement("""{"kind":"patch"}""")))
    }

    @Test
    fun replacesOneClientsRows() {
        val other = DownloadRow(id = 3, downloaderId = 4)
        val page = listOf(DownloadRow(id = 9, downloaderId = 2), DownloadRow(clientItem = true, downloaderId = 2, name = "Fresh"))
        val (rows, next) = DlSync.mergeClient(listOf(own, outside, other), sync, 2, page, mapOf("2" to 11))
        assertEquals(listOf(9L, 3L, 0L), rows.map { it.id })
        assertEquals("Fresh", rows[2].name)
        assertEquals(11L, next.revs[2])
    }

    @Test
    fun readsGoCasedAndSnakeDownloadEvents() {
        val go =
            DlSync.liveDownload(
                ApiJson.lenient.parseToJsonElement("""{"ID":7,"State":"failed","StateReason":"no seeds","Progress":0.4,"Speed":0}"""),
            )
        assertEquals(7L, go?.id)
        assertEquals("failed", go?.state)
        assertEquals("no seeds", go?.reason)
        assertTrue(DlSync.failureNotice(go!!))
        val snake =
            DlSync.liveDownload(
                ApiJson.lenient.parseToJsonElement("""{"id":7,"state":"failed","state_reason":"marked failed by admin"}"""),
            )
        assertFalse(DlSync.failureNotice(snake!!))
        val rows = DlSync.applyDownload(listOf(own, outside), go)
        assertEquals(0.4, rows[0].progress)
        assertEquals(0L, rows[0].speed)
        assertEquals(1.5, rows[1].ratio)
        assertNull(DlSync.liveDownload(ApiJson.lenient.parseToJsonElement("""{"State":"x"}""")))
    }

    @Test
    fun keepsTheNewestLiveImport() {
        val record =
            DlSync.liveImport(
                ApiJson.lenient.parseToJsonElement("""{"ID":12,"DownloadID":7,"State":"done","Result":[{"outcome":"placed"}]}"""),
            )
        assertEquals(12L, record?.id)
        val imports = DlSync.withImport(mapOf(7L to ImportRecord(id = 10, downloadRef = 7)), record!!)
        assertEquals(12L, imports[7]?.id)
        assertEquals(12L, DlSync.withImport(imports, ImportRecord(id = 11, downloadRef = 7))[7]?.id)
    }
}
