package ch.orellbuehler.acolyte.feature.move

import ch.orellbuehler.acolyte.api.ApiError
import ch.orellbuehler.acolyte.api.model.MoveBrought
import ch.orellbuehler.acolyte.api.model.MoveLine
import ch.orellbuehler.acolyte.api.model.MoveNotMoved
import ch.orellbuehler.acolyte.api.model.MovePathRule
import ch.orellbuehler.acolyte.api.model.MovePlan
import ch.orellbuehler.acolyte.api.model.MovePlanGroup
import ch.orellbuehler.acolyte.api.model.MovePlanItem
import ch.orellbuehler.acolyte.api.model.MoveRecord
import ch.orellbuehler.acolyte.api.model.MoveReport
import ch.orellbuehler.acolyte.api.model.MoveSavedKey
import ch.orellbuehler.acolyte.api.model.MoveSnapshot
import ch.orellbuehler.acolyte.feature.LoadStatus

internal data class MoveHere(
    val path: String,
    val kind: String,
)

internal data class MoveInner(
    val status: LoadStatus = LoadStatus(),
    val loaded: Boolean = false,
    val allowed: Boolean = true,
    val owner: Boolean = false,
    val accountSafeMode: Boolean = false,
    val step: SettingsMoveStep = SettingsMoveStep.READ,
    val snapshots: List<MoveSnapshot> = emptyList(),
    val moves: List<MoveRecord> = emptyList(),
    val movesProblem: ApiError? = null,
    val savedKeys: List<MoveSavedKey> = emptyList(),
    val here: List<MoveHere> = emptyList(),
    val picked: List<Long> = emptyList(),
    val brought: List<MoveBrought> = emptyList(),
    val sheet: MoveSheetState = MoveSheetState(),
    val plan: MovePlan? = null,
    val planning: Boolean = false,
    val planProblem: MoveProblem? = null,
    val picks: Map<String, FolderPick> = emptyMap(),
    val others: Map<String, String> = emptyMap(),
    val extra: List<MovePathRule> = emptyList(),
    val held: Set<String> = emptySet(),
    val defaulted: Set<String> = emptySet(),
    val suggested: Map<String, String> = emptyMap(),
    val missing: Map<String, String> = emptyMap(),
    val seen: List<String> = emptyList(),
    val seenHere: List<String> = emptyList(),
    val skip: List<String> = emptyList(),
    val noFormats: Boolean = false,
    val names: MoveNamesChoice = MoveNamesChoice.KEEP,
    val people: Map<String, String> = emptyMap(),
    val changes: Int = 0,
    val dry: MoveReport? = null,
    val dryAt: Int = -1,
    val planHash: String = "",
    val trying: Boolean = false,
    val tryFailed: Boolean = false,
    val tryProblem: MoveProblem? = null,
    val tryNotice: Boolean = false,
    val note: MoveCheckNote? = null,
    val seenChoose: Map<String, MoveLine> = emptyMap(),
    val checkingNow: Boolean = false,
    val dialog: MoveDialog = MoveDialog.NONE,
    val starting: Boolean = false,
    val problem: MoveProblem? = null,
    val gone: Boolean = false,
    val safeModeHit: Boolean = false,
    val current: MoveRecord? = null,
    val progress: Double = 0.0,
    val progressNote: String = "",
    val lost: Boolean = false,
    val followId: Long = 0,
    val notices: List<MoveNotice> = emptyList(),
    val noticeSeq: Int = 0,
    val removing: Set<Long> = emptySet(),
    val readingKind: String = "",
) {
    val stale: Boolean get() = dry != null && dryAt != changes
    val safeMode: Boolean get() = accountSafeMode || safeModeHit
    val moveRunning: Boolean get() = current?.let { it.running && it.undoState.isEmpty() } == true
    val folderErrors: Int
        get() =
            plan?.oldPaths.orEmpty().count { p ->
                picks[p] != null && MoveRules.folderProblem(p, picks[p], others[p]) != MovePathProblem.NONE
            }
    val extraErrors: Int get() = MoveRules.extraProblems(extra).count { it != MovePathProblem.NONE }
    val ruleErrors: Int get() = folderErrors + extraErrors
    val withItems: List<MovePlanGroup> get() = plan?.groups.orEmpty().filter { it.items.isNotEmpty() && it.name != "other" }
    val groupsOn: Int get() = withItems.count { it.name !in skip }
    val dryLines: List<MoveLine> get() = dry?.lines.orEmpty()
    val chooseLines: List<MoveLine>
        get() {
            val tried = dryLines.filter { it.choose.isNotEmpty() }
            val extraSeen =
                seenChoose.values.filter { l ->
                    tried.none { it.key == l.key } &&
                        people[l.key].let { it == "merge" || it == "skip" } &&
                        dryLines.any { it.key == l.key }
                }
            return tried + extraSeen
        }
    val openChoices: Int get() = chooseLines.count { it.result == "skipped" && people[it.key] != "skip" }
    val problems: Int get() = MoveRules.problems(dryLines, openChoices)
    val oldRows: List<String> get() = plan?.oldPaths.orEmpty().filter { p -> here.none { it.path == p } }
    val pickedSnapshots: List<MoveSnapshot> get() = snapshots.filter { it.id in picked }
}

internal object MoveView {
    fun build(i: MoveInner): SettingsMoveState {
        val gateStep = i.step
        val gate =
            MoveRules.stepGate(
                GateInput(
                    step = if (gateStep == SettingsMoveStep.DONE) SettingsMoveStep.DONE else gateStep,
                    picked = i.picked.size,
                    planning = i.planning,
                    planned = i.plan != null,
                    folderErrors = i.folderErrors,
                    ruleErrors = i.extraErrors,
                    groupsOn = i.groupsOn,
                    groupsWithItems = i.withItems.size,
                    trying = i.trying,
                    tried = i.dry != null,
                    failed = i.tryFailed,
                    stale = i.stale,
                    openChoices = i.openChoices,
                    moving = i.moveRunning || i.starting,
                    safeMode = i.safeMode,
                    hasProgress = i.note != null,
                ),
            )
        return SettingsMoveState(
            status = i.status,
            allowed = i.allowed,
            owner = i.owner,
            safeMode = i.safeMode,
            step = i.step,
            reads = reads(i),
            pickedCount = i.picked.size,
            savedKeys = i.savedKeys.map { MoveSavedKeyRow(it.kind, it.address) },
            moves = i.moves.map(::earlier),
            movesProblem = i.movesProblem,
            brought = i.brought.map { MoveBroughtRow(it.id, MoveVocabulary.appName(it.app), it.name, it.kind, it.needs) },
            sheet = i.sheet,
            choose = if (i.step == SettingsMoveStep.CHOOSE) choose(i) else MoveChooseState(checkingNow = i.checkingNow),
            run = run(i),
            gate = gate,
            dialog = i.dialog,
            starting = i.starting,
            problem = i.problem,
            gone = i.gone,
            notices = i.notices,
            readingKind = i.readingKind,
            canStart = i.dry != null && !i.stale && !i.starting,
            holder = MoveHolder(i),
        )
    }

    private fun reads(i: MoveInner): List<MoveReadRow> {
        val used = MoveRules.usedByMoves(i.moves, i.snapshots)
        val copies = i.snapshots.groupBy { it.kind }.mapValues { (_, list) -> list.map { it.address }.toSet().size }
        return i.snapshots.map { s ->
            MoveReadRow(
                id = s.id,
                kind = s.kind,
                name = MoveVocabulary.appName(s.kind),
                address = s.address,
                host = MoveVocabulary.hostOf(s.address),
                showHost = (copies[s.kind] ?: 0) > 1 && s.address.isNotEmpty(),
                version = s.appVersion,
                createdAt = s.createdAt,
                stats = MoveVocabulary.bestStats(s.kind, s.counts),
                usedBy = used[s.id] ?: 0,
                picked = s.id in i.picked,
            )
        }
    }

    private fun earlier(m: MoveRecord): MoveEarlierRow {
        val (word, tone) = MoveVocabulary.stateWord(m.state, m.undoState)
        val undo = m.state == "undone" || m.report?.undo == true
        return MoveEarlierRow(
            id = m.id,
            createdAt = m.createdAt,
            counts = MoveVocabulary.summary(m.report?.counts.orEmpty(), true, undo),
            word = word,
            tone = tone,
            canRemove = (m.state == "undone" || m.state == "failed") && m.undoState != "queued" && m.undoState != "running",
        )
    }

    private val BIG = listOf("titles", "wishlist", "people")

    private fun groupNote(
        g: MovePlanGroup,
        i: MoveInner,
        toCheck: Map<String, MoveLine>,
    ): Pair<MoveGroupNote, Int> {
        if (g.name in i.skip) return MoveGroupNote.LEFT_OUT to 0
        val needs = g.items.filter { it.needs.isNotEmpty() }
        if (needs.isNotEmpty() && g.name == "people") return MoveGroupNote.INVITE to needs.size
        if (needs.isNotEmpty()) {
            val keys = needs.all { it.needs.contains("key", ignoreCase = true) }
            return (if (keys) MoveGroupNote.KEYS else MoveGroupNote.KEYS_OR_PASSWORDS) to needs.size
        }
        val check = if (g.name == "titles") g.items.count { it.key in toCheck } else 0
        if (check > 0) return MoveGroupNote.CHECK_SHOWS to check
        return MoveGroupNote.NONE to 0
    }

    private fun folderRow(
        p: String,
        i: MoveInner,
        kindOf: Map<String, String>,
        shared: Map<String, List<String>>,
        unseen: Boolean,
    ): MoveFolderRow {
        val k = kindOf[p].orEmpty()
        val pick = i.picks[p] ?: FolderPick()
        val options = mutableListOf<String>()
        i.here.filter { k.isEmpty() || it.kind.isEmpty() || it.kind == k }.forEach { options += it.path }
        val sug = i.suggested[p].orEmpty()
        if (sug.isNotEmpty() && sug !in options) options.add(0, sug)
        i.seen.forEach { if (it !in options) options += it }
        val problem = MoveRules.folderProblem(p, i.picks[p], i.others[p])
        val to = MoveRules.cleanPath(MoveRules.folderTo(i.picks[p], i.others[p]))
        val miss = i.missing[p].orEmpty()
        val gone = if (problem == MovePathProblem.NONE && miss.isNotEmpty() && MoveRules.cleanPath(miss) == to) miss else ""
        return MoveFolderRow(
            oldPath = p,
            kind = k,
            unseen = unseen,
            options = options,
            suggested = sug,
            pick = pick.kind,
            path = pick.path,
            other = i.others[p].orEmpty(),
            problem = problem,
            gone = gone,
            twins = shared[p].orEmpty(),
            canAddRule = gone.isNotEmpty() && i.oldRows.isNotEmpty() && i.extra.none { it.from == p },
        )
    }

    private fun choose(i: MoveInner): MoveChooseState {
        val plan = i.plan
        val dryLines = i.dryLines
        val toCheck = MoveRules.seasonsShows(dryLines).associateBy { it.key }
        val base =
            MoveChooseState(
                planning = i.planning,
                planned = plan != null,
                note = i.note,
                planProblem = i.planProblem,
                trying = i.trying,
                tried = i.dry != null,
                stale = i.stale,
                tryFailed = i.tryFailed,
                tryProblem = i.tryProblem,
                tryNotice = i.tryNotice,
                checkingNow = i.checkingNow,
            )
        if (plan == null) return base
        val shown = plan.groups.filter { it.items.isNotEmpty() || it.name in i.skip }
        val emptyGroups = plan.groups.filter { it.items.isEmpty() && it.name !in i.skip && it.name != "other" }.map { it.label }
        val on = shown.filter { it.items.isNotEmpty() && it.name !in i.skip }
        val peopleItems =
            plan.groups
                .firstOrNull { it.name == "people" }
                ?.items
                .orEmpty()
        val peopleOn = peopleItems.isNotEmpty() && "people" !in i.skip
        val invitees =
            peopleItems.filter {
                it.person
                    ?.signIn
                    .orEmpty()
                    .isEmpty()
            }
        val inviteOn = peopleOn && invitees.isNotEmpty()
        val asked = peopleItems.filter { it.person?.wasOwner == true || it.person?.wasAdmin == true }
        val kindOf =
            plan.groups.firstOrNull { it.name == "folders" }?.items.orEmpty().associate {
                (it.folder?.oldPath ?: "") to
                    (it.folder?.kind ?: "")
            }
        val oldRows = i.oldRows
        val shared = MoveRules.sharedTargets(i.picks, i.others)
        val needPaths = oldRows.filter { it in i.held || (i.picks[it]?.kind ?: MovePickKind.NONE) == MovePickKind.NONE }
        val rowFor = { p: String -> folderRow(p, i, kindOf, shared, MoveRules.unseenFolder(plan, p)) }
        val lead =
            BIG.mapNotNull { n -> on.firstOrNull { it.name == n } }.ifEmpty {
                if (on.isEmpty()) emptyList() else listOf(on.maxBy { it.items.size })
            }
        val tiles = lead.map { MoveTile(it.name, MoveVocabulary.group(it.name), it.label, it.items.size) }
        val also = on.filter { it !in lead }.map { MoveVocabulary.group(it.name) }
        val sourceKeys =
            on
                .firstOrNull { it.name == "sources" }
                ?.items
                .orEmpty()
                .count { it.needs.isNotEmpty() }
        val downloaderKeys =
            on
                .firstOrNull { it.name == "downloaders" }
                ?.items
                .orEmpty()
                .count { it.needs.isNotEmpty() }
        val formatsAsk =
            i.noFormats ||
                plan.groups
                    .firstOrNull { it.name == "pick_rules" }
                    ?.items
                    .orEmpty()
                    .any { it.rules?.hasBody == true }
        val namesAsk =
            plan.groups
                .firstOrNull { it.name == "naming" }
                ?.items
                .orEmpty()
                .any { it.key.startsWith("names:") }
        val groupRows =
            shown.map { g ->
                val (note, noteCount) = groupNote(g, i, toCheck)
                MoveGroupRow(
                    id = g.name,
                    group = MoveVocabulary.group(g.name),
                    label = g.label,
                    count = g.items.size,
                    on = g.name !in i.skip,
                    canSkip = g.name != "other",
                    note = note,
                    noteCount = noteCount,
                    showFormats = g.name == "pick_rules" && formatsAsk && g.name !in i.skip,
                )
            }
        val notMovedApps =
            plan.notMoved
                .groupBy { it.from.substringBefore(' ') }
                .map { (app, list) -> MoveNotMovedApp(app, MoveVocabulary.appName(app), list.size) }
                .sortedByDescending { it.n }
        val outcome = MoveRules.folderOutcome(dryLines).takeIf { it.attention > 0 || it.unchecked > 0 }
        val choices =
            i.chooseLines.map { l ->
                MoveChoiceRow(
                    l.key,
                    l.name,
                    MoveVocabulary.chooseWhy(l.choose),
                    i.people[l.key]?.let { MoveRules.personChoice(it) }?.takeIf {
                        it == MovePersonChoice.MERGE ||
                            it == MovePersonChoice.SKIP
                    },
                )
            }
        return base.copy(
            empty = shown.isEmpty(),
            tiles = tiles,
            alsoGroups = also,
            stillChecking = i.dry?.checking ?: 0,
            needFolders = needPaths.map(rowFor),
            folders = oldRows.filter { it !in needPaths }.map(rowFor),
            people =
                if (peopleOn) {
                    asked.map {
                        MovePersonRow(
                            it.key,
                            it.name,
                            it.person?.app ?: it.from.substringBefore(' '),
                            it.person?.wasOwner == true,
                            i.people[it.key] == if (it.person?.wasOwner == true) "owner" else "admin",
                        )
                    }
                } else {
                    emptyList()
                },
            namesAsk = namesAsk,
            names = i.names,
            formatsAsk = formatsAsk,
            noFormats = i.noFormats,
            sourceKeys = sourceKeys,
            downloaderKeys = downloaderKeys,
            invitees = if (inviteOn) invitees.size else 0,
            groups = groupRows,
            emptyGroups = emptyGroups,
            rules = i.extra.zip(MoveRules.extraProblems(i.extra)).map { (r, p) -> MoveRuleRow(r.from, r.to, p) },
            showRules = oldRows.isEmpty() && (i.extra.isNotEmpty() || plan.notMoved.any { it.group == "titles" }),
            notMovedCount = plan.notMoved.size,
            notMovedApps = notMovedApps,
            problems = i.problems,
            openChoices = i.openChoices,
            total = MoveVocabulary.moveTotal(i.dry?.counts.orEmpty()),
            choices = choices,
            before = before(dryLines),
            outcome = outcome,
            apps =
                i.dry
                    ?.apps
                    .orEmpty()
                    .map { MoveVocabulary.appName(it) }
                    .distinct(),
            toCheck = toCheck.size,
        )
    }

    private fun before(lines: List<MoveLine>): List<MoveBeforeRow> =
        lines
            .groupBy { it.group }
            .flatMap { (group, list) ->
                listOf("not_found", "failed").mapNotNull { r ->
                    val n = list.count { it.result == r }
                    if (n == 0) null else MoveBeforeRow("$group:$r", MoveVocabulary.group(group), group, n, MoveVocabulary.resultWord(r))
                }
            }

    private fun run(i: MoveInner): MoveRunState? {
        val m = i.current ?: return null
        val rep = m.report
        val counts = rep?.counts.orEmpty()
        val lines = rep?.lines.orEmpty()
        val undone = m.state == "undone"
        val undoFailed = m.undoState == "failed" && !undone
        val (word, tone) = MoveVocabulary.stateWord(m.state, m.undoState)
        val seasons = MoveRules.seasonsShows(lines)
        val failed = counts["failed"] ?: 0
        val look =
            if (failed > 0) {
                MoveDoneLook.FAILED
            } else if (seasons.isNotEmpty()) {
                MoveDoneLook.PENDING
            } else {
                MoveDoneLook.READY
            }
        val isUndo = rep?.undo == true
        val plainNext = rep == null || isUndo || rep.lines.isEmpty()
        val noFolder = MoveRules.noFolderCount(lines)
        val renameNext = !isUndo && !undone && m.choices.names == "rename"
        val open = lines.count { it.choose.isNotEmpty() && it.result == "skipped" && m.choices.people[it.key] != "skip" }
        val madeAlerts = !isUndo && !undone && lines.any { it.group == "alerts" && it.result == "created" }
        val groups =
            lines
                .groupBy { it.group }
                .map { (g, list) ->
                    MoveResultGroup(
                        g,
                        MoveVocabulary.group(g),
                        g,
                        MoveVocabulary.summary(list.groupingBy { it.result }.eachCount(), true, isUndo),
                    )
                }
        val needsYou =
            if (plainNext) {
                rep?.next.orEmpty().isNotEmpty()
            } else {
                renameNext || noFolder > 0 || open > 0 || failed > 0 || seasons.isNotEmpty()
            }
        return MoveRunState(
            id = m.id,
            createdAt = m.createdAt,
            word = word,
            tone = tone,
            running = m.running,
            undoing = m.undoState == "queued" || m.undoState == "running",
            undone = undone,
            failed = m.state == "failed",
            undoFailed = undoFailed,
            progress = i.progress.toFloat().coerceIn(0f, 1f),
            note = MoveVocabulary.progressNote(i.progressNote),
            lost = i.lost,
            counts = MoveVocabulary.summary(counts, true, undone || isUndo),
            failedCount = failed,
            hasReport = rep != null,
            look = look,
            next = rep?.next.orEmpty(),
            plainNext = plainNext,
            noFolder = noFolder,
            renameNext = renameNext,
            openChoices = open,
            seasons = seasons.map { MoveSeasonRow(MoveRules.seasonName(it), it.titleId) },
            madeAlerts = madeAlerts,
            outcome = if (rep != null && !isUndo) MoveRules.folderOutcome(lines).takeIf { it.attention > 0 || it.unchecked > 0 } else null,
            groups = groups,
            notMoved = rep?.notMoved?.size ?: 0,
            keysAgain = rep?.needsInput?.size ?: 0,
            notes = rep?.notes?.size ?: 0,
            canUndo = (m.state == "done" || m.state == "failed") && m.undoState != "failed" && !m.running,
            needsYou = needsYou,
        )
    }

    fun list(
        i: MoveInner,
        kind: MoveListKind,
        id: String,
    ): MoveItemsList =
        MoveItemsList(
            when (kind) {
                MoveListKind.PLAN_GROUP -> {
                    val toCheck = MoveRules.seasonsShows(i.dryLines).associateBy { it.key }
                    i.plan
                        ?.groups
                        ?.firstOrNull { it.name == id }
                        ?.items
                        .orEmpty()
                        .map { planRow(it, toCheck) }
                }
                MoveListKind.INVITEES -> {
                    i.plan
                        ?.groups
                        ?.firstOrNull { it.name == "people" }
                        ?.items
                        .orEmpty()
                        .map { personRow(it, i) }
                }
                MoveListKind.REPORT_GROUP -> {
                    val rep = i.current?.report
                    rep
                        ?.lines
                        .orEmpty()
                        .filter { it.group == id }
                        .mapIndexed { n, l -> lineRow(l, n, true, rep?.undo == true) }
                }
                MoveListKind.BEFORE_GROUP -> {
                    val group = id.substringBefore(':')
                    val result = id.substringAfter(':')
                    i.dryLines.filter { it.group == group && it.result == result }.mapIndexed { n, l -> lineRow(l, n, false, false) }
                }
                MoveListKind.NOT_MOVED ->
                    i.current
                        ?.report
                        ?.notMoved
                        .orEmpty()
                        .mapIndexed { n, x -> notMovedRow(x, n) }
                MoveListKind.NOT_MOVED_APP -> {
                    val reasons = LinkedHashMap<String, MutableList<MoveNotMoved>>()
                    i.plan?.notMoved.orEmpty().filter { it.from.substringBefore(' ') == id }.forEach {
                        reasons.getOrPut(it.what + "|" + it.why) { mutableListOf() }.add(it)
                    }
                    reasons.values.sortedByDescending { it.size }.mapIndexed { n, list ->
                        MoveItemRow(key = "nm$n", name = list.first().what, hint = list.first().why, n = list.size)
                    }
                }
                MoveListKind.KEYS_AGAIN ->
                    keysAgainRows(
                        i.current
                            ?.report
                            ?.needsInput
                            .orEmpty(),
                    )
                MoveListKind.NOTES ->
                    i.current
                        ?.report
                        ?.notes
                        .orEmpty()
                        .mapIndexed { n, t -> MoveItemRow(key = "note$n", name = t) }
                MoveListKind.SOURCE_KEYS -> keyRows(i, "sources")
                MoveListKind.DOWNLOADER_KEYS -> keyRows(i, "downloaders")
            },
        )

    private fun planRow(
        it: MovePlanItem,
        toCheck: Map<String, MoveLine>,
    ): MoveItemRow {
        val check = toCheck[it.key]
        if (check != null) return MoveItemRow(key = it.key, name = MoveRules.seasonName(check), hint = check.detail, look = true)
        val rules = it.rules
        val lead = if (rules?.hasBody == true) listOf(it.detail, it.why) else listOf(it.why.ifEmpty { it.detail })
        return MoveItemRow(
            key = it.key,
            name = it.name,
            hint = lead.filter { s -> s.isNotEmpty() }.joinToString(" · "),
            traits = rules?.traits.orEmpty().joinToString(", ") { t -> "${t.name} ${if (t.points > 0) "+" else ""}${t.points}" },
            skipped = rules?.skipped.orEmpty().joinToString("; ") { s -> "${s.name} (${s.reason})" },
            needs = it.needs,
        )
    }

    private fun personRow(
        it: MovePlanItem,
        i: MoveInner,
    ): MoveItemRow {
        val badge =
            when {
                i.people[it.key] == "owner" -> MoveBadge.YOU
                it.person?.signIn == "plex" -> MoveBadge.PLEX
                it.person?.signIn == "jellyfin" -> MoveBadge.JELLYFIN
                it.person?.signIn == "emby" -> MoveBadge.EMBY
                else -> MoveBadge.INVITE
            }
        return MoveItemRow(key = it.key, name = it.name, badge = badge)
    }

    private fun lineRow(
        l: MoveLine,
        n: Int,
        after: Boolean,
        undo: Boolean,
    ): MoveItemRow =
        MoveItemRow(
            key = "${l.key}:$n",
            name = l.name,
            hint = if (l.result == "not_found") "" else l.detail,
            word = MoveVocabulary.resultWord(l.result, after, undo),
            raw = MoveVocabulary.rawWord(l.result),
            tone = MoveVocabulary.resultTone(l.result),
            look = MoveVocabulary.needsLook(l.result, l.attention),
        )

    private fun notMovedRow(
        x: MoveNotMoved,
        n: Int,
    ): MoveItemRow = MoveItemRow(key = "${x.from}$n", name = x.what, hint = x.why, group = MoveVocabulary.group(x.group))

    private fun keysAgainRows(list: List<String>): List<MoveItemRow> =
        list.mapIndexed { n, line ->
            val cuts = listOf(line.indexOf('"'), line.indexOf(':')).filter { it > 0 }
            val at = cuts.minOrNull() ?: -1
            val head = if (at > 0) line.substring(0, at).trim() else ""
            val rest = if (at > 0) line.substring(at).replace(Regex("^:\\s*"), "").trim() else line
            val label =
                if (head.isEmpty()) {
                    ""
                } else if (MoveVocabulary.apps.any { it.kind == head }) {
                    MoveVocabulary.appName(head)
                } else {
                    head
                }
            MoveItemRow(key = "k$n", name = rest.ifEmpty { line }, section = label)
        }

    private fun keyRows(
        i: MoveInner,
        which: String,
    ): List<MoveItemRow> {
        val plan = i.plan ?: return emptyList()
        val clean = { n: String -> n.replace(Regex("\\s*\\((Prowlarr|Jackett)\\)$", RegexOption.IGNORE_CASE), "") }

        fun need(it: MovePlanItem) = if (Regex("password", RegexOption.IGNORE_CASE).containsMatchIn(it.needs)) "password" else "key"
        if (which == "downloaders") {
            val dls =
                plan.groups
                    .firstOrNull { it.name == "downloaders" && it.name !in i.skip }
                    ?.items
                    .orEmpty()
                    .filter { it.needs.isNotEmpty() }
            return dls.map { MoveItemRow(key = it.key, name = clean(it.name), needs = need(it), section = "downloaders") }
        }
        val src =
            plan.groups
                .firstOrNull { it.name == "sources" && it.name !in i.skip }
                ?.items
                .orEmpty()
                .filter { it.needs.isNotEmpty() }
        val usenet = src.filter { Regex("newznab|usenet", RegexOption.IGNORE_CASE).containsMatchIn(it.detail) }
        val torrent = src.filter { it !in usenet }
        return usenet.map { MoveItemRow(key = it.key, name = clean(it.name), section = "usenet") } +
            torrent.map { MoveItemRow(key = it.key, name = clean(it.name), section = "torrent") }
    }
}
