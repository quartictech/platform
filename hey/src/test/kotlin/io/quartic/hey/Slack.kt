package io.quartic.hey

import io.quartic.hey.model.SlackAttachment
import io.quartic.hey.model.SlackColor
import io.quartic.hey.model.SlackMessage
import org.junit.Test

class Slack {
    @Test
    fun name() {
        val message = SlackMessage(
            username = "Quartic Hey",
            channel = "#infrastructure",
            attachments = listOf(
                SlackAttachment(
                    title = "Build #37 failure",
                    color = SlackColor.DANGER
                )
            )
        )


    }

    companion object {
        val SLACK_TOKEN = "T2CTQKSKU/B6QQVQENP/D2rEcAxbaiZOILANc7cTs48R"
    }
}
