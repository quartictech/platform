package io.quartic.common.client

import com.google.common.net.HttpHeaders.USER_AGENT
import feign.Feign
import feign.Logger.Level.BASIC
import feign.Retryer
import feign.codec.EncodeException
import feign.codec.Encoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.jaxrs.JAXRSContract
import feign.slf4j.Slf4jLogger
import io.quartic.common.ApplicationDetails
import io.quartic.common.serdes.OBJECT_MAPPER
import org.apache.commons.io.IOUtils.copy
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets


inline fun <reified T : Any> client(owner: Class<*>, url: String): T = client(T::class.java, owner, url)

// TODO: eliminate overload once everything ported to Kotlin
fun <T> client(target: Class<T>, owner: Class<*>, url: String): T = Feign.builder()
        .contract(JAXRSContract())
        .encoder(inputStreamEncoder(JacksonEncoder(OBJECT_MAPPER)))
        .decoder(JacksonDecoder(OBJECT_MAPPER))
        .retryer(Retryer.Default(0, 0, 1))
        .requestInterceptor { template -> template.header(USER_AGENT, userAgentFor(owner)) }
        .logger(Slf4jLogger())
        .logLevel(BASIC)
        .target(target, url)

private fun inputStreamEncoder(delegate: Encoder) = Encoder { obj, bodyType, template ->
    // Adapted from: http://www.monkeypatch.io/2016/08/10/MKTD-1-feign-vs-retrofit-&-58;-2-going-further.html
    if (InputStream::class.java == bodyType) {
        val inputStream = InputStream::class.java.cast(obj)
        try {
            ByteArrayOutputStream().use { bos ->
                copy(inputStream, bos)
                template.body(bos.toByteArray(), StandardCharsets.UTF_8)
            }
        } catch (e: IOException) {
            throw EncodeException("Cannot upload file", e)
        }
    } else {
        delegate.encode(obj, bodyType, template)
    }
}

fun userAgentFor(clazz: Class<*>): String {
    val details = ApplicationDetails(clazz)
    return details.name + "/" + details.version + " (Java " + details.javaVersion + ")"
}
