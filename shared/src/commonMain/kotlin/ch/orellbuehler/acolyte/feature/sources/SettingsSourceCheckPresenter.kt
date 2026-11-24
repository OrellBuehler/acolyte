package ch.orellbuehler.acolyte.feature.sources

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.SourcesEndpoints
import ch.orellbuehler.acolyte.api.model.SettingsSourceCheckBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsSourceCheckResult(
    val query: String,
    val kind: String,
    val lines: List<SettingsSourceCheckLine>,
) {
    val total: Int get() = lines.size

    val connected: Int get() = lines.count { it.word == SettingsSourceCheckWord.CONNECTED }

    val paused: Int get() = lines.count { it.word == SettingsSourceCheckWord.PAUSED }

    val failed: Int get() = total - connected - paused
}

data class SettingsSourceCheckState(
    val running: Boolean = false,
    val result: SettingsSourceCheckResult? = null,
    val error: ApiError? = null,
)

class SettingsSourceCheckPresenter internal constructor(
    private val api: Api,
) {
    private val mutableState = MutableStateFlow(SettingsSourceCheckState())

    val state: StateFlow<SettingsSourceCheckState> = mutableState.asStateFlow()

    suspend fun run(
        kind: String,
        title: String,
    ) {
        if (mutableState.value.running) return
        mutableState.update { it.copy(running = true, error = null) }
        val body = SettingsSourceCheckBody(title.trim().take(MAX_QUERY), kind)
        when (val result = api.result(SourcesEndpoints.check, body)) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        running = false,
                        result =
                            SettingsSourceCheckResult(
                                result.value.query,
                                result.value.kind,
                                result.value.sources.map(SettingsSourcesLogic::checkLine),
                            ),
                    )
                }
            is ApiResult.Failed -> mutableState.update { it.copy(running = false, error = result.error) }
        }
    }

    fun titleAfterKind(
        old: String,
        title: String,
        new: String,
    ): String = SettingsSourcesLogic.titleAfterKind(old, title, new)

    fun sample(kind: String): String = SettingsSourcesLogic.checkSample(kind)

    internal companion object {
        const val MAX_QUERY = 200
    }
}
