package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val id: Long = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("title_id") val titleId: Long = 0,
    @SerialName("title_name") val titleName: String = "",
    @SerialName("title_kind") val titleKind: String = "",
    val year: Int = 0,
    val poster: String = "",
    @SerialName("album_id") val albumId: Long = 0,
    @SerialName("album_name") val albumName: String = "",
    val season: Int? = null,
    @SerialName("item_id") val itemId: Long = 0,
    val episode: Int = 0,
    val track: Int = 0,
    @SerialName("item_name") val itemName: String = "",
    @SerialName("version_id") val versionId: Long = 0,
    val version: String = "",
    @SerialName("file_id") val fileId: Long = 0,
    val where: String = "",
    val kind: String = "",
    @SerialName("kind_label") val kindLabel: String = "",
    val note: String = "",
    val state: String = "",
    @SerialName("state_label") val stateLabel: String = "",
    @SerialName("made_by") val madeBy: AskWho? = null,
    @SerialName("closed_by") val closedBy: AskWho? = null,
    @SerialName("closed_at") val closedAt: String = "",
    val reply: String = "",
    val mine: Boolean = false,
)

@Serializable
data class ReportList(
    val reports: List<Report> = emptyList(),
    val open: Int = 0,
)

@Serializable
data class ReportAnswer(
    val report: Report = Report(),
)

@Serializable
internal data class NewReport(
    @SerialName("title_id") val titleId: Long,
    val kind: String,
    val note: String? = null,
    val season: Int? = null,
    @SerialName("item_id") val itemId: Long? = null,
    @SerialName("album_id") val albumId: Long? = null,
    @SerialName("version_id") val versionId: Long? = null,
)

@Serializable
internal data class ReportFix(
    val reply: String? = null,
)

@Serializable
internal data class TitleSearchBody(
    val season: Int? = null,
    @SerialName("item_id") val itemId: Long? = null,
    @SerialName("album_id") val albumId: Long? = null,
    @SerialName("version_id") val versionId: Long? = null,
)

@Serializable
data class TitleSearchQueued(
    @SerialName("job_ids") val jobIds: List<Long> = emptyList(),
    @SerialName("left_out") val leftOut: Int = 0,
)
