package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.MusicEndpoints
import ch.orellbuehler.acolyte.api.model.AddArtistBody
import ch.orellbuehler.acolyte.api.model.LibraryFolder
import ch.orellbuehler.acolyte.api.model.MusicSettings
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MusicFolderOption(
    val id: Long,
    val label: String,
    val path: String,
    val defaultRules: Long,
)

data class MusicRulesOption(
    val id: Long,
    val name: String,
)

enum class MusicAddOutcome {
    ADDED,
    ALREADY,
}

data class MusicAddState(
    val status: LoadStatus = LoadStatus(),
    val mbid: String = "",
    val name: String = "",
    val disambiguation: String = "",
    val folders: List<MusicFolderOption> = emptyList(),
    val rules: List<MusicRulesOption> = emptyList(),
    val folderId: Long = 0,
    val rulesId: Long = 0,
    val handsOff: Boolean = false,
    val albumChoice: String = "all",
    val newAlbums: String = "all",
    val albumTypes: List<String> = MusicOptions.defaultAlbumTypes,
    val secondaryTypes: List<String> = MusicOptions.defaultSecondary,
    val statuses: List<String> = MusicOptions.defaultStatuses,
    val keep: Boolean = true,
    val follow: Boolean = true,
    val titleId: Long = 0,
    val neverAdd: Boolean = false,
    val busy: Boolean = false,
    val error: ApiError? = null,
    val outcome: MusicAddOutcome? = null,
    val addedTitleId: Long = 0,
) {
    val noFolder: Boolean get() = folders.isEmpty() && !handsOff

    val canSubmit: Boolean
        get() =
            status.phase == Phase.READY &&
                !busy &&
                outcome == null &&
                !noFolder &&
                albumTypes.isNotEmpty() &&
                secondaryTypes.isNotEmpty() &&
                statuses.isNotEmpty() &&
                titleId == 0L &&
                !neverAdd
}

class MusicAddPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    mbid: String,
    name: String,
) {
    private val mutableState = MutableStateFlow(MusicAddState(mbid = mbid, name = name))

    val state: StateFlow<MusicAddState> = mutableState.asStateFlow()

    suspend fun load() {
        coroutineScope {
            val lookup = async { api.result(LibraryEndpoints.lookupArtists, "term" to "mbid:${mutableState.value.mbid}").valueOrNull() }
            val folders = async { api.result(LibraryEndpoints.folders).valueOrNull() }
            val rules = async { api.result(LibraryEndpoints.pickRules).valueOrNull() }
            val defaults = async { api.result(MusicEndpoints.settings).valueOrNull() }
            val found = lookup.await()?.artists?.firstOrNull()
            val list =
                folders
                    .await()
                    ?.libraryFolders
                    .orEmpty()
                    .filter { it.kind == "music" }
                    .map { it.toOption() }
            val sets =
                rules
                    .await()
                    ?.pickRules
                    .orEmpty()
                    .filter { it.kinds.isEmpty() || "music" in it.kinds || "artist" in it.kinds }
            val d = defaults.await() ?: MusicSettings()
            val folder =
                folders.await()?.libraryFolders.orEmpty().filter { it.kind == "music" }.let { all ->
                    all.firstOrNull {
                        it.health ==
                            "ok"
                    }
                        ?: all.firstOrNull()
                }
            val rule = sets.firstOrNull { it.id == folder?.defaultPickRulesId } ?: sets.firstOrNull()
            mutableState.update {
                it.copy(
                    status = LoadStatus.ready,
                    name = found?.name?.takeIf { n -> n.isNotEmpty() } ?: it.name,
                    disambiguation = found?.disambiguation.orEmpty(),
                    titleId = found?.titleId ?: 0,
                    neverAdd = found?.neverAdd == true,
                    folders = list,
                    rules = sets.map { r: PickRule -> MusicRulesOption(r.id, r.name) },
                    folderId = folder?.id ?: 0,
                    rulesId = rule?.id ?: 0,
                    handsOff = account()?.handsOff == true,
                    albumChoice = d.albumChoice,
                    newAlbums = d.newAlbums,
                    albumTypes = d.albumTypes.ifEmpty { MusicOptions.defaultAlbumTypes },
                    secondaryTypes = d.secondaryTypes.ifEmpty { MusicOptions.defaultSecondary },
                    statuses = d.releaseStatuses.ifEmpty { MusicOptions.defaultStatuses },
                )
            }
        }
    }

    fun pickFolder(id: Long) =
        mutableState.update { s ->
            val folder = s.folders.firstOrNull { it.id == id } ?: return@update s
            val rule = s.rules.firstOrNull { it.id == folder.defaultRules }
            s.copy(folderId = id, rulesId = rule?.id ?: s.rulesId)
        }

    fun pickRules(id: Long) = mutableState.update { it.copy(rulesId = id) }

    fun setAlbumChoice(id: String) = mutableState.update { it.copy(albumChoice = id) }

    fun setNewAlbums(id: String) = mutableState.update { it.copy(newAlbums = id) }

    fun toggleAlbumType(
        id: String,
        on: Boolean,
    ) = mutableState.update { it.copy(albumTypes = MusicOptions.toggle(it.albumTypes, id, on, MusicOptions.albumTypes)) }

    fun toggleSecondary(
        id: String,
        on: Boolean,
    ) = mutableState.update { it.copy(secondaryTypes = MusicOptions.toggle(it.secondaryTypes, id, on, MusicOptions.secondaryTypes)) }

    fun toggleStatus(
        id: String,
        on: Boolean,
    ) = mutableState.update { it.copy(statuses = MusicOptions.toggle(it.statuses, id, on, MusicOptions.statuses)) }

    fun setKeep(on: Boolean) = mutableState.update { it.copy(keep = on) }

    fun setFollow(on: Boolean) = mutableState.update { it.copy(follow = on) }

    suspend fun submit() {
        val s = mutableState.value
        if (!s.canSubmit) return
        mutableState.update { it.copy(busy = true, error = null) }
        val body =
            AddArtistBody(
                id = "mbid:${s.mbid}",
                follow = s.follow,
                libraryFolderId = if (s.folders.isEmpty()) 0 else s.folderId,
                pickRulesId = s.rulesId,
                keepUpToDate = s.keep,
                albums = s.albumChoice,
                newAlbums = s.newAlbums,
                albumTypes = s.albumTypes,
                secondaryTypes = s.secondaryTypes,
                releaseStatuses = s.statuses,
            )
        when (val result = api.result(MusicEndpoints.addArtist, body)) {
            is ApiResult.Ok ->
                mutableState.update { it.copy(busy = false, outcome = MusicAddOutcome.ADDED, addedTitleId = result.value.title?.id ?: 0) }
            is ApiResult.Failed -> {
                val inLibrary =
                    result.error.status == CONFLICT && result.error.message.contains("already in the Library", ignoreCase = true)
                mutableState.update {
                    it.copy(busy = false, error = result.error, outcome = if (inLibrary) MusicAddOutcome.ALREADY else null)
                }
            }
        }
    }

    private fun LibraryFolder.toOption(): MusicFolderOption = MusicFolderOption(id, shown, path, defaultPickRulesId ?: 0)

    private companion object {
        const val CONFLICT = 409
    }
}
