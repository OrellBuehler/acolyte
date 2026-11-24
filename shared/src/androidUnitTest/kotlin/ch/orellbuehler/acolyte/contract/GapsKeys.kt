package ch.orellbuehler.acolyte.contract

internal object GapsKeys {
    val problems: Map<String, String> =
        mapOf(
            "PUT /api/v1/settings/ai: sends a body but the spec takes none" to
                "putSettings in internal/ai/routes.go decodes {enabled}; the spec has no request body",
            "PUT /api/v1/settings/old-app-api: sends a body but the spec takes none" to
                "putSettings in internal/arrapi/routes_settings.go decodes {enabled, delete_files}; the spec has no request body",
        )
}
