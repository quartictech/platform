package io.quartic.glisten

import com.fasterxml.jackson.module.kotlin.convertValue
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.assertThrows
import io.quartic.glisten.github.model.*
import io.quartic.glisten.model.Notification
import io.quartic.glisten.model.Registration
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Instant
import javax.ws.rs.BadRequestException
import javax.ws.rs.ForbiddenException

class GithubResourceShould {
    private val notify = mock<(Notification) -> Unit>()

    private val resource = GithubResource(mapOf(
        "orgA" to Registration("github", 12345),
        "orgB" to Registration("github", 67890)
    ), notify)

    @Test
    fun ignore_non_push_event() {
        resource.handleEvent("noob", "abc", emptyMap())
        // Nothing should happen
    }

    @Test
    fun respond_with_400_if_unparseable_body() {
        val e = assertThrows<BadRequestException> {
            resource.handleEvent("push", "abc", mapOf("foo" to "bar"))
        }
        assertThat(e.message, containsString("abc"))
    }

    @Test
    fun respond_with_403_if_unregistered_installation() {
        assertThrows<ForbiddenException> {
            resource.handleEvent("push", "abc", OBJECT_MAPPER.convertValue(pushEvent(54321)))
        }
    }

    @Test
    fun notify_if_registered_installation() {
        resource.handleEvent("push", "abc", OBJECT_MAPPER.convertValue(pushEvent(12345)))

        verify(notify)(Notification("orgA", "https://github.com/noobhole/noobing.git"))
    }

    private fun pushEvent(installationId: Int) = PushEvent(
        ref = "refs/heads/master",
        after = "fc6206fd27761a1e03383287e213801105f01a25",
        before = "efadb7ddea7476c99fef529740096dce49f88279",
        commits = listOf(
            Commit(
                id = "fc6206fd27761a1e03383287e213801105f01a25",
                message = "Add some cool stuff",
                timestamp = Instant.EPOCH,
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
            cloneUrl = "https://github.com/noobhole/noobing.git"
        ),
        installation = Installation(installationId)
    )
}
