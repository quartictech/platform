package io.quartic.glisten.github.model

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class GithubModelShould {
    @Test
    fun parse_push_event_for_user_repo() {
        val push = OBJECT_MAPPER.readValue<PushEvent>(javaClass.getResourceAsStream("/push_event_user_repo.json"))

        val user = User(
            name = "Oliver Charlesworth",
            email = "oliver@quartic.io",
            username = "choliver"
        )

        assertThat(push, equalTo(
            PushEvent(
                ref = "refs/heads/master",
                after = "e7298ce9149a7a06f83b8318b8c3fa655b71861d",
                before = "a009c981dd764c771b8dc5ec37a40bc7433df801",
                commits = listOf(
                    Commit(
                        id = "e7298ce9149a7a06f83b8318b8c3fa655b71861d",
                        message = "Dummy commit",
                        timestamp = OffsetDateTime.of(2017, 7, 20, 14, 41, 10, 0, ZoneOffset.ofHours(1)).toInstant(),
                        author = user,
                        committer = user
                    )
                ),
                pusher = Pusher(
                    name = "choliver",
                    email = "oliver@quartic.io"
                ),
                sender = Sender(
                    id = 1058509,
                    login = "choliver",
                    type = "User"
                ),
                repository = Repository(
                    id = 97818780,
                    name = "test",
                    fullName = "choliver/test",
                    private = false,
                    cloneUrl = "https://github.com/choliver/test.git"
                ),
                installation = Installation(40706)
            )
        ))
    }

    @Test
    fun parse_push_event_for_org_repo() {
        // TODO
    }
}
