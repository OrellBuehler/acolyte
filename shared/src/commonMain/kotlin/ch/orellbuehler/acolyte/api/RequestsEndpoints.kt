package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.ApproveBody
import ch.orellbuehler.acolyte.api.model.Ask
import ch.orellbuehler.acolyte.api.model.AskAnswer
import ch.orellbuehler.acolyte.api.model.AskDecision
import ch.orellbuehler.acolyte.api.model.AskLimit
import ch.orellbuehler.acolyte.api.model.AskList
import ch.orellbuehler.acolyte.api.model.AskTitleSeasons
import ch.orellbuehler.acolyte.api.model.AskTurnDownBody
import ch.orellbuehler.acolyte.api.model.AskedBy
import ch.orellbuehler.acolyte.api.model.InboxMessages
import ch.orellbuehler.acolyte.api.model.NewAsk
import ch.orellbuehler.acolyte.api.model.RemoveAsks
import ch.orellbuehler.acolyte.api.model.RemovedAsks
import ch.orellbuehler.acolyte.api.model.StoredParts

internal data object RequestsEndpoints : EndpointGroup() {
    val asks = get("/api/v1/asks", AskList.serializer(), Need.SIGNED_IN, Part.WISHLIST, query = listOf("state", "kind", "limit", "offset"))
    val ask = get("/api/v1/asks/{id}", Ask.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val make = post("/api/v1/asks", NewAsk.serializer(), AskAnswer.serializer(), Need.ASK, Part.WISHLIST)
    val limit = get("/api/v1/asks/limit", AskLimit.serializer(), Need.ASK, Part.WISHLIST)
    val approve = post("/api/v1/asks/{id}/approve", ApproveBody.serializer(), AskDecision.serializer(), Need.APPROVE, Part.WISHLIST)
    val turnDown = post("/api/v1/asks/{id}/turn-down", AskTurnDownBody.serializer(), AskDecision.serializer(), Need.APPROVE, Part.WISHLIST)
    val remove = post("/api/v1/asks/remove", RemoveAsks.serializer(), RemovedAsks.serializer(), Need.APPROVE, Part.WISHLIST)
    val takeBack = delete("/api/v1/asks/{id}", NoContent, Need.ASK, Part.WISHLIST)
    val parts = get("/api/v1/asks/{id}/parts", StoredParts.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val messages = get("/api/v1/messages", InboxMessages.serializer(), Need.SIGNED_IN, Part.WISHLIST)
    val readMessages = post("/api/v1/messages/read", NoContent, Need.SIGNED_IN, Part.WISHLIST)
    val askedBy = get("/api/v1/asked-by", AskedBy.serializer(), Need.SIGNED_IN, Part.WISHLIST, query = listOf("title_id"))
    val titleSeasons = get("/api/v1/titles/{id}/seasons", AskTitleSeasons.serializer(), Need.SIGNED_IN, Part.SHOWS)
}
