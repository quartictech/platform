package io.quartic.home.howl

import io.quartic.common.serdes.decode
import io.quartic.howl.api.model.HowlStorageId
import okhttp3.*
import okio.BufferedSink
import java.io.OutputStream
import java.net.URI
import javax.ws.rs.core.HttpHeaders.USER_AGENT

class HowlStreamingClient(userAgent: String, private val baseUrl: URI) {
    private class UserAgentInterceptor(private val userAgent: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                    .header(USER_AGENT, userAgent)
                    .build()
            return chain.proceed(requestWithUserAgent)
        }
    }

    private val client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(userAgent))
            .build()

    fun uploadAnonymousObject(
            targetNamespace: String,
            identityNamespace: String,
            contentType: String,
            upload: (OutputStream) -> Unit
    ): HowlStorageId {
        val request = Request.Builder()
                    .url(url(targetNamespace, MANAGED, identityNamespace))
                    .post(requestBody(contentType, upload))
                    .build()
        return decode(client.newCall(request).execute().body()!!.string(), HowlStorageId::class.java)
    }

    private fun url(vararg components: String) = with (HttpUrl.parse(baseUrl.toString())!!.newBuilder()) {
        components.forEach { addEncodedPathSegment(it) }
        build()
    }

    private fun requestBody(contentType: String, upload: (OutputStream) -> Unit): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType {
                return MediaType.parse(contentType)!!
            }

            override fun writeTo(sink: BufferedSink) {
                upload(sink.outputStream())
            }
        }
    }

    companion object {
        private val MANAGED = "managed"
    }
}
