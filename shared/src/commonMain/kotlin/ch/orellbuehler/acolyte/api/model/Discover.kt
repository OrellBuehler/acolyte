package ch.orellbuehler.acolyte.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DiscoverFresh(
    val kind: String = "",
    val text: String = "",
    val rank: Int = 0,
)

@Serializable
internal data class DiscoverStreaming(
    val service: Long = 0,
    val name: String = "",
    val rank: Int = 0,
)

@Serializable
internal data class DiscoverItem(
    @SerialName("tmdb_id") val tmdbId: String = "",
    val kind: String = "",
    val name: String = "",
    val year: Int = 0,
    val poster: String? = null,
    val backdrop: String? = null,
    val overview: String = "",
    val genres: List<String> = emptyList(),
    @SerialName("in_library") val inLibrary: Boolean = false,
    @SerialName("title_id") val titleId: Long = 0,
    val ask: TitleAsk? = null,
    val fresh: DiscoverFresh? = null,
    @SerialName("trailer_key") val trailerKey: String = "",
    val streaming: DiscoverStreaming? = null,
    @SerialName("card_art") val cardArt: String? = null,
    @SerialName("card_logo") val cardLogo: String? = null,
    @SerialName("art_has_title") val artHasTitle: Boolean = false,
    val rank: Int = 0,
    @SerialName("season_title") val seasonTitle: String = "",
    val what: String = "",
    val following: Boolean = false,
    val upcoming: Boolean = false,
)

@Serializable
internal data class DiscoverRow(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val subtitle: String = "",
    @SerialName("title_layer") val titleLayer: String = "",
    val wide: Boolean = false,
    val items: List<DiscoverItem> = emptyList(),
)

@Serializable
internal data class ExploreHead(
    val type: String = "",
    val building: Boolean = false,
    val seeds: Int = 0,
    @SerialName("tail_cap") val tailCap: Int = 0,
    @SerialName("tail_rows") val tailRows: Int = 0,
    val taste: List<String>? = null,
    val hero: List<DiscoverItem>? = null,
    val head: List<DiscoverRow>? = null,
    @SerialName("source_off") val sourceOff: Boolean = false,
    val note: String = "",
)

@Serializable
internal data class ExploreTail(
    val type: String = "",
    val page: Int = 0,
    val more: Boolean = false,
    val rows: List<DiscoverRow> = emptyList(),
)

@Serializable
internal data class ServiceTop(
    val service: Long = 0,
    val key: String = "",
    val name: String = "",
    val source: String = "",
    @SerialName("fetched_at") val fetchedAt: String = "",
    @SerialName("age_seconds") val ageSeconds: Long = 0,
    val stale: Boolean = false,
    val ranked: Boolean = false,
    val items: List<DiscoverItem> = emptyList(),
)

@Serializable
internal data class ServiceTopAnswer(
    val region: String = "",
    val attribution: String = "",
    val services: List<ServiceTop> = emptyList(),
)

@Serializable
internal data class ExploreTop(
    val chip: String = "",
    val region: String = "",
    val source: String = "",
    val items: List<DiscoverItem> = emptyList(),
)

@Serializable
internal data class ExploreRowAnswer(
    val type: String = "",
    val row: DiscoverRow = DiscoverRow(),
    val building: Boolean = false,
)

@Serializable
data class GenreCard(
    val kind: String = "",
    val id: Int = 0,
    val name: String = "",
    val backdrop: String? = null,
    val from: String = "",
    val both: Boolean = false,
)

@Serializable
internal data class GenreRowAnswer(
    val type: String = "",
    val genres: List<GenreCard>? = null,
)

@Serializable
data class BrowseTile(
    val kind: String = "",
    val genre: Int = 0,
    @SerialName("genre_name") val genreName: String = "",
    val mood: String = "",
    val name: String = "",
    val backdrop: String? = null,
    val from: String = "",
    val both: Boolean = false,
    val category: String = "",
    val section: String = "",
    val count: Int = 0,
)

@Serializable
internal data class ExploreStrip(
    val type: String = "",
    val tiles: List<BrowseTile> = emptyList(),
    val partial: Boolean = false,
)

@Serializable
internal data class ExploreStripCounts(
    val event: String,
    val categories: List<String>,
)

@Serializable
internal data class ExploreCounts(
    @SerialName("row_type") val rowType: String,
    val event: String,
    @SerialName("title_layer") val titleLayer: String? = null,
)

@Serializable
internal data class ExploreGenreInfo(
    val id: Int = 0,
    val name: String = "",
    val kind: String = "",
)

@Serializable
internal data class ExploreGenreAnswer(
    val type: String = "",
    val genre: ExploreGenreInfo = ExploreGenreInfo(),
    val hero: List<DiscoverItem> = emptyList(),
    val rows: List<DiscoverRow> = emptyList(),
    val partial: Boolean = false,
    @SerialName("source_off") val sourceOff: Boolean = false,
)

@Serializable
internal data class OwnedItem(
    val kind: String = "",
    val tmdb: String = "",
    val mbid: String = "",
    val id: Long = 0,
    val state: String = "",
    val percent: Double = 0.0,
    val genres: List<String> = emptyList(),
)

@Serializable
internal data class OwnedList(
    val items: List<OwnedItem> = emptyList(),
)

@Serializable
data class BrowseSection(
    val id: String = "",
    val title: String = "",
)

@Serializable
internal data class BrowseAnswer(
    val type: String = "",
    val tiles: List<BrowseTile> = emptyList(),
    val sections: List<BrowseSection> = emptyList(),
)

@Serializable
internal data class BrowseSectionAnswer(
    val type: String = "",
    val section: String = "",
    val tiles: List<BrowseTile> = emptyList(),
    val building: Boolean = false,
)

@Serializable
internal data class BrowseCategoryAnswer(
    val type: String = "",
    val id: String = "",
    val name: String = "",
    val section: String = "",
    val items: List<DiscoverItem> = emptyList(),
)

@Serializable
internal data class PopularDay(
    val day: String = "",
    val items: List<DiscoverItem> = emptyList(),
)

@Serializable
internal data class PopularAnswer(
    val type: String = "",
    val page: Int = 0,
    val more: Boolean = false,
    val building: Boolean = false,
    @SerialName("services_set") val servicesSet: Boolean = false,
    val rows: List<DiscoverRow> = emptyList(),
    val hero: List<DiscoverItem> = emptyList(),
    val rail: List<PopularDay> = emptyList(),
    @SerialName("source_off") val sourceOff: Boolean = false,
    val note: String = "",
)

@Serializable
internal data class ExploreRowStat(
    @SerialName("row_type") val rowType: String = "",
    val title: String = "",
    val shown: Int = 0,
    val opens: Int = 0,
    val asks: Int = 0,
)

@Serializable
internal data class ExploreRowStats(
    val days: Int = 0,
    val rows: List<ExploreRowStat> = emptyList(),
)

@Serializable
internal data class ExploreLayerStat(
    val layer: String = "",
    val shown: Int = 0,
    val opens: Int = 0,
    val asks: Int = 0,
)

@Serializable
internal data class ExploreTitleStats(
    val days: Int = 0,
    val templates: Int = 0,
    val layers: List<ExploreLayerStat> = emptyList(),
)
