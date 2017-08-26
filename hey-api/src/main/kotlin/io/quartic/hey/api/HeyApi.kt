package io.quartic.hey.api

import com.fasterxml.jackson.annotation.JsonFormat
import io.quartic.common.client.Retrofittable
import java.net.URI
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture

data class HeyNotification(
    val attachments: List<HeyAttachment>
)

data class HeyAttachment(
    val title: String,
    val titleLink: URI? = null,
    val text: String,
    val fields: List<HeyField> = emptyList(),
    val footer: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", without = arrayOf(JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE))
    val timestamp: OffsetDateTime? = null,
    val color: HeyColor? = null
)

data class HeyField(
    val title: String,
    val value: String,
    val short: Boolean = false
)

enum class HeyColor {
    GOOD,
    WARNING,
    DANGER,
    QUARTIC,
}

@Retrofittable
interface HeyClient {
    fun notify(notification: HeyNotification): CompletableFuture<Unit>
}
