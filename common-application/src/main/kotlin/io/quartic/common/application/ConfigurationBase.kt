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
    
    val auth: AuthConfiguration = DummyAuthConfiguration()  // TODO - remove this default eventually

    // Opinionated port selection
    var url: ServerDetails = ServerDetails()
        set(value) = configureServer(value)

    val secretsCodec by lazy {
        SecretsCodec(masterKeyBase64)
    }

    init {
        configureLogging()
    }

    // Opinionated server configuration
    private fun configureServer(value: ServerDetails) {
        super.setServerFactory(DefaultServerFactory().apply {
            applicationContextPath = value.contextPath
            applicationConnectors = listOf(HttpConnectorFactory().apply {
                port = if (value.randomPort) 0 else value.port
            })
            adminConnectors = listOf(HttpConnectorFactory().apply {
                port = if (value.randomPort) 0 else (value.port + 1)
            })
        })
    }

    // Opinionated logging configuration
    private fun configureLogging() {
        super.setLoggingFactory(DefaultLoggingFactory().apply {
            level = Level.INFO
            setAppenders(listOf(ConsoleAppenderFactory<ILoggingEvent>().apply {
                logFormat = "%d{ISO8601, UTC} %highlight(%-5level) [%logger{36}]: %msg%n%rEx"
            }))
        })
    }
}

data class ServerDetails(
    val port: Int = 80,
    val contextPath: String = "/",
    val randomPort: Boolean = false
)

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TokenAuthConfiguration::class, name = "token"),
    JsonSubTypes.Type(value = DummyAuthConfiguration::class, name = "dummy")
)
sealed class AuthConfiguration

data class TokenAuthConfiguration(
    val keyEncryptedBase64: EncryptedSecret
) : AuthConfiguration()

data class DummyAuthConfiguration(
    val _dummy: Int = 0
) : AuthConfiguration()

val DEV_MASTER_KEY_BASE64 = UnsafeSecret("TyHTfhBcy/QT8W7iNaktCSz32qGfxVctboTZfOnfMZE=")
