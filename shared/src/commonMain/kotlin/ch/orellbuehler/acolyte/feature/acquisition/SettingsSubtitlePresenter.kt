package ch.orellbuehler.acolyte.feature.acquisition

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.SubtitleSettingsEndpoints
import ch.orellbuehler.acolyte.api.TitleSupportEndpoints
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleConfig
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleKind
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleSite
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleSiteTest
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleTidy
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleTranslatorTest
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleWant
import ch.orellbuehler.acolyte.api.model.SubtitleLanguage
import ch.orellbuehler.acolyte.api.model.SubtitleTranslator
import ch.orellbuehler.acolyte.api.valueOrNull
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.live.LiveEvent
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class SettingsSubtitleState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val canClean: Boolean = false,
    val saved: SettingsSubtitleConfig = SettingsSubtitleConfig(),
    val draft: SettingsSubtitleConfig = SettingsSubtitleConfig(),
    val numberText: Map<SettingsSubtitleNumber, String> = emptyMap(),
    val languages: List<SubtitleLanguage> = emptyList(),
    val cleanLanguage: String = SETTINGS_SUBTITLE_EVERY_LANGUAGE,
    val saving: Boolean = false,
    val saveError: ApiError? = null,
    val translatorSaved: SubtitleTranslator = SubtitleTranslator(),
    val translator: SettingsSubtitleTranslatorForm = SettingsSubtitleTranslatorForm(),
    val translatorSaving: Boolean = false,
    val translatorTesting: Boolean = false,
    val translatorError: ApiError? = null,
    val translatorTest: SettingsSubtitleTranslatorTest? = null,
    val kinds: List<SettingsSubtitleKind> = emptyList(),
    val sites: List<SettingsSubtitleSite> = emptyList(),
    val form: SettingsSubtitleSiteForm? = null,
    val testingSite: Long = 0,
    val siteTests: Map<Long, SettingsSubtitleSiteTest> = emptyMap(),
    val siteTestErrors: Map<Long, ApiError> = emptyMap(),
    val cleaning: Boolean = false,
    val note: SettingsSubtitleNote? = null,
) {
    val dirty: Boolean get() = draft != saved || numberText.any { (field, text) -> text != field.text(saved) }

    val numbersValid: Boolean get() = SettingsSubtitleNumber.entries.all { field -> field.parse(number(field.key)) != null }

    val wishProblem: SettingsSubtitleWishProblem? get() = settingsSubtitleWishProblem(draft.languages)

    val canSave: Boolean get() = dirty && numbersValid && wishProblem == null && !saving

    val translatorReady: Boolean get() = translatorSaved.kind.isNotEmpty() && translatorSaved.url.isNotEmpty()

    val translatorDirty: Boolean get() = translator != settingsSubtitleTranslatorForm(translatorSaved)

    val canSaveTranslator: Boolean get() = translatorDirty && translator.valid && !translatorSaving

    val canTestTranslator: Boolean get() = translatorSaved.kind.isNotEmpty() && !translatorDirty && !translatorTesting

    val modelSuggestions: List<String> get() = translatorTest?.models.orEmpty()

    val canAddLanguage: Boolean get() = draft.languages.size < SETTINGS_SUBTITLE_MAX_LANGUAGES

    val cleanRows: List<String> get() = settingsSubtitleCleanRows(draft.clean)

    val cleanAddable: List<SubtitleLanguage> get() = languages.filter { it.code !in draft.clean.keys }

    val cleanSteps: SettingsSubtitleTidy
        get() = draft.clean[cleanLanguage] ?: draft.clean[SETTINGS_SUBTITLE_EVERY_LANGUAGE] ?: SettingsSubtitleTidy()

    val offeredKinds: List<SettingsSubtitleKind> get() = kinds.filter { !it.later }

    val formKind: SettingsSubtitleKind? get() = form?.let { f -> kinds.firstOrNull { it.kind == f.kind } }

    val siteRows: List<SettingsSubtitleSiteRow>
        get() =
            sites.map { site ->
                val kind = kinds.firstOrNull { it.kind == site.kind }
                val test = siteTests[site.id]
                SettingsSubtitleSiteRow(
                    id = site.id,
                    name = site.name,
                    url = site.url,
                    kindName = kind?.name ?: site.kind,
                    offered = kind != null,
                    covers = kind?.covers() ?: SettingsSubtitleCovers.BOTH,
                    priority = site.priority,
                    needsAccount = kind?.needsAccount == true,
                    hasAccount = site.hasAccount,
                    allowLocalNetwork = site.allowLocalNetwork,
                    downloadsToday = site.downloadsToday,
                    downloadsMax = site.limits.downloadsPerDay,
                    state = settingsSubtitleSiteState(site, kind),
                    turnedOff = site.state == "disabled",
                    pauseReason = site.pauseReason.orEmpty(),
                    pausedUntil = site.pausedUntil.orEmpty(),
                    testing = testingSite == site.id,
                    testOk = test?.ok,
                    testMessage = test?.message.orEmpty(),
                    testExtra = if (test?.ok == true) settingsSubtitleTestCount(test.message) else "",
                    testError = siteTestErrors[site.id],
                )
            }

    fun number(key: String): String {
        val field = SettingsSubtitleNumber.of(key) ?: return ""
        return numberText[field] ?: field.text(draft)
    }

    fun numberValid(key: String): Boolean = SettingsSubtitleNumber.of(key)?.parse(number(key)) != null

    fun flag(key: String): Boolean = SettingsSubtitleFlag.of(key)?.get(draft) ?: false

    fun step(key: String): Boolean = SettingsSubtitleStep.of(key)?.get(cleanSteps) ?: false

    fun languageName(code: String): String = languages.firstOrNull { it.code == code }?.name ?: code
}

class SettingsSubtitlePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsSubtitleState())
    private val lock = Mutex()
    private var noteId = 0

    val state: StateFlow<SettingsSubtitleState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.mayOpen(AcquisitionPage.SUBTITLES)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return@withLock
            }
            mutableState.update { it.copy(status = it.status.started(), canClean = me.can(Need.MANAGE_LIBRARY)) }
            coroutineScope {
                val languages = async { api.result(LibraryEndpoints.subtitleLanguages) }
                val translator = async { api.result(TitleSupportEndpoints.translator) }
                val kinds = async { api.result(SubtitleSettingsEndpoints.kinds) }
                val sites = async { api.result(SubtitleSettingsEndpoints.sites) }
                when (val result = api.result(SubtitleSettingsEndpoints.settings)) {
                    is ApiResult.Ok -> {
                        val others = listOf(languages.await(), translator.await(), kinds.await(), sites.await())
                        val failure = others.filterIsInstance<ApiResult.Failed>().firstOrNull()?.error
                        mutableState.update { s ->
                            val loadedTranslator = translator.await().valueOrNull() ?: s.translatorSaved
                            s.copy(
                                status = if (failure == null) LoadStatus.ready else LoadStatus.ready.failed(failure),
                                saved = result.value,
                                draft = result.value,
                                numberText = emptyMap(),
                                cleanLanguage = SETTINGS_SUBTITLE_EVERY_LANGUAGE,
                                languages = languages.await().valueOrNull()?.languages ?: s.languages,
                                translatorSaved = loadedTranslator,
                                translator = settingsSubtitleTranslatorForm(loadedTranslator),
                                kinds = kinds.await().valueOrNull()?.kinds ?: s.kinds,
                                sites = sites.await().valueOrNull()?.sites ?: s.sites,
                            )
                        }
                    }
                    is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                }
            }
        }
    }

    suspend fun reloadSites() {
        lock.withLock {
            if (mutableState.value.status.phase != Phase.READY) return@withLock
            api.result(SubtitleSettingsEndpoints.sites).valueOrNull()?.let { list -> mutableState.update { it.copy(sites = list.sites) } }
        }
    }

    suspend fun watch() {
        api.live.collect { event ->
            val data = event.data as? JsonObject
            val kind = (data?.get("Kind") as? JsonPrimitive)?.contentOrNull.orEmpty()
            if (event.topic == LiveEvent.RESYNC || (event.topic == "event" && kind == SITE_PAUSED_EVENT)) reloadSites()
        }
    }

    fun setNumber(
        key: String,
        text: String,
    ) {
        val field = SettingsSubtitleNumber.of(key) ?: return
        val typed = text.filter { it.isDigit() || (!field.whole && it == '.') }.take(NUMBER_MAX_CHARS)
        val value = field.parse(typed)
        mutableState.update { s ->
            s.copy(
                draft = if (value == null) s.draft else field.apply(s.draft, value),
                numberText = s.numberText + (field to typed),
                saveError = null,
            )
        }
    }

    fun setFlag(
        key: String,
        on: Boolean,
    ) {
        val flag = SettingsSubtitleFlag.of(key) ?: return
        edit { flag.set(it, on) }
    }

    fun setHiSuffix(suffix: String) {
        if (suffix in SETTINGS_SUBTITLE_HI_SUFFIXES) edit { it.copy(hiSuffix = suffix) }
    }

    fun addLanguage() {
        val s = mutableState.value
        if (!s.canAddLanguage) return
        val want = SettingsSubtitleWant(language = settingsSubtitleNewLanguage(s.draft.languages, s.languages))
        edit { it.copy(languages = it.languages + want) }
    }

    fun removeLanguage(index: Int) = editWant(index) { null }

    fun setWantLanguage(
        index: Int,
        code: String,
    ) = editWant(index) { it.copy(language = code, translateFrom = if (it.translateFrom == code) "" else it.translateFrom) }

    fun setWantType(
        index: Int,
        type: String,
    ) = editWant(index) { if (type in SETTINGS_SUBTITLE_HEARING_TYPES) it.copy(hearingImpaired = type) else it }

    fun setWantForced(
        index: Int,
        forced: Boolean,
    ) = editWant(index) { it.copy(forced = forced) }

    fun setWantTranslateFrom(
        index: Int,
        code: String,
    ) = editWant(index) { it.copy(translateFrom = code) }

    fun pickCleanLanguage(code: String) {
        mutableState.update { s ->
            val clean = s.draft.clean
            val next =
                if (code == SETTINGS_SUBTITLE_EVERY_LANGUAGE || code in clean) {
                    clean
                } else {
                    clean + (code to (clean[SETTINGS_SUBTITLE_EVERY_LANGUAGE] ?: SettingsSubtitleTidy()))
                }
            s.copy(draft = s.draft.copy(clean = next), cleanLanguage = code, saveError = null)
        }
    }

    fun setStep(
        key: String,
        on: Boolean,
    ) {
        val step = SettingsSubtitleStep.of(key) ?: return
        mutableState.update { s ->
            val updated = step.set(s.cleanSteps, on)
            s.copy(draft = s.draft.copy(clean = s.draft.clean + (s.cleanLanguage to updated)), saveError = null)
        }
    }

    fun removeCleanLanguage() {
        mutableState.update { s ->
            if (s.cleanLanguage == SETTINGS_SUBTITLE_EVERY_LANGUAGE) {
                s
            } else {
                s.copy(
                    draft = s.draft.copy(clean = s.draft.clean - s.cleanLanguage),
                    cleanLanguage = SETTINGS_SUBTITLE_EVERY_LANGUAGE,
                    saveError = null,
                )
            }
        }
    }

    fun discard() =
        mutableState.update {
            it.copy(draft = it.saved, numberText = emptyMap(), cleanLanguage = SETTINGS_SUBTITLE_EVERY_LANGUAGE, saveError = null)
        }

    suspend fun save() {
        val s = mutableState.value
        if (!s.canSave) return
        mutableState.update { it.copy(saving = true, saveError = null) }
        when (val result = api.result(SubtitleSettingsEndpoints.save, s.draft)) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        saving = false,
                        saved = result.value,
                        draft = result.value,
                        numberText = emptyMap(),
                        cleanLanguage = if (result.value.clean.containsKey(it.cleanLanguage)) it.cleanLanguage else SETTINGS_SUBTITLE_EVERY_LANGUAGE,
                        note = nextNote(SettingsSubtitleNoteKind.SETTINGS_SAVED),
                    )
                }
            is ApiResult.Failed -> mutableState.update { it.copy(saving = false, saveError = result.error) }
        }
    }

    fun setTranslatorKind(kind: String) = editTranslator { it.copy(kind = kind) }

    fun setTranslatorUrl(url: String) = editTranslator { it.copy(url = url) }

    fun setTranslatorModel(model: String) = editTranslator { it.copy(model = model) }

    fun setTranslatorKey(key: String) = editTranslator { it.copy(key = key) }

    fun setTranslatorClearKey(clear: Boolean) = editTranslator { it.copy(clearKey = clear) }

    fun setTranslatorBuiltInAuto(on: Boolean) = editTranslator { it.copy(builtInAuto = on) }

    suspend fun saveTranslator() {
        val s = mutableState.value
        if (!s.canSaveTranslator) return
        mutableState.update { it.copy(translatorSaving = true, translatorError = null) }
        when (val result = api.result(SubtitleSettingsEndpoints.saveTranslator, settingsSubtitleTranslatorBody(s.translator))) {
            is ApiResult.Ok -> {
                mutableState.update {
                    it.copy(
                        translatorSaving = false,
                        translatorSaved = result.value,
                        translator = settingsSubtitleTranslatorForm(result.value),
                        translatorTest = null,
                        note = nextNote(SettingsSubtitleNoteKind.TRANSLATOR_SAVED),
                    )
                }
                if (result.value.kind.isNotEmpty()) testTranslator()
            }
            is ApiResult.Failed -> mutableState.update { it.copy(translatorSaving = false, translatorError = result.error) }
        }
    }

    suspend fun testTranslator() {
        if (!mutableState.value.canTestTranslator) return
        mutableState.update { it.copy(translatorTesting = true) }
        val test =
            when (val result = api.result(SubtitleSettingsEndpoints.testTranslator)) {
                is ApiResult.Ok -> result.value
                is ApiResult.Failed -> SettingsSubtitleTranslatorTest(ok = false, message = result.error.message)
            }
        mutableState.update { it.copy(translatorTesting = false, translatorTest = test) }
    }

    fun startAdd() {
        val s = mutableState.value
        val kind = s.offeredKinds.firstOrNull { !it.needsAccount && !it.defaultOff } ?: s.offeredKinds.firstOrNull() ?: return
        mutableState.update { it.copy(form = settingsSubtitleSiteForm(kind, null)) }
    }

    fun startChange(id: Long) {
        val s = mutableState.value
        val site = s.sites.firstOrNull { it.id == id } ?: return
        val kind = s.kinds.firstOrNull { it.kind == site.kind } ?: return
        mutableState.update { it.copy(form = settingsSubtitleSiteForm(kind, site)) }
    }

    fun pickKind(kind: String) {
        val s = mutableState.value
        val form = s.form ?: return
        if (!form.isNew) return
        val picked = s.offeredKinds.firstOrNull { it.kind == kind } ?: return
        mutableState.update { it.copy(form = settingsSubtitleSiteForm(picked, null)) }
    }

    fun cancelForm() = mutableState.update { it.copy(form = null) }

    fun setFormName(name: String) = editForm { it.copy(name = name.take(NAME_MAX_CHARS)) }

    fun setFormUrl(url: String) = editForm { it.copy(url = url) }

    fun setFormNumber(
        key: String,
        text: String,
    ) {
        val typed = text.filter { it.isDigit() }.take(NUMBER_MAX_CHARS)
        editForm {
            when (key) {
                SettingsSubtitleSiteNumber.PRIORITY.key -> it.copy(priority = typed)
                SettingsSubtitleSiteNumber.GAP_MS.key -> it.copy(gapMs = typed)
                SettingsSubtitleSiteNumber.PER_WINDOW.key -> it.copy(perWindow = typed)
                SettingsSubtitleSiteNumber.WINDOW_SECONDS.key -> it.copy(windowSeconds = typed)
                SettingsSubtitleSiteNumber.DOWNLOADS_PER_DAY.key -> it.copy(downloadsPerDay = typed)
                else -> it
            }
        }
    }

    fun setFormAccount(
        field: String,
        text: String,
    ) = editForm {
        when (field) {
            "username" -> it.copy(username = text)
            "password" -> it.copy(password = text)
            "api_key" -> it.copy(apiKey = text)
            else -> it
        }
    }

    fun setFormFlag(
        key: String,
        on: Boolean,
    ) = editForm {
        when (key) {
            SettingsSubtitleSiteFlag.ALLOW_LOCAL_NETWORK.key -> it.copy(allowLocalNetwork = on)
            SettingsSubtitleSiteFlag.MACHINE_MADE.key -> it.copy(machineMade = on)
            SettingsSubtitleSiteFlag.ENABLED.key -> it.copy(enabled = on)
            else -> it
        }
    }

    suspend fun submitForm() {
        val s = mutableState.value
        val form = s.form ?: return
        val kind = s.formKind ?: return
        if (!form.canSubmit) return
        val missing = settingsSubtitleAccountMissing(kind, form)
        if (missing.isNotEmpty()) {
            mutableState.update { it.copy(form = form.copy(missing = missing, error = null)) }
            return
        }
        mutableState.update { it.copy(form = form.copy(busy = true, missing = emptyList(), error = null)) }
        val body = settingsSubtitleSiteBody(form)
        val result =
            if (form.isNew) {
                api.result(SubtitleSettingsEndpoints.addSite, body)
            } else {
                api.result(SubtitleSettingsEndpoints.updateSite, body, "id" to form.id)
            }
        when (result) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(form = null, note = nextNote(SettingsSubtitleNoteKind.SITE_SAVED, result.value.name)) }
                reloadSites()
                testSite(result.value.id)
            }
            is ApiResult.Failed -> mutableState.update { st -> st.copy(form = st.form?.copy(busy = false, error = result.error)) }
        }
    }

    suspend fun testSite(id: Long) {
        if (mutableState.value.testingSite != 0L) return
        mutableState.update { it.copy(testingSite = id) }
        when (val result = api.result(SubtitleSettingsEndpoints.testSite, "id" to id)) {
            is ApiResult.Ok ->
                mutableState.update { it.copy(testingSite = 0, siteTests = it.siteTests + (id to result.value), siteTestErrors = it.siteTestErrors - id) }
            is ApiResult.Failed ->
                mutableState.update {
                    it.copy(
                        testingSite = 0,
                        siteTests = it.siteTests + (id to SettingsSubtitleSiteTest(ok = false, message = result.error.message)),
                        siteTestErrors = it.siteTestErrors + (id to result.error),
                    )
                }
        }
    }

    suspend fun togglePaused(id: Long) {
        val s = mutableState.value
        val site = s.sites.firstOrNull { it.id == id } ?: return
        val kind = s.kinds.firstOrNull { it.kind == site.kind } ?: return
        val pause = site.state != "disabled"
        val body = settingsSubtitleSiteBody(settingsSubtitleSiteForm(kind, site), enabled = !pause)
        when (val result = api.result(SubtitleSettingsEndpoints.updateSite, body, "id" to id)) {
            is ApiResult.Ok -> {
                mutableState.update {
                    it.copy(note = nextNote(if (pause) SettingsSubtitleNoteKind.SITE_PAUSED else SettingsSubtitleNoteKind.SITE_RESUMED, site.name))
                }
                reloadSites()
            }
            is ApiResult.Failed ->
                mutableState.update {
                    it.copy(
                        note =
                            nextNote(
                                if (pause) SettingsSubtitleNoteKind.PAUSE_FAILED else SettingsSubtitleNoteKind.RESUME_FAILED,
                                site.name,
                                result.error,
                            ),
                    )
                }
        }
    }

    suspend fun removeSite(id: Long) {
        val name = mutableState.value.sites.firstOrNull { it.id == id }?.name.orEmpty()
        when (val result = api.result(SubtitleSettingsEndpoints.removeSite, "id" to id)) {
            is ApiResult.Ok -> {
                mutableState.update { it.copy(siteTests = it.siteTests - id, siteTestErrors = it.siteTestErrors - id) }
                reloadSites()
            }
            is ApiResult.Failed ->
                mutableState.update { it.copy(note = nextNote(SettingsSubtitleNoteKind.REMOVE_FAILED, name, result.error)) }
        }
    }

    suspend fun cleanAll() {
        val s = mutableState.value
        if (s.cleaning || !s.canClean) return
        mutableState.update { it.copy(cleaning = true) }
        when (val result = api.result(SubtitleSettingsEndpoints.clean)) {
            is ApiResult.Ok -> mutableState.update { it.copy(cleaning = false, note = nextNote(SettingsSubtitleNoteKind.CLEAN_QUEUED)) }
            is ApiResult.Failed ->
                mutableState.update { it.copy(cleaning = false, note = nextNote(SettingsSubtitleNoteKind.CLEAN_FAILED, error = result.error)) }
        }
    }

    fun dismissNote() = mutableState.update { it.copy(note = null) }

    private fun nextNote(
        kind: SettingsSubtitleNoteKind,
        name: String = "",
        error: ApiError? = null,
    ): SettingsSubtitleNote {
        noteId++
        return SettingsSubtitleNote(noteId, kind, name, error)
    }

    private fun edit(transform: (SettingsSubtitleConfig) -> SettingsSubtitleConfig) =
        mutableState.update { it.copy(draft = transform(it.draft), saveError = null) }

    private fun editWant(
        index: Int,
        transform: (SettingsSubtitleWant) -> SettingsSubtitleWant?,
    ) = edit { config ->
        if (index !in config.languages.indices) {
            config
        } else {
            config.copy(languages = config.languages.mapIndexedNotNull { i, want -> if (i == index) transform(want) else want })
        }
    }

    private fun editTranslator(transform: (SettingsSubtitleTranslatorForm) -> SettingsSubtitleTranslatorForm) =
        mutableState.update { it.copy(translator = transform(it.translator), translatorError = null) }

    private fun editForm(transform: (SettingsSubtitleSiteForm) -> SettingsSubtitleSiteForm) =
        mutableState.update { s -> s.copy(form = s.form?.let { transform(it).copy(error = null, missing = emptyList()) }) }

    private companion object {
        const val SITE_PAUSED_EVENT = "subtitle_site_paused"
        const val NUMBER_MAX_CHARS = 8
        const val NAME_MAX_CHARS = 100
    }
}
