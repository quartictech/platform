package io.quartic.hey

import io.quartic.hey.model.SlackAttachment
import io.quartic.hey.model.SlackColor
import io.quartic.hey.model.SlackMessage
import io.vertx.core.json.Json
import org.junit.Test
import java.net.URI
import java.time.Instant

class Slack {
    @Test
    fun name() {
        val message = SlackMessage(
            username = "Quartic Hey",
            channel = "#infrastructure",
            attachments = listOf(
                SlackAttachment(
                    title = "Build #37 failure",
                    titleLink = URI("https://www.quartic.io"),
                    text = "Absolutely noob",
                    color = SlackColor.DANGER,
                    timestamp = Instant.now()
                )
            )
        )

        println(Json.encode(message))
    }
}
