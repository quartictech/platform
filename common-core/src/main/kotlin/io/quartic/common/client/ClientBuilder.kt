package io.quartic.common.client

import io.quartic.common.ApplicationDetails
import io.quartic.common.serdes.OBJECT_MAPPER
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.java8.Java8CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.URI
import java.util.concurrent.TimeUnit


class ClientBuilder(val owner: Class<*>) {
    constructor(owner: Any) : this(owner.javaClass)

    inline fun <reified T> retrofit(url: URI, timeoutSeconds: Long = RETROFIT_DEFAULT_TIMEOUT): T =
        retrofit(url.toString(), timeoutSeconds)
    inline fun <reified T> retrofit(url: String, timeoutSeconds: Long = RETROFIT_DEFAULT_TIMEOUT): T {
        val target = T::class.java
        if (!target.isAnnotationPresent(Retrofittable::class.java)) {
            throw IllegalArgumentException("${target.simpleName} is not marked as @${Retrofittable::class.simpleName}")
        }

        val urlWithSlash = if(url.endsWith("/")) url else url + "/"
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BASIC
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

    companion object {
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
