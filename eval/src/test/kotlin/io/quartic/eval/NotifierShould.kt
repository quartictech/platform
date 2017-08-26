package io.quartic.eval

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.database.Database.BuildResult.*
import io.quartic.hey.api.*
import org.junit.Test
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class NotifierShould {
    private val hey = mock<HeyClient>()
    private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
    private val notifier = Notifier(hey, clock)
    private val trigger = TriggerDetails(
        type = "github",
        deliveryId = "deadbeef",
        installationId = 1234,
        repoId = 5678,
        repoName = "noob",
        cloneUrl = URI("https://noob.com/foo/bar"),
        ref = "develop",
        commit = "abc123",
        timestamp = Instant.MIN
    )

    @Test
    fun send_success_on_success() {
        notifier.notifyAbout(trigger, Success(emptyList()))

        verify(hey).notify(HeyNotification(listOf(
            HeyAttachment(
                title = "Build succeeded",
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
    fun send_error_on_internal_error() {
        notifier.notifyAbout(trigger, InternalError(RuntimeException("Noob occurred")))

        verify(hey).notify(HeyNotification(listOf(
            HeyAttachment(
                title = "Build failed",
                text = "Noob occurred",
                fields = listOf(
                    HeyField("Repo", "noob", true),
                    HeyField("Branch", "develop", true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = HeyColor.DANGER
            )
        )))
    }

    @Test
    fun send_error_with_default_message_on_internal_error_without_message() {
        notifier.notifyAbout(trigger, InternalError(RuntimeException()))

        verify(hey).notify(HeyNotification(listOf(
            HeyAttachment(
                title = "Build failed",
                text = "Internal error",
                fields = listOf(
                    HeyField("Repo", "noob", true),
                    HeyField("Branch", "develop", true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = HeyColor.DANGER
            )
        )))
    }

    @Test
    fun send_error_on_user_error() {
        notifier.notifyAbout(trigger, UserError("You caused a noob"))

        verify(hey).notify(HeyNotification(listOf(
            HeyAttachment(
                title = "Build failed",
                text = "You caused a noob",
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
