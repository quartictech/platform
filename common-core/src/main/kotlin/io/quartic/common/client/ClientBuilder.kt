package io.quartic.common.client

import com.google.common.net.HttpHeaders.USER_AGENT
import feign.Contract
import feign.Feign
import feign.Logger.Level.BASIC
import feign.Retryer
import feign.codec.EncodeException
import feign.codec.Encoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.quartic.common.ApplicationDetails
import io.quartic.common.serdes.OBJECT_MAPPER
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.commons.io.IOUtils.copy
import retrofit2.Retrofit
import retrofit2.adapter.java8.Java8CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit


class ClientBuilder(val owner: Class<*>) {
    constructor(owner: Any) : this(owner.javaClass)

    inline fun <reified T> feign(url: URI): T = feign(url.toString())
    inline fun <reified T> feign(url: String): T = Feign.builder()
        .contract(Contract.Default())
        .encoder(inputStreamEncoder(JacksonEncoder(OBJECT_MAPPER)))
        .decoder(JacksonDecoder(OBJECT_MAPPER))
        .retryer(Retryer.Default(0, 0, 1))
        .requestInterceptor { template -> template.header(USER_AGENT, userAgentFor(owner)) }
        .logger(Slf4jLogger())
        .logLevel(BASIC)
        .target(T::class.java, url)


    inline fun <reified T> retrofit(url: URI, timeoutSeconds: Long = RETROFIT_DEFAULT_TIMEOUT): T =
        retrofit(url.toString(), timeoutSeconds)
    inline fun <reified T> retrofit(url: String, timeoutSeconds: Long = RETROFIT_DEFAULT_TIMEOUT): T {
        val target = T::class.java
        if (!target.isAnnotationPresent(Retrofittable::class.java)) {
            throw IllegalArgumentException("${target.simpleName} is not marked as @${Retrofittable::class.simpleName}")
        }

        val urlWithSlash = if(url.endsWith("/")) url else url + "/"
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder()
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                val original = chain.request()

                val request = original.newBuilder()
                    .header("User-Agent", userAgentFor(owner))
                    .method(original.method(), original.body())
                    .build()

                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(urlWithSlash)
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create(OBJECT_MAPPER))
            .addCallAdapterFactory(Java8CallAdapterFactory.create())
            .build()
            .create(target)
    }

    fun inputStreamEncoder(delegate: Encoder) = Encoder { obj, bodyType, template ->
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

    companion object {
        @Target(AnnotationTarget.CLASS)
        annotation class Feignable
        @Target(AnnotationTarget.CLASS)
        annotation class Retrofittable
        @Target(AnnotationTarget.CLASS)
        annotation class Jaxable

        val RETROFIT_DEFAULT_TIMEOUT: Long = 10


        fun userAgentFor(clazz: Class<*>): String {
            val details = ApplicationDetails(clazz)
            return "Quartic-${details.name}/${details.version}"
        }
    }
}
