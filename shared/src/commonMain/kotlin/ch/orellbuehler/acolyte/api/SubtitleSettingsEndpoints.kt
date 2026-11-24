package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.SettingsSubtitleCleanQueued
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleConfig
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleKindList
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleSite
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleSiteInput
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleSiteList
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleSiteTest
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleTranslatorInput
import ch.orellbuehler.acolyte.api.model.SettingsSubtitleTranslatorTest
import ch.orellbuehler.acolyte.api.model.SubtitleTranslator

internal data object SubtitleSettingsEndpoints : EndpointGroup() {
    val settings = get("/api/v1/settings/subtitles", SettingsSubtitleConfig.serializer(), Need.SETTINGS, Part.SUBTITLES)
    val save =
        put(
            "/api/v1/settings/subtitles",
            SettingsSubtitleConfig.serializer(),
            SettingsSubtitleConfig.serializer(),
            Need.SETTINGS,
            Part.SUBTITLES,
        )
    val saveTranslator =
        put(
            "/api/v1/settings/subtitles/translator",
            SettingsSubtitleTranslatorInput.serializer(),
            SubtitleTranslator.serializer(),
            Need.SETTINGS,
            Part.SUBTITLES,
        )
    val testTranslator =
        post("/api/v1/settings/subtitles/translator/test", SettingsSubtitleTranslatorTest.serializer(), Need.SETTINGS, Part.SUBTITLES)
    val sites = get("/api/v1/subtitle-sites", SettingsSubtitleSiteList.serializer(), Need.MANAGE_LIBRARY, Part.SUBTITLES)
    val kinds = get("/api/v1/subtitle-sites/kinds", SettingsSubtitleKindList.serializer(), Need.MANAGE_LIBRARY, Part.SUBTITLES)
    val addSite =
        post(
            "/api/v1/subtitle-sites",
            SettingsSubtitleSiteInput.serializer(),
            SettingsSubtitleSite.serializer(),
            Need.SETTINGS,
            Part.SUBTITLES,
        )
    val updateSite =
        put(
            "/api/v1/subtitle-sites/{id}",
            SettingsSubtitleSiteInput.serializer(),
            SettingsSubtitleSite.serializer(),
            Need.SETTINGS,
            Part.SUBTITLES,
        )
    val removeSite = delete("/api/v1/subtitle-sites/{id}", NoContent, Need.SETTINGS, Part.SUBTITLES)
    val testSite = post("/api/v1/subtitle-sites/{id}/test", SettingsSubtitleSiteTest.serializer(), Need.SETTINGS, Part.SUBTITLES)
    val clean = post("/api/v1/subtitles/clean", SettingsSubtitleCleanQueued.serializer(), Need.MANAGE_LIBRARY, Part.SUBTITLES)
}
