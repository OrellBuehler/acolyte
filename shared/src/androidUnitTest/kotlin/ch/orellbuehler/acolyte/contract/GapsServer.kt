package ch.orellbuehler.acolyte.contract

internal object GapsServer {
    val problems: Map<String, String> =
        mapOf(
            "POST /api/v1/backups/restore: the spec requires a request body" to
                "the body is the raw zip streamed from a file by Api.upload as application/octet-stream, not a JSON model",
        )
}
