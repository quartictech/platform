package io.quartic.common.application

import io.dropwizard.Application
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.quartic.common.ApplicationDetails
import io.quartic.common.auth.AuthStrategy
import io.quartic.common.auth.createAuthFilter
import io.quartic.common.auth.legacy.LegacyAuthStrategy
import io.quartic.common.client.ClientBuilder
import io.quartic.common.logging.logger
import io.quartic.common.pingpong.PingPongResource
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.serdes.configureObjectMapper

abstract class ApplicationBase<T : ConfigurationBase> : Application<T>() {
    private val LOG by logger()
    private val details = ApplicationDetails(javaClass)
    protected val clientBuilder = ClientBuilder(javaClass)
    protected lateinit var secretsCodec: SecretsCodec

    final override fun initialize(bootstrap: Bootstrap<T>) {
        with (bootstrap) {
            configureObjectMapper(objectMapper)
            initializeApplication(this)
        }
    }

    final override fun run(configuration: T, environment: Environment) {
        LOG.info("Running " + details.name + " " + details.version + " (Java " + details.javaVersion + ")")
        warnIfDevMasterKey(configuration)
        secretsCodec = configuration.secretsCodec
        val authStrategy = authStrategy(configuration)

        // TODO - CORS settings
        // TODO - check Origin and Referer headers

        with (environment.jersey()) {
            urlPattern = "/api/*"
            register(JsonProcessingExceptionMapper(true)) // So we get Jackson deserialization errors in the response
            register(PingPongResource())
            register(AuthDynamicFeature(createAuthFilter(authStrategy)))
            register(AuthValueFactoryProvider.Binder(authStrategy.principalClass))
        }

        runApplication(configuration, environment)
    }

    private fun warnIfDevMasterKey(configuration: T) {
        if (configuration.masterKeyBase64 == DEV_MASTER_KEY_BASE64) {
            LOG.warn("\n" + """
                #####################################################################
                #                                                                   #
                #           !!! RUNNING WITH DEVELOPMENT MASTER KEY !!!             #
                #                                                                   #
                #####################################################################
            """.trimIndent())
        }
    }

    protected open fun initializeApplication(bootstrap: Bootstrap<T>) = Unit

    protected abstract fun runApplication(configuration: T, environment: Environment)

    protected open fun authStrategy(configuration: T): AuthStrategy<*, *> = LegacyAuthStrategy()

    protected fun EncryptedSecret.decrypt() = secretsCodec.decrypt(this)
}
