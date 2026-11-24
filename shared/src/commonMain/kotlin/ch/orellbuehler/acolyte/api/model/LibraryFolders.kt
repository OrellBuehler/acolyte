package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibraryFolderList(
    @SerialName("library_folders") val libraryFolders: List<LibraryFolder> = emptyList(),
)

@Serializable
data class LibraryFolder(
    val id: Long = 0,
    val path: String = "",
    val kind: String = "",
    val label: String = "",
    @SerialName("default_pick_rules_id") val defaultPickRulesId: Long? = null,
    @SerialName("free_space") val freeSpace: Long = 0,
    val health: String = "",
    @SerialName("health_reason") val healthReason: String = "",
    @SerialName("health_checked_at") val healthCheckedAt: String = "",
    @SerialName("copies_expected") val copiesExpected: Boolean = false,
    @SerialName("new_title_tags") val titleTags: List<String> = emptyList(),
    @SerialName("only_tagged_sources") val onlyTaggedSources: Boolean = false,
    @SerialName("new_title_audio") val titleAudio: List<String> = emptyList(),
    @SerialName("look_only") val lookOnly: Boolean = false,
    val flat: Boolean = false,
)

@Serializable
internal data class FolderInput(
    val path: String,
    val kind: String,
    val label: String? = null,
    @SerialName("default_pick_rules_id") val defaultPickRulesId: Long? = null,
    @SerialName("new_title_tags") val titleTags: List<String>? = null,
    @SerialName("only_tagged_sources") val onlyTaggedSources: Boolean? = null,
    @SerialName("new_title_audio") val titleAudio: List<String>? = null,
    @SerialName("look_only") val lookOnly: Boolean? = null,
    val flat: Boolean? = null,
)

@Serializable
internal data class FolderChange(
    val label: String? = null,
    @SerialName("default_pick_rules_id") val defaultPickRulesId: Long? = null,
    @SerialName("copies_expected") val copiesExpected: Boolean? = null,
    @SerialName("new_title_tags") val titleTags: List<String>? = null,
    @SerialName("only_tagged_sources") val onlyTaggedSources: Boolean? = null,
    @SerialName("new_title_audio") val titleAudio: List<String>? = null,
    @SerialName("look_only") val lookOnly: Boolean? = null,
    val flat: Boolean? = null,
)

@Serializable
internal data class FolderPath(
    val path: String,
)

@Serializable
data class ImportFolders(
    @SerialName("library_folders") val libraryFolders: List<String> = emptyList(),
    @SerialName("downloader_folders") val downloaderFolders: List<String> = emptyList(),
    @SerialName("extra_folders") val extraFolders: List<String> = emptyList(),
    @SerialName("watch_folders") val watchFolders: List<String> = emptyList(),
)

@Serializable
internal data class ImportFoldersChange(
    @SerialName("extra_folders") val extraFolders: List<String>,
    @SerialName("watch_folders") val watchFolders: List<String>,
)

@Serializable
data class SideFiles(
    val on: Boolean = false,
    val types: List<String> = emptyList(),
)

@Serializable
data class HandsOff(
    val on: Boolean = false,
)

@Serializable
data class TagList(
    val tags: List<TagUse> = emptyList(),
)

@Serializable
data class TagUse(
    val label: String = "",
    val titles: Int = 0,
)
