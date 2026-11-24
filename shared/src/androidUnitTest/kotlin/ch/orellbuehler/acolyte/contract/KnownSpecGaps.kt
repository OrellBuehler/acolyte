package ch.orellbuehler.acolyte.contract

internal object KnownSpecGaps {
    private const val OUTSIDE_LIST = "Go OutsideListInput (outside_lists.go) takes title_kind, seasons and new_seasons; ListInput lags"
    private const val ASK_SETTINGS = "Go requests.Settings has 25 fields; the spec's AskSettings lists 12"
    private const val NEW_ASK = "Go makeAsk body (requests/routes_asks.go) takes follow, quality, four_k and editions; the spec body lags"

    val problems: Map<String, String> =
        mapOf(
            "GET /api/v1/titles: query parameter 'grid' is not in the spec (it has [q])" to
                "presence flag read with Query().Has(\"grid\") in cmd/holyarrs/wire_titles.go; the Library grid shape",
            "GET /api/v1/titles: query parameter 'brief' is not in the spec (it has [q])" to
                "presence flag in cmd/holyarrs/titles_brief.go; ids and posters only",
            "PUT /api/v1/titles/{id}/versions/{vid} body.audio_languages: sent by the model but not in the spec" to
                "VersionChange.AudioLanguages in internal/mediafiles/routes_versions.go",
            "GET /api/v1/holding: query parameter 'title_id' is not in the spec (it has [])" to
                "read by internal/mediafiles/routes_holding.go to list one title",
            "GET /api/v1/holding: query parameter 'count' is not in the spec (it has [])" to
                "routes_holding.go answers {count} only when count=1",
            "POST /api/v1/holding/restore body.how: sent by the model but not in the spec" to
                "swap/both choice of internal/library/holding_restore.go",
            "GET /api/v1/library/watched-cleanup: query parameter 'days' is not in the spec (it has [])" to
                "internal/mediaservers/watched_cleanup.go reads days",
            "GET /api/v1/library/watched-cleanup: query parameter 'people' is not in the spec (it has [])" to
                "internal/mediaservers/watched_cleanup.go reads the picked people",
            "POST /api/v1/library-folders body.new_title_tags: sent by the model but not in the spec" to
                "FolderInput in internal/library/folders.go",
            "POST /api/v1/library-folders body.only_tagged_sources: sent by the model but not in the spec" to
                "FolderInput in internal/library/folders.go",
            "POST /api/v1/library-folders body.new_title_audio: sent by the model but not in the spec" to
                "FolderInput in internal/library/folders.go",
            "POST /api/v1/library-folders body.look_only: sent by the model but not in the spec" to
                "FolderInput in internal/library/folders.go",
            "POST /api/v1/library-folders body.flat: sent by the model but not in the spec" to
                "FolderInput in internal/library/folders.go",
            "PUT /api/v1/library-folders/{id} body.copies_expected: sent by the model but not in the spec" to
                "FolderChange in internal/library/folders.go",
            "PUT /api/v1/library-folders/{id} body.new_title_tags: sent by the model but not in the spec" to
                "FolderChange in internal/library/folders.go",
            "PUT /api/v1/library-folders/{id} body.only_tagged_sources: sent by the model but not in the spec" to
                "FolderChange in internal/library/folders.go",
            "PUT /api/v1/library-folders/{id} body.new_title_audio: sent by the model but not in the spec" to
                "FolderChange in internal/library/folders.go",
            "PUT /api/v1/library-folders/{id} body.look_only: sent by the model but not in the spec" to
                "FolderChange in internal/library/folders.go",
            "PUT /api/v1/library-folders/{id} body.flat: sent by the model but not in the spec" to
                "FolderChange in internal/library/folders.go",
            "PUT /api/v1/settings/import-folders body.watch_folders: sent by the model but not in the spec" to
                "watch folders of internal/importer/byhand_places.go",
            "PUT /api/v1/settings/hands-off: sends a body but the spec takes none" to
                "the handler requires {on} and answers 400 without it",
            "POST /api/v1/library-folders/{id}/adopt body.follow: sent by the model but not in the spec" to
                "adopt request follow flag, default true",
            "POST /api/v1/manual-import body.items[].replace: sent by the model but not in the spec" to
                "ByHandItem.Replace in internal/importer/manual.go",
            "GET /api/v1/library/search: query parameter 'q' is not in the spec (it has [])" to
                "internal/discover/routes_library_search.go reads q",
            "GET /api/v1/library/search: query parameter 'kind' is not in the spec (it has [])" to
                "internal/discover/routes_library_search.go reads kind",
            "PUT /api/v1/titles/{id} body.pack_window: sent by the model but not in the spec" to
                "TitleChange.PackWindow exists in library/edit.go; the web sends it for season packs",
            "GET /api/v1/titles/{id}/episodes: query parameter 'files' is not in the spec (it has [season])" to
                "shows/routes_episodes.go reads files=1 to add each slot's file",
            "GET /api/v1/titles/{id}/episodes: query parameter 'points' is not in the spec (it has [season])" to
                "shows/routes_episodes.go reads points=1 to add points and good_enough_points",
            "GET /api/v1/titles/{id}/points: query parameter 'slot' is not in the spec (it has [])" to
                "the handler answers 400 without slot; the spec lists no parameter",
            "POST /api/v1/releases/{id}/turn-down body.block_group: sent by the model but not in the spec" to
                "sources/routes_releases.go reads block_group (Block group button)",
            "POST /api/v1/releases/check: sends a body but the spec takes none" to
                "sources/routes_release_check.go decodes {tmdb, name, pick_rules_id, ...}; the spec has no request body",
            "POST /api/v1/releases/check/{id}/pick: sends a body but the spec takes none" to
                "the handler decodes {release_id, title_id, force}; the spec has no request body",
            "POST /api/v1/titles body.follow: sent by the model but not in the spec" to
                "discover/title_routes.go reads follow (default true)",
            "POST /api/v1/shows body.follow: sent by the model but not in the spec" to
                "shows/routes_add.go reads follow (default true)",
            "POST /api/v1/titles/{id}/leftovers/match: sends a body but the spec takes none" to
                "library/routes_leftovers.go decodes {items, replace}; the spec has no request body",
            "POST /api/v1/titles/{id}/leftovers/unmatch: sends a body but the spec takes none" to
                "library/routes_leftovers.go decodes {file_ids}; the spec has no request body",
            "PUT /api/v1/titles/{id}/leftovers/{fid}: sends a body but the spec takes none" to
                "library/routes_leftovers.go decodes {dismissed}; the spec has no request body",
            "POST /api/v1/asks body.follow: sent by the model but not in the spec" to
                "requests/routes_asks.go reads follow (default true)",
            "POST /api/v1/outside-lists body.title_kind: sent by the model but not in the spec" to OUTSIDE_LIST,
            "POST /api/v1/outside-lists body.seasons: sent by the model but not in the spec" to OUTSIDE_LIST,
            "POST /api/v1/outside-lists body.new_seasons: sent by the model but not in the spec" to OUTSIDE_LIST,
            "PUT /api/v1/outside-lists/{id} body.title_kind: sent by the model but not in the spec" to OUTSIDE_LIST,
            "PUT /api/v1/outside-lists/{id} body.seasons: sent by the model but not in the spec" to OUTSIDE_LIST,
            "PUT /api/v1/outside-lists/{id} body.new_seasons: sent by the model but not in the spec" to OUTSIDE_LIST,
            "POST /api/v1/never-add body.kind: sent by the model but not in the spec" to
                "the handler takes kind movie (default) or show (its 400 text says so); the spec body lacks it",
            "PUT /api/v1/settings/asks body.four_k_library_folder_id: sent by the model but not in the spec" to ASK_SETTINGS,
            "PUT /api/v1/settings/asks body.four_k_pick_rules_id: sent by the model but not in the spec" to ASK_SETTINGS,
            "PUT /api/v1/settings/asks body.show_four_k_library_folder_id: sent by the model but not in the spec" to ASK_SETTINGS,
            "PUT /api/v1/settings/asks body.show_four_k_pick_rules_id: sent by the model but not in the spec" to ASK_SETTINGS,
            "PUT /api/v1/settings/asks body.plex_watchlists_everyone: sent by the model but not in the spec" to ASK_SETTINGS,
            "PUT /api/v1/settings/asks body.owner_auto_approve: sent by the model but not in the spec" to ASK_SETTINGS,
            "PUT /api/v1/settings/asks body.qualities: sent by the model but not in the spec" to ASK_SETTINGS,
            "PUT /api/v1/settings/asks body.pick_many: sent by the model but not in the spec" to ASK_SETTINGS,
            "PUT /api/v1/settings/asks body.streaming_region: sent by the model but not in the spec" to ASK_SETTINGS,
            "PUT /api/v1/settings/asks body.streaming_services: sent by the model but not in the spec" to ASK_SETTINGS,
            "PUT /api/v1/settings/asks body.skip_on_services: sent by the model but not in the spec" to ASK_SETTINGS,
            "GET /api/v1/watch-services: query parameter 'region' is not in the spec (it has [])" to
                "the handler needs region=<country code> (400 without it); the spec lists no parameters",
            "GET /api/v1/asks: query parameter 'offset' is not in the spec (it has [mine, state, limit, kind])" to
                "listAsks reads ?offset= to page past the first 200 (requests/routes_asks.go)",
            "POST /api/v1/asks body.editions: sent by the model but not in the spec" to NEW_ASK,
            "POST /api/v1/asks body.quality: sent by the model but not in the spec" to NEW_ASK,
            "POST /api/v1/asks/{id}/approve body.quality: sent by the model but not in the spec" to
                "Go ApproveOptions (requests/approve.go) takes quality to approve as another line; the spec body lags",
        )

    val liveSkips: Map<String, String> =
        mapOf(
            "GET /api/v1/watch-services" to
                "needs ?region=, which the spec does not list as a parameter (the handler answers 400 without it)",
        )
}
