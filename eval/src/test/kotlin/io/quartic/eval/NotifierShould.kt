package io.quartic.eval

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.hey.api.*
import io.quartic.registry.api.model.Customer
import org.junit.Test
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

class NotifierShould {
    private val hey = mock<HeyClient>()
    private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
    private val notifier = Notifier(hey, "http://%s", clock)
    private val trigger = TriggerDetails(
        type = "github",
        deliveryId = "deadbeef",
        installationId = 1234,
        repoId = 5678,
        repoName = "noob",
        cloneUrl = URI("https://noob.com/foo/bar"),
        ref = "refs/heads/develop",
        commit = "abc123",
        timestamp = Instant.MIN
    )

    private val customerId = CustomerId(100)

    private val customer = Customer(
        id = customerId,
        githubOrgId = 8765,
        githubRepoId = 5678,
        name = "Noobhole Ltd",
        subdomain = "noobhole",
        namespace = "noobhole"
    )

     private val build = Database.BuildRow(
        id = UUID.randomUUID(),
        customerId = customerId,
        branch = "develop",
        buildNumber = 100,
        triggerDetails = trigger
    )

    @Test
    fun send_success_on_success() {
        notifier.notifyAbout(trigger, customer, build, true)

        verify(hey).notifyAsync(HeyNotification(listOf(
            HeyAttachment(
                title = "Build #${build.buildNumber} succeeded",
                titleLink = URI.create("http://noobhole/#/pipeline/${build.buildNumber}"),
                text = "Success",
                fields = listOf(
                    HeyField("Repo", "noob", true),
                    HeyField("Branch", "develop", true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = HeyColor.GOOD
            )
        )))
    }

    @Test
    fun send_error_on_failure() {
        notifier.notifyAbout(trigger, customer, build, false)

        verify(hey).notifyAsync(HeyNotification(listOf(
            HeyAttachment(
                title = "Build #${build.buildNumber} failed",
                titleLink = URI.create("http://noobhole/#/pipeline/${build.buildNumber}"),
                text = "Failure",
                fields = listOf(
                    HeyField("Repo", "noob", true),
                    HeyField("Branch", "develop", true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = HeyColor.DANGER
            )
        )))
    }
}
