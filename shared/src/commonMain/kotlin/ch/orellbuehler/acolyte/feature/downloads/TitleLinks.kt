package ch.orellbuehler.acolyte.feature.downloads

import ch.orellbuehler.acolyte.api.Api
import ch.orellbuehler.acolyte.api.DownloadsLinkEndpoints
import ch.orellbuehler.acolyte.api.model.TitleBrief
import ch.orellbuehler.acolyte.api.valueOrNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal class TitleLinks(
    private val api: Api,
) {
    private val known = mutableMapOf<Long, TitleLink>()

    suspend fun resolve(titleId: Long): TitleLink? {
        if (titleId <= 0) return null
        known[titleId]?.let { return it }
        val title = api.result(DownloadsLinkEndpoints.title, "id" to titleId).valueOrNull() ?: return null
        val kind = title.kind.takeIf { it == "movie" || it == "show" } ?: return null
        val tmdb = (title.ids?.get("tmdb") as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "0" } ?: return null
        return TitleLink(kind, tmdb).also { known[titleId] = it }
    }

    suspend fun briefs(): Map<Long, TitleBrief>? =
        api
            .result(DownloadsLinkEndpoints.titlesBrief, "brief" to 1)
            .valueOrNull()
            ?.titles
            ?.associateBy { it.id }
}
