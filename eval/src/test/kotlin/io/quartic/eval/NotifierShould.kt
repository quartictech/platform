package io.quartic.eval

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.quartic.eval.Notifier.Event.Failure
import io.quartic.eval.Notifier.Event.Success
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.StatusCreate
import io.quartic.hey.api.*
import io.quartic.registry.api.model.Customer
import org.junit.Test
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CompletableFuture.completedFuture

class NotifierShould {
    private val hey = mock<HeyClient>()
    private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
    private val accessToken = mock<GitHubInstallationClient.GitHubInstallationAccessToken>()
    private val github = mock<GitHubInstallationClient> {
        on { accessTokenAsync(any()) } doReturn completedFuture(accessToken)
    }
    private val notifier = Notifier(hey, github, "http://%s", clock)
    private val trigger = mock<BuildTrigger.GithubWebhook> {
        on { repoOwner } doReturn "noobing"
        on { repoName } doReturn "noob"
        on { branch() } doReturn "develop"
    }
    private val customer = mock<Customer> {
        on { subdomain } doReturn "noobhole"
    }

    private val buildUri = URI.create("http://noobhole/#/pipeline/100")

    @Test
    fun send_pending_on_start() {
        notifier.notifyStart(trigger)
        verify(github).sendStatusAsync(
            owner = "noobing",
            repo = "noob",
            sha = trigger.commit,
            status = StatusCreate(
                "pending",
                targetUrl = null,
                description = Notifier.START_MESSAGE,
                context = "quartic"
            ),
            accessToken = accessToken
        )
    }


    @Test
    fun send_success_on_success() {
        notifier.notifyComplete(trigger, customer, 100, Success("Hello there"))

        verify(hey).notifyAsync(HeyNotification(listOf(
            HeyAttachment(
                title = "Build #100 succeeded",
                titleLink = URI.create("http://noobhole/#/pipeline/100"),
                text = "Hello there",
                fields = listOf(
                    HeyField("Branch", "develop", true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = HeyColor.GOOD
            )
        )))

        verify(github).sendStatusAsync(
            owner = "noobing",
            repo = "noob",
            sha = trigger.commit,
            status = StatusCreate(
                "success",
                targetUrl = buildUri,
                description = Notifier.SUCCESS_MESSAGE,
                context = "quartic"
            ),
            accessToken = accessToken
        )
    }

    @Test
    fun send_error_on_failure() {
        notifier.notifyComplete(trigger, customer, 100, Failure("Oh dear"))

        verify(hey).notifyAsync(HeyNotification(listOf(
            HeyAttachment(
                title = "Build #100 failed",
                titleLink = URI.create("http://noobhole/#/pipeline/100"),
                text = "Oh dear",
                fields = listOf(
                    HeyField("Branch", "develop", true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = HeyColor.DANGER
            )
        )))

        verify(github).sendStatusAsync(
            owner = "noobing",
            repo = "noob",
            sha = trigger.commit,
            status = StatusCreate(
                "failure",
                targetUrl = buildUri,
                description = Notifier.FAILURE_MESSAGE,
                context = "quartic"
            ),
            accessToken = accessToken
        )
    }
}
