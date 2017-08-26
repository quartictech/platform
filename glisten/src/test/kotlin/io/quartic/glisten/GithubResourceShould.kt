package io.quartic.glisten

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.assertThrows
import io.quartic.eval.api.EvalTriggerService
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.github.*
import org.apache.commons.codec.binary.Hex
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.ServerErrorException

class GithubResourceShould {
    // Recorded from a real GitHub webhook (token is now changed, obviously!)
    private val secret = UnsafeSecret("JaXAybVPJmDaLk2Z7fMx")
    private val pingPayload = javaClass.getResource("/ping_event.json").readText()
    private val pingSignature = "sha1=62c3f51e3b54b13036a062f0fb21759837280481"

    private val trigger = mock<EvalTriggerService>()
    private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
    private val resource = GithubResource(secret, trigger, clock)

    @Test
    fun respond_with_401_if_token_mismatch() {
        assertThrows<NotAuthorizedException> {
            resource.handleEvent("ping", "abc", "${pingSignature}5", pingPayload)
        }
    }

    @Test
    fun ignore_non_push_event() {
        resource.handleEvent("ping", "abc", pingSignature, pingPayload)
        // Nothing should happen
    }

    @Test
    fun respond_with_400_if_unparseable_body() {
        val payload = OBJECT_MAPPER.writeValueAsString(mapOf("foo" to "bar"))
        val e = assertThrows<BadRequestException> {
            resource.handleEvent("push", "abc", calculateSignature(payload), payload)
        }
        assertThat(e.message, containsString("abc"))
    }

    @Test
    fun send_trigger_if_event_is_regular() {
        val payload = OBJECT_MAPPER.writeValueAsString(pushEvent())
        resource.handleEvent("push", "abc", calculateSignature(payload), payload)

        verify(trigger).trigger(TriggerDetails(
            type = "github",
            deliveryId = "abc",
            installationId = 12345,
            repoId = 66666,
            repoName = "noobhole/noobing",
            cloneUrl = URI("https://github.com/noobhole/noobing.git"),
            ref = "refs/heads/master",
            commit = "fc6206fd27761a1e03383287e213801105f01a25",
            timestamp = clock.instant()
        ))
    }

    @Test
    fun succeed_even_if_trigger_throws_error() {
        whenever(trigger.trigger(any())).thenThrow(ServerErrorException("Server is noob", 500))

        val payload = OBJECT_MAPPER.writeValueAsString(pushEvent())
        resource.handleEvent("push", "abc", calculateSignature(payload), payload)

        // No exception
    }

    private fun calculateSignature(body: String): String {
        val keySpec = SecretKeySpec(secret.veryUnsafe.toByteArray(), "HmacSHA1")

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(keySpec)
        val result = mac.doFinal(body.toByteArray())

        return "sha1=${Hex.encodeHexString(result)}"
    }

    private fun pushEvent() = PushEvent(
        ref = "refs/heads/master",
        after = "fc6206fd27761a1e03383287e213801105f01a25",
        before = "efadb7ddea7476c99fef529740096dce49f88279",
        headCommit = Commit(
            id = "fc6206fd27761a1e03383287e213801105f01a25",
            message = "Add some cool stuff",
            timestamp = Instant.EPOCH.atOffset(ZoneOffset.UTC),
            author = User("Johnny Noobhole", "johnny@noobhole.io", "johnny"),
            committer = User("Johnny Noobhole", "johnny@noobhole.io", "johnny")
        ),
        commits = listOf(
            Commit(
                id = "fc6206fd27761a1e03383287e213801105f01a25",
                message = "Add some cool stuff",
                timestamp = Instant.EPOCH.atOffset(ZoneOffset.UTC),
                author = User("Johnny Noobhole", "johnny@noobhole.io", "johnny"),
                committer = User("Johnny Noobhole", "johnny@noobhole.io", "johnny")
            )
        ),
        organization = Organization(
            id = 11111,
            login = "noobhole",
            description = "Making noobholes since 1969."
        ),
        pusher = Pusher(
            name = "noob",
            email = "noob@hole.io"
        ),
        sender = Sender(
            id = 22222,
            login = "hole",
            type = "User"
        ),
        repository = Repository(
            id = 66666,
            name = "noobing",
            fullName = "noobhole/noobing",
            private = true,
            cloneUrl = URI("https://github.com/noobhole/noobing.git")
        ),
        installation = Installation(12345)
    )
}
