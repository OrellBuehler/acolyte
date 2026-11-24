package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.ApiJson
import ch.orellbuehler.acolyte.api.model.DownloadRow
import ch.orellbuehler.acolyte.api.model.DownloadsView
import ch.orellbuehler.acolyte.api.model.ImportRecord
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

internal data class DlSyncState(
    val boot: String,
    val revs: Map<Long, Long>,
)

internal data class DlPatch(
    val boot: String,
    val client: Long,
    val fromRev: Long,
    val toRev: Long,
    val upsert: List<JsonObject>,
    val remove: List<String>,
    val upSpeed: Long?,
    val speed: Long?,
)

internal data class DlHealth(
    val boot: String,
    val client: Long,
    val health: String,
    val name: String,
    val stale: Boolean,
    val seenAt: String,
)

internal sealed interface DlOutcome {
    data class Applied(
        val rows: List<DownloadRow>,
        val sync: DlSyncState,
    ) : DlOutcome

    data object Dropped : DlOutcome

    data class Reread(
        val client: Long?,
    ) : DlOutcome
}

internal data class LiveDownload(
    val id: Long,
    val state: String,
    val reason: String,
    val progress: Double?,
    val speed: Long?,
    val upSpeed: Long?,
    val seeds: Int?,
    val peers: Int?,
    val eta: Long?,
)

internal object DlSync {
    fun fromAnswer(answer: DownloadsView): DlSyncState? {
        val revs = answer.revs ?: return null
        return DlSyncState(answer.boot, revs.mapNotNull { (k, v) -> k.toLongOrNull()?.let { it to v } }.toMap())
    }

    fun parse(data: JsonElement): Any? {
        val o = data as? JsonObject ?: return null
        val client = o.long("client") ?: return null
        return when (o.string("kind")) {
            "patch" ->
                DlPatch(
                    boot = o.string("boot").orEmpty(),
                    client = client,
                    fromRev = o.long("fromRev") ?: 0,
                    toRev = o.long("toRev") ?: 0,
                    upsert = (o["upsert"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty(),
                    remove = (o["remove"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty(),
                    upSpeed = (o["totals"] as? JsonObject)?.long("up_speed"),
                    speed = (o["totals"] as? JsonObject)?.long("speed"),
                )
            "health" ->
                DlHealth(
                    boot = o.string("boot").orEmpty(),
                    client = client,
                    health = o.string("health").orEmpty(),
                    name = o.string("name").orEmpty(),
                    stale = (o["stale"] as? JsonPrimitive)?.booleanOrNull ?: false,
                    seenAt = o.string("seen_at").orEmpty(),
                )
            else -> null
        }
    }

    fun apply(
        rows: List<DownloadRow>,
        sync: DlSyncState,
        patch: DlPatch,
    ): DlOutcome {
        if (patch.boot.isNotEmpty() && sync.boot.isNotEmpty() && patch.boot != sync.boot) return DlOutcome.Reread(null)
        val have = sync.revs[patch.client] ?: 0
        if (patch.toRev <= have) return DlOutcome.Dropped
        if (patch.fromRev != have) return DlOutcome.Reread(patch.client)
        val gone = patch.remove.toSet()
        val ups = linkedMapOf<String, JsonObject>()
        for (u in patch.upsert) u.string("key")?.let { ups[it] = u }
        val out = mutableListOf<DownloadRow>()
        for (r in rows) {
            val key = r.liveKey
            if (key.isNotEmpty() && key in gone && r.clientItem) continue
            val u = if (key.isNotEmpty()) ups.remove(key) else null
            out += if (u == null) r else merge(r, u)
        }
        for (u in ups.values) {
            if ((u["client_item"] as? JsonPrimitive)?.booleanOrNull == true && u.string("name") != null) out += merge(DownloadRow(), u)
        }
        return DlOutcome.Applied(out, sync.copy(revs = sync.revs + (patch.client to patch.toRev)))
    }

    fun mergeClient(
        rows: List<DownloadRow>,
        sync: DlSyncState,
        client: Long,
        page: List<DownloadRow>,
        revs: Map<String, Long>?,
    ): Pair<List<DownloadRow>, DlSyncState> {
        val kept = rows.filter { it.downloaderId != client }
        val rev = revs?.get(client.toString()) ?: sync.revs[client] ?: 0
        val fresh = page.filter { it.downloaderId == client }
        val own = (kept.filter { !it.clientItem } + fresh.filter { !it.clientItem }).sortedByDescending { it.id }
        val outside = kept.filter { it.clientItem } + fresh.filter { it.clientItem }
        return (own + outside) to sync.copy(revs = sync.revs + (client to rev))
    }

    fun wentStale(
        before: Boolean,
        health: DlHealth,
    ): Boolean = health.stale && !before

    fun merge(
        r: DownloadRow,
        u: JsonObject,
    ): DownloadRow {
        val base =
            ApiJson.lenient
                .encodeToJsonElement(DownloadRow.serializer(), r)
                .jsonObject
                .toMutableMap()
        for ((field, value) in u) {
            if (field == "key") continue
            if (value is JsonNull) base.remove(field) else base[field] = value
        }
        if ((base["live_key"] as? JsonPrimitive)?.contentOrNull.isNullOrEmpty()) {
            u
                .string(
                    "key",
                )?.let { base["live_key"] = JsonPrimitive(it) }
        }
        return try {
            ApiJson.lenient.decodeFromJsonElement(DownloadRow.serializer(), JsonObject(base))
        } catch (e: SerializationException) {
            r
        } catch (e: IllegalArgumentException) {
            r
        }
    }

    fun liveDownload(data: JsonElement): LiveDownload? {
        val o = data as? JsonObject ?: return null
        val id = o.long("ID") ?: o.long("id") ?: return null
        return LiveDownload(
            id = id,
            state = o.string("State") ?: o.string("state").orEmpty(),
            reason = o.string("StateReason") ?: o.string("state_reason").orEmpty(),
            progress = o.double("Progress") ?: o.double("progress"),
            speed = o.long("Speed") ?: o.long("speed"),
            upSpeed = o.long("UpSpeed") ?: o.long("up_speed"),
            seeds = (o.long("Seeds") ?: o.long("seeds"))?.toInt(),
            peers = (o.long("Peers") ?: o.long("peers"))?.toInt(),
            eta = o.long("ETA") ?: o.long("eta"),
        )
    }

    fun applyDownload(
        rows: List<DownloadRow>,
        live: LiveDownload,
    ): List<DownloadRow> =
        rows.map { r ->
            if (r.id != live.id || r.clientItem) {
                r
            } else {
                r.copy(
                    progress = live.progress ?: r.progress,
                    speed = live.speed ?: r.speed,
                    upSpeed = live.upSpeed ?: r.upSpeed,
                    seeds = live.seeds ?: r.seeds,
                    peers = live.peers ?: r.peers,
                    eta = live.eta ?: r.eta,
                )
            }
        }

    fun failureNotice(live: LiveDownload): Boolean =
        live.state == "failed" &&
            !live.reason
                .trim()
                .lowercase()
                .startsWith("marked failed")

    fun liveImport(data: JsonElement): ImportRecord? =
        try {
            ApiJson.lenient.decodeFromJsonElement(ImportRecord.serializer(), data).takeIf { it.id > 0 }
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }

    fun withImport(
        imports: Map<Long, ImportRecord>,
        record: ImportRecord,
    ): Map<Long, ImportRecord> {
        if (record.downloadId <= 0) return imports
        val had = imports[record.downloadId]
        return if (had == null || record.id >= had.id) imports + (record.downloadId to record) else imports
    }

    private fun JsonObject.string(name: String): String? = (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    private fun JsonObject.long(name: String): Long? = (this[name] as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }

    private fun JsonObject.double(name: String): Double? = (this[name] as? JsonPrimitive)?.doubleOrNull
}
