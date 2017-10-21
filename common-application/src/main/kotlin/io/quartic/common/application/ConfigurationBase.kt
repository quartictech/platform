package io.quartic.common.application

import ch.qos.logback.classic.spi.ILoggingEvent
import io.dropwizard.Configuration
import io.dropwizard.jetty.HttpConnectorFactory
import io.dropwizard.logging.ConsoleAppenderFactory
import io.dropwizard.logging.DefaultLoggingFactory
import io.dropwizard.server.DefaultServerFactory
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret

abstract class ConfigurationBase : Configuration() {
    val masterKeyBase64: UnsafeSecret = with(System.getenv("MASTER_KEY_BASE64")) {
        if (this != null) {
            UnsafeSecret(this)
        } else {
            DEV_MASTER_KEY_BASE64
        }
    }

    var url: ServerDetails = ServerDetails()
        set(value) = configureServer(value)

    var logLevel: String = "INFO"
        set(value) = configureLogging(value)

    val secretsCodec by lazy { SecretsCodec(masterKeyBase64) }

    init {
        configureLogging(logLevel)
    }

    // Opinionated server configuration
    private fun configureServer(details: ServerDetails) {
        super.setServerFactory(DefaultServerFactory().apply {
            applicationContextPath = details.contextPath
            applicationConnectors = listOf(HttpConnectorFactory().apply {
                port = if (details.randomPort) 0 else details.port
            })
            adminConnectors = listOf(HttpConnectorFactory().apply {
                port = if (details.randomPort) 0 else (details.port + 1)
            })
        })
    }

    // Opinionated logging configuration
    private fun configureLogging(level: String) {
        super.setLoggingFactory(DefaultLoggingFactory().apply {
            this.level = level
            setAppenders(listOf(ConsoleAppenderFactory<ILoggingEvent>().apply {
                logFormat = "%d{ISO8601, UTC} %highlight(%-5level) [%logger{36}]: %msg%n%yellow(%rEx) %nopex"
            }))
        })
    }
}

data class ServerDetails(
    val port: Int = 80,
    val contextPath: String = "/",
    val randomPort: Boolean = false
)

val DEV_MASTER_KEY_BASE64 = UnsafeSecret("TyHTfhBcy/QT8W7iNaktCSz32qGfxVctboTZfOnfMZE=")
