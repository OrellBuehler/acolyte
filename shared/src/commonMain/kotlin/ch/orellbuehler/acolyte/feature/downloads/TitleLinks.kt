package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.LibraryEndpoints
import ch.orellbuehler.acolyte.api.model.GridTitle
import ch.orellbuehler.acolyte.api.valueOrNull

internal class TitleLinks(
    private val api: Api,
) {
    private val known = mutableMapOf<Long, DownloadTitleLink>()

    suspend fun resolve(titleId: Long): DownloadTitleLink? {
        if (titleId <= 0) return null
        known[titleId]?.let { return it }
        val title = api.result(LibraryEndpoints.titleVersions, "id" to titleId).valueOrNull() ?: return null
        val kind = title.kind.takeIf { it == "movie" || it == "show" } ?: return null
        val tmdb = title.ids["tmdb"]?.takeIf { it.isNotBlank() && it != "0" } ?: return null
        return DownloadTitleLink(kind, tmdb, titleId).also { known[titleId] = it }
    }

    suspend fun briefs(): Map<Long, GridTitle>? =
        api
            .result(LibraryEndpoints.titles, "brief" to 1)
            .valueOrNull()
            ?.titles
            ?.associateBy { it.id }
}
