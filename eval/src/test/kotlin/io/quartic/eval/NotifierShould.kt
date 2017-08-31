package io.quartic.eval

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.github.AccessToken
import io.quartic.github.GitHubInstallationClient
import io.quartic.hey.api.*
import io.quartic.registry.api.model.Customer
import org.junit.Test
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CompletableFuture

class NotifierShould {
    private val hey = mock<HeyClient>()
    private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
    private val github = mock<GitHubInstallationClient>()
    private val accessToken = mock<GitHubInstallationClient.GitHubInstallationAccessToken>()

    private val notifier = Notifier(hey, github, "http://%s", clock)
    private val trigger = TriggerDetails(
        type = "github",
        deliveryId = "deadbeef",
        installationId = 1234,
        repoId = 5678,
        repoFullName = "noobing/noob",
        repoName = "noob",
        repoOwner = "noobing",
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

    init {
        whenever(github.accessTokenAsync(any())).thenReturn(CompletableFuture.completedFuture(accessToken))
    }

    @Test
    fun send_success_on_success() {
        notifier.notifyAbout(trigger, customer, 100, true)

        verify(hey).notifyAsync(HeyNotification(listOf(
            HeyAttachment(
                title = "Build #100 succeeded",
                titleLink = URI.create("http://noobhole/#/pipeline/100"),
                text = "Success",
                fields = listOf(
                    HeyField("Repo", "noobing/noob", true),
                    HeyField("Branch", "develop", true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = HeyColor.GOOD
            )
        )))
    }

    @Test
    fun send_error_on_failure() {
        notifier.notifyAbout(trigger, customer, 100, false)

        verify(hey).notifyAsync(HeyNotification(listOf(
            HeyAttachment(
                title = "Build #100 failed",
                titleLink = URI.create("http://noobhole/#/pipeline/100"),
                text = "Failure",
                fields = listOf(
                    HeyField("Repo", "noobing/noob", true),
                    HeyField("Branch", "develop", true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = HeyColor.DANGER
            )
        )))
    }
}
