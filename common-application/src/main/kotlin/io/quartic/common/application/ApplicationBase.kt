package io.quartic.common.application

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.quartic.common.ApplicationDetails
import io.quartic.common.auth.QuarticAuthFilter
import io.quartic.common.auth.User
import io.quartic.common.logging.logger
import io.quartic.common.pingpong.PingPongResource
import io.quartic.common.serdes.configureObjectMapper
import org.apache.commons.io.IOUtils.toInputStream
import org.apache.commons.io.IOUtils.toString
import java.io.FileInputStream
import java.io.IOException
import java.io.SequenceInputStream
import java.nio.charset.StandardCharsets.UTF_8

abstract class ApplicationBase<T : Configuration> : Application<T>() {
    private val LOG by logger()
    private val details = ApplicationDetails(javaClass)

    final override fun initialize(bootstrap: Bootstrap<T>) {
        with (bootstrap) {
            configureObjectMapper(objectMapper)

            setConfigurationSourceProvider { path ->
                SequenceInputStream(
                        FileInputStream(path),
                        toInputStream("\n" + baseConfig, UTF_8)
                )
            }

            addBundle(TemplateConfigBundle())
            initializeApplication(this)
        }
    }

    final override fun run(configuration: T, environment: Environment) {
        LOG.info("Running " + details.name + " " + details.version + " (Java " + details.javaVersion + ")")

        with (environment.jersey()) {
            urlPattern = "/api/*"
            register(JsonProcessingExceptionMapper(true)) // So we get Jackson deserialization errors in the response
            register(PingPongResource())
            register(AuthDynamicFeature(QuarticAuthFilter.create()))
            register(AuthValueFactoryProvider.Binder(User::class.java))
        }

        runApplication(configuration, environment)
    }

    protected open fun initializeApplication(bootstrap: Bootstrap<T>) = Unit

    protected abstract fun runApplication(configuration: T, environment: Environment)

    // TODO: this string substitution is gross, should come up with something better
    private val baseConfig: String
        get() = try {
            toString(javaClass.getResourceAsStream("/application.yml"), UTF_8)
                    .replace("\\$\\{APPLICATION_NAME\\}".toRegex(), details.name.toLowerCase())
        } catch (e: IOException) {
            throw RuntimeException("Couldn't read base config", e)
        }
}
