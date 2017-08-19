package io.quartic.hey

import io.quartic.common.client.userAgentFor
import io.quartic.common.logging.logger
import io.quartic.hey.model.SlackAttachment
import io.quartic.hey.model.SlackColor
import io.quartic.hey.model.SlackMessage
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import java.net.URI
import java.time.Instant

class HeyApplication : AbstractVerticle() {

    private val LOG by logger()

    override fun start() {
        val client = WebClient.create(vertx, WebClientOptions().setUserAgent(userAgentFor(javaClass)))

        val router = Router.router(vertx)

        router.route().handler(LoggerHandler.create(LoggerFormat.SHORT))
        router.route().handler(BodyHandler.create())    // TODO - do we need this?

        router.get("/").handler { ctx ->
            val message = SlackMessage(
                username = SLACK_USERNAME,
                channel = SLACK_CHANNEL,
                attachments = listOf(
                    SlackAttachment(
                        title = "Build #37 failure",
                        titleLink = URI("https://www.quartic.io"),
                        text = "There was a serious problem here.",
                        timestamp = Instant.now(),
                        color = SlackColor.DANGER
                    )
                )
            )

            client.post(443, "hooks.slack.com", "/services/$SLACK_TOKEN")
                .ssl(true)
                .sendJson(message) { ar ->
                    if (ar.succeeded() && ar.result().statusCode() in 200..299) {
                        LOG.info("Message sent successfully")
                    } else {
                        LOG.error("Message failed: ${ar.cause().message})")
                    }
                }
            ctx.response().end()
        }

        vertx.createHttpServer()
            .requestHandler { router.accept(it) }
            .listen(DEV_PORT) { res ->
                if (res.succeeded()) {
                    LOG.info("Listening on port ${DEV_PORT}")
                } else {
                    LOG.error("Could not start server", res.cause())
                    vertx.close()
                }
            }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val vertx = Vertx.vertx()
            vertx.deployVerticle(HeyApplication())
        }

        val DEV_PORT = 8220     // TODO

        val SLACK_TOKEN = "T2CTQKSKU/B6QQVQENP/D2rEcAxbaiZOILANc7cTs48R"
        val SLACK_CHANNEL = "#infrastructure"
        val SLACK_USERNAME = "Quartic Hey"
    }
}
