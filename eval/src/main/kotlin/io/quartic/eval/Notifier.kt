package io.quartic.eval

import io.quartic.eval.api.model.TriggerDetails
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.StatusCreate
import io.quartic.hey.api.*
import io.quartic.registry.api.model.Customer
import java.net.URI
import java.time.Clock
import java.time.ZoneOffset

class Notifier(
    private val client: HeyClient,
    private val github: GitHubInstallationClient,
    private val homeUrlFormat: String,
    private val clock: Clock = Clock.systemUTC()
) {

    fun notifyStart(trigger: TriggerDetails) {
        sendGithubStatus(
            trigger = trigger,
            state = "pending",
            targerUrl = null,
            description = "Quartic is validating your pipeline"
        )
    }

    // TODO - this is currently triggered by build completion, so we lose the nuance of internal vs user errors
    // on phases.  We could add this back in by taking the message from the last failing phase.
    fun notifyComplete(trigger: TriggerDetails, customer: Customer, buildNumber: Long, success: Boolean) {
        val buildUri = URI.create("${homeUrlFormat.format(customer.subdomain)}/#/pipeline/${buildNumber}")
        client.notifyAsync(HeyNotification(listOf(
            HeyAttachment(
                title = "Build #${buildNumber} ${if (success) "succeeded" else "failed"}",
                titleLink = buildUri,
                text = if (success) "Success" else "Failure",
                fields = listOf(
                    HeyField("Repo", trigger.repoFullName, true),
                    HeyField("Branch", trigger.branch(), true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = if (success) HeyColor.GOOD else HeyColor.DANGER
            )
        )))

        sendGithubStatus(
            trigger = trigger,
            state = if (success) "success" else "failure",
            targerUrl = buildUri,
            description = if (success) "Quartic successfully validated your pipeline"
                else "There are some problems with your pipeline"
        )
    }

    private fun sendGithubStatus(trigger: TriggerDetails, state: String, targerUrl: URI?, description: String) =
        github.sendStatusAsync(
            trigger.repoOwner,
            trigger.repoName,
            trigger.commit,
            StatusCreate(state, targerUrl, description, "quartic"),
            github.accessTokenAsync(trigger.installationId).get()
        )
}
