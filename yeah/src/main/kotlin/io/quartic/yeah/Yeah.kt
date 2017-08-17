package io.quartic.yeah

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx

class Yeah : AbstractVerticle() {

    override fun start() {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val vertx = Vertx.vertx()
            vertx.deployVerticle(Yeah())
        }
    }
}
