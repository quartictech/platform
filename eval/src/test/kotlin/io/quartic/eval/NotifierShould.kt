package io.quartic.eval

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.test.exceptionalFuture
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

    private  val manualTrigger  = mock<BuildTrigger.Manual> {
        on { silent } doReturn false
    }
    private  val silentManualTrigger  = mock<BuildTrigger.Manual>() {
        on { silent } doReturn true
    }

    private val customer = mock<Customer> {
        on { subdomain } doReturn "noobhole"
        on { name } doReturn "noob co"
        on { slackChannel } doReturn "#noobery"
    }

    private val buildUri = URI.create("http://noobhole/build/100")

    @Test
    fun send_pending_on_queue() {
        notifier.notifyQueue(trigger)
        verify(github).sendStatusAsync(
            owner = "noobing",
            repo = "noob",
            sha = trigger.commit,
            status = StatusCreate(
                "pending",
                targetUrl = null,
                description = Notifier.QUEUE_MESSAGE,
                context = "quartic"
            ),
            accessToken = accessToken
        )
    }

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
    fun not_send_pending_for_manual_trigger() {
        notifier.notifyStart(manualTrigger)
        verifyZeroInteractions(github)
    }

    @Test
    fun not_notify_on_silent() {
        notifier.notifyComplete(silentManualTrigger, customer, 100, Success("Hi"))
        verifyZeroInteractions(hey)
    }

    @Test
    fun send_success_on_success() {
        notifier.notifyComplete(trigger, customer, 100, Success("Hello there"))

        verify(hey).notifyAsync(HeyNotification(
            "#noobery",
            listOf(
                HeyAttachment(
                    title = "Build #100 succeeded",
                    titleLink = URI.create("http://noobhole/build/100"),
                    text = "Hello there",
                    fields = listOf(
                        HeyField("Branch", "develop", true),
                        HeyField("Customer", customer.name, true)
                    ),
                    timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                    color = HeyColor.GOOD
                )
            )
        ))

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

        verify(hey).notifyAsync(HeyNotification(
            "#noobery",
            listOf(
                HeyAttachment(
                    title = "Build #100 failed",
                    titleLink = URI.create("http://noobhole/build/100"),
                    text = "Oh dear",
                    fields = listOf(
                        HeyField("Branch", "develop", true),
                        HeyField("Customer", customer.name, true)
                    ),
                    timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                    color = HeyColor.DANGER
                )
            )
        ))

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

    @Test
    fun not_throw_if_github_auth_fails() {
        whenever(github.accessTokenAsync(any())).thenReturn(exceptionalFuture())

        notifier.notifyStart(trigger)
        // Not expecting to throw
    }
}
