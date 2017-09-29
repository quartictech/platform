package io.quartic.eval

import com.google.common.base.Throwables.getRootCause
import io.quartic.common.logging.logger
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.api.model.BuildTrigger.GithubWebhook
import io.quartic.eval.api.model.BuildTrigger.Manual
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
    private val LOG by logger()

    sealed class Event {
        abstract val message: String
        data class Success(override val message: String) : Event()
        data class Failure(override val message: String) : Event()
    }

    fun notifyQueue(trigger: BuildTrigger) = maybeSendGithubStatus(
        trigger = trigger,
        state = "pending",
        targetUrl = null,
        description = QUEUE_MESSAGE
    )

    fun notifyStart(trigger: BuildTrigger) = maybeSendGithubStatus(
        trigger = trigger,
        state = "pending",
        targetUrl = null,
        description = START_MESSAGE
    )

    fun notifyComplete(
        trigger: BuildTrigger,
        customer: Customer,
        buildNumber: Long,
        event: Event) {
        val buildUri = URI.create("${homeUrlFormat.format(customer.subdomain)}/build/${buildNumber}")

        maybeSendHeyNotification(customer, event, buildNumber, buildUri, trigger)

        maybeSendGithubStatus(
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

    private fun maybeSendHeyNotification(customer: Customer, event: Event, buildNumber: Long, buildUri: URI?, trigger: BuildTrigger) {
        if (trigger is GithubWebhook || (trigger is BuildTrigger.Manual && !trigger.silent)) {
            client.notifyAsync(HeyNotification(
                customer.slackChannel,
                listOf(
                    HeyAttachment(
                        title = when (event) {
                            is Event.Success -> "Build #${buildNumber} succeeded"
                            is Event.Failure -> "Build #${buildNumber} failed"
                        },
                        titleLink = buildUri,
                        text = event.message,
                        fields = listOf(
                            HeyField("Branch", trigger.branch(), true),
                            HeyField("Customer", customer.name, true)
                        ),
                        timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                        color = when (event) {
                            is Event.Success -> HeyColor.GOOD
                            is Event.Failure -> HeyColor.DANGER
                        }
                    )
                )
            ))
        }
    }

    private fun maybeSendGithubStatus(trigger: BuildTrigger, state: String, targetUrl: URI?, description: String) {
        if (trigger is GithubWebhook) {
            github.accessTokenAsync(trigger.installationId)
                .thenAccept {
                    github.sendStatusAsync(
                        trigger.repoOwner,
                        trigger.repoName,
                        trigger.commit,
                        StatusCreate(state, targetUrl, description, "quartic"),
                        it
                    )
                }
                .exceptionally { LOG.warn("Error notifying GitHub", getRootCause(it)); null }
        }
    }


    companion object {
        internal val QUEUE_MESSAGE = "Your pipeline is queued for validation with Quartic"
        internal val START_MESSAGE = "Quartic is validating your pipeline"
        internal val SUCCESS_MESSAGE = "Quartic successfully validated your pipeline"
        internal val FAILURE_MESSAGE = "Quartic found some problems with your pipeline"
    }
}
