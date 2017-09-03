package io.quartic.eval.model

import io.quartic.eval.api.model.TriggerDetails

fun TriggerDetails.toTriggerReceived() = BuildEvent.TriggerReceived(
    triggerType = this.type,
    deliveryId = this.deliveryId,
    installationId = this.installationId,
    repoId = this.repoId,
    repoFullName = this.repoFullName,
    repoName = this.repoName,
    repoOwner = this.repoOwner,
    cloneUrl = this.cloneUrl,
    ref = this.ref,
    commit = this.commit,
    timestamp = this.timestamp,
    rawWebhook = this.rawWebhook
)

fun BuildEvent.TriggerReceived.toTriggerDetails() = TriggerDetails(
    type = this.triggerType,
    deliveryId = this.deliveryId,
    installationId = this.installationId,
    repoId = this.repoId,
    repoFullName = this.repoFullName,
    repoName = this.repoName,
    repoOwner = this.repoOwner,
    cloneUrl = this.cloneUrl,
    ref = this.ref,
    commit = this.commit,
    timestamp = this.timestamp,
    rawWebhook = this.rawWebhook
)
