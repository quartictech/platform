package io.quartic.eval

import io.quartic.eval.api.model.TriggerDetails
import io.quartic.hey.api.*
import io.quartic.registry.api.model.Customer
import java.net.URI
import java.time.Clock
import java.time.ZoneOffset

class Notifier(
    private val client: HeyClient,
    private val homeUrlFormat: String,
    private val clock: Clock = Clock.systemUTC()
) {
    sealed class Event {
        abstract val message: String
        data class Success(override val message: String) : Event()
        data class Failure(override val message: String) : Event()
    }

    fun notifyAbout(
        trigger: TriggerDetails,
        customer: Customer,
        buildNumber: Long,
        event: Event
    ) {
        client.notifyAsync(HeyNotification(listOf(
            HeyAttachment(
                title = when (event) {
                    is Event.Success -> "Build #${buildNumber} succeeded"
                    is Event.Failure -> "Build #${buildNumber} failed"
                },
                titleLink = URI.create(
                    "${homeUrlFormat.format(customer.subdomain)}/#/pipeline/${buildNumber}"
                ),
                text = event.message,
                fields = listOf(
                    HeyField("Repo", trigger.repoName, true),
                    HeyField("Branch", trigger.branch(), true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = when (event) {
                    is Event.Success -> HeyColor.GOOD
                    is Event.Failure -> HeyColor.DANGER
                }
            )
        )))
    }
}
