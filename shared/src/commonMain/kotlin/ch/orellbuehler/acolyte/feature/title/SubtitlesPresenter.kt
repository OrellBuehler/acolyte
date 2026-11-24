package ch.orellbuehler.acolyte.feature.title

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.TitleEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.FileSubtitleSearch
import ch.orellbuehler.acolyte.api.model.SubtitleChoices
import ch.orellbuehler.acolyte.api.model.SubtitleChooseBody
import ch.orellbuehler.acolyte.api.model.SubtitleFileView
import ch.orellbuehler.acolyte.api.model.SubtitleLanguage
import ch.orellbuehler.acolyte.api.model.SubtitleReasonBody
import ch.orellbuehler.acolyte.api.model.SubtitleRowView
import ch.orellbuehler.acolyte.api.model.SubtitleSyncBody
import ch.orellbuehler.acolyte.api.model.SubtitleTranslateBody
import ch.orellbuehler.acolyte.api.model.SubtitleWant
import ch.orellbuehler.acolyte.api.model.SubtitleWantBody
import ch.orellbuehler.acolyte.api.model.SubtitleWishBody
import ch.orellbuehler.acolyte.api.model.TitleSubtitles
import ch.orellbuehler.acolyte.api.onFailure
import ch.orellbuehler.acolyte.api.onSuccess
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.live.LiveEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

enum class SubtitleMode {
    DEFAULT,
    OFF,
    CUSTOM,
}

data class SubtitleChoiceState(
    val fileId: Long,
    val loading: Boolean = true,
    val choices: SubtitleChoices? = null,
)

data class SubtitlesState(
    val itemId: Long,
    val movie: Boolean,
    val status: LoadStatus = LoadStatus(),
    val data: TitleSubtitles = TitleSubtitles(),
    val languages: List<SubtitleLanguage> = emptyList(),
    val translator: Boolean = false,
    val editing: Boolean = false,
    val editOwn: Boolean = false,
    val editWants: List<SubtitleWant> = emptyList(),
    val choices: SubtitleChoiceState? = null,
    val busy: String = "",
    val feedback: Feedback? = null,
) {
    val files: List<SubtitleFileView>
        get() = if (itemId > 0) data.files.filter { it.itemId == itemId } else data.files

    val listMode: Boolean get() = !movie && itemId == 0L

    val ownWish: List<SubtitleWant>? get() = data.wishes.firstOrNull { it.itemId == itemId && it.versionId == 0L }?.languages

    val mode: SubtitleMode
        get() {
            val own = ownWish ?: return SubtitleMode.DEFAULT
            return if (own.isEmpty()) SubtitleMode.OFF else SubtitleMode.CUSTOM
        }

    val wish: List<SubtitleWant> get() = files.firstOrNull()?.wish?.languages ?: ownWish.orEmpty()

    val wishFrom: String get() =
        files.firstOrNull()?.wish?.from
            ?: if (ownWish != null) (if (itemId > 0) "episode" else "title") else "default"

    val wishCodes: String get() = wish.map { it.language.uppercase() }.distinct().joinToString(", ")

    val missingCount: Int get() = data.files.sumOf { TitleLogic.missingSubtitles(it.subtitles).size }

    val canCleanAll: Boolean get() =
        data.files.any { f ->
            f.subtitles.any {
                TitleLogic.ourSubtitle(it) &&
                    it.relativePath.endsWith(".srt", true)
            }
        }

    val editProblem: WishProblem? get() = if (editOwn) TitleLogic.wishProblem(editWants) else null

    fun row(id: Long): SubtitleRowView? = data.files.firstNotNullOfOrNull { f -> f.subtitles.firstOrNull { it.id == id } }

    fun languageName(code: String): String = languages.firstOrNull { it.code == code }?.name ?: code

    fun present(file: SubtitleFileView): List<SubtitleRowView> = TitleLogic.presentSubtitles(file.subtitles)

    fun missing(file: SubtitleFileView): List<SubtitleRowView> = TitleLogic.missingSubtitles(file.subtitles)

    fun translatable(
        file: SubtitleFileView,
        language: String,
    ): List<SubtitleRowView> = present(file).filter { it.language != language && !it.picture }
}

class SubtitlesPresenter internal constructor(
    private val api: Api,
    private val titleId: Long,
    itemId: Long,
    movie: Boolean,
    private val settings: Boolean,
) {
    private val mutableState = MutableStateFlow(SubtitlesState(itemId, movie))
    private val feedback = FeedbackCounter()

    val state: StateFlow<SubtitlesState> = mutableState.asStateFlow()

    private val current: SubtitlesState get() = mutableState.value

    private val item: Long? get() = current.itemId.takeIf { it > 0 }

    suspend fun load() {
        mutableState.update { it.copy(status = it.status.started()) }
        when (val result = api.result(TitleEndpoints.subtitles, "id" to titleId)) {
            is ApiResult.Ok -> mutableState.update { it.copy(status = LoadStatus.ready, data = result.value) }
            is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
        }
    }

    suspend fun loadExtras() {
        if (current.languages.isEmpty()) {
            api.result(LibraryEndpoints.subtitleLanguages).onSuccess { v -> mutableState.update { it.copy(languages = v.languages) } }
        }
        if (settings) {
            api.result(TitleSupportEndpoints.translator).onSuccess { v ->
                mutableState.update { it.copy(translator = v.kind.isNotEmpty() && v.url.isNotEmpty()) }
            }
        }
    }

    suspend fun watch() {
        api.live.collect { event ->
            val data = event.data as? JsonObject
            val title = (data?.get("TitleID") as? JsonPrimitive)?.longOrNull
            val kind = (data?.get("Kind") as? JsonPrimitive)?.contentOrNull.orEmpty()
            if (event.topic == LiveEvent.RESYNC || (event.topic == "event" && title == titleId && kind.startsWith("subtitle_"))) load()
        }
    }

    suspend fun useDefault() = wish(SubtitleWishBody(inherit = true, itemId = item), FeedbackKind.SUBTITLES_DEFAULT)

    suspend fun turnOff() = wish(SubtitleWishBody(languages = emptyList(), itemId = item), FeedbackKind.SUBTITLES_SAVED)

    fun startEdit() {
        val s = current
        val own = s.ownWish
        mutableState.update { it.copy(editing = true, editOwn = own != null, editWants = own ?: s.wish) }
    }

    fun cancelEdit() = mutableState.update { it.copy(editing = false) }

    fun setEditOwn(own: Boolean) = mutableState.update { it.copy(editOwn = own) }

    fun addWant() {
        val wants = current.editWants
        if (wants.size >= MAX_WANTS) return
        mutableState.update { it.copy(editWants = wants + SubtitleWant(language = TitleLogic.nextLanguage(wants))) }
    }

    fun removeWant(index: Int) = mutableState.update { it.copy(editWants = it.editWants.filterIndexed { i, _ -> i != index }) }

    fun updateWant(
        index: Int,
        want: SubtitleWant,
    ) = mutableState.update { it.copy(editWants = it.editWants.mapIndexed { i, w -> if (i == index) want else w }) }

    suspend fun saveEdit() {
        val s = current
        if (s.editProblem != null) return
        val body =
            if (s.editOwn) {
                SubtitleWishBody(
                    languages =
                        s.editWants.map {
                            SubtitleWantBody(
                                it.language,
                                it.hearingImpaired,
                                it.forced,
                                it.translateFrom.ifEmpty { null },
                            )
                        },
                    itemId = item,
                )
            } else {
                SubtitleWishBody(inherit = true, itemId = item)
            }
        if (wish(body, if (s.editOwn) FeedbackKind.SUBTITLES_SAVED else FeedbackKind.SUBTITLES_DEFAULT)) {
            mutableState.update { it.copy(editing = false) }
        }
    }

    private suspend fun wish(
        body: SubtitleWishBody,
        done: FeedbackKind,
    ): Boolean {
        var ok = false
        busy("wish") {
            api
                .result(TitleEndpoints.subtitleWish, body, "id" to titleId)
                .onSuccess {
                    ok = true
                    say(feedback.next(done))
                }.onFailure { say(feedback.failed(it)) }
        }
        if (ok) load()
        return ok
    }

    suspend fun search() {
        busy("search") {
            api
                .result(LibraryEndpoints.searchFileSubtitles, FileSubtitleSearch(titleId = titleId, itemId = item))
                .onSuccess { v ->
                    say(
                        feedback.next(
                            if (v.queued >
                                0
                            ) {
                                FeedbackKind.SUBTITLES_SEARCHING
                            } else {
                                FeedbackKind.SUBTITLES_NO_FILE
                            },
                        ),
                    )
                }.onFailure { say(feedback.failed(it)) }
        }
    }

    suspend fun openChoices(fileId: Long) {
        mutableState.update { it.copy(choices = SubtitleChoiceState(fileId)) }
        val result = api.result(TitleEndpoints.subtitleChoices, "id" to fileId)
        mutableState.update { st ->
            if (st.choices?.fileId != fileId) return@update st
            when (result) {
                is ApiResult.Ok -> st.copy(choices = SubtitleChoiceState(fileId, loading = false, choices = result.value))
                is ApiResult.Failed -> st.copy(choices = null, feedback = feedback.failed(result.error))
            }
        }
    }

    fun closeChoices() = mutableState.update { it.copy(choices = null) }

    suspend fun choose(
        fileId: Long,
        site: String,
        key: String,
    ) {
        var ok = false
        busy("choose-$key") {
            api
                .result(TitleEndpoints.subtitleChoose, SubtitleChooseBody(site, key), "id" to fileId)
                .onSuccess { v ->
                    ok = true
                    say(feedback.next(FeedbackKind.SUBTITLE_SAVED, v.path.substringAfterLast('/')))
                }.onFailure { say(feedback.failed(it)) }
        }
        if (ok) {
            mutableState.update { it.copy(choices = null) }
            load()
        }
    }

    suspend fun translate(
        fileId: Long,
        want: SubtitleRowView,
        from: SubtitleRowView,
    ) {
        val body =
            if (from.place == "beside") {
                SubtitleTranslateBody(
                    language = want.language,
                    forced = want.forced.takeIf { it },
                    source = "beside",
                    path = from.relativePath,
                )
            } else {
                SubtitleTranslateBody(
                    language = want.language,
                    from = from.language,
                    forced = want.forced.takeIf { it },
                    source = "built_in",
                    track = from.trackIndex,
                )
            }
        busy("translate-${want.id}") {
            api
                .result(TitleEndpoints.subtitleTranslate, body, "id" to fileId)
                .onSuccess { say(feedback.next(FeedbackKind.TRANSLATING, current.languageName(want.language))) }
                .onFailure { say(feedback.failed(it)) }
        }
    }

    suspend fun sync(
        subtitleId: Long,
        offsetMs: Int,
        fromFps: Double?,
        toFps: Double?,
    ): Boolean {
        var ok = false
        busy("sync-$subtitleId") {
            val body = SubtitleSyncBody(offsetMs = offsetMs.takeIf { it != 0 }, fromFps = fromFps, toFps = toFps)
            api
                .result(TitleEndpoints.subtitleSync, body, "id" to subtitleId)
                .onSuccess {
                    ok = true
                    say(feedback.next(FeedbackKind.TIMING_FIXED))
                }.onFailure { say(feedback.failed(it)) }
        }
        if (ok) load()
        return ok
    }

    suspend fun clean(subtitleId: Long) {
        busy("clean-$subtitleId") {
            api
                .result(TitleEndpoints.subtitleClean, "id" to subtitleId)
                .onSuccess { v -> say(feedback.next(if (v.changed) FeedbackKind.CLEANED else FeedbackKind.NOTHING_TO_CLEAN, count = 1)) }
                .onFailure { say(feedback.failed(it)) }
        }
        load()
    }

    suspend fun cleanAll() {
        busy("clean") {
            api
                .result(TitleEndpoints.subtitlesClean, "id" to titleId)
                .onSuccess { v ->
                    say(
                        feedback.next(
                            if (v.changed >
                                0
                            ) {
                                FeedbackKind.CLEANED
                            } else {
                                FeedbackKind.NOTHING_TO_CLEAN
                            },
                            count = v.changed,
                        ),
                    )
                }.onFailure { say(feedback.failed(it)) }
        }
        load()
    }

    suspend fun turnDown(
        subtitleId: Long,
        reason: String,
    ): Boolean {
        var ok = false
        busy("down-$subtitleId") {
            api
                .result(
                    TitleEndpoints.subtitleTurnDown,
                    SubtitleReasonBody(reason.trim().take(MAX_REASON).ifEmpty { null }),
                    "id" to subtitleId,
                ).onSuccess {
                    ok = true
                    say(feedback.next(FeedbackKind.SUBTITLE_TURNED_DOWN))
                }.onFailure { say(feedback.failed(it)) }
        }
        if (ok) load()
        return ok
    }

    private inline fun busy(
        key: String,
        block: () -> Unit,
    ) {
        mutableState.update { it.copy(busy = key) }
        try {
            block()
        } finally {
            mutableState.update { if (it.busy == key) it.copy(busy = "") else it }
        }
    }

    private fun say(next: Feedback) {
        mutableState.update { it.copy(feedback = next) }
    }

    internal companion object {
        const val MAX_WANTS = 10
        const val MAX_REASON = 200
    }
}
