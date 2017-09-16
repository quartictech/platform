package io.quartic.howl.api

import io.quartic.common.serdes.decode
import okhttp3.*
import okio.BufferedSink
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import javax.ws.rs.core.HttpHeaders.USER_AGENT

class HowlClient(userAgent: String, private val baseUrl: URI) : HowlService {
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

    override fun uploadFile(
        targetNamespace: String,
        identityNamespace: String,
        key: String,
        contentType: String,
        upload: (OutputStream) -> Unit
    ) = uploadFile(url(targetNamespace, MANAGED, identityNamespace, key), contentType, upload)

    private fun uploadFile(url: HttpUrl, contentType: String, upload: (OutputStream) -> Unit) {
        val request = Request.Builder()
                .url(url)
                .put(requestBody(contentType, upload))
                .build()
        client.newCall(request).execute()
    }

    override fun uploadAnonymousFile(
            targetNamespace: String,
            identityNamespace: String,
            contentType: String,
            upload: (OutputStream) -> Unit
    ) = uploadAnonymousFile(url(targetNamespace, MANAGED, identityNamespace), contentType, upload)

    private fun uploadAnonymousFile(url: HttpUrl, contentType: String, upload: (OutputStream) -> Unit): HowlStorageId {
        val request = Request.Builder()
                .url(url)
                .post(requestBody(contentType, upload))
                .build()
        return decode(client.newCall(request).execute().body()!!.string(), HowlStorageId::class.java)
    }

    override fun downloadUnmanagedFile(targetNamespace: String, key: String)
        = downloadFile(url(targetNamespace, UNMANAGED, key))

    override fun downloadManagedFile(targetNamespace: String, identityNamespace: String, key: String)
        = downloadFile(url(targetNamespace, MANAGED, identityNamespace, key))

    private fun downloadFile(url: HttpUrl): InputStream? {
        val request = Request.Builder()
                .url(url)
                .get()
                .build()

        val response = client.newCall(request).execute()

        return if (response.isSuccessful) {
            response.body()!!.byteStream()
        } else {
            null
        }
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
        private val UNMANAGED = "unmanaged"
        private val MANAGED = "managed"
    }
}
