package io.quartic.hey

import io.quartic.common.logging.logger
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler

class Hey : AbstractVerticle() {

    private val LOG by logger()

    override fun start() {
        val router = Router.router(vertx)

        router.route().handler(LoggerHandler.create(LoggerFormat.SHORT))
        router.route().handler(BodyHandler.create())    // TODO - do we need this?

        router.get("/").handler { ctx ->
            LOG.info("Oh yeah")
            ctx.response().end("Hello world!")
        }

        vertx.createHttpServer()
            .requestHandler { router.accept(it) }
            .listen(DEV_PORT)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val vertx = Vertx.vertx()
            vertx.deployVerticle(Hey())
        }

        val DEV_PORT = 8220     // TODO
    }
}
