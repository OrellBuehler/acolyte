package ch.orellbuehler.acolyte.feature.move

import ch.orellbuehler.acolyte.api.model.MoveLine
import ch.orellbuehler.acolyte.api.model.MovePathRule
import ch.orellbuehler.acolyte.api.model.MovePlan
import ch.orellbuehler.acolyte.api.model.MoveRecord
import ch.orellbuehler.acolyte.api.model.MoveSnapshot
import ch.orellbuehler.acolyte.feature.downloads.Days

enum class MovePickKind {
    NONE,
    LEAVE,
    OTHER,
    PATH,
}

internal data class FolderPick(
    val kind: MovePickKind = MovePickKind.NONE,
    val path: String = "",
)

internal data class GateInput(
    val step: SettingsMoveStep,
    val picked: Int,
    val planning: Boolean = false,
    val planned: Boolean = false,
    val folderErrors: Int = 0,
    val ruleErrors: Int = 0,
    val groupsOn: Int = 0,
    val groupsWithItems: Int = 0,
    val trying: Boolean = false,
    val tried: Boolean = false,
    val failed: Boolean = false,
    val stale: Boolean = false,
    val openChoices: Int = 0,
    val moving: Boolean = false,
    val safeMode: Boolean = false,
    val hasProgress: Boolean = false,
)

internal object MoveRules {
    private val FULL = Regex("^[A-Za-z]:[\\\\/]")
    private val DOTS = Regex("(^|[\\\\/])\\.\\.([\\\\/]|$)")

    fun ruleProblem(
        from: String,
        to: String,
    ): MovePathProblem {
        fun full(p: String) = p.startsWith("/") || FULL.containsMatchIn(p)
        return when {
            from.isEmpty() || to.isEmpty() -> MovePathProblem.FILL_BOTH
            !full(from) || !full(to) -> MovePathProblem.FULL_PATH
            DOTS.containsMatchIn("$from/$to") -> MovePathProblem.DOTS
            else -> MovePathProblem.NONE
        }
    }

    fun folderTo(
        pick: FolderPick?,
        other: String?,
    ): String =
        when (pick?.kind) {
            null, MovePickKind.NONE, MovePickKind.LEAVE -> ""
            MovePickKind.OTHER -> other.orEmpty().trim()
            MovePickKind.PATH -> pick.path
        }

    fun folderProblem(
        from: String,
        pick: FolderPick?,
        other: String?,
    ): MovePathProblem {
        if (pick == null || pick.kind == MovePickKind.NONE || pick.kind == MovePickKind.LEAVE) return MovePathProblem.NONE
        if (pick.kind == MovePickKind.OTHER && other.orEmpty().trim().isEmpty()) return MovePathProblem.TYPE_PATH
        return ruleProblem(from, folderTo(pick, other))
    }

    fun cleanPath(path: String): String {
        val c = path.trim().replace('\\', '/').replace(Regex("/{2,}"), "/")
        return if (c.length > 1) c.removeSuffix("/") else c
    }

    fun sharedTargets(
        picks: Map<String, FolderPick>,
        others: Map<String, String>,
    ): Map<String, List<String>> {
        val by = LinkedHashMap<String, MutableList<String>>()
        for ((from, pick) in picks) {
            val to = cleanPath(folderTo(pick, others[from]))
            if (to.isNotEmpty()) by.getOrPut(to) { mutableListOf() }.add(from)
        }
        val out = LinkedHashMap<String, List<String>>()
        for (froms in by.values) if (froms.size > 1) for (f in froms) out[f] = froms.filter { it != f }
        return out
    }

    fun extraProblems(rules: List<MovePathRule>): List<MovePathProblem> =
        rules.map { r ->
            val from = r.from.trim()
            val to = r.to.trim()
            if (from.isEmpty() && to.isEmpty()) MovePathProblem.NONE else ruleProblem(from, to)
        }

    fun cleanRules(rules: List<MovePathRule>): List<MovePathRule> =
        rules.map { MovePathRule(it.from.trim(), it.to.trim()) }.filter { it.from.isNotEmpty() || it.to.isNotEmpty() }

    fun rulesFrom(
        picks: Map<String, FolderPick>,
        others: Map<String, String>,
        extra: List<MovePathRule>,
    ): List<MovePathRule> {
        val out = mutableListOf<MovePathRule>()
        for ((from, pick) in picks) {
            val to = folderTo(pick, others[from])
            if (to.isNotEmpty()) out += MovePathRule(from, to)
        }
        for (r in cleanRules(extra)) if (out.none { it.from == r.from }) out += r
        return out
    }

    fun unseenFolder(
        plan: MovePlan?,
        path: String,
    ): Boolean = plan?.groups.orEmpty().any { g -> g.items.any { it.folder?.oldPath == path && it.folder.unseen } }

    fun leaveFolders(
        plan: MovePlan?,
        here: List<String>,
        picks: Map<String, FolderPick>,
    ): List<String> =
        plan?.oldPaths.orEmpty().filter { p ->
            p !in here &&
                (
                    picks[p]?.kind == MovePickKind.LEAVE ||
                        (picks[p].let { it == null || it.kind == MovePickKind.NONE } && !unseenFolder(plan, p))
                )
        }

    fun instanceKey(s: MoveSnapshot): String = "${s.kind} ${s.address}"

    fun latestPerInstance(snaps: List<MoveSnapshot>): List<MoveSnapshot> {
        val seen = HashSet<String>()
        return snaps.filter { seen.add(instanceKey(it)) }
    }

    fun usedByMoves(
        moves: List<MoveRecord>,
        snapshots: List<MoveSnapshot>,
    ): Map<Long, Long> {
        val made = snapshots.associate { it.id to (Days.parseInstant(it.createdAt)?.toEpochMilliseconds() ?: 0L) }
        val out = LinkedHashMap<Long, Long>()
        for (m in moves) {
            if (m.state != "done") continue
            val at = Days.parseInstant(m.createdAt)?.toEpochMilliseconds() ?: 0L
            for (id in m.snapshots) if (id !in out && !((made[id] ?: 0L) > at)) out[id] = m.id
        }
        return out
    }

    private val CAME = setOf("create", "created", "adopt", "adopted")

    fun folderOutcome(lines: List<MoveLine>): MoveFolderOutcome {
        var came = 0
        var attention = 0
        var unchecked = 0
        val attentionFolders = mutableListOf<String>()
        val uncheckedFolders = mutableListOf<String>()

        fun add(
            list: MutableList<String>,
            f: String,
        ) {
            if (f.isNotEmpty() && f !in list) list += f
        }
        for (l in lines) {
            if (l.group == "folders" && l.attention == "unchecked") add(uncheckedFolders, l.folder)
            if (l.group != "titles") continue
            when {
                l.attention == "unchecked" -> {
                    unchecked++
                    add(uncheckedFolders, l.folder)
                }
                l.attention == "unreachable" -> {
                    attention++
                    add(attentionFolders, l.folder)
                }
                l.result in CAME -> came++
            }
        }
        return MoveFolderOutcome(came, attention, attentionFolders, unchecked, uncheckedFolders)
    }

    fun noFolderCount(lines: List<MoveLine>): Int {
        val came = folderOutcome(lines).attentionFolders
        return lines.count { l ->
            l.result == "not_found" && l.group != "pick_rules" && !(l.group == "folders" && l.key.removePrefix("folder:") in came)
        }
    }

    fun seasonsShows(lines: List<MoveLine>): List<MoveLine> = lines.filter { it.group == "titles" && it.here.isNotEmpty() }

    fun seasonName(line: MoveLine): String = "${line.here} (${line.name.replace(Regex("\\s*\\(\\d{4}\\)$"), "")})"

    fun problems(
        lines: List<MoveLine>,
        openChoices: Int,
    ): Int = lines.count { it.result == "not_found" || it.result == "failed" } + openChoices

    fun stepGate(input: GateInput): MoveGate =
        when (input.step) {
            SettingsMoveStep.READ -> if (input.picked > 0) ok() else MoveGate(false, MoveGateReason.TURN_ON_READ)
            SettingsMoveStep.CHOOSE -> chooseGate(input)
            SettingsMoveStep.RUN -> importGate(input)
            SettingsMoveStep.DONE -> ok()
        }

    private fun ok() = MoveGate(true, MoveGateReason.NONE)

    private fun chooseGate(s: GateInput): MoveGate =
        when {
            s.picked == 0 -> MoveGate(false, MoveGateReason.TURN_ON_READ)
            s.planning && s.hasProgress -> MoveGate(false, MoveGateReason.PROGRESS)
            s.planning || !s.planned -> MoveGate(false, MoveGateReason.UPDATING)
            s.folderErrors > 0 -> MoveGate(false, MoveGateReason.FIX_FOLDER)
            s.ruleErrors > 0 -> MoveGate(false, MoveGateReason.FIX_RULE)
            s.groupsWithItems == 0 -> MoveGate(false, MoveGateReason.NOTHING_NEW)
            s.groupsOn == 0 -> MoveGate(false, MoveGateReason.TURN_ON_GROUP)
            s.safeMode -> MoveGate(false, MoveGateReason.SAFE_MODE)
            else -> ok()
        }

    private fun importGate(s: GateInput): MoveGate {
        if (s.moving) return MoveGate(false, MoveGateReason.NONE)
        if (s.trying) {
            return MoveGate(
                false,
                if (s.hasProgress) {
                    MoveGateReason.PROGRESS
                } else if (s.tried) {
                    MoveGateReason.UPDATING
                } else {
                    MoveGateReason.TRYING
                },
            )
        }
        return when {
            s.failed -> MoveGate(false, MoveGateReason.TEST_FAILED)
            s.stale || !s.tried -> MoveGate(false, MoveGateReason.CHANGED_SINCE_TEST)
            s.safeMode -> MoveGate(false, MoveGateReason.SAFE_MODE)
            s.openChoices == 1 -> MoveGate(true, MoveGateReason.ONE_NEEDS_CHOICE, 1)
            s.openChoices > 1 -> MoveGate(true, MoveGateReason.MANY_NEED_CHOICE, s.openChoices)
            else -> ok()
        }
    }

    fun personChoice(value: String): MovePersonChoice? =
        when (value) {
            "owner" -> MovePersonChoice.OWNER
            "admin" -> MovePersonChoice.ADMIN
            "merge" -> MovePersonChoice.MERGE
            "skip" -> MovePersonChoice.SKIP
            else -> null
        }

    fun wire(choice: MovePersonChoice): String = choice.name.lowercase()
}
