package ch.orellbuehler.acolyte.api

internal data object ArtworkEndpoints : EndpointGroup() {
    val tmdb = get("/api/v1/artwork/tmdb/{size}/{file}", NoContent, Need.SIGNED_IN, Part.MEDIA)
    val stored = get("/api/v1/artwork/{name}", NoContent, Need.SIGNED_IN, Part.MEDIA)
    val avatar = get("/api/v1/users/{id}/avatar", NoContent, Need.SIGNED_IN)
}
