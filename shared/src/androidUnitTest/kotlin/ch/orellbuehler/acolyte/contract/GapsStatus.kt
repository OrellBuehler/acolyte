package ch.orellbuehler.acolyte.contract

internal object GapsStatus {
    private const val HEALTH_SETTINGS = "health.Settings (internal/health/settings.go) has update_check_*; the spec HealthSettings lists 4"

    val problems: Map<String, String> =
        mapOf(
            "PUT /api/v1/app/update/mode: sends a body but the spec takes none" to
                "internal/updates/install_routes.go setMode decodes {mode, check_on}; the spec has no request body",
            "PUT /api/v1/settings/health body.update_check_local: sent by the model but not in the spec" to HEALTH_SETTINGS,
            "PUT /api/v1/settings/health body.update_prerelease: sent by the model but not in the spec" to HEALTH_SETTINGS,
            "PUT /api/v1/settings/health body.update_check_every_days: sent by the model but not in the spec" to HEALTH_SETTINGS,
        )
}
