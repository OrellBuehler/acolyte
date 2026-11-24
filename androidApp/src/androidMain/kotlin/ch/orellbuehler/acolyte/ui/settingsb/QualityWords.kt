package ch.orellbuehler.acolyte.ui.settingsb

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleProblem
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleUse
import ch.orellbuehler.acolyte.feature.quality.SettingsPickRuleUsePart
import ch.orellbuehler.acolyte.feature.quality.SettingsTraitChoice
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@StringRes
fun qualityStepRes(id: String): Int? =
    when (id) {
        "cam" -> R.string.acq_q_step_cam
        "telesync" -> R.string.acq_q_step_telesync
        "sd_tv" -> R.string.acq_q_step_sd_tv
        "sd_dvd" -> R.string.acq_q_step_sd_dvd
        "720p_tv" -> R.string.acq_q_step_720p_tv
        "720p_web" -> R.string.acq_q_step_720p_web
        "720p_bluray" -> R.string.acq_q_step_720p_bluray
        "1080p_tv" -> R.string.acq_q_step_1080p_tv
        "1080p_web" -> R.string.acq_q_step_1080p_web
        "1080p_bluray" -> R.string.acq_q_step_1080p_bluray
        "1080p_remux" -> R.string.acq_q_step_1080p_remux
        "2160p_web" -> R.string.acq_q_step_2160p_web
        "2160p_bluray" -> R.string.acq_q_step_2160p_bluray
        "2160p_remux" -> R.string.acq_q_step_2160p_remux
        "lossy_low" -> R.string.acq_q_step_lossy_low
        "lossy_mid" -> R.string.acq_q_step_lossy_mid
        "lossy_high" -> R.string.acq_q_step_lossy_high
        "lossless" -> R.string.acq_q_step_lossless
        "lossless_hires" -> R.string.acq_q_step_lossless_hires
        else -> null
    }

@StringRes
fun qualityTraitRes(id: String): Int? =
    when (id) {
        "hevc" -> R.string.acq_q_trait_hevc
        "ddp_audio" -> R.string.acq_q_trait_ddp_audio
        "web_dl" -> R.string.acq_q_trait_web_dl
        "upscaled" -> R.string.acq_q_trait_upscaled
        "dubbed_only" -> R.string.acq_q_trait_dubbed_only
        "hardcoded_subs" -> R.string.acq_q_trait_hardcoded_subs
        "3d" -> R.string.acq_q_trait_3d
        "cam" -> R.string.acq_q_trait_cam
        "telesync" -> R.string.acq_q_trait_telesync
        "remux" -> R.string.acq_q_trait_remux
        "av1" -> R.string.acq_q_trait_av1
        "dv" -> R.string.acq_q_trait_dv
        "hdr10" -> R.string.acq_q_trait_hdr10
        "hdr10plus" -> R.string.acq_q_trait_hdr10plus
        "atmos" -> R.string.acq_q_trait_atmos
        "h264" -> R.string.acq_q_trait_h264
        "dts_audio" -> R.string.acq_q_trait_dts_audio
        "truehd_audio" -> R.string.acq_q_trait_truehd_audio
        "dv_no_fallback" -> R.string.acq_q_trait_dv_no_fallback
        "cd_source" -> R.string.acq_q_trait_cd_source
        "web_source" -> R.string.acq_q_trait_web_source
        "vinyl_source" -> R.string.acq_q_trait_vinyl_source
        "log_cue" -> R.string.acq_q_trait_log_cue
        "hires" -> R.string.acq_q_trait_hires
        "mp3" -> R.string.acq_q_trait_mp3
        "aac" -> R.string.acq_q_trait_aac
        "opus" -> R.string.acq_q_trait_opus
        "vorbis" -> R.string.acq_q_trait_vorbis
        "flac" -> R.string.acq_q_trait_flac
        "alac" -> R.string.acq_q_trait_alac
        "single_image" -> R.string.acq_q_trait_single_image
        "lossy_transcode" -> R.string.acq_q_trait_lossy_transcode
        "deluxe" -> R.string.acq_q_trait_deluxe
        "sample" -> R.string.acq_q_trait_sample
        "executable" -> R.string.acq_q_trait_executable
        "password_protected" -> R.string.acq_q_trait_password_protected
        "disc_image" -> R.string.acq_q_trait_disc_image
        else -> null
    }

@StringRes
fun qualityLanguageRes(code: String): Int? =
    when (code) {
        "en" -> R.string.acq_q_lang_en
        "fr" -> R.string.acq_q_lang_fr
        "de" -> R.string.acq_q_lang_de
        "it" -> R.string.acq_q_lang_it
        "es" -> R.string.acq_q_lang_es
        "ru" -> R.string.acq_q_lang_ru
        "ja" -> R.string.acq_q_lang_ja
        "ko" -> R.string.acq_q_lang_ko
        "pl" -> R.string.acq_q_lang_pl
        "nl" -> R.string.acq_q_lang_nl
        "pt" -> R.string.acq_q_lang_pt
        "sv" -> R.string.acq_q_lang_sv
        "da" -> R.string.acq_q_lang_da
        "no" -> R.string.acq_q_lang_no
        "fi" -> R.string.acq_q_lang_fi
        "cs" -> R.string.acq_q_lang_cs
        "hu" -> R.string.acq_q_lang_hu
        "el" -> R.string.acq_q_lang_el
        "he" -> R.string.acq_q_lang_he
        "tr" -> R.string.acq_q_lang_tr
        "ar" -> R.string.acq_q_lang_ar
        "hi" -> R.string.acq_q_lang_hi
        "zh" -> R.string.acq_q_lang_zh
        "th" -> R.string.acq_q_lang_th
        "vi" -> R.string.acq_q_lang_vi
        "uk" -> R.string.acq_q_lang_uk
        else -> null
    }

@StringRes
fun qualityDeviceRes(id: String): Int? =
    when (id) {
        "apple_tv" -> R.string.acq_q_dev_apple_tv
        "fire_tv" -> R.string.acq_q_dev_fire_tv
        "roku" -> R.string.acq_q_dev_roku
        "chromecast" -> R.string.acq_q_dev_chromecast
        "web_browser" -> R.string.acq_q_dev_web_browser
        else -> null
    }

@StringRes
fun qualityKindRes(kind: String): Int? =
    when (kind) {
        "movie" -> R.string.acq_q_kind_movie
        "show" -> R.string.acq_q_kind_show
        "music" -> R.string.acq_q_kind_music
        else -> null
    }

@StringRes
fun qualityOrderRes(id: String): Int? =
    when (id) {
        "quality_then_points" -> R.string.acq_q_order_quality_then_points
        "points_then_quality" -> R.string.acq_q_order_points_then_quality
        else -> null
    }

@StringRes
fun qualityOrderDescRes(id: String): Int? =
    when (id) {
        "quality_then_points" -> R.string.acq_q_order_quality_then_points_desc
        "points_then_quality" -> R.string.acq_q_order_points_then_quality_desc
        else -> null
    }

@StringRes
fun qualityProperRes(id: String): Int? =
    when (id) {
        "take_any" -> R.string.acq_q_proper_take_any
        "same_group_only" -> R.string.acq_q_proper_same_group_only
        "prefer_only" -> R.string.acq_q_proper_prefer_only
        "ignore" -> R.string.acq_q_proper_ignore
        else -> null
    }

@StringRes
fun qualityProperDescRes(id: String): Int? =
    when (id) {
        "take_any" -> R.string.acq_q_proper_take_any_desc
        "same_group_only" -> R.string.acq_q_proper_same_group_only_desc
        "prefer_only" -> R.string.acq_q_proper_prefer_only_desc
        "ignore" -> R.string.acq_q_proper_ignore_desc
        else -> null
    }

@StringRes
fun qualityPreferRes(id: String): Int? =
    when (id) {
        "either" -> R.string.acq_q_prefer_either
        "torrent" -> R.string.acq_q_prefer_torrent
        "usenet" -> R.string.acq_q_prefer_usenet
        else -> null
    }

@StringRes
fun qualityVerdictRes(verdict: String): Int? =
    when (verdict) {
        "would_choose" -> R.string.acq_q_verdict_would_choose
        "would_hold" -> R.string.acq_q_verdict_would_hold
        "refused" -> R.string.acq_q_verdict_refused
        "passed_over" -> R.string.acq_q_verdict_passed_over
        "turned_down" -> R.string.acq_q_verdict_turned_down
        "judged" -> R.string.acq_q_verdict_judged
        else -> null
    }

@StringRes
fun qualityMatchRes(match: String): Int? =
    when (match) {
        "name" -> R.string.acq_q_ot_match_name
        "group" -> R.string.acq_q_ot_match_group
        "size" -> R.string.acq_q_ot_match_size
        else -> null
    }

@StringRes
fun qualityMatchHintRes(match: String): Int? =
    when (match) {
        "name" -> R.string.acq_q_ot_hint_name
        "group" -> R.string.acq_q_ot_hint_group
        "size" -> R.string.acq_q_ot_hint_size
        else -> null
    }

@StringRes
fun qualitySpecKindRes(kind: String): Int? =
    when (kind) {
        "name" -> R.string.acq_q_ot_kind_name
        "group" -> R.string.acq_q_ot_kind_group
        "source" -> R.string.acq_q_ot_kind_source
        "resolution" -> R.string.acq_q_ot_kind_resolution
        "size" -> R.string.acq_q_ot_kind_size
        "edition" -> R.string.acq_q_ot_kind_edition
        "language" -> R.string.acq_q_ot_kind_language
        "release_type" -> R.string.acq_q_ot_kind_release_type
        "year" -> R.string.acq_q_ot_kind_year
        else -> null
    }

@StringRes
fun qualityProblemRes(code: String): Int? =
    when (code) {
        "rules_name" -> R.string.acq_q_err_rules_name
        "name_200" -> R.string.acq_q_err_name_200
        "name_60" -> R.string.acq_q_err_name_60
        "kinds" -> R.string.acq_q_err_kinds
        "kind_once" -> R.string.acq_q_err_kind_once
        "music_alone" -> R.string.acq_q_err_music_alone
        "steps_other" -> R.string.acq_q_err_steps_other
        "step_once" -> R.string.acq_q_err_step_once
        "step_on" -> R.string.acq_q_err_step_on
        "good_enough" -> R.string.acq_q_err_good_enough
        "trait_once" -> R.string.acq_q_err_trait_once
        "own_removed" -> R.string.acq_q_err_own_removed
        "not_trait" -> R.string.acq_q_err_not_trait
        "points" -> R.string.acq_q_err_points
        "size_step" -> R.string.acq_q_err_size_step
        "size_neg" -> R.string.acq_q_err_size_neg
        "size_order" -> R.string.acq_q_err_size_order
        "size_big" -> R.string.acq_q_err_size_big
        "size_preferred" -> R.string.acq_q_err_size_preferred
        "wait" -> R.string.acq_q_err_wait
        "lang_unknown" -> R.string.acq_q_err_lang_unknown
        "lang_once" -> R.string.acq_q_err_lang_once
        "lang_wait" -> R.string.acq_q_err_lang_wait
        "days" -> R.string.acq_q_err_days
        "trait_name" -> R.string.acq_q_err_trait_name
        "terms_min" -> R.string.acq_q_err_terms_min
        "terms_50" -> R.string.acq_q_err_terms_50
        "term_200" -> R.string.acq_q_err_term_200
        "size_range" -> R.string.acq_q_err_size_range
        "cannot_be_must" -> R.string.acq_q_err_cannot_be_must
        else -> null
    }

fun qualityUsePluralRes(key: String): Int? =
    when (key) {
        "versions" -> R.plurals.acq_q_use_versions
        "library_folders" -> R.plurals.acq_q_use_library_folders
        "outside_lists" -> R.plurals.acq_q_use_outside_lists
        "wishlist" -> R.plurals.acq_q_use_wishlist
        "title_defaults" -> R.plurals.acq_q_use_title_defaults
        else -> null
    }

fun qualityCardFor(field: String): String =
    when (field) {
        "name", "kinds" -> "name"
        "steps", "good_enough_at", "good_enough_points" -> "steps"
        "traits", "order", "upgrade_min_points", "min_points", "proper_policy", "max_age_days" -> "traits"
        "must_have", "never" -> "must"
        "size_limits" -> "size"
        "prefer", "wait_minutes_torrent", "wait_minutes_usenet" -> "waiting"
        "language", "wait_days" -> "language"
        else -> ""
    }

fun qualityJoinWords(
    words: List<String>,
    template: String,
): String =
    when (words.size) {
        0 -> ""
        1 -> words.first()
        else -> template.format(words.dropLast(1).joinToString(", "), words.last())
    }

fun qualityLocalTime(
    iso: String,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String =
    try {
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(zone)
            .format(Instant.parse(iso))
    } catch (e: Exception) {
        iso
    }

fun qualityTrashAppRes(app: String): Int = if (app == "radarr") R.string.acq_q_e_trash_movies else R.string.acq_q_e_trash_shows

@Composable
fun qualityLabel(
    @StringRes res: Int?,
    fallback: String,
): String = if (res != null) stringResource(res) else fallback

@Composable
fun qualityStepLabel(id: String): String = qualityLabel(qualityStepRes(id), id)

@Composable
fun qualityLanguageLabel(code: String): String = qualityLabel(qualityLanguageRes(code), code)

@Composable
fun qualityKindLabel(kind: String): String = qualityLabel(qualityKindRes(kind), kind)

@Composable
fun qualityBuiltInTraitLabel(id: String): String = qualityLabel(qualityTraitRes(id), id)

@Composable
fun qualityTraitLabel(choice: SettingsTraitChoice): String =
    when {
        !choice.own -> qualityBuiltInTraitLabel(choice.id)
        choice.name.isNotEmpty() -> choice.name
        else -> stringResource(R.string.acq_q_e_removed_trait)
    }

@Composable
fun qualityProblemText(problem: SettingsPickRuleProblem): String {
    if (problem.code == "server") return problem.arg
    val res = qualityProblemRes(problem.code) ?: return problem.code
    return when (problem.code) {
        "not_trait", "lang_unknown" -> stringResource(res, problem.arg)
        "cannot_be_must" -> stringResource(res, qualityBuiltInTraitLabel(problem.arg))
        else -> stringResource(res)
    }
}

@Composable
fun qualityUseWords(use: SettingsPickRuleUse): String {
    val words = use.parts.map { qualityUsePart(it) }
    return qualityJoinWords(words, stringResource(R.string.acq_q_and_join))
}

@Composable
private fun qualityUsePart(part: SettingsPickRuleUsePart): String {
    val res = qualityUsePluralRes(part.key) ?: return part.key
    return pluralStringResource(res, part.count, part.count)
}

@Composable
fun qualityUsedBy(use: SettingsPickRuleUse): String = stringResource(R.string.acq_q_used_by, qualityUseWords(use))

@Composable
fun qualityUsedIn(count: Int): String = pluralStringResource(R.plurals.acq_q_used_in, count, count)

@Composable
fun qualityKindsLine(kinds: List<String>): String =
    if (kinds.isEmpty()) {
        stringResource(R.string.acq_q_l_every_kind)
    } else {
        kinds.map { qualityKindLabel(it) }.joinToString(", ")
    }
