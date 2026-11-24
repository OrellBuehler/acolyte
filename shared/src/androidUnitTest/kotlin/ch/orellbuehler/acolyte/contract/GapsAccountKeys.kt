package ch.orellbuehler.acolyte.contract

internal object GapsAccountKeys {
    val problems: Map<String, String> =
        mapOf(
            "PUT /api/v1/settings/ui body.signin_library_art: sent by the model but not in the spec" to
                "UISettings in internal/health/routes_ui_settings.go has six fields; the spec lists only autoplay_trailers",
            "PUT /api/v1/settings/ui body.artwork_cache_gb: sent by the model but not in the spec" to
                "UISettings in internal/health/routes_ui_settings.go has six fields; the spec lists only autoplay_trailers",
            "PUT /api/v1/settings/ui body.history_routine_days: sent by the model but not in the spec" to
                "UISettings in internal/health/routes_ui_settings.go has six fields; the spec lists only autoplay_trailers",
            "PUT /api/v1/settings/ui body.history_other_days: sent by the model but not in the spec" to
                "UISettings in internal/health/routes_ui_settings.go has six fields; the spec lists only autoplay_trailers",
            "PUT /api/v1/settings/ui body.count_install: sent by the model but not in the spec" to
                "UISettings in internal/health/routes_ui_settings.go has six fields; the spec lists only autoplay_trailers",
        )
}
