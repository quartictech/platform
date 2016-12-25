package io.quartic.common.client

import com.google.common.net.HttpHeaders.USER_AGENT
import feign.Feign
import feign.Logger.Level.BASIC
import feign.Retryer
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.jaxrs.JAXRSContract
import feign.slf4j.Slf4jLogger
import io.quartic.common.ApplicationDetails
import io.quartic.common.serdes.objectMapper


inline fun <reified T : Any> client(owner: Class<*>, url: String): T = client(T::class.java, owner, url)

// TODO: eliminate overload once everything ported to Kotlin
fun <T> client(target: Class<T>, owner: Class<*>, url: String): T = Feign.builder()
        .contract(JAXRSContract())
        .encoder(InputStreamEncoder(JacksonEncoder(objectMapper())))
        .decoder(JacksonDecoder(objectMapper()))
        .retryer(Retryer.Default(0, 0, 1))
        .requestInterceptor { template -> template.header(USER_AGENT, userAgentFor(owner)) }
        .logger(Slf4jLogger())
        .logLevel(BASIC)
        .target(target, url)

fun userAgentFor(clazz: Class<*>): String {
    val details = ApplicationDetails(clazz)
    return details.name + "/" + details.version + " (Java " + details.javaVersion + ")"
}
