package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.AccountEndpoints
import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.MusicEndpoints
import ch.orellbuehler.acolyte.api.model.MusicAlbumFollowBody
import ch.orellbuehler.acolyte.api.model.MusicBrowseTile
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MusicTile(
    val key: String,
    val name: String,
    val target: MusicTarget,
    val color: String,
    val hue: Int,
    val covers: List<String>,
)

data class MusicTileSection(
    val id: String,
    val title: String,
    val tiles: List<MusicTile>,
)

internal fun MusicBrowseTile.toTile(): MusicTile = MusicTile(key, name, MusicRules.tileTarget(this), MusicRules.tileColor(key), hue, covers)

data class MusicNewState(
    val status: LoadStatus = LoadStatus(),
    val kid: Boolean = false,
    val building: Boolean = false,
    val hero: List<MusicItem> = emptyList(),
    val rows: List<MusicShelfRow> = emptyList(),
    val tiles: List<MusicTile> = emptyList(),
    val genres: List<String> = emptyList(),
    val canAdd: Boolean = false,
    val canAsk: Boolean = false,
    val note: MusicNote? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && !kid && !building && hero.isEmpty() && rows.isEmpty()
}

class MusicNewPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val region: String,
    private val today: () -> String,
) {
    private val mutableState = MutableStateFlow(MusicNewState())
    private val notes = MusicNotes()

    val state: StateFlow<MusicNewState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        mutableState.update { it.copy(status = it.status.started()) }
        var tries = 0
        while (true) {
            if (!fetch()) return
            if (!mutableState.value.building || tries >= BUILDING_TRIES) break
            delay(BUILDING_MILLIS)
            tries++
        }
    }

    private suspend fun fetch(): Boolean =
        coroutineScope {
            val news = async { api.result(MusicEndpoints.newMusic, "region" to region) }
            val rising = async { api.result(MusicEndpoints.rising).valueOrNull() }
            val tiles = async { api.result(MusicEndpoints.tiles, "region" to region).valueOrNull() }
            when (val result = news.await()) {
                is ApiResult.Failed -> {
                    mutableState.update { it.copy(status = it.status.failed(result.error)) }
                    false
                }
                is ApiResult.Ok -> {
                    val a = result.value
                    val day = today()
                    val access = MusicAccess.of(account())
                    val rows = a.rows.toMutableList()
                    rising.await()?.takeIf { it.items.isNotEmpty() && !a.kid }?.let { r ->
                        val first = rows.indexOfFirst { it.shape != "wide" && it.id != "np_music_tracks" }
                        rows.add(if (first >= 0) first + 1 else rows.size, r)
                    }
                    mutableState.update {
                        it.copy(
                            status = LoadStatus.ready,
                            kid = a.kid,
                            building = a.building,
                            hero = a.hero.map { c -> c.toItem("hero", day, access, MusicRules.newLine("hero", c, day)) },
                            rows = rows.map { r -> r.toNewShelf(day, access) },
                            tiles =
                                tiles
                                    .await()
                                    ?.explore
                                    .orEmpty()
                                    .map { t -> t.toTile() },
                            genres = a.genres,
                            canAdd = access.manage,
                            canAsk = access.ask,
                        )
                    }
                    true
                }
            }
        }

    private fun ch.orellbuehler.acolyte.api.model.MusicRow.toNewShelf(
        day: String,
        access: MusicAccess,
    ): MusicShelfRow =
        MusicShelfRow(
            id = id,
            type = type,
            title = title,
            subtitle = reason,
            shape = MusicRules.shape(shape),
            items = items.map { c -> c.toItem(id, day, access, MusicRules.newLine(id, c, day)) },
        )

    suspend fun toggleFollow(key: String) {
        val item = mutableState.value.let { s -> (s.hero + s.rows.flatMap { it.items }).firstOrNull { it.key == key } } ?: return
        if (!item.upcoming || item.kind != MusicCardKind.ALBUM) return
        val on = !item.following
        setFollowing(item.mbid, on)
        val body = MusicAlbumFollowBody(item.name, item.artist, MusicRules.dayOf(item.date))
        val result =
            if (on) {
                api.result(MusicEndpoints.followAlbum, body, "mbid" to item.mbid)
            } else {
                api.result(AccountEndpoints.unfollowAlbum, "mbid" to item.mbid)
            }
        if (result is ApiResult.Failed) {
            setFollowing(item.mbid, !on)
            mutableState.update { it.copy(note = notes.next(MusicNoteKind.FOLLOW_FAILED, text = result.error.message)) }
        }
    }

    private fun setFollowing(
        mbid: String,
        on: Boolean,
    ) = mutableState.update { s ->
        s.copy(
            hero = s.hero.map { if (it.mbid == mbid) it.copy(following = on) else it },
            rows = s.rows.map { r -> r.copy(items = r.items.map { if (it.mbid == mbid) it.copy(following = on) else it }) },
        )
    }

    private companion object {
        const val BUILDING_TRIES = 30
        const val BUILDING_MILLIS = 3_000L
    }
}

data class MusicBrowseState(
    val status: LoadStatus = LoadStatus(),
    val kid: Boolean = false,
    val sections: List<MusicTileSection> = emptyList(),
    val tiles: List<MusicTile> = emptyList(),
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && sections.isEmpty() && tiles.isEmpty()
}

class MusicBrowsePresenter internal constructor(
    private val api: Api,
    private val region: String,
) {
    private val mutableState = MutableStateFlow(MusicBrowseState())

    val state: StateFlow<MusicBrowseState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        mutableState.update { it.copy(status = it.status.started()) }
        when (val result = api.result(MusicEndpoints.tiles, "region" to region)) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        status = LoadStatus.ready,
                        kid = result.value.kid,
                        sections =
                            result.value.sections
                                .filter { s -> s.tiles.isNotEmpty() }
                                .map { s -> MusicTileSection(s.id, s.title, s.tiles.map { t -> t.toTile() }) },
                        tiles = result.value.explore.map { t -> t.toTile() },
                    )
                }
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
        }
    }
}
