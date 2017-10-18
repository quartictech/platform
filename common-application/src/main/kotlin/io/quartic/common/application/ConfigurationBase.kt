package io.quartic.common.application

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.dropwizard.Configuration
import io.dropwizard.jetty.HttpConnectorFactory
import io.dropwizard.logging.ConsoleAppenderFactory
import io.dropwizard.logging.DefaultLoggingFactory
import io.dropwizard.server.DefaultServerFactory
import io.quartic.common.secrets.EncryptedSecret
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

    val auth: AuthConfiguration = LegacyAuthConfiguration()  // TODO - remove this default eventually

    var url: ServerDetails = ServerDetails()
        set(value) = configureServer(value)

    var logLevel: Level = Level.INFO
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
    private fun configureLogging(level: Level) {
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

// TODO - this is kind of a mess, it doesn't make sense for the config file to dictate the type of auth in use
@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = FrontendAuthConfiguration::class, name = "frontend"),
    JsonSubTypes.Type(value = InternalAuthConfiguration::class, name = "internal"),
    JsonSubTypes.Type(value = LegacyAuthConfiguration::class, name = "legacy")
)
sealed class AuthConfiguration

data class FrontendAuthConfiguration(val keyEncryptedBase64: EncryptedSecret) : AuthConfiguration()
data class InternalAuthConfiguration(val keyEncryptedBase64: EncryptedSecret) : AuthConfiguration()
data class LegacyAuthConfiguration(val _dummy: Int = 0) : AuthConfiguration()

val DEV_MASTER_KEY_BASE64 = UnsafeSecret("TyHTfhBcy/QT8W7iNaktCSz32qGfxVctboTZfOnfMZE=")
