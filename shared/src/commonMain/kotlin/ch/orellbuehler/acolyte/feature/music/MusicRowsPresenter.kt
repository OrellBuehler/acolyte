package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.MusicEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.MusicCard
import ch.orellbuehler.acolyte.api.model.MusicExploreAnswer
import ch.orellbuehler.acolyte.api.model.MusicRow
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class MusicCardAction {
    NONE,
    ADD,
    ASK,
}

data class MusicItem(
    val key: String,
    val kind: MusicCardKind,
    val mbid: String,
    val name: String,
    val artist: String,
    val artistMbid: String,
    val titleId: Long,
    val partId: Long,
    val image: String,
    val images: List<String>,
    val cover: String,
    val line: MusicLine,
    val mark: MusicMark,
    val fresh: String,
    val kicker: String,
    val badge: String,
    val explicit: Boolean,
    val upcoming: Boolean,
    val following: Boolean,
    val rank: Int,
    val hue: Int,
    val color: String,
    val target: MusicTarget,
    val action: MusicCardAction,
    val date: String,
    val track: String,
)

data class MusicShelfRow(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val shape: MusicShape,
    val items: List<MusicItem>,
    val regional: Boolean = false,
)

data class MusicRowsState(
    val status: LoadStatus = LoadStatus(),
    val kind: String = "",
    val slug: String = "",
    val tileKind: String = "",
    val title: String = "",
    val hue: Int = 0,
    val color: String = "",
    val collage: List<String> = emptyList(),
    val leads: List<String> = emptyList(),
    val hero: MusicItem? = null,
    val spotlight: List<MusicItem> = emptyList(),
    val strip: List<MusicItem> = emptyList(),
    val grid: List<MusicShelfRow> = emptyList(),
    val rows: List<MusicShelfRow> = emptyList(),
    val region: String = "",
    val regions: List<String> = emptyList(),
    val cold: Boolean = false,
    val thin: Boolean = false,
    val similar: List<MusicSimilar> = emptyList(),
    val kid: Boolean = false,
    val partial: Boolean = false,
    val more: Boolean = false,
    val loadingMore: Boolean = false,
    val canAdd: Boolean = false,
    val canAsk: Boolean = false,
) {
    val isEmpty: Boolean get() =
        status.phase == Phase.READY &&
            !kid &&
            hero == null &&
            rows.isEmpty() &&
            strip.isEmpty() &&
            grid.isEmpty() &&
            !thin
    val isPage: Boolean get() = kind.isNotEmpty()
    val bandHue: Int get() = MusicRules.bandHue(hue)
}

data class MusicSimilar(
    val key: String,
    val name: String,
    val target: MusicTarget,
)

class MusicAccess internal constructor(
    val manage: Boolean,
    val ask: Boolean,
    val show: Boolean,
    val settings: Boolean = false,
    val kid: Boolean = false,
    val handsOff: Boolean = false,
) {
    companion object {
        internal fun of(me: Account?): MusicAccess =
            MusicAccess(
                manage = me?.allows(Need.MANAGE_LIBRARY, Part.MUSIC) == true,
                ask = me?.allows(Need.ASK, Part.WISHLIST) == true && me.isOn(Part.MUSIC),
                show = me?.shows(ch.orellbuehler.acolyte.nav.Section.MUSIC) == true,
                settings = me?.allows(Need.SETTINGS, Part.MUSIC) == true,
                kid = me?.isKid == true,
                handsOff = me?.handsOff == true,
            )
    }
}

internal fun MusicCard.toItem(
    rowType: String,
    today: String,
    access: MusicAccess,
    line: MusicLine? = null,
): MusicItem {
    val kind = MusicRules.cardKind(kind)
    val canAct =
        artistMbid.isNotEmpty() &&
            titleId == 0L &&
            rowType != "your_asks" &&
            !upcoming &&
            kind != MusicCardKind.GENRE &&
            (access.manage || access.ask)
    return MusicItem(
        key = key.ifEmpty { "$kind:$mbid:$name" },
        kind = kind,
        mbid = mbid,
        name = name.ifEmpty { track },
        artist = artist,
        artistMbid = artistMbid.ifEmpty { if (kind == MusicCardKind.ARTIST) mbid else "" },
        titleId = titleId,
        partId = partId,
        image = image,
        images = images.ifEmpty { listOfNotNull(image.takeIf { it.isNotEmpty() }) },
        cover = cover,
        line = line ?: MusicRules.cardLine(rowType, this, today),
        mark = MusicRules.cardMark(state, rowType),
        fresh = fresh?.text.orEmpty(),
        kicker = kicker,
        badge = badge,
        explicit = explicit,
        upcoming = upcoming,
        following = following,
        rank = rank,
        hue = hue,
        color = if (kind == MusicCardKind.GENRE) MusicRules.tileColor(key.ifEmpty { "genre:$mbid" }) else "",
        target = MusicRules.target(this),
        action =
            if (!canAct) {
                MusicCardAction.NONE
            } else if (access.manage) {
                MusicCardAction.ADD
            } else {
                MusicCardAction.ASK
            },
        date = date,
        track = track,
    )
}

internal fun MusicRow.toShelf(
    today: String,
    access: MusicAccess,
): MusicShelfRow =
    MusicShelfRow(
        id = id,
        type = type,
        title = title,
        subtitle = reason,
        shape = MusicRules.shape(shape),
        items = items.map { it.toItem(type, today, access) },
        regional = type == "popular_country" || type == "top_albums",
    )

class MusicRowsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val kind: String,
    private val slug: String,
    region: String,
    private val today: () -> String,
) {
    private val mutableState = MutableStateFlow(MusicRowsState(kind = kind, slug = slug, tileKind = kind, region = region))
    private val lock = Mutex()
    private var answer = MusicExploreAnswer()
    private var rows: List<MusicRow> = emptyList()
    private var rising: MusicRow? = null
    private var next = ""
    private var tries = 0

    val state: StateFlow<MusicRowsState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            tries = 0
            var first = true
            while (true) {
                if (!fetchFirst()) return
                if (first && kind.isEmpty() && !answer.kid) loadRising()
                first = false
                publish()
                mutableState.update { it.copy(status = LoadStatus.ready) }
                if (!answer.partial || tries >= REASKS.size) return
                delay(REASKS[tries++])
            }
        }
    }

    suspend fun setRegion(region: String) {
        if (region == mutableState.value.region) return
        mutableState.update { it.copy(region = region, status = LoadStatus(), rows = emptyList(), hero = null) }
        rows = emptyList()
        next = ""
        refresh()
    }

    suspend fun loadMore() {
        lock.withLock {
            val s = mutableState.value
            if (next.isEmpty() || s.loadingMore || s.status.phase != Phase.READY) return
            mutableState.update { it.copy(loadingMore = true) }
            when (val result = api.result(MusicEndpoints.explore, "region" to s.region, "cursor" to next)) {
                is ApiResult.Ok -> {
                    val known = rows.map { it.id }.toSet()
                    rows = rows + result.value.rows.filter { it.id !in known }
                    next = result.value.next
                    publish()
                    mutableState.update { it.copy(loadingMore = false) }
                }
                is ApiResult.Failed -> {
                    next = ""
                    mutableState.update { it.copy(loadingMore = false, more = false) }
                    if (result.error.status == EXPIRED) {
                        mutableState.update { it.copy(status = LoadStatus()) }
                    }
                }
            }
        }
    }

    private suspend fun fetchFirst(): Boolean {
        val region = mutableState.value.region
        val result =
            when {
                kind.isEmpty() -> api.result(MusicEndpoints.explore, "region" to region)
                kind == "genre" -> api.result(MusicEndpoints.genre, "slug" to slug, "region" to region)
                else -> api.result(MusicEndpoints.browse, "kind" to kind, "slug" to slug, "region" to region)
            }
        return when (result) {
            is ApiResult.Ok -> {
                val previous = rows
                answer = result.value
                rows = if (answer.partial && answer.rows.size < previous.size) previous else answer.rows
                next = answer.next
                true
            }
            is ApiResult.Failed -> {
                mutableState.update { it.copy(status = it.status.failed(result.error)) }
                false
            }
        }
    }

    private suspend fun loadRising() {
        rising = api.result(MusicEndpoints.rising).valueOrNull()?.takeIf { it.items.isNotEmpty() }
    }

    private fun publish() {
        val day = today()
        val access = MusicAccess.of(account())
        val a = answer
        val all = rows.toMutableList()
        rising?.let { r ->
            if (all.none { it.id == r.id }) {
                val at = all.indexOfFirst { it.type == "trending_artists" }.let { if (it >= 0) it + 1 else minOf(2, all.size) }
                all.add(at, r)
            }
        }
        val strip = all.filter { it.shape == "tile" }.flatMap { it.items }.map { it.toItem("", day, access) }
        val grid = all.filter { it.shape == "grid" }.map { it.toShelf(day, access) }
        val shown = all.filter { it.shape != "tile" && it.shape != "grid" }.map { it.toShelf(day, access) }
        val tile = a.tile
        val covers =
            (
                listOfNotNull(a.hero?.image) +
                    shown
                        .firstOrNull()
                        ?.items
                        ?.map { it.image }
                        .orEmpty()
            ).filter { it.isNotEmpty() }
                .distinct()
                .take(COLLAGE)
        val leads =
            (
                listOfNotNull(a.hero?.artist) +
                    shown.flatMap { r ->
                        r.items.map { it.artist }
                    }
            ).filter { it.isNotEmpty() }.distinct().take(LEADS)
        mutableState.update {
            it.copy(
                tileKind = tile?.key?.let { k -> MusicRules.tileKind(k) } ?: kind,
                title = tile?.name.orEmpty(),
                hue = tile?.hue ?: 0,
                color = tile?.key?.let { k -> MusicRules.tileColor(k) } ?: "",
                collage = covers,
                leads = leads,
                hero = a.hero?.toItem("hero", day, access),
                spotlight = a.heroAlts.map { h -> h.toItem("hero", day, access) },
                strip = strip,
                grid = grid,
                rows = shown,
                region = a.region.ifEmpty { it.region },
                regions = a.regions,
                cold = a.cold,
                thin = a.thin,
                similar = a.similar.map { s -> MusicSimilar(s.key, s.name, MusicRules.pageTarget(s.href)) },
                kid = a.kid,
                partial = a.partial,
                more = next.isNotEmpty(),
                canAdd = access.manage,
                canAsk = access.ask,
            )
        }
    }

    private companion object {
        val REASKS = listOf(1_500L, 2_500L, 4_000L, 6_000L, 9_000L)
        const val COLLAGE = 5
        const val LEADS = 3
        const val EXPIRED = 400
    }
}
