package ch.orellbuehler.acolyte.session

import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.api.model.Me
import ch.orellbuehler.acolyte.api.model.MyProfile
import ch.orellbuehler.acolyte.api.model.PartView
import ch.orellbuehler.acolyte.api.model.Parts
import ch.orellbuehler.acolyte.nav.LibraryTool
import ch.orellbuehler.acolyte.nav.MoreGroup
import ch.orellbuehler.acolyte.nav.MoreTab
import ch.orellbuehler.acolyte.nav.Section
import ch.orellbuehler.acolyte.nav.SectionGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

enum class Role {
    OWNER,
    ADMIN,
    MEMBER,
    UNKNOWN,
}

enum class SignInMethod {
    PASSWORD,
    MEDIA_SERVER,
    API_KEY,
}

@Serializable
internal data class AccountSnapshot(
    val me: Me,
    val parts: Parts,
    val profile: MyProfile? = null,
    val serverVersion: String? = null,
)

data class Account(
    val serverUrl: String,
    val serverName: String,
    val method: SignInMethod,
    val userId: Long,
    val userName: String,
    val displayName: String,
    val role: Role,
    val permissions: List<String>,
    val parts: List<PartView>,
    val serverVersion: String?,
    val avatarPath: String?,
    val mustSetUpTwoFactor: Boolean,
    val isKid: Boolean,
    val safeMode: Boolean,
    val handsOff: Boolean = false,
    val savedViews: Map<String, String> = emptyMap(),
    val twoFactor: Boolean = false,
    val hasPassword: Boolean = true,
    val authMethod: String = "",
    val via: String = "",
    val setupOpen: Boolean = false,
    val setupSkipped: Boolean = false,
    val held: List<String> = emptyList(),
) {
    val aiAppsAvailable: Boolean get() = "ai_apps" !in held

    val usesCookies: Boolean get() = method != SignInMethod.API_KEY

    fun can(need: Need): Boolean =
        when {
            role == Role.OWNER -> true
            need == Need.PUBLIC || need == Need.SIGNED_IN || need == Need.SELF -> true
            need == Need.USERS -> role == Role.ADMIN
            need == Need.OWNER -> false
            else -> need.wire in permissions
        }

    fun isOn(part: Part): Boolean =
        when (part) {
            Part.CORE -> true
            Part.MEDIA, Part.TITLE -> listOf(Part.MOVIES, Part.SHOWS, Part.MUSIC).any { partOn(it) }
            else -> partOn(part)
        }

    fun allows(
        need: Need,
        part: Part,
    ): Boolean = !mustSetUpTwoFactor && can(need) && isOn(part)

    val isManager: Boolean get() = can(Need.MANAGE_LIBRARY) || can(Need.APPROVE) || can(Need.SETTINGS)

    fun shows(section: Section): Boolean {
        if (section == Section.ACCOUNT) return true
        if (mustSetUpTwoFactor || !isOn(section.part)) return false
        return when (section) {
            Section.REQUESTS -> can(Need.ASK) || can(Need.APPROVE)
            Section.MOVIES -> partOn(Part.MOVIES)
            Section.SHOWS -> partOn(Part.SHOWS)
            Section.MUSIC -> partOn(Part.MUSIC) && !isKid
            Section.SETTINGS -> can(Need.SETTINGS) || can(Need.USERS)
            else -> can(section.need)
        }
    }

    val sections: List<Section> get() = Section.entries.filter { shows(it) }

    val tabs: List<Section>
        get() {
            val content = listOf(Section.HOME, Section.EXPLORE, Section.LIBRARY, Section.SEARCH).filter { shows(it) }.toMutableList()
            if (!isManager && shows(Section.REQUESTS)) content += Section.REQUESTS
            if (content.size > MAX_CONTENT_TABS) content.remove(Section.EXPLORE)
            return content
        }

    val moreTab: MoreTab get() = if (isManager) MoreTab.MANAGE else MoreTab.MORE

    val moreGroups: List<MoreGroup>
        get() {
            val inTabs = tabs.toSet()
            val visible = sections.filter { it !in inTabs }
            return SectionGroup.entries
                .map { group -> MoreGroup(group, visible.filter { groupOf(it) == group }) }
                .filter { it.sections.isNotEmpty() }
        }

    fun groupOf(section: Section): SectionGroup =
        if (section == Section.DOWNLOADS && can(Need.MANAGE_LIBRARY)) SectionGroup.ACTIVITY else section.group

    val managesLibrary: Boolean get() = can(Need.MANAGE_LIBRARY) && !isKid && !mustSetUpTwoFactor

    val libraryTools: List<LibraryTool>
        get() =
            if (managesLibrary) {
                LibraryTool.entries.filter {
                    allows(
                        it.need,
                        it.part,
                    ) &&
                        !(it.touchesFiles && handsOff)
                }
            } else {
                emptyList()
            }

    private fun partOn(part: Part): Boolean = parts.firstOrNull { it.part == part.wire }?.on ?: true

    internal companion object {
        const val MAX_CONTENT_TABS = 4

        fun from(
            serverUrl: String,
            method: SignInMethod,
            snapshot: AccountSnapshot,
        ): Account {
            val me = snapshot.me
            return Account(
                serverUrl = serverUrl,
                serverName = ServerAddress.displayName(serverUrl),
                method = method,
                userId = me.id,
                userName = me.name,
                displayName = snapshot.profile?.displayName?.takeIf { it.isNotBlank() } ?: me.name,
                role =
                    when (me.role) {
                        "owner" -> Role.OWNER
                        "admin" -> Role.ADMIN
                        "member" -> Role.MEMBER
                        else -> Role.UNKNOWN
                    },
                permissions = me.perms,
                parts = snapshot.parts.parts,
                serverVersion = snapshot.serverVersion?.takeIf { it.isNotBlank() },
                avatarPath = me.avatar?.takeIf { it.isNotBlank() },
                mustSetUpTwoFactor = me.mustTwoFactor,
                isKid = me.maxRating.isNotBlank(),
                safeMode = me.safeMode || snapshot.parts.safeMode,
                handsOff = me.handsOff,
                twoFactor = me.twoFactor,
                hasPassword = me.hasPassword,
                authMethod = me.authMethod,
                via = me.via,
                setupOpen = me.setupOpen,
                setupSkipped = me.setupSkipped,
                held = me.held,
                savedViews = me.views.mapNotNull { (name, value) -> (value as? JsonPrimitive)?.contentOrNull?.let { name to it } }.toMap(),
            )
        }
    }
}
