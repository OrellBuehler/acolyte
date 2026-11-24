package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.NameCheckAnswer
import ch.orellbuehler.acolyte.api.model.NameCheckBody
import ch.orellbuehler.acolyte.api.model.OwnTrait
import ch.orellbuehler.acolyte.api.model.OwnTraitInput
import ch.orellbuehler.acolyte.api.model.OwnTraitList
import ch.orellbuehler.acolyte.api.model.OwnTraitRemoved
import ch.orellbuehler.acolyte.api.model.PickRuleDetail
import ch.orellbuehler.acolyte.api.model.PickRuleInput
import ch.orellbuehler.acolyte.api.model.TrashApplied
import ch.orellbuehler.acolyte.api.model.TrashApply
import ch.orellbuehler.acolyte.api.model.TrashProfiles

internal data object QualityEndpoints : EndpointGroup() {
    val detail = get("/api/v1/pick-rules/{id}", PickRuleDetail.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val create = post("/api/v1/pick-rules", PickRuleInput.serializer(), PickRuleDetail.serializer(), Need.SETTINGS, Part.MEDIA)
    val update = put("/api/v1/pick-rules/{id}", PickRuleInput.serializer(), PickRuleDetail.serializer(), Need.SETTINGS, Part.MEDIA)
    val remove = delete("/api/v1/pick-rules/{id}", NoContent, Need.SETTINGS, Part.MEDIA)
    val trash = post("/api/v1/pick-rules/{id}/trash", TrashApply.serializer(), TrashApplied.serializer(), Need.SETTINGS, Part.MEDIA)
    val trashProfiles = get("/api/v1/trash/profiles", TrashProfiles.serializer(), Need.SETTINGS, Part.MEDIA, query = listOf("app"))

    val ownTraits = get("/api/v1/own-traits", OwnTraitList.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
    val addTrait = post("/api/v1/own-traits", OwnTraitInput.serializer(), OwnTrait.serializer(), Need.SETTINGS, Part.MEDIA)
    val changeTrait = put("/api/v1/own-traits/{id}", OwnTraitInput.serializer(), OwnTrait.serializer(), Need.SETTINGS, Part.MEDIA)
    val removeTrait = delete("/api/v1/own-traits/{id}", OwnTraitRemoved.serializer(), Need.SETTINGS, Part.MEDIA)

    val nameCheck =
        post("/api/v1/releases/name-check", NameCheckBody.serializer(), NameCheckAnswer.serializer(), Need.MANAGE_LIBRARY, Part.MEDIA)
}
