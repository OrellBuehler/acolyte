package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class HomeAlbum(
    val id: Long = 0,
    val name: String = "",
    val cover: String? = null,
)

@Serializable
internal data class HomeCard(
    val key: String = "",
    val kind: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    val name: String = "",
    val year: Int = 0,
    val line: String = "",
    val href: String = "",
    val backdrop: String? = null,
    val poster: String? = null,
    val logo: String? = null,
    val season: Int = 0,
    val albums: List<HomeAlbum> = emptyList(),
    val at: String = "",
)

@Serializable
internal data class HomeLibraryCount(
    val titles: Int = 0,
    val parts: Int = 0,
    val items: Int = 0,
    @SerialName("on_disk") val onDisk: Int = 0,
    val missing: Int = 0,
    val downloading: Int = 0,
    val size: Long = 0,
)

@Serializable
internal data class HomeAskProgress(
    val progress: Double = 0.0,
    val state: String = "",
    val eta: Long = 0,
)

@Serializable
internal data class HomeAnswer(
    val rows: List<String> = emptyList(),
    val owner: Boolean = false,
    val kid: Boolean = false,
    val arrived: List<HomeCard> = emptyList(),
    @SerialName("new_seasons") val newSeasons: List<HomeCard> = emptyList(),
    val following: List<HomeCard> = emptyList(),
    val soon: List<HomeCard> = emptyList(),
    @SerialName("ask_progress") val askProgress: Map<String, HomeAskProgress> = emptyMap(),
    val library: Map<String, HomeLibraryCount> = emptyMap(),
)

@Serializable
internal data class HomeDownload(
    val id: Long = 0,
    val phase: String = "",
    val state: String = "",
    val progress: Double = 0.0,
    val eta: Long = 0,
    val speed: Double = 0.0,
    val active: Boolean = false,
    val title: String = "",
    val name: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    val replaced: Boolean = false,
    @SerialName("client_hint") val clientHint: String = "",
    @SerialName("client_item") val clientItem: Boolean = false,
    val outside: Boolean = false,
    @SerialName("finished_at") val finishedAt: String = "",
    val text: String = "",
)

@Serializable
internal data class HomeDownloads(
    val downloads: List<HomeDownload> = emptyList(),
)

@Serializable
internal data class HomeHealthPart(
    val name: String = "",
    val state: String = "",
)

@Serializable
internal data class HomeHealth(
    val version: String = "",
    @SerialName("uptime_s") val uptimeSeconds: Long = 0,
    val sources: List<HomeHealthPart>? = null,
    val downloaders: List<HomeHealthPart>? = null,
)

@Serializable
internal data class HomeEpisode(
    val label: String = "",
)

@Serializable
internal data class HomeDecision(
    @SerialName("slot_id") val slotId: Long = 0,
    val title: String = "",
    val reason: String = "",
    val episode: HomeEpisode? = null,
)

@Serializable
internal data class HomeDecisions(
    val items: List<HomeDecision> = emptyList(),
    val count: Int = 0,
)
