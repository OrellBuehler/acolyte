package ch.orellbuehler.acolyte.feature.acquisition

import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.Part
import ch.orellbuehler.acolyte.session.Account

enum class AcquisitionPage(
    internal val need: Need,
    internal val part: Part,
) {
    SOURCES(Need.SETTINGS, Part.CORE),
    DOWNLOADERS(Need.SETTINGS, Part.CORE),
    PICK_RULES(Need.MANAGE_LIBRARY, Part.MEDIA),
    NAMING(Need.SETTINGS, Part.MEDIA),
    SUBTITLES(Need.SETTINGS, Part.SUBTITLES),
    SEARCH(Need.SETTINGS, Part.MEDIA),
    MOVE(Need.USERS, Part.CORE),
}

fun Account.mayOpen(page: AcquisitionPage): Boolean =
    when (page) {
        AcquisitionPage.PICK_RULES -> allows(Need.SETTINGS, page.part) || allows(Need.MANAGE_LIBRARY, page.part)
        else -> allows(page.need, page.part)
    }

fun Account.acquisitionPages(): List<AcquisitionPage> = AcquisitionPage.entries.filter { mayOpen(it) }

val Account.mayEditAcquisition: Boolean get() = can(Need.SETTINGS)
