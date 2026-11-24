package ch.orellbuehler.acolyte.feature.move

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.ApiErrorCode
import ch.orellbuehler.acolyte.api.ApiResult
import ch.orellbuehler.acolyte.api.Endpoint
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.MoveEndpoints
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.model.MoveBringBody
import ch.orellbuehler.acolyte.api.model.MoveCheck
import ch.orellbuehler.acolyte.api.model.MoveFindBody
import ch.orellbuehler.acolyte.api.model.MoveInput
import ch.orellbuehler.acolyte.api.model.MovePathRule
import ch.orellbuehler.acolyte.api.model.MoveReadAnswer
import ch.orellbuehler.acolyte.api.model.MoveReadLinked
import ch.orellbuehler.acolyte.api.model.MoveSource
import ch.orellbuehler.acolyte.feature.Phase
import ch.orellbuehler.acolyte.feature.account.ReauthGate
import ch.orellbuehler.acolyte.feature.acquisition.AcquisitionPage
import ch.orellbuehler.acolyte.feature.acquisition.mayOpen
import ch.orellbuehler.acolyte.session.Account
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private enum class CheckKind {
    PLAN,
    DRY,
    STOP,
}

private data class CheckRequest(
    val kind: CheckKind,
    val waitMillis: Long = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsMovePresenter internal constructor(
    private val api: Api,
    private val account: () -> Account?,
    val reauth: ReauthGate,
) {
    private val mutableState = MutableStateFlow(SettingsMoveState())
    private val requests = MutableSharedFlow<CheckRequest>(extraBufferCapacity = 16)
    private val lock = Mutex()
    private var planSeq = 0
    private var drySeq = 0

    val state: StateFlow<SettingsMoveState> = mutableState.asStateFlow()

    private val inner: MoveInner get() = mutableState.value.holder.data

    private fun commit(block: (MoveInner) -> MoveInner) {
        mutableState.update { MoveView.build(block(it.holder.data)) }
    }

    private fun problem(error: ApiError): MoveProblem = MoveProblem(error, MoveVocabulary.reachFailure(error.status, error.message))

    private fun notice(
        kind: MoveNoticeKind,
        name: String = "",
        n: Int = 0,
        id: Long = 0,
        counts: List<MoveCountPart> = emptyList(),
        problem: MoveProblem? = null,
    ) = commit { s ->
        val seq = s.noticeSeq + 1
        s.copy(noticeSeq = seq, notices = (s.notices + MoveNotice(seq, kind, name, n, id, counts, problem)).takeLast(MAX_NOTICES))
    }

    fun dismissNotice(seq: Int) = commit { s -> s.copy(notices = s.notices.filter { it.seq != seq }) }

    fun dismissProblem() = commit { it.copy(problem = null) }

    fun items(
        kind: MoveListKind,
        id: String,
    ): MoveItemsList = MoveView.list(inner, kind, id)

    fun reportName(): String = "holyarrs-move-${inner.current?.id ?: 0}.md"

    suspend fun load() {
        if (inner.status.phase != Phase.READY) refresh()
    }

    suspend fun refresh() {
        var busy = 0L
        lock.withLock {
            val me = account()
            val allowed = me?.mayOpen(AcquisitionPage.MOVE) == true
            commit {
                it.copy(
                    status = it.status.started(),
                    allowed = allowed,
                    owner = me?.can(Need.OWNER) == true,
                    accountSafeMode = me?.safeMode == true,
                )
            }
            if (!allowed) {
                commit { it.copy(status = ch.orellbuehler.acolyte.feature.LoadStatus.ready) }
                return
            }
            val snaps =
                when (val r = api.result(MoveEndpoints.snapshots)) {
                    is ApiResult.Ok -> r.value.snapshots
                    is ApiResult.Failed -> {
                        commit { it.copy(status = it.status.failed(r.error)) }
                        return
                    }
                }
            coroutineScope {
                val movesCall = launch { loadMoves() }
                val savedCall = launch { loadSaved() }
                val foldersCall = launch { loadFolders() }
                movesCall.join()
                savedCall.join()
                foldersCall.join()
            }
            commit { s ->
                val used = MoveRules.usedByMoves(s.moves, snaps)
                val picked =
                    if (!s.loaded) {
                        MoveRules.latestPerInstance(snaps).filter { it.id !in used }.map { it.id }
                    } else {
                        s.picked.filter { id -> snaps.any { it.id == id } }
                    }
                s.copy(snapshots = snaps, picked = picked, loaded = true, status = ch.orellbuehler.acolyte.feature.LoadStatus.ready)
            }
            busy = inner.moves.firstOrNull { it.running }?.id ?: 0
        }
        if (busy > 0 && inner.current == null) openMove(busy)
    }

    private suspend fun loadMoves() {
        when (val r = api.result(MoveEndpoints.moves)) {
            is ApiResult.Ok -> commit { it.copy(moves = r.value.moves, movesProblem = null) }
            is ApiResult.Failed -> commit { it.copy(movesProblem = r.error) }
        }
    }

    suspend fun reloadMoves() = loadMoves()

    private suspend fun loadSaved() {
        if (!inner.owner) {
            commit { it.copy(savedKeys = emptyList()) }
            return
        }
        val saved = (api.result(MoveEndpoints.savedKeys) as? ApiResult.Ok)?.value?.saved.orEmpty()
        commit { it.copy(savedKeys = saved) }
    }

    private suspend fun loadFolders() {
        val folders = (api.result(LibraryEndpoints.folders) as? ApiResult.Ok)?.value?.libraryFolders.orEmpty()
        commit { s -> s.copy(here = folders.map { MoveHere(it.path, it.kind) }) }
    }

    private suspend fun reloadReads() {
        val r = api.result(MoveEndpoints.snapshots) as? ApiResult.Ok ?: return
        commit { s ->
            val snaps = r.value.snapshots
            val picked = s.picked.filter { id -> snaps.any { it.id == id } }
            s.copy(snapshots = snaps, picked = picked, changes = if (picked.size != s.picked.size) s.changes + 1 else s.changes)
        }
    }

    fun togglePick(
        id: Long,
        on: Boolean,
    ) = commit { s ->
        s.copy(picked = if (on) s.picked.filter { it != id } + id else s.picked.filter { it != id }, changes = s.changes + 1, gone = false)
    }

    suspend fun forget(id: Long): Boolean =
        when (val r = api.result(MoveEndpoints.forget, "id" to id)) {
            is ApiResult.Ok -> {
                commit { s ->
                    s.copy(
                        snapshots = s.snapshots.filter { it.id != id },
                        picked = s.picked.filter { it != id },
                        changes = s.changes + 1,
                    )
                }
                loadSaved()
                true
            }
            is ApiResult.Failed -> {
                commit { it.copy(problem = problem(r.error)) }
                false
            }
        }

    suspend fun removeMove(id: Long) {
        commit { it.copy(removing = it.removing + id) }
        when (val r = api.result(MoveEndpoints.removeMove, "id" to id)) {
            is ApiResult.Ok -> commit { s -> s.copy(moves = s.moves.filter { it.id != id }, removing = s.removing - id) }
            is ApiResult.Failed -> commit { it.copy(removing = it.removing - id, problem = problem(r.error)) }
        }
    }

    fun resetSheet() = commit { it.copy(sheet = MoveSheetState(), readingKind = "") }

    fun clearFound() = commit { it.copy(sheet = it.sheet.copy(found = emptyList(), findRan = false, findProblem = null)) }

    fun clearProbe() =
        commit { it.copy(sheet = it.sheet.copy(probe = null, detected = "", linked = emptyList(), linkedFrom = "", readProblem = null)) }

    private fun source(
        form: MoveReadForm,
        add: List<String>? = null,
    ): MoveSource {
        val url = MoveVocabulary.normalizeAddress(form.address).ifEmpty { null }
        if (form.useSavedKey) return MoveSource(form.kind, url = url, useSavedKey = true, add = add)
        val login = form.kind == "qbittorrent" && (form.username.isNotEmpty() || form.password.isNotEmpty())
        return MoveSource(
            kind = form.kind,
            url = url,
            key = form.key.ifEmpty { null },
            username = if (login) form.username else null,
            password = if (login) form.password else null,
            allowLocalNetwork = form.allowLocalNetwork,
            add = add,
        )
    }

    suspend fun probe(form: MoveReadForm) {
        commit {
            it.copy(
                sheet =
                    it.sheet.copy(
                        probing = true,
                        probe = null,
                        detected = "",
                        linked = emptyList(),
                        linkedFrom = "",
                        readProblem = null,
                    ),
            )
        }
        when (val r = api.result(MoveEndpoints.probe, source(form))) {
            is ApiResult.Ok -> {
                val app = r.value.app
                val pair = MoveVocabulary.linkedSources
                val detected = if (app.kind.isNotEmpty() && app.kind != form.kind && form.kind in pair && app.kind in pair) app.kind else ""
                commit { s ->
                    s.copy(
                        sheet =
                            s.sheet.copy(
                                probing = false,
                                detected = detected,
                                probe = MoveProbeResult(true, MoveVocabulary.appName(detected.ifEmpty { form.kind }), app.version),
                            ),
                    )
                }
                loadLinked(form)
            }
            is ApiResult.Failed ->
                commit { s ->
                    s.copy(sheet = s.sheet.copy(probing = false, probe = MoveProbeResult(false, "", "", problem(r.error))))
                }
        }
    }

    private suspend fun loadLinked(form: MoveReadForm) {
        if (!inner.owner || form.kind !in LINKED_FROM) return
        val r = api.result(MoveEndpoints.linked, source(form)) as? ApiResult.Ok ?: return
        val known = inner.snapshots.map { it.address }
        commit { s ->
            s.copy(
                sheet =
                    s.sheet.copy(
                        linked =
                            r.value.apps.filter { it.address !in known }.map {
                                MoveLinkedRow(
                                    it.kind,
                                    it.name,
                                    it.address,
                                    it.fourK,
                                    it.hasKey,
                                    it.reachable,
                                    it.version,
                                    MoveVocabulary.apps.any { a -> a.kind == it.kind },
                                )
                            },
                        linkedFrom = form.kind,
                    ),
            )
        }
    }

    suspend fun addLinked(form: MoveReadForm) {
        val addable = inner.sheet.addable.map { it.address }
        if (addable.isEmpty()) return
        commit { it.copy(sheet = it.sheet.copy(addingLinked = true, readProblem = null)) }
        when (val r = api.result(MoveEndpoints.linked, source(form, addable))) {
            is ApiResult.Ok -> {
                for (x in r.value.read) applyLinked(x)
                for (f in r.value.failed) {
                    notice(
                        MoveNoticeKind.LINKED_FAILED,
                        name = MoveVocabulary.appName(f.kind),
                        problem = MoveProblem(ApiError(ApiErrorCode.UNKNOWN, 0, f.error), MoveVocabulary.reachFailure(0, f.error)),
                    )
                }
                val done =
                    r.value.read
                        .map { it.snapshot.address }
                        .toSet()
                commit { s ->
                    val left = s.sheet.linked.filter { it.address !in done }
                    s.copy(
                        sheet =
                            s.sheet.copy(
                                addingLinked = false,
                                linked = left,
                                linkedFrom = if (left.isEmpty()) "" else s.sheet.linkedFrom,
                            ),
                    )
                }
            }
            is ApiResult.Failed -> commit { it.copy(sheet = it.sheet.copy(addingLinked = false, readProblem = problem(r.error))) }
        }
    }

    suspend fun find(
        host: String,
        form: MoveReadForm,
    ) {
        if (!inner.owner) return
        commit { it.copy(sheet = it.sheet.copy(finding = true, findProblem = null)) }
        val addresses =
            (
                listOf(
                    MoveVocabulary.normalizeAddress(form.address),
                ) + inner.snapshots.map { it.address } + inner.savedKeys.map { it.address }
            ).filter { it.isNotEmpty() }
                .distinct()
                .take(MAX_FIND_ADDRESSES)
        val body = MoveFindBody(host.ifEmpty { null }, addresses.ifEmpty { null })
        var result = api.result(MoveEndpoints.find, body)
        if (result is ApiResult.Ok && result.value.found.isEmpty()) result = api.result(MoveEndpoints.find, body)
        when (result) {
            is ApiResult.Ok -> {
                val kinds = MoveVocabulary.apps.map { it.kind }
                val found = result.value.found.filter { it.kind in kinds }
                commit { s ->
                    s.copy(
                        sheet =
                            s.sheet.copy(
                                finding = false,
                                findRan = true,
                                searched = result.value.searched,
                                found =
                                    found.map {
                                        MoveFoundRow(
                                            it.kind,
                                            MoveVocabulary.appName(it.kind),
                                            it.address.removePrefix("http://"),
                                            it.version,
                                        )
                                    },
                            ),
                    )
                }
            }
            is ApiResult.Failed -> commit { it.copy(sheet = it.sheet.copy(finding = false, findProblem = problem(result.error))) }
        }
    }

    suspend fun read(form: MoveReadForm): Boolean {
        if (inner.sheet.reading) return false
        commit { it.copy(sheet = it.sheet.copy(reading = true, readProblem = null, stopped = false), readingKind = form.kind) }
        try {
            return when (val r = api.result(MoveEndpoints.read, source(form))) {
                is ApiResult.Ok -> {
                    commit { it.copy(sheet = it.sheet.copy(reading = false), readingKind = "") }
                    applyRead(r.value, form.kind)
                    true
                }
                is ApiResult.Failed -> {
                    commit { it.copy(sheet = it.sheet.copy(reading = false, readProblem = problem(r.error)), readingKind = "") }
                    false
                }
            }
        } catch (e: CancellationException) {
            commit { it.copy(sheet = it.sheet.copy(reading = false, stopped = true), readingKind = "") }
            throw e
        }
    }

    private suspend fun applyRead(
        answer: MoveReadAnswer,
        kind: String,
    ) {
        val got = listOf(answer.snapshot) + answer.linked.map { it.snapshot }
        val gone = answer.replaced + answer.linked.flatMap { it.replaced }
        commit { s ->
            s.copy(
                snapshots = got + s.snapshots.filter { x -> got.none { it.id == x.id } && x.id !in gone },
                picked = s.picked.filter { it != answer.snapshot.id && it !in gone } + answer.snapshot.id,
                changes = s.changes + 1,
                gone = false,
            )
        }
        notice(
            MoveNoticeKind.READ_DONE,
            name = MoveVocabulary.appName(kind),
            n = answer.needsReentry.size,
            counts = MoveVocabulary.countParts(kind, answer.snapshot.counts, 1),
        )
        val ids = if (kind in MoveVocabulary.downloaderSources) listOf(answer.snapshot.id) else answer.linked.map { it.snapshot.id }
        bring(ids)
        loadSaved()
    }

    private suspend fun applyLinked(x: MoveReadLinked) {
        commit { s ->
            s.copy(
                snapshots = listOf(x.snapshot) + s.snapshots.filter { it.id != x.snapshot.id && it.id !in x.replaced },
                picked = s.picked.filter { it != x.snapshot.id && it !in x.replaced } + x.snapshot.id,
                changes = s.changes + 1,
            )
        }
        notice(
            MoveNoticeKind.READ_DONE,
            name = MoveVocabulary.appName(x.kind),
            n = x.needsReentry.size,
            counts = MoveVocabulary.countParts(x.kind, x.snapshot.counts, 1),
        )
        if (x.kind in MoveVocabulary.downloaderSources) bring(listOf(x.snapshot.id))
        loadSaved()
    }

    private suspend fun bring(ids: List<Long>) {
        if (ids.isEmpty()) return
        val r = api.result(MoveEndpoints.downloaders, MoveBringBody(ids)) as? ApiResult.Ok ?: return
        val made = r.value.downloaders.filter { it.id > 0 }
        commit { s -> s.copy(brought = s.brought.filter { b -> made.none { it.id == b.id } } + made) }
    }

    fun enter(step: SettingsMoveStep) {
        val s0 = inner
        var s = step
        if (s == SettingsMoveStep.DONE && s0.current == null) s = SettingsMoveStep.READ
        if ((s == SettingsMoveStep.CHOOSE || s == SettingsMoveStep.RUN) && s0.picked.isEmpty()) s = SettingsMoveStep.READ
        if (s == SettingsMoveStep.RUN && s0.current == null) s = SettingsMoveStep.CHOOSE
        commit {
            var next = it.copy(step = s, problem = null)
            if (s !=
                SettingsMoveStep.CHOOSE
            ) {
                next =
                    next.copy(tryNotice = false, dialog = MoveDialog.NONE, planning = false, trying = false, note = null)
            }
            if (s == SettingsMoveStep.READ) next = next.copy(gone = false)
            next
        }
        if (s == SettingsMoveStep.CHOOSE) {
            requests.tryEmit(CheckRequest(CheckKind.PLAN))
        } else {
            planSeq++
            drySeq++
            requests.tryEmit(CheckRequest(CheckKind.STOP))
        }
    }

    fun goBackToReads() {
        commit { it.copy(gone = false) }
        enter(SettingsMoveStep.READ)
    }

    private fun changed(next: MoveInner): MoveInner = next.copy(changes = next.changes + 1, gone = false)

    private fun replan(wait: Boolean) {
        requests.tryEmit(CheckRequest(CheckKind.PLAN, if (wait) REPLAN_MILLIS else 0))
    }

    fun pickFolder(
        oldPath: String,
        kind: MovePickKind,
        path: String,
    ) {
        commit { s -> changed(s.copy(picks = s.picks + (oldPath to FolderPick(kind, if (kind == MovePickKind.PATH) path else "")))) }
        if (kind != MovePickKind.OTHER) replan(true)
    }

    fun typeOther(
        oldPath: String,
        text: String,
    ) = commit { s -> changed(s.copy(others = s.others + (oldPath to text))) }

    fun commitFolders() = replan(false)

    fun addRule(from: String) = commit { s -> changed(s.copy(extra = s.extra + MovePathRule(from, ""))) }

    fun editRule(
        index: Int,
        from: String,
        to: String,
    ) = commit { s ->
        if (index !in
            s.extra.indices
        ) {
            s
        } else {
            changed(s.copy(extra = s.extra.mapIndexed { n, r -> if (n == index) MovePathRule(from, to) else r }))
        }
    }

    fun removeRule(index: Int) {
        commit { s -> if (index !in s.extra.indices) s else changed(s.copy(extra = s.extra.filterIndexed { n, _ -> n != index })) }
        replan(false)
    }

    fun setGroup(
        id: String,
        on: Boolean,
    ) {
        if (id == "other") return
        commit { s -> changed(s.copy(skip = if (on) s.skip.filter { it != id } else (s.skip.filter { it != id } + id))) }
        replan(true)
    }

    fun setPerson(
        key: String,
        choice: MovePersonChoice?,
    ) {
        commit { s -> changed(s.copy(people = if (choice == null) s.people - key else s.people + (key to MoveRules.wire(choice)))) }
        replan(true)
    }

    fun setNames(choice: MoveNamesChoice) {
        commit { s -> changed(s.copy(names = choice)) }
        replan(true)
    }

    fun setNoFormats(noFormats: Boolean) {
        commit { s -> changed(s.copy(noFormats = noFormats)) }
        replan(true)
    }

    fun chooseOnce(
        key: String,
        choice: MovePersonChoice,
    ) {
        commit { s -> changed(s.copy(people = s.people + (key to MoveRules.wire(choice)))) }
        requests.tryEmit(CheckRequest(CheckKind.DRY))
    }

    private fun body(
        s: MoveInner,
        hash: String? = null,
    ): MoveInput {
        val ok = s.picks.filter { (p, v) -> MoveRules.folderProblem(p, v, s.others[p]) == MovePathProblem.NONE }
        val extra = s.extra.filterIndexed { n, _ -> MoveRules.extraProblems(s.extra)[n] == MovePathProblem.NONE }
        val leave = MoveRules.leaveFolders(s.plan, s.here.map { it.path }, s.picks)
        return MoveInput(
            snapshots = s.picked,
            pathRules = MoveRules.rulesFrom(ok, s.others, extra),
            skip = s.skip,
            people = s.people,
            noFormats = s.noFormats.takeIf { it },
            leaveFolders = leave.ifEmpty { null },
            names = s.names.name.lowercase(),
            planHash = hash,
        )
    }

    private suspend fun runCheck(
        endpoint: Endpoint<MoveInput, MoveCheck>,
        input: MoveInput,
        superseded: () -> Boolean,
    ): ApiResult<MoveCheck> {
        var attempt = 0
        while (true) {
            var check =
                when (val r = api.result(endpoint, input)) {
                    is ApiResult.Ok -> r.value
                    is ApiResult.Failed -> return r
                }
            var restart = false
            while (check.state == "running") {
                if (superseded()) return ApiResult.Failed(ApiError(ApiErrorCode.UNKNOWN, 0))
                val note = MoveVocabulary.checkNote(check)
                commit { it.copy(note = note) }
                delay(CHECK_MILLIS)
                if (superseded()) return ApiResult.Failed(ApiError(ApiErrorCode.UNKNOWN, 0))
                when (val r = api.result(MoveEndpoints.check, "id" to check.checkId)) {
                    is ApiResult.Ok -> check = r.value
                    is ApiResult.Failed ->
                        if (r.error.status == GONE && attempt < MAX_RESTARTS) {
                            attempt++
                            restart = true
                            break
                        } else {
                            return r
                        }
                }
            }
            if (restart) continue
            return if (check.state == "failed") ApiResult.Failed(ApiError(ApiErrorCode.SERVER, 500)) else ApiResult.Ok(check)
        }
    }

    internal suspend fun runPlan() {
        val s0 = inner
        if (s0.picked.isEmpty() || s0.ruleErrors > 0) return
        val seq = ++planSeq
        commit { it.copy(planning = true, planProblem = null) }
        var again = false
        try {
            when (val r = runCheck(MoveEndpoints.plan, body(inner), { seq != planSeq })) {
                is ApiResult.Ok -> if (seq == planSeq) again = applyPlan(r.value)
                is ApiResult.Failed ->
                    if (seq == planSeq) {
                        if (r.error.status == 404) commit { it.copy(gone = true) } else commit { it.copy(planProblem = problem(r.error)) }
                    }
            }
        } finally {
            if (seq == planSeq) commit { it.copy(planning = false, note = null) }
        }
        if (again) runPlan()
    }

    private fun applyPlan(check: MoveCheck): Boolean {
        val plan = check.plan ?: return false
        var added = false
        commit { s ->
            var picks = s.picks
            val suggested = check.suggestedPathRules.associate { it.from to it.to }
            for ((from, to) in suggested) {
                if (from !in picks && s.extra.none { it.from.trim() == from }) {
                    picks = picks + (from to FolderPick(MovePickKind.PATH, to))
                    added = true
                }
            }
            var people = s.people
            var defaulted = s.defaulted
            val peopleGroup = plan.groups.firstOrNull { it.name == "people" }
            val owners =
                if (peopleGroup != null &&
                    "people" !in s.skip
                ) {
                    peopleGroup.items.filter { it.person?.wasOwner == true }
                } else {
                    emptyList()
                }
            if (owners.size == 1) {
                val key = owners[0].key
                if (key !in defaulted) {
                    defaulted = defaulted + key
                    if (people[key] == null) {
                        people = people + (key to "owner")
                        added = true
                    }
                }
            }
            val rows = plan.oldPaths.filter { p -> s.here.none { it.path == p } }
            val held = s.held + rows.filter { p -> (picks[p]?.kind ?: MovePickKind.NONE) == MovePickKind.NONE }
            val next =
                s.copy(
                    plan = plan,
                    suggested = suggested,
                    missing = check.missingPaths,
                    seen = check.seenFolders,
                    seenHere = check.hereFolders,
                    picks = picks,
                    people = people,
                    defaulted = defaulted,
                    held = held,
                )
            if (added) changed(next) else next
        }
        return added
    }

    internal suspend fun runDry() {
        val s0 = inner
        if (s0.picked.isEmpty() || s0.ruleErrors > 0) return
        if (s0.plan == null) {
            runPlan()
            if (inner.plan == null) return
        }
        val seq = ++drySeq
        val at = inner.changes
        commit { it.copy(trying = true, tryFailed = false, tryProblem = null) }
        var rerun = false
        try {
            when (val r = runCheck(MoveEndpoints.dryRun, body(inner), { seq != drySeq })) {
                is ApiResult.Ok ->
                    if (seq == drySeq) {
                        val report = r.value.report
                        commit { s ->
                            val seen =
                                s.seenChoose + (
                                    report
                                        ?.lines
                                        .orEmpty()
                                        .filter { it.choose.isNotEmpty() }
                                        .associateBy { it.key }
                                )
                            s.copy(dry = report, planHash = r.value.planHash, dryAt = at, seenChoose = seen)
                        }
                        rerun = (report?.checking ?: 0) > 0
                    }
                is ApiResult.Failed ->
                    if (seq == drySeq) {
                        if (r.error.status == 404) {
                            commit { it.copy(gone = true) }
                        } else {
                            commit { it.copy(tryFailed = true, tryProblem = problem(r.error)) }
                        }
                    }
            }
        } finally {
            if (seq == drySeq) commit { it.copy(trying = false, note = null) }
        }
        if (rerun) {
            delay(RECHECK_MILLIS)
            if (seq == drySeq) runDry()
        }
    }

    suspend fun importNow() {
        if (inner.checkingNow || inner.starting) return
        commit { it.copy(checkingNow = true, problem = null) }
        try {
            for (n in 0 until MAX_DRY_TRIES) {
                val s = inner
                if (s.dry != null && !s.stale && !s.trying) break
                runDry()
                if (inner.tryFailed || inner.gone) return
            }
            val s = inner
            if (s.dry == null || s.stale || s.step != SettingsMoveStep.CHOOSE) return
            commit { it.copy(dialog = if (it.problems > 0) MoveDialog.BEFORE else MoveDialog.CONFIRM) }
        } finally {
            commit { it.copy(checkingNow = false) }
        }
    }

    fun closeDialog() = commit { it.copy(dialog = MoveDialog.NONE) }

    fun askUndo() = commit { it.copy(dialog = MoveDialog.UNDO) }

    suspend fun start() {
        val s = inner
        if (s.starting || s.dry == null || s.stale) return
        commit { it.copy(starting = true, problem = null) }
        var recheck = false
        when (val r = reauth.guard { api.result(MoveEndpoints.start, body(s, s.planHash)) }) {
            is ApiResult.Ok -> {
                val m = r.value.move
                commit { st ->
                    st.copy(
                        current = m,
                        progress = 0.0,
                        progressNote = "",
                        lost = false,
                        moves = listOf(m) + st.moves.filter { it.id != m.id },
                        step = SettingsMoveStep.RUN,
                        dialog = MoveDialog.NONE,
                        followId = if (m.running) m.id else 0,
                    )
                }
                if (!m.running) finished(m)
            }
            is ApiResult.Failed -> {
                val error = r.error
                val text = error.message
                when {
                    error.code == ApiErrorCode.CONFLICT && text.contains("safe mode", ignoreCase = true) ->
                        commit { it.copy(safeModeHit = true, problem = problem(error), dialog = MoveDialog.NONE) }
                    error.code == ApiErrorCode.CONFLICT && text.contains("changed", ignoreCase = true) -> {
                        commit { it.copy(dryAt = -1, tryNotice = true, dialog = MoveDialog.NONE) }
                        recheck = true
                    }
                    error.status == 404 -> commit { it.copy(gone = true, dialog = MoveDialog.NONE) }
                    else -> commit { it.copy(problem = problem(error), dialog = MoveDialog.NONE) }
                }
            }
        }
        commit { it.copy(starting = false) }
        if (recheck) importNow()
    }

    suspend fun openMove(id: Long) {
        commit { it.copy(problem = null) }
        when (val r = api.result(MoveEndpoints.move, "id" to id)) {
            is ApiResult.Ok -> {
                val a = r.value
                commit { s ->
                    s.copy(
                        current = a.move,
                        progress = a.progress,
                        progressNote = a.progressNote,
                        lost = false,
                        step = if (a.move.running && a.move.undoState.isEmpty()) SettingsMoveStep.RUN else SettingsMoveStep.DONE,
                        followId = if (a.move.running) id else 0,
                    )
                }
            }
            is ApiResult.Failed -> {
                enter(SettingsMoveStep.READ)
                commit { it.copy(problem = problem(r.error)) }
            }
        }
    }

    suspend fun undo() {
        val m = inner.current ?: return
        commit { it.copy(dialog = MoveDialog.NONE, problem = null) }
        when (val r = reauth.guard { api.result(MoveEndpoints.undo, "id" to m.id) }) {
            is ApiResult.Ok ->
                commit { s ->
                    s.copy(current = r.value.move, progress = 0.0, progressNote = "", lost = false, followId = m.id)
                }
            is ApiResult.Failed -> {
                val safe = r.error.code == ApiErrorCode.CONFLICT && r.error.message.contains("safe mode", ignoreCase = true)
                commit { it.copy(problem = problem(r.error), safeModeHit = it.safeModeHit || safe) }
            }
        }
    }

    suspend fun reportText(): String? {
        val m = inner.current ?: return null
        return when (val r = api.text(MoveEndpoints.report, "id" to m.id)) {
            is ApiResult.Ok -> r.value
            is ApiResult.Failed -> {
                commit { it.copy(problem = problem(r.error)) }
                null
            }
        }
    }

    suspend fun done() {
        commit {
            it.copy(
                current = null,
                dry = null,
                dryAt = -1,
                planHash = "",
                plan = null,
                suggested = emptyMap(),
                missing = emptyMap(),
                tryNotice = false,
                followId = 0,
            )
        }
        enter(SettingsMoveStep.READ)
        loadMoves()
        reloadReads()
    }

    fun chooseAgain() {
        val s = inner
        val m = s.current ?: return
        val rules = m.choices.pathRules
        var picks = s.picks
        var extra = s.extra
        if (rules.isNotEmpty()) {
            val free = mutableListOf<MovePathRule>()
            for (r in rules) {
                if (r.from in picks) continue
                if (s.here.any { it.path == r.to }) picks = picks + (r.from to FolderPick(MovePickKind.PATH, r.to)) else free += r
            }
            extra = extra.filter { x -> free.none { it.from == x.from.trim() } } + free
        }
        commit {
            changed(
                it.copy(
                    picked = m.snapshots.filter { id -> it.snapshots.any { s2 -> s2.id == id } },
                    people = it.people + m.choices.people,
                    skip = m.choices.skip,
                    noFormats = m.choices.noFormats,
                    names = if (m.choices.names == "rename") MoveNamesChoice.RENAME else MoveNamesChoice.KEEP,
                    picks = picks,
                    extra = extra,
                    current = null,
                    dry = null,
                    followId = 0,
                ),
            )
        }
        enter(SettingsMoveStep.CHOOSE)
    }

    private fun finished(m: ch.orellbuehler.acolyte.api.model.MoveRecord) {
        val kind =
            when {
                m.state == "undone" -> MoveNoticeKind.IMPORT_UNDONE
                m.undoState == "failed" -> MoveNoticeKind.UNDO_FAILED
                m.state == "done" -> MoveNoticeKind.IMPORT_FINISHED
                else -> MoveNoticeKind.IMPORT_FAILED
            }
        commit { s ->
            var next = s.copy(moves = listOf(m) + s.moves.filter { it.id != m.id }, followId = 0, lost = false)
            if (m.state != "undone" && m.undoState.isEmpty()) {
                next =
                    changed(
                        next.copy(
                            dry = null,
                            dryAt = -1,
                            planHash = "",
                            suggested = emptyMap(),
                            picked = next.picked.filter { it !in m.snapshots },
                        ),
                    )
            }
            if (next.current?.id == m.id) {
                next = next.copy(current = m, progress = 1.0)
                if (next.step == SettingsMoveStep.RUN || next.step == SettingsMoveStep.DONE) next = next.copy(step = SettingsMoveStep.DONE)
            }
            next
        }
        notice(kind, id = m.id)
    }

    internal suspend fun follow(id: Long) {
        var lost = 0
        while (true) {
            when (val r = api.result(MoveEndpoints.move, "id" to id)) {
                is ApiResult.Ok -> {
                    lost = 0
                    val a = r.value
                    commit { s ->
                        val same = s.current?.id == a.move.id
                        s.copy(
                            lost = false,
                            moves = s.moves.map { if (it.id == a.move.id) a.move.copy(report = it.report) else it },
                            current = if (same) a.move else s.current,
                            progress = if (same) a.progress else s.progress,
                            progressNote = if (same) a.progressNote else s.progressNote,
                        )
                    }
                    if (!a.move.running) {
                        finished(a.move)
                        if (inner.step == SettingsMoveStep.CHOOSE) enter(SettingsMoveStep.CHOOSE)
                        loadSaved()
                        return
                    }
                    delay(FOLLOW_MILLIS)
                }
                is ApiResult.Failed -> {
                    lost++
                    commit { it.copy(lost = true) }
                    delay(BACKOFF_MILLIS[minOf(lost - 1, BACKOFF_MILLIS.size - 1)])
                }
            }
        }
    }

    suspend fun watch() {
        coroutineScope {
            launch {
                requests
                    .onStart {
                        val s = inner
                        if (s.step == SettingsMoveStep.CHOOSE && !s.planning && !s.trying && (s.plan == null || s.dry == null || s.stale)) {
                            emit(CheckRequest(CheckKind.PLAN))
                        }
                    }.collectLatest { handle(it) }
            }
            launch {
                mutableState.map { it.holder.data.followId }.distinctUntilChanged().collectLatest { if (it > 0) follow(it) }
            }
        }
    }

    private suspend fun handle(request: CheckRequest) {
        when (request.kind) {
            CheckKind.PLAN -> {
                if (request.waitMillis > 0) delay(request.waitMillis)
                runPlan()
                val s = inner
                if (s.step == SettingsMoveStep.CHOOSE && s.plan != null && (s.dry == null || s.stale)) runDry()
            }
            CheckKind.DRY -> runDry()
            CheckKind.STOP -> Unit
        }
    }

    internal fun stateForTest(): MoveInner = inner

    private companion object {
        const val CHECK_MILLIS = 800L
        const val FOLLOW_MILLIS = 1_000L
        const val RECHECK_MILLIS = 5_000L
        const val REPLAN_MILLIS = 400L
        const val GONE = 410
        const val MAX_RESTARTS = 3
        const val MAX_DRY_TRIES = 3
        const val MAX_NOTICES = 6
        const val MAX_FIND_ADDRESSES = 20
        val BACKOFF_MILLIS = longArrayOf(2_000L, 4_000L, 10_000L)
        val LINKED_FROM = setOf("seerr", "overseerr", "prowlarr")
    }
}
