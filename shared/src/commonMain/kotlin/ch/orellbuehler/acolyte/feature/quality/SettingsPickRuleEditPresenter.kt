package ch.orellbuehler.acolyte.feature.quality

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.QualityEndpoints
import ch.orellbuehler.acolyte.api.model.OwnTrait
import ch.orellbuehler.acolyte.api.model.PickRuleDetail
import ch.orellbuehler.acolyte.api.model.PickRuleInput
import ch.orellbuehler.acolyte.api.model.TrashApplied
import ch.orellbuehler.acolyte.api.model.TrashApply
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.acquisition.AcquisitionPage
import ch.orellbuehler.acolyte.feature.acquisition.mayEditAcquisition
import ch.orellbuehler.acolyte.feature.acquisition.mayOpen
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class SettingsTraitChoice(
    val id: String,
    val own: Boolean,
    val name: String,
)

data class SettingsTrashProfile(
    val id: String,
    val name: String,
    val description: String,
)

data class SettingsTrashSkipped(
    val name: String,
    val kinds: List<String>,
    val reason: String,
)

data class SettingsTrashResult(
    val name: String,
    val scored: Int,
    val added: Int,
    val updated: Int,
    val skipped: List<SettingsTrashSkipped>,
)

data class SettingsTrashState(
    val app: String = "sonarr",
    val profiles: List<SettingsTrashProfile> = emptyList(),
    val profilesLoaded: Boolean = false,
    val pick: String = "",
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: ApiError? = null,
    val result: SettingsTrashResult? = null,
) {
    val choice: SettingsTrashProfile? get() = profiles.firstOrNull { it.id == pick }
}

data class SettingsPickRuleEditState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val mayEdit: Boolean = false,
    val gone: Boolean = false,
    val id: Long = 0,
    val preset: Boolean = false,
    val savedName: String = "",
    val use: SettingsPickRuleUse = SettingsPickRuleUse(),
    val form: SettingsPickRuleForm = SettingsPickRuleForm(),
    val saved: SettingsPickRuleForm = SettingsPickRuleForm(),
    val mine: List<SettingsTraitChoice> = emptyList(),
    val tried: Boolean = false,
    val serverProblem: SettingsPickRuleProblem? = null,
    val saving: Boolean = false,
    val formError: ApiError? = null,
    val formUse: SettingsPickRuleUse? = null,
    val trash: SettingsTrashState = SettingsTrashState(),
    val notice: SettingsQualityNotice? = null,
) {
    val isNew: Boolean get() = id == 0L

    val ready: Boolean get() = status.phase == Phase.READY && !gone

    val needsPermission: Boolean get() = status.phase == Phase.READY && isNew && !mayEdit

    val dirty: Boolean get() = ready && form != saved

    val showSaveBar: Boolean get() = ready && mayEdit && (isNew || dirty)

    val showTrash: Boolean get() = ready && !form.music && !isNew

    val problems: List<SettingsPickRuleProblem>
        get() {
            val client =
                if (tried) {
                    checkPickRuleBody(form.name, form.kinds, form.toBody(), mine.map { it.id }.toSet()).values.toList()
                } else {
                    emptyList()
                }
            val server = serverProblem
            return if (server == null) client else client.filter { it.field != server.field } + server
        }

    val firstProblem: String get() = problems.firstOrNull()?.field.orEmpty()

    fun problem(field: String): SettingsPickRuleProblem? = problems.firstOrNull { it.field == field }

    fun choice(id: String): SettingsTraitChoice =
        mine.firstOrNull { it.id == id } ?: SettingsTraitChoice(id, id.startsWith(OWN_PREFIX), "")

    val traitChoices: List<SettingsTraitChoice> get() = form.traitChoices.map { SettingsTraitChoice(it, false, "") } + mine

    val neverChoices: List<SettingsTraitChoice> get() = form.neverChoices.map { SettingsTraitChoice(it, false, "") } + mine

    fun freeTraitChoices(index: Int): List<SettingsTraitChoice> {
        val taken = form.traits.filterIndexed { i, _ -> i != index }.map { it.trait }.toSet()
        return traitChoices.filter { it.id !in taken }
    }

    val canAddTrait: Boolean get() = form.traits.size < traitChoices.size

    val sizeChoices: List<String> get() = form.sizeChoices
}

class SettingsPickRuleEditPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    private val routeId: Long,
    private val from: Long,
) {
    private val mutableState = MutableStateFlow(SettingsPickRuleEditState(id = routeId))
    private val lock = Mutex()
    private val notices = QualityNotices()

    val state: StateFlow<SettingsPickRuleEditState> = mutableState.asStateFlow()

    suspend fun load() {
        if (mutableState.value.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        lock.withLock {
            val me = account()
            if (me == null || !me.mayOpen(AcquisitionPage.PICK_RULES)) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = false) }
                return
            }
            val mayEdit = me.mayEditAcquisition
            if (routeId == 0L && !mayEdit) {
                mutableState.update { it.copy(status = LoadStatus.ready, allowed = true, mayEdit = false) }
                return
            }
            mutableState.update { it.copy(status = it.status.started(), allowed = true, mayEdit = mayEdit, gone = false) }
            val traits =
                when (val result = api.result(QualityEndpoints.ownTraits)) {
                    is ApiResult.Ok -> result.value.ownTraits
                    is ApiResult.Failed -> {
                        mutableState.update { it.copy(status = it.status.failed(result.error)) }
                        return
                    }
                }
            val sourceId = if (routeId == 0L) from else routeId
            if (sourceId == 0L) {
                val form = blankForm("movie")
                mutableState.update {
                    it.copy(status = LoadStatus.ready, mine = choicesOf(traits), form = form, saved = form, tried = false)
                }
                return
            }
            when (val result = api.result(QualityEndpoints.detail, "id" to sourceId)) {
                is ApiResult.Ok -> {
                    mutableState.update { it.copy(status = LoadStatus.ready, mine = choicesOf(traits)) }
                    show(result.value, copy = routeId == 0L)
                }
                is ApiResult.Failed ->
                    mutableState.update {
                        if (result.error.code == ApiErrorCode.NOT_FOUND) {
                            it.copy(status = LoadStatus.ready, gone = true)
                        } else {
                            it.copy(status = it.status.failed(result.error))
                        }
                    }
            }
        }
    }

    fun dismissNotice(seq: Int) = mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }

    fun discard() = mutableState.update { it.copy(form = it.saved, tried = false, serverProblem = null, formError = null, formUse = null) }

    fun setName(text: String) = edit { it.copy(name = text.take(200)) }

    fun setKind(
        kind: String,
        on: Boolean,
    ) = edit { form ->
        val next = toggleKind(form.kinds, kind, on)
        if (next.isNotEmpty() && ("music" in next) != form.music) {
            blankForm(next.first()).copy(name = form.name, kinds = next)
        } else {
            form.copy(kinds = next)
        }
    }

    fun setStepAllowed(
        quality: String,
        on: Boolean,
    ) = edit { form ->
        val steps = form.steps.map { if (it.quality == quality) it.copy(allowed = on) else it }
        val next = form.copy(steps = steps)
        val best = next.allowedBestFirst
        if (best.isNotEmpty() && best.none { it.quality == next.goodEnoughAt }) next.copy(goodEnoughAt = best.first().quality) else next
    }

    fun moveStep(
        quality: String,
        up: Boolean,
    ) = edit { it.copy(steps = moveStep(it.steps, quality, up)) }

    fun setGoodEnoughAt(quality: String) = edit { if (it.allowedBestFirst.any { s -> s.quality == quality }) it.copy(goodEnoughAt = quality) else it }

    fun setGoodEnoughPoints(text: String) = edit { it.copy(goodEnoughPoints = intText(text, true)) }

    fun setTraitKey(
        index: Int,
        key: String,
    ) = edit { form ->
        if (index !in form.traits.indices || form.traits.anyIndexed { i, t -> i != index && t.trait == key }) {
            form
        } else {
            form.copy(traits = form.traits.mapIndexed { i, t -> if (i == index) t.copy(trait = key) else t })
        }
    }

    fun setTraitPoints(
        index: Int,
        text: String,
    ) = edit { form ->
        form.copy(traits = form.traits.mapIndexed { i, t -> if (i == index) t.copy(points = intText(text, true)) else t })
    }

    fun addTrait() =
        edit { form ->
            val all = mutableState.value.traitChoices.map { it.id }
            val free = all.firstOrNull { id -> form.traits.none { it.trait == id } }
            if (free == null) form else form.copy(traits = form.traits + SettingsPickRuleTrait(free, "0"))
        }

    fun removeTrait(index: Int) = edit { form -> form.copy(traits = form.traits.filterIndexed { i, _ -> i != index }) }

    fun setOrder(order: String) = edit { if (order in SettingsPickRuleChoices.orders) it.copy(order = order) else it }

    fun setUpgradeMinPoints(text: String) = edit { it.copy(upgradeMinPoints = intText(text, true)) }

    fun setMinPoints(text: String) = edit { it.copy(minPoints = intText(text, true)) }

    fun setProperMode(mode: String) = edit { if (mode in SettingsPickRuleChoices.properModes) it.copy(properMode = mode) else it }

    fun setProperAge(text: String) = edit { it.copy(properAge = intText(text, false)) }

    fun addMustHave(id: String) = edit { if (id.isNotEmpty() && id !in it.mustHave) it.copy(mustHave = it.mustHave + id) else it }

    fun removeMustHave(id: String) = edit { it.copy(mustHave = it.mustHave - id) }

    fun addNever(id: String) = edit { if (id.isNotEmpty() && id !in it.never) it.copy(never = it.never + id) else it }

    fun removeNever(id: String) = edit { it.copy(never = it.never - id) }

    fun addBlockedGroup(raw: String) = edit { it.copy(blockedGroups = addBlockedGroup(it.blockedGroups, raw)) }

    fun removeBlockedGroup(group: String) = edit { it.copy(blockedGroups = it.blockedGroups - group) }

    fun toggleDevice(id: String) =
        edit { if (id !in SettingsPickRuleChoices.devices) it else it.copy(devices = if (id in it.devices) it.devices - id else it.devices + id) }

    fun setSizeMin(text: String) = edit { it.copy(sizeMin = sizeText(text)) }

    fun setSizeMax(text: String) = edit { it.copy(sizeMax = sizeText(text)) }

    fun addStepSize(step: String) =
        edit { form ->
            if (step !in form.sizeChoices) {
                form
            } else {
                val ladder = form.steps.map { it.quality }
                val rows = (form.stepSizes + SettingsPickRuleStepSize(step, form.sizeMin, "", form.sizeMax)).sortedBy { ladder.indexOf(it.step) }
                form.copy(stepSizes = rows)
            }
        }

    fun setStepSize(
        step: String,
        which: String,
        text: String,
    ) = edit { form ->
        val value = sizeText(text)
        form.copy(
            stepSizes =
                form.stepSizes.map {
                    when {
                        it.step != step -> it
                        which == "min" -> it.copy(min = value)
                        which == "preferred" -> it.copy(preferred = value)
                        which == "max" -> it.copy(max = value)
                        else -> it
                    }
                },
        )
    }

    fun removeStepSize(step: String) = edit { form -> form.copy(stepSizes = form.stepSizes.filter { it.step != step }) }

    fun setReplaceUnderMin(on: Boolean) = edit { it.copy(replaceUnderMin = on) }

    fun setPrefer(prefer: String) = edit { if (prefer in SettingsPickRuleChoices.prefers) it.copy(prefer = prefer) else it }

    fun setWaitTorrent(text: String) = edit { it.copy(waitTorrent = intText(text, false)) }

    fun setWaitUsenet(text: String) = edit { it.copy(waitUsenet = intText(text, false)) }

    fun setSkipWait(on: Boolean) = edit { it.copy(skipWait = on) }

    fun addTag(raw: String) = edit { it.copy(tags = addTags(it.tags, raw)) }

    fun removeTag(tag: String) = edit { it.copy(tags = it.tags - tag) }

    fun toggleLanguage(code: String) =
        edit {
            if (code !in SettingsPickRuleChoices.languages) {
                it
            } else {
                it.copy(languages = if (code in it.languages) it.languages - code else it.languages + code)
            }
        }

    fun toggleFrench(version: String) =
        edit {
            if (version !in SettingsPickRuleChoices.frenchVersions) {
                it
            } else {
                it.copy(frenchVersions = if (version in it.frenchVersions) it.frenchVersions - version else it.frenchVersions + version)
            }
        }

    fun setOriginalOk(on: Boolean) = edit { it.copy(originalOk = on) }

    fun setLanguageWait(text: String) = edit { it.copy(languageWait = intText(text, false)) }

    suspend fun save() {
        val s = mutableState.value
        if (!s.ready || !s.mayEdit || s.saving) return
        val body = s.form.toBody()
        val problems = checkPickRuleBody(s.form.name, s.form.kinds, body, s.mine.map { it.id }.toSet())
        mutableState.update { it.copy(tried = true, serverProblem = null, formError = null, formUse = null) }
        if (problems.isNotEmpty()) return
        mutableState.update { it.copy(saving = true) }
        val input = PickRuleInput(s.form.name.trim(), s.form.kinds, body.toJson())
        val result =
            if (s.isNew) {
                api.result(QualityEndpoints.create, input)
            } else {
                api.result(QualityEndpoints.update, input, "id" to s.id)
            }
        when (result) {
            is ApiResult.Ok -> {
                show(result.value, copy = false)
                mutableState.update { it.copy(saving = false, notice = notices.make(if (s.isNew) "made" else "saved", result.value.name)) }
            }
            is ApiResult.Failed -> {
                val error = result.error
                if (error.code == ApiErrorCode.BAD_REQUEST) reloadOwnTraits()
                val field = settingsPickRuleFieldKey(error.fieldName())
                mutableState.update {
                    if (field in SETTINGS_PICK_RULE_FIELDS) {
                        it.copy(saving = false, serverProblem = SettingsPickRuleProblem(field, "server", error.serverText()))
                    } else {
                        it.copy(saving = false, formError = error, formUse = error.inUse())
                    }
                }
            }
        }
    }

    suspend fun setTrashApp(app: String) {
        if (app !in SettingsPickRuleChoices.trashApps) return
        mutableState.update { it.copy(trash = SettingsTrashState(app = app, result = it.trash.result)) }
        loadTrash()
    }

    suspend fun loadTrash() {
        val app = mutableState.value.trash.app
        mutableState.update { it.copy(trash = it.trash.copy(loading = true, error = null)) }
        when (val result = api.result(QualityEndpoints.trashProfiles, "app" to app)) {
            is ApiResult.Ok ->
                mutableState.update {
                    if (it.trash.app != app) {
                        it
                    } else {
                        val profiles = result.value.profiles.map { p -> SettingsTrashProfile(p.id, p.name, p.description) }
                        val synced = it.form.synced
                        val pick =
                            if (synced != null && synced.app == app && profiles.any { p -> p.id == synced.profileId }) {
                                synced.profileId
                            } else {
                                profiles.firstOrNull()?.id.orEmpty()
                            }
                        it.copy(trash = it.trash.copy(loading = false, profiles = profiles, profilesLoaded = true, pick = pick))
                    }
                }
            is ApiResult.Failed -> mutableState.update { it.copy(trash = it.trash.copy(loading = false, error = result.error)) }
        }
    }

    fun setTrashPick(id: String) = mutableState.update { it.copy(trash = it.trash.copy(pick = id)) }

    suspend fun applyTrash() {
        val s = mutableState.value
        if (s.trash.pick.isEmpty()) return
        runTrash(s.trash.app, s.trash.pick)
    }

    suspend fun syncAgain() {
        val synced = mutableState.value.form.synced ?: return
        runTrash(synced.app, synced.profileId)
    }

    private suspend fun runTrash(
        app: String,
        profileId: String,
    ) {
        val s = mutableState.value
        if (s.isNew || s.dirty || s.trash.busy || !s.mayEdit) return
        mutableState.update { it.copy(trash = it.trash.copy(busy = true, error = null)) }
        when (val result = api.result(QualityEndpoints.trash, TrashApply(app, profileId), "id" to s.id)) {
            is ApiResult.Ok -> {
                reloadOwnTraits()
                val applied = result.value
                show(applied.pickRules, copy = false)
                mutableState.update {
                    it.copy(
                        trash = it.trash.copy(busy = false, result = trashResult(applied)),
                        notice = notices.make("applied", it.form.synced?.name.orEmpty(), applied.scored.size),
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(trash = it.trash.copy(busy = false, error = result.error)) }
        }
    }

    private suspend fun reloadOwnTraits() {
        val result = api.result(QualityEndpoints.ownTraits)
        if (result is ApiResult.Ok) mutableState.update { it.copy(mine = choicesOf(result.value.ownTraits)) }
    }

    private fun show(
        detail: PickRuleDetail,
        copy: Boolean,
    ) {
        val body = PickBody.from(detail.body)
        val form = body.toForm(if (copy) settingsPickRuleCopyName(detail.name) else detail.name, detail.kinds, detail.body)
        mutableState.update {
            it.copy(
                id = if (copy) 0 else detail.id,
                preset = !copy && detail.preset,
                savedName = if (copy) "" else detail.name,
                use = if (copy) SettingsPickRuleUse() else detail.inUse.toPublic(),
                form = form,
                saved = form,
                tried = false,
                serverProblem = null,
                formError = null,
                formUse = null,
            )
        }
    }

    private fun edit(transform: (SettingsPickRuleForm) -> SettingsPickRuleForm) =
        mutableState.update { s -> if (s.mayEdit) s.copy(form = transform(s.form), formError = null) else s }
}

private inline fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
    for (i in indices) if (predicate(i, this[i])) return true
    return false
}

private fun choicesOf(traits: List<OwnTrait>): List<SettingsTraitChoice> =
    traits.sortedBy { it.name.lowercase() }.map { SettingsTraitChoice(it.key.ifEmpty { "own:${it.id}" }, true, it.name) }

private fun trashResult(applied: TrashApplied): SettingsTrashResult =
    SettingsTrashResult(
        name = PickBody.from(applied.pickRules.body).synced?.let { (it["name"] as? JsonPrimitive)?.contentOrNull }.orEmpty(),
        scored = applied.scored.size,
        added = applied.added,
        updated = applied.updated,
        skipped = applied.skipped.map { SettingsTrashSkipped(it.name, it.kinds, it.reason) },
    )
