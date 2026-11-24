package ch.orellbuehler.acolyte.feature.quality

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.QualityEndpoints
import ch.orellbuehler.acolyte.api.model.NameCheckAnswer
import ch.orellbuehler.acolyte.api.model.NameCheckBody
import ch.orellbuehler.acolyte.api.model.NameCheckParsed
import ch.orellbuehler.acolyte.api.model.NameCheckShow
import ch.orellbuehler.acolyte.api.model.OwnTrait
import ch.orellbuehler.acolyte.api.model.OwnTraitInput
import ch.orellbuehler.acolyte.api.model.PickRule
import ch.orellbuehler.acolyte.feature.LoadStatus
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.Tone
import ch.orellbuehler.acolyte.feature.acquisition.AcquisitionPage
import ch.orellbuehler.acolyte.feature.acquisition.mayEditAcquisition
import ch.orellbuehler.acolyte.feature.acquisition.mayOpen
import ch.orellbuehler.acolyte.feature.library.ReleaseText
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToLong

data class SettingsPickRuleRow(
    val id: Long,
    val name: String,
    val kinds: List<String>,
    val preset: Boolean,
    val inUse: Int,
    val readyChip: Boolean,
)

data class SettingsTraitSpec(
    val name: String,
    val kind: String,
    val negate: Boolean,
)

data class SettingsTraitRow(
    val id: Long,
    val name: String,
    val match: String,
    val terms: List<String>,
    val source: String,
    val app: String,
    val specs: List<SettingsTraitSpec>,
) {
    val over: String get() = if (match == "size") (terms.firstOrNull().orEmpty().substringBefore('-', "")).trim() else ""

    val upTo: String get() = if (match == "size") (terms.firstOrNull().orEmpty().substringAfter('-', "")).trim() else ""
}

data class SettingsTraitSheet(
    val id: Long = 0,
    val name: String = "",
    val match: String = "name",
    val terms: String = "",
    val over: String = "",
    val upTo: String = "",
    val tried: Boolean = false,
    val busy: Boolean = false,
    val problems: List<SettingsPickRuleProblem> = emptyList(),
    val formError: ApiError? = null,
) {
    fun problem(field: String): SettingsPickRuleProblem? = problems.firstOrNull { it.field == field }
}

data class SettingsTraitRemove(
    val id: Long,
    val name: String,
    val busy: Boolean = false,
    val error: ApiError? = null,
)

data class SettingsPickRuleRemove(
    val id: Long,
    val name: String,
    val inUse: Int,
    val busy: Boolean = false,
    val error: ApiError? = null,
    val use: SettingsPickRuleUse? = null,
)

data class SettingsNameCheckReason(
    val effect: String,
    val text: String,
)

data class SettingsNameCheckCopy(
    val header: String,
    val rulesName: String,
    val showRules: Boolean,
    val verdict: String,
    val tone: Tone,
    val passed: Boolean,
    val points: Int,
    val hasFile: Boolean,
    val reasons: List<SettingsNameCheckReason>,
)

data class SettingsNameCheckResult(
    val title: String,
    val year: Int,
    val tags: List<String>,
    val matchName: String,
    val matchDetail: String,
    val hint: String,
    val copies: List<SettingsNameCheckCopy>,
)

data class SettingsNameCheckState(
    val name: String = "",
    val rulesId: Long = 0,
    val size: String = "",
    val busy: Boolean = false,
    val error: ApiError? = null,
    val result: SettingsNameCheckResult? = null,
) {
    val canCheck: Boolean get() = name.isNotBlank() && !busy
}

data class SettingsPickRulesState(
    val status: LoadStatus = LoadStatus(),
    val allowed: Boolean = true,
    val mayEdit: Boolean = false,
    val rules: List<SettingsPickRuleRow> = emptyList(),
    val traitsLoaded: Boolean = false,
    val traitsError: ApiError? = null,
    val handMade: List<SettingsTraitRow> = emptyList(),
    val trashTraits: List<SettingsTraitRow> = emptyList(),
    val importedTraits: List<SettingsTraitRow> = emptyList(),
    val traitSheet: SettingsTraitSheet? = null,
    val traitRemove: SettingsTraitRemove? = null,
    val remove: SettingsPickRuleRemove? = null,
    val check: SettingsNameCheckState = SettingsNameCheckState(),
    val notice: SettingsQualityNotice? = null,
) {
    val isEmpty: Boolean get() = status.phase == Phase.READY && rules.isEmpty()

    val traitsEmpty: Boolean get() = traitsLoaded && handMade.isEmpty()
}

class SettingsPickRulesPresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
) {
    private val mutableState = MutableStateFlow(SettingsPickRulesState())
    private val lock = Mutex()
    private val notices = QualityNotices()
    private var ownWire: List<OwnTrait> = emptyList()

    val state: StateFlow<SettingsPickRulesState> = mutableState.asStateFlow()

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
            mutableState.update { it.copy(status = it.status.started(), allowed = true, mayEdit = me.mayEditAcquisition) }
            coroutineScope {
                val traits = async { api.result(QualityEndpoints.ownTraits) }
                when (val result = api.result(LibraryEndpoints.pickRules)) {
                    is ApiResult.Ok ->
                        mutableState.update { it.copy(status = LoadStatus.ready, rules = rowsOf(result.value.pickRules)) }
                    is ApiResult.Failed -> mutableState.update { it.copy(status = it.status.failed(result.error)) }
                }
                when (val result = traits.await()) {
                    is ApiResult.Ok -> {
                        ownWire = result.value.ownTraits
                        mutableState.update { sortedTraits(it, ownWire).copy(traitsLoaded = true, traitsError = null) }
                    }
                    is ApiResult.Failed -> mutableState.update { it.copy(traitsError = result.error) }
                }
            }
        }
    }

    fun dismissNotice(seq: Int) = mutableState.update { if (it.notice?.seq == seq) it.copy(notice = null) else it }

    fun askRemove(id: Long) {
        val row = mutableState.value.rules.firstOrNull { it.id == id } ?: return
        if (row.preset || !mutableState.value.mayEdit) return
        mutableState.update { it.copy(remove = SettingsPickRuleRemove(row.id, row.name, row.inUse)) }
    }

    fun cancelRemove() = mutableState.update { it.copy(remove = null) }

    suspend fun confirmRemove() {
        val r = mutableState.value.remove ?: return
        if (r.busy) return
        mutableState.update { it.copy(remove = r.copy(busy = true, error = null, use = null)) }
        when (val result = api.result(QualityEndpoints.remove, "id" to r.id)) {
            is ApiResult.Ok ->
                mutableState.update {
                    it.copy(
                        rules = it.rules.filter { row -> row.id != r.id },
                        remove = null,
                        notice = notices.make("removed", r.name),
                        check = it.check.copy(rulesId = if (it.check.rulesId == r.id) 0 else it.check.rulesId),
                    )
                }
            is ApiResult.Failed ->
                mutableState.update {
                    it.copy(remove = r.copy(busy = false, error = result.error, use = result.error.inUse()))
                }
        }
    }

    fun openTrait(id: Long) {
        val s = mutableState.value
        if (!s.mayEdit) return
        val row = (s.handMade + s.trashTraits + s.importedTraits).firstOrNull { it.id == id }
        mutableState.update {
            it.copy(
                traitSheet =
                    if (row == null || id == 0L) {
                        SettingsTraitSheet()
                    } else {
                        SettingsTraitSheet(
                            id = row.id,
                            name = row.name,
                            match = row.match,
                            terms = if (row.match == "size") "" else row.terms.joinToString("\n"),
                            over = row.over,
                            upTo = row.upTo,
                        )
                    },
            )
        }
    }

    fun closeTrait() = mutableState.update { it.copy(traitSheet = null) }

    fun setTraitName(text: String) = editSheet { it.copy(name = text.take(120)) }

    fun setTraitMatch(match: String) = editSheet { if (match in MATCHES) it.copy(match = match) else it }

    fun setTraitTerms(text: String) = editSheet { it.copy(terms = text) }

    fun setTraitOver(text: String) = editSheet { it.copy(over = sizeText(text)) }

    fun setTraitUpTo(text: String) = editSheet { it.copy(upTo = sizeText(text)) }

    suspend fun saveTrait() {
        val sheet = mutableState.value.traitSheet ?: return
        if (sheet.busy) return
        val terms = ownTerms(sheet.terms)
        val problems = checkTrait(sheet, terms)
        if (problems.isNotEmpty()) {
            mutableState.update { it.copy(traitSheet = sheet.copy(tried = true, problems = problems, formError = null)) }
            return
        }
        mutableState.update { it.copy(traitSheet = sheet.copy(tried = true, busy = true, problems = emptyList(), formError = null)) }
        val body =
            OwnTraitInput(
                sheet.name.trim(),
                sheet.match,
                if (sheet.match == "size") listOf(sizeTerm(sheet.over, sheet.upTo)) else terms,
            )
        val result =
            if (sheet.id == 0L) {
                api.result(QualityEndpoints.addTrait, body)
            } else {
                api.result(QualityEndpoints.changeTrait, body, "id" to sheet.id)
            }
        when (result) {
            is ApiResult.Ok -> {
                val saved = result.value
                ownWire = ownWire.filter { it.id != saved.id } + saved
                mutableState.update {
                    sortedTraits(it, ownWire).copy(
                        traitSheet = null,
                        notice = notices.make(if (sheet.id == 0L) "trait_made" else "trait_saved", saved.name),
                    )
                }
            }
            is ApiResult.Failed -> {
                val field = result.error.fieldName()
                mutableState.update {
                    val current = it.traitSheet ?: sheet
                    it.copy(
                        traitSheet =
                            if (field in TRAIT_FIELDS) {
                                current.copy(
                                    busy = false,
                                    problems = listOf(SettingsPickRuleProblem(field, "server", result.error.serverText())),
                                )
                            } else {
                                current.copy(busy = false, formError = result.error)
                            },
                    )
                }
            }
        }
    }

    fun askRemoveTrait(id: Long) {
        val s = mutableState.value
        if (!s.mayEdit) return
        val row = (s.handMade + s.trashTraits + s.importedTraits).firstOrNull { it.id == id } ?: return
        mutableState.update { it.copy(traitRemove = SettingsTraitRemove(row.id, row.name)) }
    }

    fun cancelRemoveTrait() = mutableState.update { it.copy(traitRemove = null) }

    suspend fun confirmRemoveTrait() {
        val r = mutableState.value.traitRemove ?: return
        if (r.busy) return
        mutableState.update { it.copy(traitRemove = r.copy(busy = true, error = null)) }
        when (val result = api.result(QualityEndpoints.removeTrait, "id" to r.id)) {
            is ApiResult.Ok -> {
                ownWire = ownWire.filter { it.id != r.id }
                mutableState.update {
                    sortedTraits(it, ownWire).copy(
                        traitRemove = null,
                        notice = notices.make("trait_removed", r.name, result.value.pickRulesChanged),
                    )
                }
            }
            is ApiResult.Failed -> mutableState.update { it.copy(traitRemove = r.copy(busy = false, error = result.error)) }
        }
    }

    fun setCheckName(text: String) = editCheck { it.copy(name = text.take(1000)) }

    fun setCheckRules(id: Long) = editCheck { it.copy(rulesId = id) }

    fun setCheckSize(text: String) = editCheck { it.copy(size = sizeText(text)) }

    suspend fun runCheck() {
        val c = mutableState.value.check
        if (!c.canCheck) return
        mutableState.update { it.copy(check = c.copy(busy = true, error = null)) }
        val gb = gbOf(c.size)?.takeIf { it > 0 }
        val size = gb?.let { (it * BYTES_PER_GB).roundToLong() } ?: 0L
        when (val result = api.result(QualityEndpoints.nameCheck, NameCheckBody(c.name.trim(), size, c.rulesId))) {
            is ApiResult.Ok ->
                mutableState.update { it.copy(check = it.check.copy(busy = false, result = nameCheckResult(result.value))) }
            is ApiResult.Failed ->
                mutableState.update { it.copy(check = it.check.copy(busy = false, result = null, error = result.error)) }
        }
    }

    private fun rowsOf(rules: List<PickRule>): List<SettingsPickRuleRow> {
        val anyOwn = rules.any { !it.preset }
        return rules.map { SettingsPickRuleRow(it.id, it.name, it.kinds, it.preset, it.inUse, it.preset && anyOwn) }
    }

    private fun sortedTraits(
        s: SettingsPickRulesState,
        traits: List<OwnTrait>,
    ): SettingsPickRulesState {
        val sorted = traits.sortedBy { it.name.lowercase() }.map { it.toRow() }
        return s.copy(
            handMade = sorted.filter { it.source.isEmpty() },
            trashTraits = sorted.filter { it.source == "trash" },
            importedTraits = sorted.filter { it.source == "import" || it.source == "arrapi" },
        )
    }

    private fun editSheet(transform: (SettingsTraitSheet) -> SettingsTraitSheet) =
        mutableState.update { s ->
            val sheet = s.traitSheet ?: return@update s
            val next = transform(sheet)
            s.copy(traitSheet = next.copy(problems = if (next.tried) checkTrait(next, ownTerms(next.terms)) else emptyList()))
        }

    private fun editCheck(transform: (SettingsNameCheckState) -> SettingsNameCheckState) =
        mutableState.update { it.copy(check = transform(it.check)) }

    private companion object {
        val MATCHES = listOf("name", "group", "size")
        val TRAIT_FIELDS = listOf("name", "terms", "match")
    }
}

private fun OwnTrait.toRow(): SettingsTraitRow =
    SettingsTraitRow(id, name, match, terms, source, app, specs.map { SettingsTraitSpec(it.name, it.kind, it.negate) })

internal fun ownTerms(text: String): List<String> = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

internal fun sizeTerm(
    over: String,
    upTo: String,
): String = "${over.trim().replace(',', '.')}-${upTo.trim().replace(',', '.')}"

internal fun sizeText(text: String): String {
    var dot = false
    val out = StringBuilder()
    for (c in text) {
        when {
            c.isDigit() -> out.append(c)
            (c == '.' || c == ',') && !dot -> {
                dot = true
                out.append(c)
            }
        }
    }
    return out.toString().take(12)
}

internal fun intText(
    text: String,
    signed: Boolean,
): String {
    val out = StringBuilder()
    for ((i, c) in text.withIndex()) {
        if (c.isDigit() || (signed && i == 0 && c == '-')) out.append(c)
    }
    return out.toString().take(12)
}

private fun checkTrait(
    sheet: SettingsTraitSheet,
    terms: List<String>,
): List<SettingsPickRuleProblem> {
    val out = mutableListOf<SettingsPickRuleProblem>()
    val n = sheet.name.trim()
    if (n.isEmpty()) {
        out += SettingsPickRuleProblem("name", "trait_name")
    } else if (n.length > 60) {
        out += SettingsPickRuleProblem("name", "name_60")
    }
    if (sheet.match == "size") {
        val lo = if (sheet.over.isBlank()) 0.0 else gbOf(sheet.over)
        val hi = if (sheet.upTo.isBlank()) 0.0 else gbOf(sheet.upTo)
        if (lo == null || hi == null || lo < 0 || hi < 0 || (lo == 0.0 && hi == 0.0) || (hi > 0 && hi <= lo)) {
            out += SettingsPickRuleProblem("terms", "size_range")
        }
    } else if (terms.isEmpty()) {
        out += SettingsPickRuleProblem("terms", "terms_min")
    } else if (terms.size > 50) {
        out += SettingsPickRuleProblem("terms", "terms_50")
    } else if (terms.any { it.length > 200 }) {
        out += SettingsPickRuleProblem("terms", "term_200")
    }
    return out
}

internal fun nameCheckVerdict(outcome: String): Pair<String, Tone> =
    when (outcome) {
        "chosen" -> "would_choose" to Tone.OK
        "held" -> "would_hold" to Tone.WAIT
        "refused" -> "refused" to Tone.FAIL
        "passed_over" -> "passed_over" to Tone.IDLE
        "turned_down" -> "turned_down" to Tone.IDLE
        else -> "judged" to Tone.IDLE
    }

private fun two(n: Int): String = n.toString().padStart(2, '0')

internal fun showNumbers(show: NameCheckShow?): String {
    if (show == null) return ""
    if (show.airDate.isNotEmpty()) return show.airDate
    var out = ""
    if (show.seasons.size > 1) {
        out = "S${two(show.seasons.first())}-S${two(show.seasons.last())}"
    } else if (show.seasons.size == 1) {
        out = "S${two(show.seasons.first())}"
    }
    if (show.episodes.size == 1) {
        out += "E${two(show.episodes.first())}"
    } else if (show.episodes.size > 1) {
        out += "E${two(show.episodes.first())}-E${two(show.episodes.last())}"
    }
    if (out.isEmpty() && show.absolute.isNotEmpty()) out = show.absolute.joinToString(", ") { "#$it" }
    return out
}

internal fun nameCheckExtraTags(parsed: NameCheckParsed): List<String> =
    buildList {
        showNumbers(parsed.show).takeIf { it.isNotEmpty() }?.let { add(it) }
        parsed.edition.forEach { add(it.replace('_', ' ')) }
        parsed.flags.filter { it != "proper" && it != "repack" }.forEach { add(it.replace('_', ' ')) }
        parsed.group
            ?.value
            ?.takeIf { it.isNotEmpty() }
            ?.let { add("-$it") }
    }

internal fun nameCheckResult(answer: NameCheckAnswer): SettingsNameCheckResult {
    val title = answer.title
    val hint =
        when {
            answer.note == "no_title" && answer.copies.isEmpty() -> "no_title"
            answer.note == "no_album" && title != null -> "no_album"
            title != null && answer.note.isEmpty() && answer.copies.isEmpty() -> "no_copies"
            else -> ""
        }
    return SettingsNameCheckResult(
        title =
            answer.parsed.title
                ?.value
                .orEmpty(),
        year = answer.parsed.year?.value ?: 0,
        tags = ReleaseText.tags(answer.read) + nameCheckExtraTags(answer.parsed),
        matchName = title?.let { if (it.year > 0) "${it.name} (${it.year})" else it.name }.orEmpty(),
        matchDetail = title?.detail.orEmpty(),
        hint = hint,
        copies =
            answer.copies.map { c ->
                val (verdict, tone) = nameCheckVerdict(c.outcome)
                SettingsNameCheckCopy(
                    header = listOf(c.version, c.album, c.episodes).filter { it.isNotEmpty() }.joinToString(" · ").ifEmpty { c.pickRules },
                    rulesName = c.pickRules,
                    showRules = c.version.isNotEmpty(),
                    verdict = verdict,
                    tone = tone,
                    passed = c.passed,
                    points = c.points,
                    hasFile = c.hasFile,
                    reasons = c.reasons.map { SettingsNameCheckReason(it.effect, it.text) },
                )
            },
    )
}
