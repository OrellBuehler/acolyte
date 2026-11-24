package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

@Serializable
internal data class MoveSnapshot(
    val id: Long = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("owner_id") val ownerId: Long = 0,
    val kind: String = "",
    val via: String = "",
    val address: String = "",
    @SerialName("app_version") val appVersion: String = "",
    val counts: Map<String, Int> = emptyMap(),
    val size: Long = 0,
)

@Serializable
internal data class MoveSnapshotList(
    val snapshots: List<MoveSnapshot> = emptyList(),
)

@Serializable
internal data class MoveSnapshotDetail(
    val snapshot: MoveSnapshot = MoveSnapshot(),
    val data: JsonElement = JsonNull,
)

@Serializable
internal data class MoveSource(
    val kind: String,
    val url: String? = null,
    val key: String? = null,
    val username: String? = null,
    val password: String? = null,
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean? = null,
    @SerialName("use_saved_key") val useSavedKey: Boolean? = null,
    val add: List<String>? = null,
    @SerialName("skip_linked") val skipLinked: List<String>? = null,
)

@Serializable
internal data class MoveProbeApp(
    val kind: String = "",
    val via: String = "",
    val version: String = "",
    val schema: Int = 0,
    val notes: List<String> = emptyList(),
)

@Serializable
internal data class MoveProbe(
    val app: MoveProbeApp = MoveProbeApp(),
)

@Serializable
internal data class MoveAddress(
    val kind: String = "",
    val address: String = "",
)

@Serializable
internal data class MoveReadLinked(
    val kind: String = "",
    val snapshot: MoveSnapshot = MoveSnapshot(),
    val replaced: List<Long> = emptyList(),
    @SerialName("needs_reentry") val needsReentry: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
)

@Serializable
internal data class MoveReadAnswer(
    val snapshot: MoveSnapshot = MoveSnapshot(),
    val replaced: List<Long> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    @SerialName("needs_reentry") val needsReentry: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val linked: List<MoveReadLinked> = emptyList(),
    val unread: List<MoveAddress> = emptyList(),
    val skipped: List<MoveAddress> = emptyList(),
)

@Serializable
internal data class MoveFindBody(
    val host: String? = null,
    val addresses: List<String>? = null,
    val kinds: List<String>? = null,
)

@Serializable
internal data class MoveFound(
    val kind: String = "",
    val address: String = "",
    val version: String = "",
)

@Serializable
internal data class MoveFindAnswer(
    val found: List<MoveFound> = emptyList(),
    val searched: List<String> = emptyList(),
)

@Serializable
internal data class MoveLinkedApp(
    val kind: String = "",
    val name: String = "",
    val address: String = "",
    @SerialName("four_k") val fourK: Boolean = false,
    @SerialName("has_key") val hasKey: Boolean = false,
    val reachable: Boolean = false,
    val version: String = "",
)

@Serializable
internal data class MoveLinkedFailed(
    val kind: String = "",
    val address: String = "",
    val error: String = "",
)

@Serializable
internal data class MoveLinkedAnswer(
    val apps: List<MoveLinkedApp> = emptyList(),
    val read: List<MoveReadLinked> = emptyList(),
    val failed: List<MoveLinkedFailed> = emptyList(),
)

@Serializable
internal data class MoveSavedKey(
    val kind: String = "",
    val address: String = "",
    @SerialName("allow_local_network") val allowLocalNetwork: Boolean = false,
)

@Serializable
internal data class MoveSavedKeys(
    val saved: List<MoveSavedKey> = emptyList(),
)

@Serializable
internal data class MovePathRule(
    val from: String = "",
    val to: String = "",
)

@Serializable
internal data class MoveInput(
    val snapshots: List<Long>,
    @SerialName("path_rules") val pathRules: List<MovePathRule> = emptyList(),
    val skip: List<String> = emptyList(),
    val people: Map<String, String>? = null,
    @SerialName("no_formats") val noFormats: Boolean? = null,
    @SerialName("leave_folders") val leaveFolders: List<String>? = null,
    val names: String? = null,
    @SerialName("plan_hash") val planHash: String? = null,
    val quick: Boolean? = null,
)

@Serializable
internal data class MoveNotMoved(
    val group: String = "",
    val from: String = "",
    val what: String = "",
    val why: String = "",
)

@Serializable
internal data class MovePersonPlan(
    val app: String = "",
    val email: String = "",
    val role: String = "",
    @SerialName("was_admin") val wasAdmin: Boolean = false,
    @SerialName("was_owner") val wasOwner: Boolean = false,
    val choice: String = "",
    @SerialName("sign_in") val signIn: String = "",
)

@Serializable
internal data class MoveFolderPlan(
    @SerialName("old_path") val oldPath: String = "",
    val path: String = "",
    val kind: String = "",
    val label: String = "",
    val left: Boolean = false,
    val unseen: Boolean = false,
    val checked: Int = 0,
    val found: Int = 0,
    val missing: Boolean = false,
    @SerialName("read_only") val readOnly: Boolean = false,
    val suggest: String = "",
)

@Serializable
internal data class MoveTitlePlan(
    val kind: String = "",
    val library: String = "",
    val folder: String = "",
)

@Serializable
internal data class MoveRuleTrait(
    val name: String = "",
    val points: Int = 0,
)

@Serializable
internal data class MoveRuleSkip(
    val name: String = "",
    val reason: String = "",
)

@Serializable
internal data class MoveRulesPlan(
    val app: String = "",
    val profile: String = "",
    val preset: String = "",
    val name: String = "",
    val body: JsonElement = JsonNull,
    val traits: List<MoveRuleTrait> = emptyList(),
    val skipped: List<MoveRuleSkip> = emptyList(),
    val notes: List<String> = emptyList(),
) {
    val hasBody: Boolean get() = body !is JsonNull
}

@Serializable
internal data class MovePlanItem(
    val key: String = "",
    val name: String = "",
    val from: String = "",
    val detail: String = "",
    val why: String = "",
    val needs: String = "",
    val person: MovePersonPlan? = null,
    val folder: MoveFolderPlan? = null,
    val title: MoveTitlePlan? = null,
    val rules: MoveRulesPlan? = null,
)

@Serializable
internal data class MovePlanGroup(
    val name: String = "",
    val label: String = "",
    val skipped: Boolean = false,
    val items: List<MovePlanItem> = emptyList(),
)

@Serializable
internal data class MovePlan(
    val snapshots: List<Long> = emptyList(),
    val apps: List<String> = emptyList(),
    val groups: List<MovePlanGroup> = emptyList(),
    @SerialName("not_moved") val notMoved: List<MoveNotMoved> = emptyList(),
    @SerialName("needs_input") val needsInput: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    @SerialName("old_paths") val oldPaths: List<String> = emptyList(),
)

@Serializable
internal data class MoveLine(
    val group: String = "",
    val key: String = "",
    val name: String = "",
    val result: String = "",
    val detail: String = "",
    val needs: String = "",
    val choose: String = "",
    val attention: String = "",
    val folder: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    val here: String = "",
)

@Serializable
internal data class MoveReport(
    val move: Long = 0,
    val dry: Boolean = false,
    val undo: Boolean = false,
    val apps: List<String> = emptyList(),
    val at: String = "",
    val lines: List<MoveLine> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    @SerialName("not_moved") val notMoved: List<MoveNotMoved> = emptyList(),
    @SerialName("needs_input") val needsInput: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val next: List<String> = emptyList(),
    val checking: Int = 0,
)

@Serializable
internal data class MoveCheck(
    @SerialName("check_id") val checkId: String = "",
    val kind: String = "",
    val state: String = "",
    val step: String = "",
    val done: Int = 0,
    val total: Int = 0,
    val now: String = "",
    val counts: Map<String, Int> = emptyMap(),
    val plan: MovePlan? = null,
    @SerialName("suggested_path_rules") val suggestedPathRules: List<MovePathRule> = emptyList(),
    @SerialName("missing_paths") val missingPaths: Map<String, String> = emptyMap(),
    @SerialName("seen_folders") val seenFolders: List<String> = emptyList(),
    @SerialName("here_folders") val hereFolders: List<String> = emptyList(),
    @SerialName("plan_hash") val planHash: String = "",
    val report: MoveReport? = null,
)

@Serializable
internal data class MoveChoices(
    @SerialName("path_rules") val pathRules: List<MovePathRule> = emptyList(),
    val skip: List<String> = emptyList(),
    val people: Map<String, String> = emptyMap(),
    @SerialName("no_formats") val noFormats: Boolean = false,
    @SerialName("leave_folders") val leaveFolders: List<String> = emptyList(),
    val names: String = "",
)

@Serializable
internal data class MoveRecord(
    val id: Long = 0,
    @SerialName("created_at") val createdAt: String = "",
    val snapshots: List<Long> = emptyList(),
    val choices: MoveChoices = MoveChoices(),
    val state: String = "",
    @SerialName("job_id") val jobId: Long = 0,
    val report: MoveReport? = null,
    @SerialName("finished_at") val finishedAt: String = "",
    @SerialName("undone_at") val undoneAt: String = "",
    @SerialName("undo_state") val undoState: String = "",
    @SerialName("undo_job_id") val undoJobId: Long = 0,
) {
    val running: Boolean
        get() = state == "queued" || state == "running" || undoState == "queued" || undoState == "running"
}

@Serializable
internal data class MoveList(
    val moves: List<MoveRecord> = emptyList(),
)

@Serializable
internal data class MoveAnswer(
    val move: MoveRecord = MoveRecord(),
    val progress: Double = 0.0,
    @SerialName("progress_note") val progressNote: String = "",
)

@Serializable
internal data class MoveStarted(
    val move: MoveRecord = MoveRecord(),
    @SerialName("job_id") val jobId: Long = 0,
    @SerialName("undo_job_id") val undoJobId: Long = 0,
)

@Serializable
internal data class MoveBringBody(
    val snapshots: List<Long>,
)

@Serializable
internal data class MoveBrought(
    val id: Long = 0,
    val app: String = "",
    val name: String = "",
    val result: String = "",
    val detail: String = "",
    val kind: String = "",
    val needs: String = "",
)

@Serializable
internal data class MoveBroughtList(
    val downloaders: List<MoveBrought> = emptyList(),
)
