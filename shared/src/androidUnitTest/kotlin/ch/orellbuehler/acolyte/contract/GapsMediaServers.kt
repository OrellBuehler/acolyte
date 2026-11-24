package ch.orellbuehler.acolyte.contract

internal object GapsMediaServers {
    private const val PLEX_SIGN_IN =
        "Input.UsePlexSignIn in internal/mediaservers/service.go; routes.go plexSignInToken takes the token from the owner's Plex sign-in"

    val problems: Map<String, String> =
        mapOf(
            "POST /api/v1/media-servers body.use_plex_sign_in: sent by the model but not in the spec" to PLEX_SIGN_IN,
            "PUT /api/v1/media-servers/{id} body.use_plex_sign_in: sent by the model but not in the spec" to PLEX_SIGN_IN,
            "PUT /api/v1/settings/watch-history: sends a body but the spec takes none" to
                "watchSetting {enabled} in internal/mediaservers/routes_watch.go; the spec lists no request body",
            "PUT /api/v1/settings/plex-labels: sends a body but the spec takes none" to
                "labelInput {enabled, label, askers} in internal/mediaservers/routes_labels.go; the spec lists no request body",
            "PUT /api/v1/settings/default-tracks: sends a body but the spec takes none" to
                "trackInput {enabled, audio, subtitles} in internal/mediaservers/routes_tracks.go; the spec lists no request body",
        )

    val liveSkips: Map<String, String> =
        mapOf(
            "GET /api/v1/auth/plex/servers" to
                "session-only and owner-only: answers 409 until a Plex account is linked and 403 to an API key",
        )
}
