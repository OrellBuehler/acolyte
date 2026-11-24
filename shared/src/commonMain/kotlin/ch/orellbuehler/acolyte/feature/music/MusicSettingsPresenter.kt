package ch.orellbuehler.acolyte.feature.music

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.MusicEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.model.MusicSettings
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MusicSettingsState(
    val status: LoadStatus = LoadStatus(),
    val saved: MusicSettings = MusicSettings(),
    val form: MusicSettings = MusicSettings(),
    val hours: String = "24",
    val canChange: Boolean = false,
    val saving: Boolean = false,
    val error: ApiError? = null,
    val justSaved: Boolean = false,
) {
    val dirty: Boolean get() = form != saved || hours != saved.cacheHours.toString()

    val emptyKinds: Boolean get() = form.albumTypes.isEmpty()
    val emptyExtra: Boolean get() = form.secondaryTypes.isEmpty()
    val emptyStatuses: Boolean get() = form.releaseStatuses.isEmpty()
    val hoursValid: Boolean get() = hours.toIntOrNull()?.let { it in 1..MAX_HOURS } == true

    val canSave: Boolean
        get() = dirty && !saving && canChange && !emptyKinds && !emptyExtra && !emptyStatuses && hoursValid

    private companion object {
        const val MAX_HOURS = 720
    }
}

class MusicSettingsPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(MusicSettingsState())

    val state: StateFlow<MusicSettingsState> = mutableState.asStateFlow()

    suspend fun load() {
        mutableState.update { it.copy(status = it.status.started()) }
        when (val result = api.result(MusicEndpoints.settings)) {
            is ApiResult.Ok -> {
                val v = result.value
                mutableState.update {
                    it.copy(
                        status = LoadStatus.ready,
                        saved = v,
                        form = v,
                        hours = v.cacheHours.toString(),
                        canChange = account()?.can(Need.SETTINGS) == true,
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
        }
    }

    private fun edit(block: (MusicSettings) -> MusicSettings) =
        mutableState.update {
            it.copy(form = block(it.form), justSaved = false, error = null)
        }

    fun setAlbumChoice(id: String) = edit { it.copy(albumChoice = id) }

    fun setNewAlbums(id: String) = edit { it.copy(newAlbums = id) }

    fun setPressingRule(id: String) = edit { it.copy(pressingRule = id) }

    fun toggleAlbumType(
        id: String,
        on: Boolean,
    ) = edit { it.copy(albumTypes = MusicOptions.toggle(it.albumTypes, id, on, MusicOptions.albumTypes)) }

    fun toggleSecondary(
        id: String,
        on: Boolean,
    ) = edit { it.copy(secondaryTypes = MusicOptions.toggle(it.secondaryTypes, id, on, MusicOptions.secondaryTypes)) }

    fun toggleStatus(
        id: String,
        on: Boolean,
    ) = edit { it.copy(releaseStatuses = MusicOptions.toggle(it.releaseStatuses, id, on, MusicOptions.statuses)) }

    fun setUrl(text: String) = edit { it.copy(musicbrainzUrl = text) }

    fun setHours(text: String) = mutableState.update { it.copy(hours = text.filter(Char::isDigit).take(HOURS_DIGITS), justSaved = false) }

    fun discard() = mutableState.update { it.copy(form = it.saved, hours = it.saved.cacheHours.toString(), error = null) }

    suspend fun save() {
        val s = mutableState.value
        if (!s.canSave) return
        mutableState.update { it.copy(saving = true, error = null) }
        val body = s.form.copy(musicbrainzUrl = s.form.musicbrainzUrl.trim(), cacheHours = s.hours.toInt())
        when (val result = api.result(MusicEndpoints.saveSettings, body)) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        saving = false,
                        saved = result.value,
                        form = result.value,
                        hours = result.value.cacheHours.toString(),
                        justSaved = true,
                    )
                }
            is ApiResult.Failed -> mutableState.update { it.copy(saving = false, error = result.error) }
        }
    }

    private companion object {
        const val HOURS_DIGITS = 4
    }
}
