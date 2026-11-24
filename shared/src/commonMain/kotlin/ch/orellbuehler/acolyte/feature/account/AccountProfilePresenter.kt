package ch.orellbuehler.acolyte.feature.account

import ch.orellbuehler.acolyte.api.AccountEndpoints
import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AppEndpoints
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.model.AccountAlerts
import ch.orellbuehler.acolyte.api.model.AccountFollow
import ch.orellbuehler.acolyte.api.model.AccountLanguageChange
import ch.orellbuehler.acolyte.api.model.AccountNameChange
import ch.orellbuehler.acolyte.api.model.AccountPicture
import ch.orellbuehler.acolyte.api.model.MyProfile
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.session.Account
import ch.orellbuehler.acolyte.session.AccountRules
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val ACCOUNT_LANGUAGES: List<String> =
    (
        "en es pt-br fr de ru uk zh-cn ja ko hi ar it nl pl tr pt-pt sv nb da fi cs hu el he ro id vi th zh-tw fa bg sk ca ms sr hr " +
            "lt fil bn et lv is ta"
    ).split(" ")

const val ACCOUNT_PICTURE_LIMIT: Int = 5 * 1024 * 1024

enum class AccountSaved {
    NAME,
    PICTURE,
    PICTURE_REMOVED,
    LANGUAGE,
    ALERTS,
    UNFOLLOWED,
}

enum class AccountProblem {
    NAME_INVALID,
    PICTURE_TOO_BIG,
    PICTURE_EMPTY,
}

data class AccountProfileState(
    val status: LoadStatus = LoadStatus(),
    val profile: MyProfile = MyProfile(),
    val alerts: AccountAlerts = AccountAlerts(),
    val follows: List<AccountFollow> = emptyList(),
    val followsOn: Boolean = false,
    val busy: Boolean = false,
    val saved: AccountSaved? = null,
    val problem: AccountProblem? = null,
    val actionError: ApiError? = null,
) {
    val newEpisodes: Boolean get() = alerts.newEpisodes ?: true
}

class AccountProfilePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val refreshAccount: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow(AccountProfileState())
    private val lock = Mutex()

    val state: StateFlow<AccountProfileState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            mutableState.update { it.copy(status = it.status.started()) }
            val wishlist = account()?.isOn(Part.WISHLIST) == true
            val (profile, alerts, follows) =
                coroutineScope {
                    val p = async { api.result(AppEndpoints.profile) }
                    val a = async { api.result(AccountEndpoints.alerts) }
                    val f = async { if (wishlist) api.result(AccountEndpoints.follows) else null }
                    Triple(p.await(), a.await(), f.await())
                }
            if (profile is ApiResult.Failed) {
                mutableState.update { it.copy(status = it.status.failed(profile.error)) }
                return
            }
            mutableState.update {
                it.copy(
                    status = LoadStatus.ready,
                    profile = (profile as ApiResult.Ok).value,
                    alerts = alerts.valueOrNull() ?: it.alerts,
                    follows = follows?.valueOrNull()?.follows ?: it.follows,
                    followsOn = wishlist,
                )
            }
        }
    }

    fun consume() {
        mutableState.update { it.copy(saved = null, problem = null, actionError = null) }
    }

    suspend fun saveName(name: String) {
        val trimmed = name.trim()
        if (!AccountRules.validDisplayName(trimmed)) {
            mutableState.update { it.copy(problem = AccountProblem.NAME_INVALID) }
            return
        }
        act(AccountSaved.NAME) {
            api.result(AccountEndpoints.updateName, AccountNameChange(trimmed)).onSuccess { profile ->
                setProfile(profile)
            }
        }
    }

    suspend fun saveLanguage(code: String) {
        act(AccountSaved.LANGUAGE) { api.result(AccountEndpoints.updateLanguage, AccountLanguageChange(code)).onSuccess { setProfile(it) } }
    }

    suspend fun uploadPicture(
        bytes: ByteArray,
        type: String,
    ) {
        if (bytes.isEmpty()) {
            mutableState.update { it.copy(problem = AccountProblem.PICTURE_EMPTY) }
            return
        }
        if (bytes.size > ACCOUNT_PICTURE_LIMIT) {
            mutableState.update { it.copy(problem = AccountProblem.PICTURE_TOO_BIG) }
            return
        }
        act(AccountSaved.PICTURE) { api.result(AccountEndpoints.avatarPut, AccountPicture(bytes, type)) }
        refreshAccount()
    }

    suspend fun removePicture() {
        act(AccountSaved.PICTURE_REMOVED) { api.result(AccountEndpoints.avatarDelete) }
        refreshAccount()
    }

    suspend fun saveAlerts(
        email: String,
        discordId: String,
        newEpisodes: Boolean,
    ) {
        val body = AccountAlerts(email.trim(), discordId.trim(), newEpisodes)
        act(AccountSaved.ALERTS) {
            api.result(AccountEndpoints.saveAlerts, body).onSuccess { answer -> mutableState.update { it.copy(alerts = answer) } }
        }
    }

    suspend fun unfollow(follow: AccountFollow) {
        val path = follow.unfollow
        val result: ApiResult<*> =
            when {
                path.startsWith("/titles/") ->
                    api.result(
                        TitleEndpoints.followTitleOff,
                        "id" to path.removePrefix("/titles/").substringBefore('/'),
                    )
                path.startsWith("/metadata/movie/") ->
                    api.result(TitleEndpoints.followMovieOff, "tmdb_id" to path.removePrefix("/metadata/movie/").substringBefore('/'))
                path.startsWith("/metadata/show/") ->
                    api.result(TitleEndpoints.followShowOff, "tmdb_id" to path.removePrefix("/metadata/show/").substringBefore('/'))
                path.startsWith("/metadata/album/") ->
                    api.result(AccountEndpoints.unfollowAlbum, "mbid" to path.removePrefix("/metadata/album/").substringBefore('/'))
                else -> return
            }
        when (result) {
            is ApiResult.Ok<*> ->
                mutableState.update { state ->
                    state.copy(follows = state.follows - follow, saved = AccountSaved.UNFOLLOWED, actionError = null)
                }
            is ApiResult.Failed -> mutableState.update { it.copy(actionError = result.error) }
        }
    }

    private fun setProfile(profile: MyProfile) {
        mutableState.update { it.copy(profile = profile) }
    }

    private suspend fun <T> act(
        saved: AccountSaved,
        block: suspend () -> ApiResult<T>,
    ) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, actionError = null, problem = null, saved = null) }
        block()
            .onSuccess { mutableState.update { it.copy(busy = false, saved = saved) } }
            .onFailure { error -> mutableState.update { it.copy(busy = false, actionError = error) } }
        if (saved == AccountSaved.NAME) refreshAccount()
    }
}
