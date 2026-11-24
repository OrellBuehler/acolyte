package ch.orellbuehler.acolyte.feature.library

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.model.ReleaseRead
import ch.orellbuehler.acolyte.api.model.ReleaseView
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.live.LiveEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ReleaseVerdict(
    val tone: Tone,
) {
    TURNED_DOWN(Tone.FAIL),
    CHOSEN(Tone.INFO),
    HELD(Tone.WAIT),
    WOULD_CHOOSE(Tone.OK),
    WOULD_HOLD(Tone.WAIT),
    REFUSED(Tone.FAIL),
    PASSED_OVER(Tone.IDLE),
    JUDGED(Tone.IDLE),
}

data class ReleaseRow(
    val id: Long,
    val title: String,
    val version: String,
    val name: String,
    val poster: String?,
    val tags: List<String>,
    val verdict: ReleaseVerdict,
    val sourceName: String,
    val size: Long,
    val seeders: Int,
    val transport: String,
    val points: Int,
    val rank: Int,
    val holdUntil: String,
    val since: String,
    val reasons: List<String>,
)

data class LibraryReleasesState(
    val turnedDown: Boolean = false,
    val status: LoadStatus = LoadStatus(),
    val rows: List<ReleaseRow> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = PAGE,
    val busy: Boolean = false,
    val notice: LibraryNotice? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && rows.isEmpty()
    val hasPrevious: Boolean get() = offset > 0
    val hasNext: Boolean get() = offset + limit < total
    val from: Int get() = if (total == 0) 0 else minOf(offset, total - 1) + 1
    val to: Int get() = minOf(offset + limit, total)

    companion object {
        const val PAGE = 25
    }
}

internal object ReleaseText {
    private val sourceWord =
        mapOf(
            "remux" to "Remux",
            "bluray" to "BluRay",
            "web_dl" to "WEB-DL",
            "web_rip" to "WEBRip",
            "tv" to "HDTV",
            "dvd" to "DVD",
            "disc_image" to "Disc",
            "cam" to "CAM",
            "telesync" to "TS",
        )
    private val hdrWord = mapOf("dv" to "Dolby Vision", "hdr10plus" to "HDR10+", "hdr10" to "HDR10", "hlg" to "HLG")
    private val codecWord = mapOf("x265" to "x265", "h265" to "x265", "hevc" to "x265", "x264" to "x264", "h264" to "x264", "av1" to "AV1")

    fun tags(read: ReleaseRead?): List<String> {
        read ?: return emptyList()
        return buildList {
            if (read.format.isNotEmpty()) {
                add(read.format.uppercase())
                if (read.bitDepth > 0) {
                    add("${read.bitDepth}-bit")
                } else if (read.bitrate > 0) {
                    add("${read.bitrate} kbps")
                }
            }
            if (read.resolution > 0) add(if (read.resolution >= 2160) "4K" else "${read.resolution}p")
            sourceWord[read.source]?.let { add(it) }
            read.hdr.mapNotNull { hdrWord[it] }.forEach { add(it) }
            if (read.codec.isNotEmpty()) add(codecWord[read.codec.lowercase()] ?: read.codec)
            if (read.atmos) {
                add("Atmos")
            } else if (read.channels.isNotEmpty()) {
                add(read.channels)
            }
            read.languages.forEach { add(it.uppercase()) }
            if (read.proper) add("PROPER")
        }
    }

    fun verdict(r: ReleaseView): ReleaseVerdict =
        when {
            r.state == "turned_down" -> ReleaseVerdict.TURNED_DOWN
            r.state == "chosen" -> ReleaseVerdict.CHOSEN
            r.state == "held" -> ReleaseVerdict.HELD
            r.outcome == "chosen" -> ReleaseVerdict.WOULD_CHOOSE
            r.outcome == "held" -> ReleaseVerdict.WOULD_HOLD
            r.outcome == "refused" || r.state == "refused" -> ReleaseVerdict.REFUSED
            r.outcome == "passed_over" || r.state == "passed_over" -> ReleaseVerdict.PASSED_OVER
            else -> ReleaseVerdict.JUDGED
        }

    fun listedName(r: ReleaseView): String {
        if (r.album.isNotEmpty()) return listOf(r.titleName, r.album).filter { it.isNotEmpty() }.joinToString(" – ")
        val episode = if (r.state == "turned_down" && r.turnedDownFor.isNotEmpty()) r.turnedDownFor else r.episode
        return listOf(r.titleName, episode).filter { it.isNotEmpty() }.joinToString(" ")
    }
}

class LibraryReleasesPresenter internal constructor(
    private val api: Api,
    turnedDown: Boolean,
) {
    private val mutableState = MutableStateFlow(LibraryReleasesState(turnedDown = turnedDown))
    private val lock = Mutex()
    private val notices = Notices()
    private var art: Map<Long, String>? = null

    val state: StateFlow<LibraryReleasesState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val current = mutableState.value
            mutableState.update { it.copy(status = it.status.started()) }
            if (art == null) art = TitleArt.load(api)
            val endpoint = if (current.turnedDown) LibraryEndpoints.turnedDown else LibraryEndpoints.held
            when (val result = api.result(endpoint, "limit" to current.limit, "offset" to current.offset)) {
                is ApiResult.Ok -> {
                    val page = result.value
                    mutableState.update { it.copy(status = LoadStatus.ready, total = page.total, rows = page.releases.map { r -> row(r) }) }
                }
                is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
            }
        }
    }

    private fun row(r: ReleaseView): ReleaseRow =
        ReleaseRow(
            id = r.id,
            title = ReleaseText.listedName(r),
            version = r.version.takeIf { it != "Main" }.orEmpty(),
            name = r.name,
            poster = r.titleId?.let { art?.get(it) },
            tags = ReleaseText.tags(r.read),
            verdict = ReleaseText.verdict(r),
            sourceName = r.sourceName,
            size = r.size,
            seeders = r.seeders ?: -1,
            transport = r.protocol,
            points = if (r.passed) r.points else -1,
            rank = if (r.passed && r.rank > 0) r.rank else -1,
            holdUntil = r.holdUntil,
            since = r.createdAt,
            reasons = r.reasons.filter { it.effect in setOf("fail", "rank", "hold", "warn") }.map { it.text },
        )

    suspend fun nextPage() {
        if (!mutableState.value.hasNext) return
        mutableState.update { it.copy(offset = it.offset + it.limit) }
        refresh()
    }

    suspend fun previousPage() {
        if (!mutableState.value.hasPrevious) return
        mutableState.update { it.copy(offset = maxOf(0, it.offset - it.limit)) }
        refresh()
    }

    suspend fun remove(id: Long) {
        val current = mutableState.value
        mutableState.update { it.copy(busy = true) }
        val result =
            if (current.turnedDown) api.result(LibraryEndpoints.takeOff, "id" to id) else api.result(LibraryEndpoints.unhold, "id" to id)
        when (result) {
            is ApiResult.Ok -> {
                val lastOnPage = current.rows.size == 1 && current.offset > 0
                mutableState.update {
                    it.copy(
                        busy = false,
                        offset = if (lastOnPage) maxOf(0, it.offset - it.limit) else it.offset,
                        notice = notices.make(LibraryNoticeKind.TAKEN_OFF, count = result.value.removed),
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, notice = notices.failed(result.error)) }
        }
        refresh()
    }

    suspend fun clearAll() {
        val current = mutableState.value
        mutableState.update { it.copy(busy = true) }
        val result = if (current.turnedDown) api.result(LibraryEndpoints.takeOffAll) else api.result(LibraryEndpoints.unholdAll)
        when (result) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(busy = false, offset = 0, notice = notices.make(LibraryNoticeKind.CLEARED, count = result.value.removed))
                }
            is ApiResult.Failed -> mutableState.update { it.copy(busy = false, notice = notices.failed(result.error)) }
        }
        refresh()
    }

    fun dismissNotice(seq: Int) {
        mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }
    }

    suspend fun watch() {
        api.live
            .filter { it.topic == "releases" || it.topic == "slot" || it.topic == LiveEvent.RESYNC }
            .conflate()
            .collect { refresh() }
    }
}
