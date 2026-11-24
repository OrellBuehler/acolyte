package ch.orellbuehler.acolyte.contract

internal object GapsAccountKeys {
    val problems: Map<String, String> =
        mapOf(
            "POST /api/v1/auth/keys body.mcp_groups: sent by the model but not in the spec" to
                "AI keys carry mcp_groups in internal/accounts keys handler; the spec body lags",
            "POST /api/v1/auth/import/{kind} body.people[].confirm_not_kid: sent by the model but not in the spec" to
                "importing a restricted server account as an adult needs confirm_not_kid in internal/accounts import handler",
        )
}
