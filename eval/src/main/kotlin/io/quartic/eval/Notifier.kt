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
    sealed class Event {
        abstract val message: String
        data class Success(override val message: String) : Event()
        data class Failure(override val message: String) : Event()
    }

    fun notifyStart(trigger: TriggerDetails) {
        sendGithubStatus(
            trigger = trigger,
            state = "pending",
            targetUrl = null,
            description = START_MESSAGE
        )
    }

    fun notifyComplete(
        trigger: TriggerDetails,
        customer: Customer,
        buildNumber: Long,
        event: Event) {
        val buildUri = URI.create("${homeUrlFormat.format(customer.subdomain)}/#/pipeline/${buildNumber}")
        client.notifyAsync(HeyNotification(listOf(
            HeyAttachment(
                title = when (event) {
                    is Event.Success -> "Build #${buildNumber} succeeded"
                    is Event.Failure -> "Build #${buildNumber} failed"
                },
                titleLink = buildUri,
                text = event.message,
                fields = listOf(
                    HeyField("Repo", trigger.repoFullName, true),
                    HeyField("Branch", trigger.branch(), true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = when (event) {
                    is Event.Success -> HeyColor.GOOD
                    is Event.Failure -> HeyColor.DANGER
                }
            )
        )))

        sendGithubStatus(
            trigger = trigger,
            state = when (event) {
                is Event.Success -> "success"
                is Event.Failure -> "failure"
            },
            targetUrl = buildUri,
            description = when (event) {
                is Event.Success -> SUCCESS_MESSAGE
                is Event.Failure -> FAILURE_MESSAGE
            }
        )
    }

    private fun sendGithubStatus(trigger: TriggerDetails, state: String, targetUrl: URI?, description: String) =
        github.sendStatusAsync(
            trigger.repoOwner,
            trigger.repoName,
            trigger.commit,
            StatusCreate(state, targetUrl, description, "quartic"),
            github.accessTokenAsync(trigger.installationId).get()
        )

    companion object {
        internal val START_MESSAGE = "Quartic is validating your pipeline"
        internal val SUCCESS_MESSAGE = "Quartic successfully validated your pipeline"
        internal val FAILURE_MESSAGE = "Quartic found some problems with your pipeline"
    }
}
