package io.quartic.github

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * These tests run Jackson deserialisation against captured webhook POST content.
 */
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
                headCommit = Commit(
                        id = "e7298ce9149a7a06f83b8318b8c3fa655b71861d",
                        message = "Dummy commit",
                        timestamp = OffsetDateTime.of(2017, 7, 20, 14, 41, 10, 0, ZoneOffset.ofHours(1)),
                        author = user,
                        committer = user
                ),
                commits = listOf(
                    Commit(
                        id = "e7298ce9149a7a06f83b8318b8c3fa655b71861d",
                        message = "Dummy commit",
                        timestamp = OffsetDateTime.of(2017, 7, 20, 14, 41, 10, 0, ZoneOffset.ofHours(1)),
                        author = user,
                        committer = user
                    )
                ),
                organization = null,
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
                    owner = Owner(
                        login = "choliver"
                    ),
                    private = false,
                    cloneUrl = URI("https://github.com/choliver/test.git"),
                    defaultBranch = "master"
                ),
                installation = Installation(40706)
            )
        ))
    }

    @Test
    fun parse_push_event_for_org_repo() {
        val push = OBJECT_MAPPER.readValue<PushEvent>(javaClass.getResourceAsStream("/push_event_org_repo.json"))

        val user = User(
            name = "Oliver Charlesworth",
            email = "oliver@quartic.io",
            username = "choliver"
        )

        assertThat(push, equalTo(
            PushEvent(
                ref = "refs/heads/feature/github",
                after = "fc6206fd27761a1e03383287e213801105f01a25",
                before = "efadb7ddea7476c99fef529740096dce49f88279",
                headCommit = Commit(
                        id = "fc6206fd27761a1e03383287e213801105f01a25",
                        message = "Add unfilled test",
                        timestamp = OffsetDateTime.of(2017, 7, 20, 16, 15, 23, 0, ZoneOffset.ofHours(1)),
                        author = user,
                        committer = user
                ),
                commits = listOf(
                    Commit(
                        id = "fc6206fd27761a1e03383287e213801105f01a25",
                        message = "Add unfilled test",
                        timestamp = OffsetDateTime.of(2017, 7, 20, 16, 15, 23, 0, ZoneOffset.ofHours(1)),
                        author = user,
                        committer = user
                    )
                ),
                organization = Organization(
                    id = 22931189,
                    login = "quartictech",
                    description = "Big data for big maintenance."
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
                    id = 71576535,
                    name = "platform",
                    fullName = "quartictech/platform",
                    private = true,
                    cloneUrl = URI("https://github.com/quartictech/platform.git"),
                    owner = Owner(
                        login = "quartictech"
                    ),
                    defaultBranch = "develop"
                ),
                installation = Installation(40737)
            )
        ))
    }
}
