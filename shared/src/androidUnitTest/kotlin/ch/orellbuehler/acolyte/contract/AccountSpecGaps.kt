package ch.orellbuehler.acolyte.contract

internal object AccountSpecGaps {
    val problems: Map<String, String> =
        mapOf(
            "PUT /api/v1/my/alerts body.new_episodes: sent by the model but not in the spec" to
                "requests.Targets in internal/requests/settings.go has new_episodes; the spec's AlertTargets lags",
            "PUT /api/v1/account/avatar: sends a body but the spec takes none" to
                "the body is the raw picture (JPEG, PNG or WebP, up to 5 MB); accounts/avatars.go reads it",
            "POST /api/v1/auth/plex/my-link/start: sends a body but the spec takes none" to "the web client sends {}",
            "POST /api/v1/auth/media-link/{server}: sends a body but the spec takes none" to
                "accounts/media_link.go decodes {name, password}",
            "POST /api/v1/auth/ai-connect: sends a body but the spec takes none" to "accounts/ai_signin.go decodes {query}",
            "PUT /api/v1/users/{id}/media-link: sends a body but the spec takes none" to
                "accounts/media_link.go decodes {server, account}",
            "POST /api/v1/auth/owner-reset/code: sends a body but the spec takes none" to "the web client sends {}",
            "POST /api/v1/auth/setup/link/{kind}: sends a body but the spec takes none" to
                "accounts/setup_link.go decodes {url, name, password}",
            "POST /api/v1/users/invites body.max_uses: sent by the model but not in the spec" to
                "accounts/invites.go takes max_uses (1 to 1000, 0 for no limit); the spec lags",
            "POST /api/v1/auth/keys body.mcp_groups: sent by the model but not in the spec" to
                "accounts/keys.go takes mcp_groups for AI keys; the spec lags",
            "POST /api/v1/auth/import/{kind} body.people[].confirm_not_kid: sent by the model but not in the spec" to
                "accounts/import.go takes confirm_not_kid for restricted server accounts",
            "POST /api/v1/auth/setup body.display_name: sent by the model but not in the spec" to
                "accounts/setup_owner.go takes display_name",
            "POST /api/v1/auth/setup/done: query parameter 'skipped' is not in the spec (it has [])" to
                "accounts/setup_done.go reads skipped=1 for Skip setup for now",
        ) +
            listOf("response_type", "client_id", "redirect_uri", "code_challenge", "code_challenge_method", "state", "resource", "scope")
                .associate {
                    "GET /api/v1/auth/ai-connect: query parameter '$it' is not in the spec (it has [])" to
                        "the authorize query of the AI app is sent verbatim; accounts/ai_signin.go validates it"
                }
}
