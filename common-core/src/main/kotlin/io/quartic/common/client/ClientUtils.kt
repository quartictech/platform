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
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.commons.io.IOUtils.copy
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import retrofit2.Retrofit
import retrofit2.adapter.java8.Java8CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory


inline fun <reified T : Any> client(owner: Class<*>, url: URL): T = client(T::class.java, owner, url.toString())
inline fun <reified T : Any> client(owner: Class<*>, url: String): T = client(T::class.java, owner, url)

// TODO: eliminate overloads once everything ported to Kotlin
@JvmOverloads
fun <T> client(target: Class<T>, owner: Class<*>, url: String, contract: Contract = Contract.Default()): T = Feign.builder()
            .contract(contract)
            .encoder(inputStreamEncoder(JacksonEncoder(OBJECT_MAPPER)))
            .decoder(JacksonDecoder(OBJECT_MAPPER))
            .retryer(Retryer.Default(0, 0, 1))
            .requestInterceptor { template -> template.header(USER_AGENT, userAgentFor(owner)) }
            .logger(Slf4jLogger())
            .logLevel(BASIC)
            .target(target, url)


inline fun <reified T: Any> retrofitClient(owner: Class<*>, url: String): T = retrofitClient(T::class.java, owner, url)

fun <T> retrofitClient(target: Class<T>, owner: Class<*>, url: String): T {
    val interceptor = HttpLoggingInterceptor()
    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
    val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .addInterceptor(object: Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response? {
                val original = chain.request();

                val request = original.newBuilder()
                    .header("User-Agent", userAgentFor(owner))
                    .method(original.method(), original.body())
                    .build();

                return chain.proceed(request);
            }
        })
        .build()


    val retrofit = Retrofit.Builder()
        .baseUrl(url)
        .client(client)
        .addConverterFactory(JacksonConverterFactory.create(OBJECT_MAPPER))
        .addCallAdapterFactory(Java8CallAdapterFactory.create())
        .build()

    return retrofit.create(target)
}

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
