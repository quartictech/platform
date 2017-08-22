package io.quartic.common.vertx

import com.google.common.base.Throwables.getRootCause
import io.quartic.common.client.userAgentFor
import io.quartic.common.logging.logger
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.serdes.configureObjectMapper
import io.vertx.config.ConfigRetriever
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.kotlin.config.ConfigRetrieverOptions
import io.vertx.kotlin.config.ConfigStoreOptions
import org.slf4j.Logger

// TODO - healthchecks
// TODO - metrics
// TODO - auth filter
// TODO - configuration
// TODO - static assets
abstract class ApplicationBase(private val devPort: Int) : AbstractVerticle() {
    protected val LOG by logger()
    protected val client: WebClient by lazy {
        WebClient.create(vertx, WebClientOptions().setUserAgent(userAgentFor(javaClass)))
    }
    protected val router: Router by lazy { Router.router(vertx) }

    private lateinit var secretsCodec: SecretsCodec
    protected lateinit var rawConfig: JsonObject

    final override fun start(future: Future<Void>) {
        configureObjectMappers()

        vertx.exceptionHandler { throw it }     // The default exceptionHandler seems to be null :(

        rawConfigRetriever().getConfig { ar ->
            if (ar.failed()) {
                throw RuntimeException("Failed to retrieve configuration", ar.cause())
            }
            rawConfig = ar.result()

            val baseConfig = getConfiguration<ConfigurationBase>()

            performSafetyChecks(baseConfig)

            secretsCodec = SecretsCodec(baseConfig.masterKeyBase64)

            router.route().handler(LoggerHandler.create(LoggerFormat.SHORT))
            router.route().handler(BodyHandler.create())    // TODO - do we need this?

            customise(future, ar.result())

            startServer(baseConfig)
        }
    }

    private fun configureObjectMappers() {
        configureObjectMapper(Json.mapper)
        configureObjectMapper(Json.prettyMapper)
    }

    private fun rawConfigRetriever() = ConfigRetriever.create(
        vertx,
        ConfigRetrieverOptions(stores = listOf(
            ConfigStoreOptions(type = "env")
        ))
    )

    protected fun EncryptedSecret.decrypt() = secretsCodec.decrypt(this)

    protected inline fun <reified C> getConfiguration(): C = try {
        rawConfig.mapTo(C::class.java)
    } catch (e: Exception) {
        throw RuntimeException("Couldn't parse config", getRootCause(e))
    }

    private fun performSafetyChecks(config: ConfigurationBase) {
        if (config.devMode) {
            LOG.banner("!!! RUNNING IN DEV MODE !!!")
        }
        if (!config.devMode && config.masterKeyBase64 == DEV_MASTER_KEY_BASE64) {
            throw RuntimeException("Running with developer master key in production")
        }
    }

    private fun startServer(config: ConfigurationBase) {
        val actualPort = if (config.devMode) devPort else 80

        vertx.createHttpServer()
            .requestHandler { router.accept(it) }
            .listen(actualPort) { res ->
                if (res.succeeded()) {
                    LOG.info("Listening on port ${actualPort}")
                } else {
                    throw RuntimeException("Could not start server", res.cause())
                }
            }
    }

    abstract fun customise(future: Future<Void>, rawConfig: JsonObject)

    private fun Logger.banner(message: String) {
        val width = 80
        val gap = width - 2 - message.length
        warn("\n" +
            "#".repeat(width) + "\n" +
            "#${" ".repeat(width - 2)}#\n" +
            "#${" ".repeat(gap / 2)}${message}${" ".repeat((gap  + 1) / 2)}#\n" +
            "#${" ".repeat(width - 2)}#\n" +
            "#".repeat(width)
        )
    }

    protected inline fun <reified T> RoutingContext.withConvertedBody(handler: (T) -> Unit) {
        val body = try {
            bodyAsJson.mapTo(T::class.java)
        } catch (e: Exception) {
            LOG.warn("Could not convert body JSON: ${e.message}")
            fail(400)
            return
        }
        handler(body)
    }

    companion object {
        fun deploy(app: ApplicationBase) {
            val LOG by logger()
            System.setProperty("vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory::class.qualifiedName)
            val vertx = Vertx.vertx()
            vertx.deployVerticle(app) { ar ->
                if (ar.failed()) {
                    LOG.error("Verticle failed", ar.cause())
                }
                vertx.close()
                if (ar.failed()) {
                    System.exit(1)
                }
            }
        }

        // TODO - duplicated with ConfigurationBase
        val DEV_MASTER_KEY_BASE64 = UnsafeSecret("TyHTfhBcy/QT8W7iNaktCSz32qGfxVctboTZfOnfMZE=")
    }
}
