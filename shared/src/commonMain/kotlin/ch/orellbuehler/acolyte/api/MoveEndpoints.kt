package ch.orellbuehler.acolyte.api

import ch.orellbuehler.acolyte.api.model.MoveAnswer
import ch.orellbuehler.acolyte.api.model.MoveBringBody
import ch.orellbuehler.acolyte.api.model.MoveBroughtList
import ch.orellbuehler.acolyte.api.model.MoveCheck
import ch.orellbuehler.acolyte.api.model.MoveFindAnswer
import ch.orellbuehler.acolyte.api.model.MoveFindBody
import ch.orellbuehler.acolyte.api.model.MoveInput
import ch.orellbuehler.acolyte.api.model.MoveLinkedAnswer
import ch.orellbuehler.acolyte.api.model.MoveList
import ch.orellbuehler.acolyte.api.model.MoveProbe
import ch.orellbuehler.acolyte.api.model.MoveReadAnswer
import ch.orellbuehler.acolyte.api.model.MoveSavedKeys
import ch.orellbuehler.acolyte.api.model.MoveSnapshotDetail
import ch.orellbuehler.acolyte.api.model.MoveSnapshotList
import ch.orellbuehler.acolyte.api.model.MoveSource
import ch.orellbuehler.acolyte.api.model.MoveStarted

internal data object MoveEndpoints : EndpointGroup() {
    val snapshots = get("/api/v1/migrate/snapshots", MoveSnapshotList.serializer(), Need.USERS)
    val snapshot = get("/api/v1/migrate/snapshots/{id}", MoveSnapshotDetail.serializer(), Need.USERS)
    val forget = delete("/api/v1/migrate/snapshots/{id}", NoContent, Need.USERS)
    val probe = post("/api/v1/migrate/probe", MoveSource.serializer(), MoveProbe.serializer(), Need.USERS)
    val read = post("/api/v1/migrate/read", MoveSource.serializer(), MoveReadAnswer.serializer(), Need.USERS)
    val find = post("/api/v1/migrate/find", MoveFindBody.serializer(), MoveFindAnswer.serializer(), Need.OWNER)
    val linked = post("/api/v1/migrate/linked", MoveSource.serializer(), MoveLinkedAnswer.serializer(), Need.OWNER)
    val plan = post("/api/v1/migrate/plan", MoveInput.serializer(), MoveCheck.serializer(), Need.USERS)
    val dryRun = post("/api/v1/migrate/dry-run", MoveInput.serializer(), MoveCheck.serializer(), Need.USERS)
    val check = get("/api/v1/migrate/checks/{id}", MoveCheck.serializer(), Need.USERS)
    val moves = get("/api/v1/migrate/moves", MoveList.serializer(), Need.USERS)
    val start = post("/api/v1/migrate/moves", MoveInput.serializer(), MoveStarted.serializer(), Need.USERS)
    val move = get("/api/v1/migrate/moves/{id}", MoveAnswer.serializer(), Need.USERS)
    val removeMove = delete("/api/v1/migrate/moves/{id}", NoContent, Need.USERS)
    val report = get("/api/v1/migrate/moves/{id}/report", NoContent, Need.USERS)
    val undo = post("/api/v1/migrate/moves/{id}/undo", MoveStarted.serializer(), Need.USERS)
    val downloaders = post("/api/v1/migrate/downloaders", MoveBringBody.serializer(), MoveBroughtList.serializer(), Need.USERS)
    val savedKeys = get("/api/v1/migrate/saved-keys", MoveSavedKeys.serializer(), Need.OWNER)
}
