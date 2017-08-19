package io.quartic.hey.model

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.net.URI
import java.time.Instant

data class SlackMessage(
    val username: String,
    val channel: String,
    val text: String? = null,
    val attachments: List<SlackAttachment> = emptyList()
)

data class SlackAttachment(
    val title: String,
    @get:JsonProperty("title_link")
    val titleLink: URI? = null,
    val pretext: String? = null,
    val text: String? = null,
    val footer: String? = null,
    @get:JsonProperty("footer_icon")
    val footerIcon: URI? = null,
    @get:JsonIgnore
    val timestamp: Instant? = null,
    val color: SlackColor? = SlackColor.QUARTIC
) {
    @get:JsonGetter("ts")
    val timestampAsSeconds = timestamp?.epochSecond
}

class SlackColor(private val value: String) {
    @JsonValue
    override fun toString() = value

    companion object {
        val GOOD = SlackColor("good")
        val WARNING = SlackColor("warning")
        val DANGER = SlackColor("danger")
        val QUARTIC = SlackColor("#db1e7b")
    }
}
