package io.quartic.eval

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.quartic.eval.Notifier.Event.Failure
import io.quartic.eval.Notifier.Event.Success
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.hey.api.*
import io.quartic.registry.api.model.Customer
import org.junit.Test
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class NotifierShould {
    private val hey = mock<HeyClient>()
    private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
    private val notifier = Notifier(hey, "http://%s", clock)
    private val trigger = mock<TriggerDetails> {
        on { repoName } doReturn "noob"
        on { branch() } doReturn "develop"
    }
    private val customer = mock<Customer> {
        on { subdomain } doReturn "noobhole"
    }

    @Test
    fun send_success_on_success() {
        notifier.notifyAbout(trigger, customer, 100, Success("Hello there"))

        verify(hey).notifyAsync(HeyNotification(listOf(
            HeyAttachment(
                title = "Build #100 succeeded",
                titleLink = URI.create("http://noobhole/#/pipeline/100"),
                text = "Hello there",
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
        notifier.notifyAbout(trigger, customer, 100, Failure("Oh dear"))

        verify(hey).notifyAsync(HeyNotification(listOf(
            HeyAttachment(
                title = "Build #100 failed",
                titleLink = URI.create("http://noobhole/#/pipeline/100"),
                text = "Oh dear",
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
