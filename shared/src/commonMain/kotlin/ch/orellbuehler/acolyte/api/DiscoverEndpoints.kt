package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.BrowseAnswer
import ch.orellbuehler.acolyte.api.model.BrowseCategoryAnswer
import ch.orellbuehler.acolyte.api.model.BrowseSectionAnswer
import ch.orellbuehler.acolyte.api.model.ExploreCounts
import ch.orellbuehler.acolyte.api.model.ExploreGenreAnswer
import ch.orellbuehler.acolyte.api.model.ExploreHead
import ch.orellbuehler.acolyte.api.model.ExploreRowAnswer
import ch.orellbuehler.acolyte.api.model.ExploreRowStats
import ch.orellbuehler.acolyte.api.model.ExploreStrip
import ch.orellbuehler.acolyte.api.model.ExploreStripCounts
import ch.orellbuehler.acolyte.api.model.ExploreTail
import ch.orellbuehler.acolyte.api.model.ExploreTitleStats
import ch.orellbuehler.acolyte.api.model.ExploreTop
import ch.orellbuehler.acolyte.api.model.FilmographyPage
import ch.orellbuehler.acolyte.api.model.GenreRowAnswer
import ch.orellbuehler.acolyte.api.model.HiddenList
import ch.orellbuehler.acolyte.api.model.OwnedList
import ch.orellbuehler.acolyte.api.model.PeoplePage
import ch.orellbuehler.acolyte.api.model.PeopleShelf
import ch.orellbuehler.acolyte.api.model.PeopleShelves
import ch.orellbuehler.acolyte.api.model.PlainAnswer
import ch.orellbuehler.acolyte.api.model.PopularAnswer
import ch.orellbuehler.acolyte.api.model.SeriesDetail
import ch.orellbuehler.acolyte.api.model.SeriesRemoveRequest
import ch.orellbuehler.acolyte.api.model.SeriesReview
import ch.orellbuehler.acolyte.api.model.ServiceTopAnswer
import ch.orellbuehler.acolyte.api.model.SuggestAnswer

internal data object DiscoverEndpoints : EndpointGroup() {
    private val region = listOf("type", "tz", "services", "region")

    val head = get("/api/v1/explore/head", ExploreHead.serializer(), Need.SIGNED_IN, Part.MEDIA, query = region)
    val tail = get("/api/v1/explore/tail", ExploreTail.serializer(), Need.SIGNED_IN, Part.MEDIA, query = region + "page")
    val serviceTop =
        get("/api/v1/explore/service-top", ServiceTopAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("type", "region"))
    val top =
        get(
            "/api/v1/explore/top",
            ExploreTop.serializer(),
            Need.SIGNED_IN,
            Part.MEDIA,
            query = listOf("type", "chip", "region", "services", "genre"),
        )
    val newServices = get("/api/v1/explore/new/services", ExploreRowAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("type"))
    val genreRow = get("/api/v1/explore/genre-row", GenreRowAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("type"))
    val strip = get("/api/v1/explore/strip", ExploreStrip.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("type", "row", "tz"))
    val stripCounts = post("/api/v1/explore/strip/counts", ExploreStripCounts.serializer(), NoContent, Need.SIGNED_IN, Part.MEDIA)
    val resetStripCounts = delete("/api/v1/explore/strip/counts", NoContent, Need.SIGNED_IN, Part.MEDIA)
    val counts = post("/api/v1/explore/counts", ExploreCounts.serializer(), NoContent, Need.SIGNED_IN, Part.MEDIA)
    val genre = get("/api/v1/explore/genre", ExploreGenreAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("type", "genre"))
    val owned = get("/api/v1/titles/owned", OwnedList.serializer(), Need.SIGNED_IN, Part.MEDIA)
    val browse = get("/api/v1/explore/browse", BrowseAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("type"))
    val browseSection =
        get(
            "/api/v1/explore/browse/section",
            BrowseSectionAnswer.serializer(),
            Need.SIGNED_IN,
            Part.MEDIA,
            query = listOf("type", "section"),
        )
    val browseCategory =
        get("/api/v1/explore/browse/category", BrowseCategoryAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("type", "id"))
    val popular = get("/api/v1/explore/new", PopularAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("type", "page"))
    val popularService =
        get("/api/v1/explore/new/service", ExploreRowAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("id", "type", "row"))
    val rowStats = get("/api/v1/explore/row-stats", ExploreRowStats.serializer(), Need.USERS, Part.MEDIA)
    val titleStats = get("/api/v1/explore/title-stats", ExploreTitleStats.serializer(), Need.OWNER, Part.MEDIA)

    val suggest = get("/api/v1/search/suggest", SuggestAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("q", "kind"))
    val plainSearch = get("/api/v1/search", PlainAnswer.serializer(), Need.SIGNED_IN, Part.MEDIA, query = listOf("q"))

    val peopleSearch = get("/api/v1/people/search", PeoplePage.serializer(), Need.SIGNED_IN, Part.MOVIES, query = listOf("q", "page"))
    val peoplePopular = get("/api/v1/people/popular", PeoplePage.serializer(), Need.SIGNED_IN, Part.MOVIES, query = listOf("page"))
    val peopleShelves = get("/api/v1/people/shelves", PeopleShelves.serializer(), Need.SIGNED_IN, Part.MOVIES)
    val peopleShelf = get("/api/v1/people/shelves/{id}", PeopleShelf.serializer(), Need.SIGNED_IN, Part.MOVIES)
    val person =
        get(
            "/api/v1/people/{id}",
            FilmographyPage.serializer(),
            Need.SIGNED_IN,
            Part.MOVIES,
            query = listOf("role", "sort", "page", "page_size"),
        )

    val series = get("/api/v1/collections/{id}", SeriesDetail.serializer(), Need.SIGNED_IN, Part.MOVIES)
    val seriesPreview =
        post(
            "/api/v1/collections/{id}/remove/preview",
            SeriesRemoveRequest.serializer(),
            SeriesReview.serializer(),
            Need.MANAGE_LIBRARY,
            Part.MOVIES,
        )
    val seriesRemove =
        post(
            "/api/v1/collections/{id}/remove",
            SeriesRemoveRequest.serializer(),
            SeriesReview.serializer(),
            Need.MANAGE_LIBRARY,
            Part.MOVIES,
        )

    val hidden = get("/api/v1/hidden", HiddenList.serializer(), Need.USERS, Part.MEDIA, query = listOf("user_id"))
}
