package io.quartic.hey

import com.google.common.base.Throwables.getRootCause
import io.quartic.common.vertx.ApplicationBase
import io.quartic.hey.api.HeyColor
import io.quartic.hey.api.HeyNotification
import io.quartic.hey.model.SlackAttachment
import io.quartic.hey.model.SlackColor
import io.quartic.hey.model.SlackField
import io.quartic.hey.model.SlackMessage
import io.vertx.core.Future
import io.vertx.core.json.JsonObject

class HeyApplication : ApplicationBase(HEY_DEV_PORT) {
    override fun customise(future: Future<Void>, rawConfig: JsonObject) {
        val config = getConfiguration<HeyConfiguration>()
        val slackToken = config.slackTokenEncrypted.decrypt()

        router.post("/").handler { ctx ->
            ctx.withConvertedBody<HeyNotification> { note ->
                val message = noteToMessage(config, note)

                client.post(443, "hooks.slack.com", "/services/${slackToken.veryUnsafe}")
                    .ssl(true)
                    .sendJson(message) { ar ->
                        if (ar.succeeded()) {
                            if (ar.result().statusCode() in 200..299) {
                                LOG.info("Message sent successfully")
                            } else {
                                LOG.error("Message failed with status code: ${ar.result().statusCode()} (${ar.result().bodyAsString()})")
                            }
                        } else {
                            LOG.error("Message failed: ${getRootCause(ar.cause()).message})")
                        }
                    }
                ctx.response().end()
            }
        }
    }

    private fun noteToMessage(config: HeyConfiguration, note: HeyNotification) = SlackMessage(
        username = config.slackUsername,
        channel = config.slackChannel,
        attachments = note.attachments.map { a ->
            SlackAttachment(
                title = a.title,
                titleLink = a.titleLink,
                text = a.text,
                fields = a.fields.map { f ->
                    SlackField(
                        title = f.title,
                        value = f.value,
                        short = f.short
                    )
                },
                footer = a.footer,
                timestamp = a.timestamp,
                color = when (a.color) {
                    HeyColor.GOOD -> SlackColor.GOOD
                    HeyColor.WARNING -> SlackColor.WARNING
                    HeyColor.DANGER -> SlackColor.DANGER
                    HeyColor.QUARTIC -> SlackColor("#db1e7b")
                    null -> null
                }
            )
        }
    )

    companion object {
        @JvmStatic fun main(args: Array<String>) = deploy(HeyApplication())

        val HEY_DEV_PORT = 8220
    }
}
