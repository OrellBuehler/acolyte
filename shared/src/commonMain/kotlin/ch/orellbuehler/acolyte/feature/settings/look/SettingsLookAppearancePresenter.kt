package ch.orellbuehler.acolyte.feature.settings.look

import ch.orellbuehler.acolyte.api.AccountEndpoints
import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.AppEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.SettingsSharedEndpoints
import ch.orellbuehler.acolyte.api.model.AccountLanguageChange
import ch.orellbuehler.acolyte.api.model.SettingsUiChange
import ch.orellbuehler.acolyte.api.model.SettingsUiSettings
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal val MOST_USED_LANGUAGES: List<String> = listOf("en", "es", "pt-br", "fr", "de", "it", "ru", "zh-cn", "ja", "ar")

internal val OTHER_LANGUAGES: List<String> =
    listOf(
        "pt-pt",
        "nl",
        "sv",
        "nb",
        "da",
        "fi",
        "uk",
        "pl",
        "cs",
        "hu",
        "ro",
        "el",
        "sk",
        "bg",
        "sr",
        "hr",
        "ca",
        "lt",
        "lv",
        "et",
        "is",
        "zh-tw",
        "ko",
        "hi",
        "bn",
        "ta",
        "id",
        "ms",
        "fil",
        "vi",
        "th",
        "tr",
        "he",
        "fa",
    )

enum class SettingsLookAppearanceSaved {
    NONE,
    LANGUAGE,
    AUTOPLAY_ON,
    AUTOPLAY_OFF,
    ART_LIBRARY,
    ART_TRENDING,
}

data class SettingsLookAppearanceState(
    val status: LoadStatus = LoadStatus(),
    val language: String = "",
    val languageSaving: Boolean = false,
    val languageError: ApiError? = null,
    val canChangeEveryone: Boolean = false,
    val everyoneLoaded: Boolean = false,
    val autoplayTrailers: Boolean = true,
    val signInArt: Boolean = false,
    val everyoneSaving: Boolean = false,
    val everyoneError: ApiError? = null,
    val canSeeStats: Boolean = false,
    val savedCount: Int = 0,
    val saved: SettingsLookAppearanceSaved = SettingsLookAppearanceSaved.NONE,
) {
    val showEveryone: Boolean get() = canChangeEveryone && everyoneLoaded

    val mostUsedLanguages: List<String> get() = MOST_USED_LANGUAGES

    val otherLanguages: List<String> get() = OTHER_LANGUAGES
}

class SettingsLookAppearancePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsLookAppearanceState())
    private val lock = Mutex()

    val state: StateFlow<SettingsLookAppearanceState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            val canChange = me?.can(Need.SETTINGS) == true
            mutableState.update {
                it.copy(
                    status = it.status.started(),
                    canChangeEveryone = canChange,
                    canSeeStats = me?.allows(Need.USERS, Part.MEDIA) == true,
                )
            }
            coroutineScope {
                val ui = async { if (canChange) api.result(SettingsSharedEndpoints.uiSettings).valueOrNull() else null }
                val profile = api.result(AppEndpoints.profile)
                val settings = ui.await()
                when (profile) {
                    is ApiResult.Ok ->
                        mutableState.update {
                            it.copy(
                                status = LoadStatus.ready,
                                language = profile.value.language,
                                everyoneLoaded = settings != null,
                                autoplayTrailers = settings?.autoplayTrailers ?: it.autoplayTrailers,
                                signInArt = settings?.signInLibraryArt ?: it.signInArt,
                            )
                        }
                    is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(profile.error)) }
                }
            }
        }
    }

    suspend fun setLanguage(code: String) {
        val wanted = code.trim().lowercase()
        if (wanted.isNotEmpty() && wanted !in MOST_USED_LANGUAGES && wanted !in OTHER_LANGUAGES) return
        lock.withLock {
            if (mutableState.value.language == wanted || mutableState.value.languageSaving) return
            mutableState.update { it.copy(languageSaving = true, languageError = null) }
            when (val result = api.result(AccountEndpoints.updateLanguage, AccountLanguageChange(wanted))) {
                is ApiResult.Ok ->
                    mutableState.update {
                        it.copy(
                            language = result.value.language.ifEmpty { wanted },
                            languageSaving = false,
                            savedCount = it.savedCount + 1,
                            saved = SettingsLookAppearanceSaved.LANGUAGE,
                        )
                    }
                is ApiResult.Failed -> mutableState.update { it.copy(languageSaving = false, languageError = result.error) }
            }
        }
    }

    suspend fun setAutoplayTrailers(on: Boolean) =
        change(SettingsUiChange(autoplayTrailers = on)) {
            if (it.autoplayTrailers) SettingsLookAppearanceSaved.AUTOPLAY_ON else SettingsLookAppearanceSaved.AUTOPLAY_OFF
        }

    suspend fun setSignInArt(on: Boolean) =
        change(SettingsUiChange(signInLibraryArt = on)) {
            if (it.signInLibraryArt) SettingsLookAppearanceSaved.ART_LIBRARY else SettingsLookAppearanceSaved.ART_TRENDING
        }

    private suspend fun change(
        body: SettingsUiChange,
        saved: (SettingsUiSettings) -> SettingsLookAppearanceSaved,
    ) {
        lock.withLock {
            if (!mutableState.value.showEveryone || mutableState.value.everyoneSaving) return
            mutableState.update { it.copy(everyoneSaving = true, everyoneError = null) }
            when (val result = api.result(SettingsSharedEndpoints.changeUiSettings, body)) {
                is ApiResult.Ok ->
                    mutableState.update {
                        it.copy(
                            autoplayTrailers = result.value.autoplayTrailers,
                            signInArt = result.value.signInLibraryArt,
                            everyoneSaving = false,
                            savedCount = it.savedCount + 1,
                            saved = saved(result.value),
                        )
                    }
                is ApiResult.Failed -> mutableState.update { it.copy(everyoneSaving = false, everyoneError = result.error) }
            }
        }
    }
}
