package io.quartic.hey.model

data class SlackMessage(
    val username: String,
    val channel: String,
    val text: String? = null,
    val attachments: List<SlackAttachment> = emptyList()
)

data class SlackAttachment(
    val title: String,
    val pretext: String? = null,
    val text: String? = null,
    val color: SlackColor? = SlackColor.QUARTIC
)

class SlackColor(private val value: String) {
    override fun toString() = value

    companion object {
        val GOOD = SlackColor("good")
        val WARNING = SlackColor("warning")
        val DANGER = SlackColor("danger")
        val QUARTIC = SlackColor("#db1e7b")
    }
}
