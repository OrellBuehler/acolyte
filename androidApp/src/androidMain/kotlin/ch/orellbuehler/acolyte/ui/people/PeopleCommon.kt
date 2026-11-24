package ch.orellbuehler.acolyte.ui.people

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.orellbuehler.acolyte.R
import ch.orellbuehler.acolyte.api.model.TitleUser
import ch.orellbuehler.acolyte.feature.account.PeoplePreset

@Composable
fun permLabel(perm: String): String =
    stringResource(
        when (perm) {
            "ask" -> R.string.people_perm_ask
            "auto_approve" -> R.string.people_perm_auto
            "auto_approve_four_k" -> R.string.people_perm_auto_4k
            "approve" -> R.string.people_perm_approve
            "manage_library" -> R.string.people_perm_library
            else -> R.string.people_perm_settings
        },
    )

@Composable
fun presetLabel(preset: PeoplePreset): String =
    stringResource(
        when (preset) {
            PeoplePreset.ADMIN -> R.string.people_preset_admin
            PeoplePreset.MANAGER -> R.string.people_preset_manager
            PeoplePreset.MEMBER -> R.string.people_preset_member
            PeoplePreset.KID -> R.string.people_preset_kid
            PeoplePreset.CUSTOM -> R.string.people_preset_custom
        },
    )

@Composable
fun roleLabel(user: TitleUser): String =
    stringResource(
        when (user.role) {
            "owner" -> R.string.role_owner
            "admin" -> R.string.role_admin
            else -> R.string.role_member
        },
    )

@Composable
fun permissionsSummary(user: TitleUser): String =
    when {
        user.role == "owner" -> stringResource(R.string.people_can_everything)
        user.perms.isEmpty() -> stringResource(R.string.people_can_nothing)
        else -> user.perms.map { permLabel(it) }.joinToString(", ")
    }
